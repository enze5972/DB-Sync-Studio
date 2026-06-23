package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.metadata.DatabaseMetadataScanner;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.TransformPlan;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;
import com.dbsyncstudio.model.sync.dto.FullSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.FullSyncResultVO;
import com.dbsyncstudio.model.sync.entity.SyncCheckpointDO;
import com.dbsyncstudio.store.repository.SyncCheckpointRepository;

import lombok.NonNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcFullSyncEngine implements FullSyncEngine {

    @NonNull
    private final DatabaseMetadataScanner metadataScanner;
    @NonNull
    private final DatasourceConnectionOpener connectionOpener;
    private final SyncCheckpointRepository checkpointRepository;

    public JdbcFullSyncEngine() {
        this(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), null);
    }

    public JdbcFullSyncEngine(DatabaseMetadataScanner metadataScanner, DatasourceConnectionOpener connectionOpener) {
        this(metadataScanner, connectionOpener, null);
    }

    public JdbcFullSyncEngine(DatabaseMetadataScanner metadataScanner, DatasourceConnectionOpener connectionOpener,
                              SyncCheckpointRepository checkpointRepository) {
        this.metadataScanner = metadataScanner;
        this.connectionOpener = connectionOpener;
        this.checkpointRepository = checkpointRepository;
    }

    @Override
    public FullSyncResultVO sync(FullSyncRequestDTO request) {
        return sync(request, null);
    }

    public FullSyncResultVO sync(FullSyncRequestDTO request, SyncTaskProgressListener progressListener) {
        return sync(request, progressListener, null);
    }

    public FullSyncResultVO sync(FullSyncRequestDTO request, SyncTaskProgressListener progressListener, TransformPlan transformPlan) {
        long startTime = System.currentTimeMillis();
        try {
            validateRequest(request);

            try (Connection sourceConnection = connectionOpener.open(request.getSourceDatasource());
                 Connection targetConnection = connectionOpener.open(request.getTargetDatasource())) {
                targetConnection.setAutoCommit(false);
                try {
                    TableMetadataDO sourceTable = findSourceTable(sourceConnection, request.getSourceTableName(), request.getSourceSchemaName());
                    TableMetadataDO targetTable = cloneTargetTable(sourceTable, request.getTargetTableName(), request.getTargetSchemaName());
                    boolean createdTargetTable = ensureTargetTableExists(targetConnection, targetTable);
                    long totalRowCount = countRows(sourceConnection, sourceTable);
                    long startOffset = parseCheckpointValue(request.getCheckpointValue());
                    boolean resumed = startOffset > 0L;

                    if (request.isReplaceTargetData() && !resumed) {
                        clearTargetTable(targetConnection, targetTable);
                    }

                    if (progressListener != null) {
                        progressListener.updateProgress(totalRowCount, 0L, 0L, 0L, 0.0d,
                                Long.valueOf(startTime), null, null, "Starting full sync");
                    }

                    long insertedRows = copyTableData(sourceConnection, targetConnection, sourceTable, targetTable, request,
                            progressListener, totalRowCount, startTime, startOffset, transformPlan);
                    targetConnection.commit();

                    String checkpointKey = request.getCheckpointKey();
                    if (progressListener != null && checkpointKey != null && checkpointKey.trim().length() > 0) {
                        progressListener.saveCheckpoint(checkpointKey, null);
                    }

                    return FullSyncResultVO.builder()
                            .success(true)
                            .message("Full sync completed successfully")
                            .sourceRowCount(insertedRows)
                            .insertedRowCount(insertedRows)
                            .durationMillis(System.currentTimeMillis() - startTime)
                            .createdTargetTable(createdTargetTable)
                            .checkpointValue(null)
                            .resumed(resumed)
                            .build();
                } catch (SQLException ex) {
                    targetConnection.rollback();
                    throw ex;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Full sync failed: " + ex.getMessage(), ex);
        }
    }

    private void validateRequest(FullSyncRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Full sync request must not be null");
        }
        if (request.getSourceDatasource() == null) {
            throw new IllegalArgumentException("Source datasource must not be null");
        }
        if (request.getTargetDatasource() == null) {
            throw new IllegalArgumentException("Target datasource must not be null");
        }
        if (request.getSourceTableName() == null || request.getSourceTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Source table name must not be blank");
        }
        if (request.getTargetTableName() == null || request.getTargetTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Target table name must not be blank");
        }
        if (request.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (request.getBatchSize() <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }
    }

    private TableMetadataDO findSourceTable(Connection sourceConnection, String sourceTableName, String sourceSchemaName)
            throws SQLException {
        List<SchemaMetadataDO> schemas = metadataScanner.scan(sourceConnection);
        for (SchemaMetadataDO schemaMetadata : schemas) {
            if (sourceSchemaName != null && sourceSchemaName.trim().length() > 0
                    && !sourceSchemaName.equalsIgnoreCase(schemaMetadata.getSchemaName())) {
                continue;
            }
            if (schemaMetadata.getTables() == null) {
                continue;
            }
            for (TableMetadataDO tableMetadata : schemaMetadata.getTables()) {
                if (sourceTableName.equalsIgnoreCase(tableMetadata.getTableName())) {
                    return tableMetadata;
                }
            }
        }
        throw new SQLException("Source table not found: " + sourceTableName);
    }

    private TableMetadataDO cloneTargetTable(TableMetadataDO sourceTable, String targetTableName, String targetSchemaName) {
        TableMetadataDO targetTable = new TableMetadataDO();
        targetTable.setSchemaName(targetSchemaName != null && targetSchemaName.trim().length() > 0
                ? targetSchemaName.trim()
                : sourceTable.getSchemaName());
        targetTable.setTableName(targetTableName);
        targetTable.setColumns(new ArrayList<ColumnMetadataDO>(sourceTable.getColumns()));
        return targetTable;
    }

    private boolean ensureTargetTableExists(Connection targetConnection, TableMetadataDO targetTable) throws SQLException {
        if (tableExists(targetConnection, targetTable.getSchemaName(), targetTable.getTableName())) {
            return false;
        }

        String createTableSql = buildCreateTableSql(targetConnection, targetTable);
        try (Statement statement = targetConnection.createStatement()) {
            statement.execute(createTableSql);
        }
        return true;
    }

    private boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), normalizeSchemaName(schemaName), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String currentSchemaName = tables.getString("TABLE_SCHEM");
                String currentTableName = tables.getString("TABLE_NAME");
                boolean schemaMatches = schemaName == null
                        || schemaName.trim().length() == 0
                        || currentSchemaName == null
                        || schemaName.equalsIgnoreCase(currentSchemaName);
                if (schemaMatches && currentTableName != null && tableName.equalsIgnoreCase(currentTableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private void clearTargetTable(Connection targetConnection, TableMetadataDO targetTable) throws SQLException {
        String sql = "DELETE FROM " + qualifiedTableName(targetTable.getSchemaName(), targetTable.getTableName());
        try (Statement statement = targetConnection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private long copyTableData(Connection sourceConnection, Connection targetConnection, TableMetadataDO sourceTable,
                               TableMetadataDO targetTable, FullSyncRequestDTO request, SyncTaskProgressListener progressListener,
                               long totalRowCount, long startTime, long startOffset, TransformPlan transformPlan) throws SQLException {
        List<ColumnMetadataDO> columns = sourceTable.getColumns();
        String selectSql = buildSelectSql(sourceTable, request.getPageSize());
        String insertSql = buildInsertSql(targetTable);
        long offset = startOffset;
        long insertedRows = 0L;
        long successRows = 0L;
        long failedRows = 0L;

        while (true) {
            if (progressListener != null) {
                if (progressListener.isStopRequested()) {
                    saveCheckpoint(progressListener, request.getCheckpointKey(), offset);
                    throw new SyncTaskStoppedException("Full sync stopped");
                }
                if (progressListener.isPauseRequested()) {
                    saveCheckpoint(progressListener, request.getCheckpointKey(), offset);
                    throw new SyncTaskPausedException("Full sync paused");
                }
            }
            List<Object[]> rows = loadRows(sourceConnection, selectSql, columns, request.getPageSize(), offset);
            if (rows.isEmpty()) {
                break;
            }

            try (PreparedStatement statement = targetConnection.prepareStatement(insertSql)) {
                int batchCounter = 0;
                for (Object[] row : rows) {
                    Object[] transformedRow = transformPlan == null ? row : transformRow(row, columns, request, transformPlan);
                    bindRow(statement, transformedRow);
                    statement.addBatch();
                    insertedRows++;
                    batchCounter++;
                    if (batchCounter >= request.getBatchSize()) {
                        statement.executeBatch();
                        batchCounter = 0;
                    }
                }
                if (batchCounter > 0) {
                    statement.executeBatch();
                }
            }
            offset += request.getPageSize();
            successRows = insertedRows;

            if (progressListener != null) {
                long elapsed = System.currentTimeMillis() - startTime;
                double speed = elapsed <= 0L ? 0.0d : (successRows * 1000.0d) / elapsed;
                long remaining = totalRowCount > 0L ? totalRowCount - successRows : 0L;
                progressListener.updateProgress(totalRowCount, successRows, successRows, failedRows, speed,
                        Long.valueOf(startTime), null, Long.valueOf(elapsed), "Copied " + successRows + " rows, remaining " + remaining);
            }
        }

        return insertedRows;
    }

    private Object[] transformRow(Object[] row, List<ColumnMetadataDO> columns, FullSyncRequestDTO request, TransformPlan transformPlan) {
        if (row == null || columns == null || transformPlan == null) {
            return row;
        }
        java.util.Map<String, Object> sourceRow = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < columns.size() && i < row.length; i++) {
            sourceRow.put(columns.get(i).getName(), row[i]);
        }
        TransformContext context = TransformContext.builder()
                .taskId(request.getTaskId())
                .runId(request.getRunId())
                .tableTaskId(request.getTableTaskId())
                .sourceRow(sourceRow)
                .build();
        java.util.Map<String, Object> transformedRow = transformPlan.transformRow(sourceRow, context);
        Object[] result = new Object[row.length];
        for (int i = 0; i < columns.size() && i < result.length; i++) {
            String columnName = columns.get(i).getName();
            result[i] = transformedRow.containsKey(columnName) ? transformedRow.get(columnName) : sourceRow.get(columnName);
        }
        return result;
    }

    private void saveCheckpoint(SyncTaskProgressListener progressListener, String checkpointKey, long offset) {
        if (progressListener == null || checkpointKey == null || checkpointKey.trim().length() == 0) {
            return;
        }
        progressListener.saveCheckpoint(checkpointKey, String.valueOf(offset));
    }

    private long countRows(Connection sourceConnection, TableMetadataDO sourceTable) throws SQLException {
        String sql = "SELECT COUNT(1) FROM " + qualifiedTableName(sourceTable.getSchemaName(), sourceTable.getTableName());
        try (Statement statement = sourceConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            return 0L;
        }
    }

    private long parseCheckpointValue(String checkpointValue) {
        if (checkpointValue == null || checkpointValue.trim().length() == 0) {
            return 0L;
        }
        try {
            return Long.parseLong(checkpointValue.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private List<Object[]> loadRows(Connection sourceConnection, String selectSql, List<ColumnMetadataDO> columns, int pageSize, long offset)
            throws SQLException {
        List<Object[]> rows = new ArrayList<Object[]>();
        try (PreparedStatement statement = sourceConnection.prepareStatement(selectSql)) {
            statement.setInt(1, pageSize);
            statement.setLong(2, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Object[] row = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        row[i] = resultSet.getObject(i + 1);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void bindRow(PreparedStatement statement, Object[] row) throws SQLException {
        for (int i = 0; i < row.length; i++) {
            statement.setObject(i + 1, row[i]);
        }
    }

    private String buildSelectSql(TableMetadataDO sourceTable, int pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        List<ColumnMetadataDO> columns = sourceTable.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(quoteIdentifier(columns.get(i).getName()));
        }
        sql.append(" FROM ").append(qualifiedTableName(sourceTable.getSchemaName(), sourceTable.getTableName()));
        sql.append(" ORDER BY ").append(buildOrderByClause(columns));
        sql.append(" LIMIT ? OFFSET ?");
        return sql.toString();
    }

    private String buildInsertSql(TableMetadataDO targetTable) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(qualifiedTableName(targetTable.getSchemaName(), targetTable.getTableName())).append(" (");
        List<ColumnMetadataDO> columns = targetTable.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(quoteIdentifier(columns.get(i).getName()));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    private String buildCreateTableSql(Connection connection, TableMetadataDO tableMetadata) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName())).append(" (");
        List<ColumnMetadataDO> columns = tableMetadata.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(quoteIdentifier(columns.get(i).getName()))
                    .append(" ")
                    .append(resolveColumnDefinition(columns.get(i)));
            if (!columns.get(i).isNullable()) {
                sql.append(" NOT NULL");
            }
        }

        List<ColumnMetadataDO> primaryKeyColumns = new ArrayList<ColumnMetadataDO>();
        for (ColumnMetadataDO columnMetadata : columns) {
            if (columnMetadata.isPrimaryKey()) {
                primaryKeyColumns.add(columnMetadata);
            }
        }
        if (!primaryKeyColumns.isEmpty()) {
            sql.append(", PRIMARY KEY (");
            for (int i = 0; i < primaryKeyColumns.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(quoteIdentifier(primaryKeyColumns.get(i).getName()));
            }
            sql.append(")");
        }
        sql.append(")");
        return sql.toString();
    }

    private String resolveColumnDefinition(ColumnMetadataDO columnMetadata) {
        String dataType = columnMetadata.getDataType();
        if (dataType == null || dataType.trim().length() == 0) {
            return "VARCHAR(255)";
        }

        String upperCaseDataType = dataType.toUpperCase();
        if (isVariableLengthType(upperCaseDataType)) {
            Integer size = columnMetadata.getColumnSize();
            if (size != null && size.intValue() > 0) {
                return upperCaseDataType + "(" + size + ")";
            }
            return upperCaseDataType + "(255)";
        }

        if (isDecimalType(upperCaseDataType)) {
            Integer size = columnMetadata.getColumnSize();
            Integer decimalDigits = columnMetadata.getDecimalDigits();
            if (size != null && size.intValue() > 0 && decimalDigits != null && decimalDigits.intValue() >= 0) {
                return upperCaseDataType + "(" + size + "," + decimalDigits + ")";
            }
            if (size != null && size.intValue() > 0) {
                return upperCaseDataType + "(" + size + ")";
            }
        }

        return upperCaseDataType;
    }

    private boolean isVariableLengthType(String dataType) {
        return "CHAR".equals(dataType)
                || "VARCHAR".equals(dataType)
                || "CHARACTER".equals(dataType)
                || "CHARACTER VARYING".equals(dataType)
                || "NCHAR".equals(dataType)
                || "NVARCHAR".equals(dataType)
                || "LONGVARCHAR".equals(dataType)
                || "TEXT".equals(dataType)
                || "CLOB".equals(dataType);
    }

    private boolean isDecimalType(String dataType) {
        return "DECIMAL".equals(dataType)
                || "NUMERIC".equals(dataType)
                || "NUMBER".equals(dataType);
    }

    private String buildOrderByClause(List<ColumnMetadataDO> columns) {
        for (ColumnMetadataDO columnMetadata : columns) {
            if (columnMetadata.isPrimaryKey()) {
                return quoteIdentifier(columnMetadata.getName());
            }
        }
        if (columns.isEmpty()) {
            return "1";
        }
        return quoteIdentifier(columns.get(0).getName());
    }

    private String quoteIdentifier(String identifier) {
        return identifier;
    }

    private String qualifiedTableName(String schemaName, String tableName) {
        if (schemaName == null || schemaName.trim().length() == 0 || "default".equalsIgnoreCase(schemaName)) {
            return quoteIdentifier(tableName);
        }
        return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    private String normalizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().length() == 0) {
            return null;
        }
        return schemaName.trim();
    }
}
