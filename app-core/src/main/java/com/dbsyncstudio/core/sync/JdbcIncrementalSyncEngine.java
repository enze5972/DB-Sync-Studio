package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.metadata.DatabaseMetadataScanner;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.TransformPlan;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;
import com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO;
import com.dbsyncstudio.store.repository.ExecutionLogRepository;
import com.dbsyncstudio.model.sync.entity.IncrementalSyncCheckpointEntryDO;
import com.dbsyncstudio.store.repository.IncrementalSyncCheckpointRepository;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.dto.IncrementalSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.IncrementalSyncResultVO;

import lombok.NonNull;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JdbcIncrementalSyncEngine implements IncrementalSyncEngine {

    @NonNull
    private final DatabaseMetadataScanner metadataScanner;
    @NonNull
    private final DatasourceConnectionOpener connectionOpener;
    @NonNull
    private final ExecutionLogRepository executionLogRepository;
    @NonNull
    private final IncrementalSyncCheckpointRepository checkpointRepository;

    public JdbcIncrementalSyncEngine(ExecutionLogRepository executionLogRepository,
                                     IncrementalSyncCheckpointRepository checkpointRepository) {
        this(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), executionLogRepository, checkpointRepository);
    }

    public JdbcIncrementalSyncEngine(DatabaseMetadataScanner metadataScanner,
                                     DatasourceConnectionOpener connectionOpener,
                                     ExecutionLogRepository executionLogRepository,
                                     IncrementalSyncCheckpointRepository checkpointRepository) {
        this.metadataScanner = metadataScanner;
        this.connectionOpener = connectionOpener;
        this.executionLogRepository = executionLogRepository;
        this.checkpointRepository = checkpointRepository;
    }

    @Override
    public IncrementalSyncResultVO sync(IncrementalSyncRequestDTO request) {
        return sync(request, null);
    }

    public IncrementalSyncResultVO sync(IncrementalSyncRequestDTO request, SyncTaskProgressListener progressListener) {
        return sync(request, progressListener, null);
    }

    public IncrementalSyncResultVO sync(IncrementalSyncRequestDTO request, SyncTaskProgressListener progressListener, TransformPlan transformPlan) {
        long startTime = System.currentTimeMillis();
        try {
            validateRequest(request);
            IncrementalSyncCheckpointEntryDO existingCheckpoint = loadCheckpoint(request.getTaskId());
            String startValue = request.getCheckpointValue() != null ? request.getCheckpointValue()
                    : (existingCheckpoint == null ? null : existingCheckpoint.getCheckpointValue());
            boolean resumed = existingCheckpoint != null && startValue != null;

            try (Connection sourceConnection = connectionOpener.open(request.getSourceDatasource());
                 Connection targetConnection = connectionOpener.open(request.getTargetDatasource())) {
                targetConnection.setAutoCommit(false);
                try {
                    TableMetadataDO sourceTable = findSourceTable(sourceConnection, request.getSourceTableName(), request.getSourceSchemaName());
                    TableMetadataDO targetTable = cloneTargetTable(sourceTable, request.getTargetTableName(), request.getTargetSchemaName());
                    boolean createdTargetTable = ensureTargetTableExists(targetConnection, targetTable);
                    IncrementalSyncPlan incrementalSyncPlan = resolveIncrementalPlan(sourceTable, request);
                    CopyRowsResult copyRowsResult = copyRows(sourceConnection, targetConnection, sourceTable, targetTable, request,
                            incrementalSyncPlan, startValue, progressListener, startTime, transformPlan);
                    long insertedRows = copyRowsResult.getInsertedRows();
                    String nextValue = copyRowsResult.getCheckpointValue();
                    if (nextValue != null) {
                        saveCheckpoint(request, incrementalSyncPlan, nextValue);
                    }
                    if (progressListener != null && request.getCheckpointKey() != null && request.getCheckpointKey().trim().length() > 0) {
                        progressListener.saveCheckpoint(request.getCheckpointKey(), nextValue);
                    }
                    targetConnection.commit();
                    return IncrementalSyncResultVO.builder()
                            .success(true)
                            .message("Incremental sync completed successfully")
                            .sourceRowCount(insertedRows)
                            .insertedRowCount(insertedRows)
                            .durationMillis(System.currentTimeMillis() - startTime)
                            .createdTargetTable(createdTargetTable)
                            .checkpointValue(nextValue)
                            .resumed(resumed)
                            .build();
                } catch (SQLException ex) {
                    targetConnection.rollback();
                    logError(taskKey(request), "Incremental sync failed: " + ex.getMessage());
                    throw ex;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Incremental sync failed: " + ex.getMessage(), ex);
        }
    }

    private void validateRequest(IncrementalSyncRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Incremental sync request must not be null");
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
        if (request.getCheckpointKey() == null || request.getCheckpointKey().trim().length() == 0) {
            throw new IllegalArgumentException("Checkpoint key must not be blank");
        }
        if (request.getPageSize() <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (request.getBatchSize() <= 0) {
            throw new IllegalArgumentException("Batch size must be greater than 0");
        }
    }

    private IncrementalSyncCheckpointEntryDO loadCheckpoint(Long taskId) {
        if (taskId == null) {
            return null;
        }
        Optional<IncrementalSyncCheckpointEntryDO> checkpoint = checkpointRepository.findByTaskId(taskId.longValue());
        return checkpoint.orElse(null);
    }

    private void saveCheckpoint(IncrementalSyncRequestDTO request, IncrementalSyncPlan plan, String checkpointValue) {
        if (request.getTaskId() == null) {
            return;
        }
        IncrementalSyncCheckpointEntryDO entry = new IncrementalSyncCheckpointEntryDO();
        entry.setTaskId(request.getTaskId());
        entry.setCheckpointMode(modeName(request.getIncrementalMode()));
        entry.setCheckpointValue(checkpointValue);
        entry.setCheckpointTieBreakerValue(plan.getTieBreakerColumnName());
        entry.setCheckpointCompositeValue(plan.getCompositeColumnName());
        entry.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        checkpointRepository.save(entry);
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
        targetTable.setIndexes(new ArrayList<com.dbsyncstudio.model.metadata.entity.IndexMetadataDO>(sourceTable.getIndexes()));
        return targetTable;
    }

    private boolean ensureTargetTableExists(Connection targetConnection, TableMetadataDO targetTable) throws SQLException {
        if (tableExists(targetConnection, targetTable.getSchemaName(), targetTable.getTableName())) {
            return false;
        }
        try (Statement statement = targetConnection.createStatement()) {
            statement.execute(buildCreateTableSql(targetTable));
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

    private CopyRowsResult copyRows(Connection sourceConnection, Connection targetConnection, TableMetadataDO sourceTable, TableMetadataDO targetTable,
                                    IncrementalSyncRequestDTO request, IncrementalSyncPlan plan, String startValue,
                                    SyncTaskProgressListener progressListener, long startTime, TransformPlan transformPlan) throws SQLException {
        List<ColumnMetadataDO> columns = sourceTable.getColumns();
        String selectSql = buildIncrementalSelectSql(sourceTable, request, plan, startValue != null);
        String insertSql = buildInsertSql(targetTable);
        long insertedRows = 0L;
        String nextCheckpointValue = null;
        long offset = 0L;

        while (true) {
            if (progressListener != null) {
                if (progressListener.isStopRequested()) {
                    saveCheckpoint(request, plan, nextCheckpointValue);
                    throw new SyncTaskStoppedException("Incremental sync stopped");
                }
                if (progressListener.isPauseRequested()) {
                    saveCheckpoint(request, plan, nextCheckpointValue);
                    throw new SyncTaskPausedException("Incremental sync paused");
                }
            }
            List<Object[]> rows = loadRows(sourceConnection, selectSql, columns, request, plan, startValue, offset);
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
            Object[] lastRow = rows.get(rows.size() - 1);
            nextCheckpointValue = stringValue(lastRow[columnIndex(columns, plan.getWatermarkColumnName())]);
            offset += request.getPageSize();
            if (progressListener != null) {
                long elapsed = System.currentTimeMillis() - startTime;
                double speed = elapsed <= 0L ? 0.0d : (insertedRows * 1000.0d) / elapsed;
                progressListener.updateProgress(insertedRows, insertedRows, insertedRows, 0L, speed,
                        Long.valueOf(startTime), null, Long.valueOf(elapsed), "Copied " + insertedRows + " rows");
            }
        }
        return new CopyRowsResult(insertedRows, nextCheckpointValue);
    }

    private Object[] transformRow(Object[] row, List<ColumnMetadataDO> columns, IncrementalSyncRequestDTO request, TransformPlan transformPlan) {
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

    private static final class CopyRowsResult {

        private final long insertedRows;
        private final String checkpointValue;

        private CopyRowsResult(long insertedRows, String checkpointValue) {
            this.insertedRows = insertedRows;
            this.checkpointValue = checkpointValue;
        }

        private long getInsertedRows() {
            return insertedRows;
        }

        private String getCheckpointValue() {
            return checkpointValue;
        }
    }

    private List<Object[]> loadRows(Connection sourceConnection, String selectSql, List<ColumnMetadataDO> columns, IncrementalSyncRequestDTO request,
                                    IncrementalSyncPlan plan, String startValue, long offset) throws SQLException {
        List<Object[]> rows = new ArrayList<Object[]>();
        try (PreparedStatement statement = sourceConnection.prepareStatement(selectSql)) {
            int index = 1;
            if (startValue != null) {
                if (plan.getTieBreakerColumnName() != null) {
                    String[] checkpointParts = splitCheckpointValue(startValue);
                    statement.setObject(index++, parseCheckpointValue(plan.getWatermarkColumn(), checkpointParts[0]));
                    statement.setObject(index++, parseCheckpointValue(plan.getTieBreakerColumn(), checkpointParts[0]));
                    statement.setObject(index++, parseCheckpointValue(plan.getTieBreakerColumn(), checkpointParts[1]));
                } else {
                    statement.setObject(index++, parseCheckpointValue(plan.getWatermarkColumn(), startValue));
                }
            }
            statement.setInt(index++, request.getPageSize());
            statement.setLong(index, offset);
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

    private String buildIncrementalSelectSql(TableMetadataDO sourceTable, IncrementalSyncRequestDTO request, IncrementalSyncPlan plan,
                                             boolean hasCheckpoint) {
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
        if (hasCheckpoint && request.getIncrementalMode() != IncrementalSyncMode.NONE) {
            sql.append(" WHERE ").append(buildIncrementalWhereClause(plan));
        }
        sql.append(" ORDER BY ").append(buildIncrementalOrderByClause(plan));
        sql.append(" LIMIT ? OFFSET ?");
        return sql.toString();
    }

    private String buildIncrementalWhereClause(IncrementalSyncPlan plan) {
        if (plan.getTieBreakerColumnName() != null) {
            return quoteIdentifier(plan.getWatermarkColumnName()) + " > ? OR (" + quoteIdentifier(plan.getWatermarkColumnName()) + " = ? AND "
                    + quoteIdentifier(plan.getTieBreakerColumnName()) + " > ? )";
        }
        return quoteIdentifier(plan.getWatermarkColumnName()) + " > ?";
    }

    private String buildIncrementalOrderByClause(IncrementalSyncPlan plan) {
        if (plan.getTieBreakerColumnName() != null) {
            return quoteIdentifier(plan.getWatermarkColumnName()) + ", " + quoteIdentifier(plan.getTieBreakerColumnName());
        }
        return quoteIdentifier(plan.getWatermarkColumnName());
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

    private String buildCreateTableSql(TableMetadataDO tableMetadata) {
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
        String upperCaseDataType = dataType.toUpperCase(Locale.ROOT);
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

    private void bindRow(PreparedStatement statement, Object[] row) throws SQLException {
        for (int i = 0; i < row.length; i++) {
            statement.setObject(i + 1, row[i]);
        }
    }

    private Object parseCheckpointValue(ColumnMetadataDO watermarkColumn, String checkpointValue) {
        if (checkpointValue == null || checkpointValue.trim().length() == 0) {
            return null;
        }
        String dataType = watermarkColumn == null || watermarkColumn.getDataType() == null ? "" : watermarkColumn.getDataType().toUpperCase(Locale.ROOT);
        if ("BIGINT".equals(dataType) || "INT".equals(dataType) || "INTEGER".equals(dataType) || "SMALLINT".equals(dataType)) {
            return Long.valueOf(checkpointValue);
        }
        if ("TIMESTAMP".equals(dataType) || "DATETIME".equals(dataType)) {
            return java.sql.Timestamp.valueOf(checkpointValue);
        }
        if ("DATE".equals(dataType)) {
            return java.sql.Date.valueOf(checkpointValue);
        }
        return checkpointValue;
    }

    private IncrementalSyncPlan resolveIncrementalPlan(TableMetadataDO sourceTable, IncrementalSyncRequestDTO request) throws SQLException {
        String columnName = resolveIncrementColumnName(request);
        if (columnName == null || columnName.trim().length() == 0) {
            throw new SQLException("Incremental column name must not be blank");
        }
        ColumnMetadataDO watermarkColumn = null;
        ColumnMetadataDO tieBreakerColumn = null;
        for (ColumnMetadataDO columnMetadata : sourceTable.getColumns()) {
            if (columnName.equalsIgnoreCase(columnMetadata.getName())) {
                watermarkColumn = columnMetadata;
            }
            if (request.getCompositeTieBreakerColumnName() != null && request.getCompositeTieBreakerColumnName().equalsIgnoreCase(columnMetadata.getName())) {
                tieBreakerColumn = columnMetadata;
            }
        }
        if (watermarkColumn == null) {
            throw new SQLException("Incremental column not found: " + columnName);
        }
        return new IncrementalSyncPlan(watermarkColumn, tieBreakerColumn, trimToNull(request.getCompositeWatermarkColumnName()));
    }

    private String resolveIncrementColumnName(IncrementalSyncRequestDTO request) {
        if (request.getIncrementalMode() == IncrementalSyncMode.AUTO_INCREMENT_ID) {
            return firstNonBlank(request.getAutoIncrementColumnName(), request.getWatermarkColumnName(), "id");
        }
        if (request.getIncrementalMode() == IncrementalSyncMode.COMPOSITE) {
            return firstNonBlank(request.getCompositeWatermarkColumnName(), request.getWatermarkColumnName(), "updated_at");
        }
        return firstNonBlank(request.getWatermarkColumnName(), "updated_at", "updated_at");
    }

    private String[] splitCheckpointValue(String checkpointValue) {
        String[] parts = new String[]{checkpointValue, null};
        if (checkpointValue == null) {
            return parts;
        }
        int separatorIndex = checkpointValue.indexOf(',');
        if (separatorIndex < 0) {
            return parts;
        }
        parts[0] = checkpointValue.substring(0, separatorIndex);
        parts[1] = checkpointValue.substring(separatorIndex + 1);
        return parts;
    }

    private int columnIndex(List<ColumnMetadataDO> columns, String columnName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columnName != null && columnName.equalsIgnoreCase(columns.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String buildCompositeCheckpointValue(Object[] lastRow, List<ColumnMetadataDO> columns, IncrementalSyncPlan plan) {
        String watermarkValue = stringValue(lastRow[columnIndex(columns, plan.getWatermarkColumnName())]);
        if (plan.getTieBreakerColumnName() == null) {
            return watermarkValue;
        }
        String tieBreakerValue = stringValue(lastRow[columnIndex(columns, plan.getTieBreakerColumnName())]);
        return watermarkValue + "," + tieBreakerValue;
    }

    private String firstNonBlank(String first, String second, String third) {
        String value = trimToNull(first);
        if (value != null) {
            return value;
        }
        value = trimToNull(second);
        if (value != null) {
            return value;
        }
        return trimToNull(third);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private String modeName(IncrementalSyncMode mode) {
        return mode == null ? IncrementalSyncMode.TIMESTAMP.name() : mode.name();
    }

    private void logError(String taskKey, String message) {
        executionLogRepository.append(ExecutionLogEntryDO.builder()
                .taskId(Long.valueOf(taskKey.hashCode()))
                .logLevel("ERROR")
                .logMessage(message)
                .createdAt(Long.valueOf(System.currentTimeMillis()))
                .build());
    }

    private String taskKey(IncrementalSyncRequestDTO request) {
        return request.getCheckpointKey();
    }

    private static final class IncrementalSyncPlan {

        private final ColumnMetadataDO watermarkColumn;
        private final ColumnMetadataDO tieBreakerColumn;
        private final String compositeColumnName;

        private IncrementalSyncPlan(ColumnMetadataDO watermarkColumn, ColumnMetadataDO tieBreakerColumn, String compositeColumnName) {
            this.watermarkColumn = watermarkColumn;
            this.tieBreakerColumn = tieBreakerColumn;
            this.compositeColumnName = compositeColumnName;
        }

        private ColumnMetadataDO getWatermarkColumn() {
            return watermarkColumn;
        }

        private String getWatermarkColumnName() {
            return watermarkColumn == null ? null : watermarkColumn.getName();
        }

        private String getTieBreakerColumnName() {
            return tieBreakerColumn == null ? null : tieBreakerColumn.getName();
        }

        private String getCompositeColumnName() {
            return compositeColumnName;
        }

        private ColumnMetadataDO getTieBreakerColumn() {
            return tieBreakerColumn;
        }
    }

    private String qualifiedTableName(String schemaName, String tableName) {
        if (schemaName == null || schemaName.trim().length() == 0 || "default".equalsIgnoreCase(schemaName)) {
            return quoteIdentifier(tableName);
        }
        return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    private String quoteIdentifier(String identifier) {
        return identifier;
    }

    private String normalizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().length() == 0) {
            return null;
        }
        return schemaName.trim();
    }

    private void logInfo(String taskKey, String message) {
        executionLogRepository.append(ExecutionLogEntryDO.builder()
                .taskId(Long.valueOf(taskKey.hashCode()))
                .logLevel("INFO")
                .logMessage(message)
                .createdAt(Long.valueOf(System.currentTimeMillis()))
                .build());
    }
}
