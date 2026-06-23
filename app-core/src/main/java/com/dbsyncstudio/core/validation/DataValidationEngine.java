package com.dbsyncstudio.core.validation;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.schema.DatabaseDialect;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.schema.SchemaSqlDialect;
import com.dbsyncstudio.core.schema.SchemaTableUtils;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.TransformPlan;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;
import com.dbsyncstudio.model.validation.RepairType;
import com.dbsyncstudio.model.validation.entity.ValidationDifferenceDO;
import com.dbsyncstudio.model.validation.ValidationMode;
import com.dbsyncstudio.model.validation.dto.ValidationRequestDTO;
import com.dbsyncstudio.model.validation.vo.ValidationResultVO;
import com.dbsyncstudio.model.validation.entity.ValidationRunDO;
import com.dbsyncstudio.model.validation.ValidationSampleMode;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.CRC32;

public class DataValidationEngine {

    private static final int DEFAULT_PAGE_SIZE = 200;
    private static final int DEFAULT_SAMPLE_COUNT = 20;
    private static final String DEFAULT_HASH_ALGORITHM = "MD5";

    private final com.dbsyncstudio.core.metadata.DatabaseMetadataScanner metadataScanner;
    private final DatasourceConnectionOpener connectionOpener;
    private final ObjectMapper objectMapper;

    public DataValidationEngine(com.dbsyncstudio.core.metadata.DatabaseMetadataScanner metadataScanner,
                                DatasourceConnectionOpener connectionOpener) {
        this.metadataScanner = metadataScanner;
        this.connectionOpener = connectionOpener;
        this.objectMapper = new ObjectMapper();
    }

    public ValidationResultVO validate(ValidationRequestDTO request) throws SQLException {
        return validate(request, null);
    }

    public ValidationResultVO validate(ValidationRequestDTO request, TransformPlan transformPlan) throws SQLException {
        validateRequest(request);
        String runId = normalizeRunId(request.getRunId(), request.getTaskId());
        request.setRunId(runId);
        long startedAt = System.currentTimeMillis();
        ValidationRunDO run = ValidationRunDO.builder()
                .taskId(request.getTaskId())
                .runId(runId)
                .validationMethod(request.getValidationMode().name())
                .sourceTableName(request.getSourceTableName())
                .targetTableName(request.getTargetTableName())
                .whereClause(trimToNull(request.getWhereClause()))
                .incrementalCondition(trimToNull(request.getIncrementalCondition()))
                .status("RUNNING")
                .startedAt(Long.valueOf(startedAt))
                .createdAt(Long.valueOf(startedAt))
                .build();

        ValidationResultVO result = ValidationResultVO.builder()
                .run(run)
                .build();

        try (Connection sourceConnection = connectionOpener.open(request.getSourceDatasource());
             Connection targetConnection = connectionOpener.open(request.getTargetDatasource())) {
            TableMetadataDO sourceTable = findTable(metadataScanner.scan(sourceConnection), request.getSourceSchemaName(), request.getSourceTableName());
            TableMetadataDO targetTable = findTable(metadataScanner.scan(targetConnection), request.getTargetSchemaName(), request.getTargetTableName());
            if (sourceTable == null) {
                throw new SQLException("Source table not found: " + request.getSourceTableName());
            }
            if (targetTable == null) {
                throw new SQLException("Target table not found: " + request.getTargetTableName());
            }

            List<String> primaryKeyColumns = resolvePrimaryKeyColumns(sourceTable, targetTable);
            if (primaryKeyColumns.isEmpty()) {
                throw new SQLException("Primary key columns not found for table: " + request.getSourceTableName());
            }

            SchemaSqlDialect sourceDialect = new SchemaSqlDialect(DatabaseDialect.from(request.getSourceDatasource().getType()));
            SchemaSqlDialect targetDialect = new SchemaSqlDialect(DatabaseDialect.from(request.getTargetDatasource().getType()));

            long sourceRowCount = countRows(sourceConnection, sourceTable, sourceDialect, request);
            long targetRowCount = countRows(targetConnection, targetTable, targetDialect, request);
            run.setSourceRowCount(Long.valueOf(sourceRowCount));
            run.setTargetRowCount(Long.valueOf(targetRowCount));

            List<ValidationDifferenceDO> differences = new ArrayList<ValidationDifferenceDO>();
            if (request.getValidationMode() == ValidationMode.ROW_COUNT) {
                if (sourceRowCount != targetRowCount) {
                    differences.add(buildCountDifference(run, sourceRowCount, targetRowCount));
                }
                run.setMissingCount(Long.valueOf(Math.abs(sourceRowCount - targetRowCount)));
                run.setInconsistentCount(Long.valueOf(0L));
            } else if (request.getValidationMode() == ValidationMode.PRIMARY_KEY_EXISTS) {
                validatePrimaryKeyExistence(request, sourceConnection, targetConnection, sourceTable, targetTable,
                        primaryKeyColumns, sourceDialect, targetDialect, differences, transformPlan);
            } else if (request.getValidationMode() == ValidationMode.SAMPLE || request.getValidationMode() == ValidationMode.HASH) {
                validateRowSamples(request, sourceConnection, targetConnection, sourceTable, targetTable,
                        primaryKeyColumns, sourceDialect, targetDialect, differences, transformPlan);
                run.setMissingCount(Long.valueOf(countDifferences(differences, "MISSING_TARGET")));
                run.setInconsistentCount(Long.valueOf(countDifferences(differences, "INCONSISTENT_ROW")));
                run.setSampleCount(Long.valueOf(differences.size()));
            }
            if (request.getValidationMode() == ValidationMode.PRIMARY_KEY_EXISTS) {
                run.setMissingCount(Long.valueOf(countDifferences(differences, "MISSING_TARGET")));
                run.setInconsistentCount(Long.valueOf(0L));
                run.setSampleCount(Long.valueOf(differences.size()));
            }
            run.setStatus("SUCCESS");
            run.setEndedAt(Long.valueOf(System.currentTimeMillis()));
            run.setElapsedMillis(Long.valueOf(run.getEndedAt().longValue() - startedAt));

            result.setRun(run);
            result.setDifferences(differences);
            return result;
        } catch (SQLException ex) {
            run.setStatus("FAILED");
            run.setErrorMessage(ex.getMessage());
            run.setEndedAt(Long.valueOf(System.currentTimeMillis()));
            run.setElapsedMillis(Long.valueOf(run.getEndedAt().longValue() - startedAt));
            result.setRun(run);
            throw ex;
        }
    }

    private void validatePrimaryKeyExistence(ValidationRequestDTO request, Connection sourceConnection, Connection targetConnection,
                                             TableMetadataDO sourceTable, TableMetadataDO targetTable, List<String> primaryKeyColumns,
                                             SchemaSqlDialect sourceDialect, SchemaSqlDialect targetDialect,
                                             List<ValidationDifferenceDO> differences, TransformPlan transformPlan) throws SQLException {
        int pageSize = DEFAULT_PAGE_SIZE;
        long sourceCount = countRows(sourceConnection, sourceTable, sourceDialect, request);
        long totalPages = (sourceCount + pageSize - 1L) / pageSize;
        for (long pageIndex = 0L; pageIndex < totalPages; pageIndex++) {
            List<Map<String, Object>> sourceRows = loadRows(sourceConnection, sourceTable, sourceDialect, request,
                    primaryKeyColumns, pageSize, pageIndex * pageSize);
            for (Map<String, Object> sourceRow : sourceRows) {
                Map<String, Object> transformedSourceRow = transformRow(request, sourceRow, transformPlan);
                Map<String, Object> primaryKey = extractPrimaryKey(transformedSourceRow, primaryKeyColumns);
                Map<String, Object> targetRow = findRowByPrimaryKey(targetConnection, targetTable, targetDialect, primaryKeyColumns, primaryKey);
                if (targetRow == null) {
                    differences.add(ValidationDifferenceDO.builder()
                            .taskId(request.getTaskId())
                            .runId(request.getRunId())
                            .differenceType("MISSING_TARGET")
                            .primaryKeyJson(toJson(primaryKey))
                            .sourceRowJson(toJson(transformedSourceRow))
                            .suggestedRepairType(RepairType.INSERT_MISSING.name())
                            .status("OPEN")
                            .createdAt(Long.valueOf(System.currentTimeMillis()))
                            .build());
                }
            }
        }
    }

    private void validateRowSamples(ValidationRequestDTO request, Connection sourceConnection, Connection targetConnection,
                                    TableMetadataDO sourceTable, TableMetadataDO targetTable, List<String> primaryKeyColumns,
                                    SchemaSqlDialect sourceDialect, SchemaSqlDialect targetDialect,
                                    List<ValidationDifferenceDO> differences, TransformPlan transformPlan) throws SQLException {
        int sampleCount = request.getSampleCount() == null || request.getSampleCount().intValue() <= 0
                ? DEFAULT_SAMPLE_COUNT
                : request.getSampleCount().intValue();
        List<Map<String, Object>> sampledRows = sampleRows(request, sourceConnection, sourceTable, sourceDialect, primaryKeyColumns, sampleCount);
        List<String> comparisonColumns = resolveComparisonColumns(sourceTable, request);
        for (Map<String, Object> sourceRow : sampledRows) {
            Map<String, Object> transformedSourceRow = transformRow(request, sourceRow, transformPlan);
            Map<String, Object> primaryKey = extractPrimaryKey(transformedSourceRow, primaryKeyColumns);
            Map<String, Object> targetRow = findRowByPrimaryKey(targetConnection, targetTable, targetDialect, primaryKeyColumns, primaryKey);
            if (targetRow == null) {
                differences.add(ValidationDifferenceDO.builder()
                        .taskId(request.getTaskId())
                        .runId(request.getRunId())
                        .differenceType("MISSING_TARGET")
                        .primaryKeyJson(toJson(primaryKey))
                        .sourceRowJson(toJson(transformedSourceRow))
                        .suggestedRepairType(RepairType.INSERT_MISSING.name())
                        .status("OPEN")
                        .createdAt(Long.valueOf(System.currentTimeMillis()))
                        .build());
                continue;
            }

            if (request.getValidationMode() == ValidationMode.HASH) {
                String sourceHash = hashRow(transformedSourceRow, comparisonColumns, request.getHashAlgorithm());
                String targetHash = hashRow(targetRow, comparisonColumns, request.getHashAlgorithm());
                if (!sourceHash.equals(targetHash)) {
                    List<String> differingColumns = compareColumns(transformedSourceRow, targetRow, comparisonColumns);
                    differences.add(buildRowDifference(request, primaryKey, transformedSourceRow, targetRow, differingColumns));
                }
            } else {
                List<String> differingColumns = compareColumns(transformedSourceRow, targetRow, comparisonColumns);
                if (!differingColumns.isEmpty()) {
                    differences.add(buildRowDifference(request, primaryKey, transformedSourceRow, targetRow, differingColumns));
                }
            }
        }
    }

    private ValidationDifferenceDO buildRowDifference(ValidationRequestDTO request, Map<String, Object> primaryKey,
                                                    Map<String, Object> sourceRow, Map<String, Object> targetRow,
                                                    List<String> differingColumns) throws SQLException {
        return ValidationDifferenceDO.builder()
                .taskId(request.getTaskId())
                .runId(request.getRunId())
                .differenceType("INCONSISTENT_ROW")
                .primaryKeyJson(toJson(primaryKey))
                .sourceRowJson(toJson(sourceRow))
                .targetRowJson(toJson(targetRow))
                .differingColumnsJson(toJson(differingColumns))
                .suggestedRepairType(RepairType.UPDATE_INCONSISTENT.name())
                .status("OPEN")
                .createdAt(Long.valueOf(System.currentTimeMillis()))
                .build();
    }

    private ValidationDifferenceDO buildCountDifference(ValidationRunDO run, long sourceRowCount, long targetRowCount) throws SQLException {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("sourceRowCount", Long.valueOf(sourceRowCount));
        summary.put("targetRowCount", Long.valueOf(targetRowCount));
        return ValidationDifferenceDO.builder()
                .taskId(run.getTaskId())
                .runId(run.getRunId())
                .differenceType("ROW_COUNT_MISMATCH")
                .sourceRowJson(toJson(summary))
                .status("OPEN")
                .createdAt(Long.valueOf(System.currentTimeMillis()))
                .build();
    }

    private long countRows(Connection connection, TableMetadataDO tableMetadata, SchemaSqlDialect dialect, ValidationRequestDTO request) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(1) FROM ").append(dialect.qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName()));
        String whereClause = buildWhereClause(request);
        if (whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        try (PreparedStatement statement = connection.prepareStatement(sql.toString());
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        }
    }

    private List<Map<String, Object>> loadRows(Connection connection, TableMetadataDO tableMetadata, SchemaSqlDialect dialect,
                                               ValidationRequestDTO request, List<String> primaryKeyColumns, int limit, long offset)
            throws SQLException {
        List<String> columns = resolveSelectColumns(tableMetadata);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(joinQuotedColumns(dialect, columns))
                .append(" FROM ").append(dialect.qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName()));
        String whereClause = buildWhereClause(request);
        if (whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        sql.append(" ORDER BY ").append(joinQuotedColumns(dialect, primaryKeyColumns));
        sql.append(" LIMIT ? OFFSET ?");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setInt(1, limit);
            statement.setLong(2, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    private List<Map<String, Object>> sampleRows(ValidationRequestDTO request, Connection connection, TableMetadataDO tableMetadata,
                                                 SchemaSqlDialect dialect, List<String> primaryKeyColumns, int sampleCount)
            throws SQLException {
        long totalRows = countRows(connection, tableMetadata, dialect, request);
        if (totalRows <= 0L) {
            return Collections.emptyList();
        }
        int safeSampleCount = Math.min(sampleCount, (int) totalRows);
        List<Long> offsets = new ArrayList<Long>();
        if (request.getSampleMode() == ValidationSampleMode.PRIMARY_KEY_RANGE) {
            long step = Math.max(1L, totalRows / safeSampleCount);
            for (int i = 0; i < safeSampleCount; i++) {
                long offset = Math.min(totalRows - 1L, step * i);
                offsets.add(Long.valueOf(offset));
            }
        } else {
            Random random = new Random(System.currentTimeMillis());
            while (offsets.size() < safeSampleCount) {
                long offset = Math.abs(random.nextLong()) % totalRows;
                Long value = Long.valueOf(offset);
                if (!offsets.contains(value)) {
                    offsets.add(value);
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Long offset : offsets) {
            result.addAll(loadRows(connection, tableMetadata, dialect, request, primaryKeyColumns, 1, offset.longValue()));
        }
        return result;
    }

    private Map<String, Object> findRowByPrimaryKey(Connection connection, TableMetadataDO tableMetadata, SchemaSqlDialect dialect,
                                                    List<String> primaryKeyColumns, Map<String, Object> primaryKey) throws SQLException {
        if (tableMetadata == null) {
            return null;
        }
        List<String> columns = resolveSelectColumns(tableMetadata);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(joinQuotedColumns(dialect, columns))
                .append(" FROM ").append(dialect.qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName()));
        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(dialect.quoteIdentifier(primaryKeyColumns.get(i))).append(" = ?");
        }
        sql.append(" LIMIT 1");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (String primaryKeyColumn : primaryKeyColumns) {
                statement.setObject(index++, lookupValue(primaryKey, primaryKeyColumn));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRow(resultSet);
                }
                return null;
            }
        }
    }

    private List<String> compareColumns(Map<String, Object> sourceRow, Map<String, Object> targetRow, List<String> columns) {
        List<String> differingColumns = new ArrayList<String>();
        for (String column : columns) {
            String left = normalizeValue(lookupValue(sourceRow, column));
            String right = normalizeValue(lookupValue(targetRow, column));
            if (!left.equals(right)) {
                differingColumns.add(column);
            }
        }
        return differingColumns;
    }

    private String hashRow(Map<String, Object> row, List<String> columns, String hashAlgorithm) throws SQLException {
        StringBuilder payload = new StringBuilder();
        for (String column : columns) {
            if (payload.length() > 0) {
                payload.append('|');
            }
            payload.append(normalizeValue(lookupValue(row, column)));
        }
        byte[] bytes = payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String algorithm = trimToNull(hashAlgorithm) == null ? DEFAULT_HASH_ALGORITHM : hashAlgorithm.trim().toUpperCase(Locale.ROOT);
        if ("CRC32".equals(algorithm)) {
            CRC32 crc32 = new CRC32();
            crc32.update(bytes);
            return Long.toHexString(crc32.getValue());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new SQLException("Unsupported hash algorithm: " + algorithm, ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    private String normalizeValue(Object value) {
        if (value == null) {
            return "__NULL__";
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        }
        if (value instanceof java.sql.Date) {
            return formatDate(((java.sql.Date) value).getTime());
        }
        if (value instanceof Timestamp) {
            return formatDate(((Timestamp) value).getTime());
        }
        if (value instanceof Date) {
            return formatDate(((Date) value).getTime());
        }
        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }
        return String.valueOf(value);
    }

    private String formatDate(long timeMillis) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(timeMillis));
    }

    private Map<String, Object> mapRow(ResultSet resultSet) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            row.put(resultSet.getMetaData().getColumnLabel(i), resultSet.getObject(i));
        }
        return row;
    }

    private Map<String, Object> transformRow(ValidationRequestDTO request, Map<String, Object> sourceRow, TransformPlan transformPlan) {
        if (sourceRow == null || transformPlan == null) {
            return sourceRow;
        }
        TransformContext context = TransformContext.builder()
                .taskId(request.getTaskId())
                .runId(request.getRunId())
                .sourceRow(sourceRow)
                .build();
        return transformPlan.transformRow(sourceRow, context);
    }

    private List<String> resolveSelectColumns(TableMetadataDO tableMetadata) {
        List<String> columns = new ArrayList<String>();
        if (tableMetadata != null && tableMetadata.getColumns() != null) {
            for (ColumnMetadataDO column : tableMetadata.getColumns()) {
                if (column != null && column.getName() != null) {
                    columns.add(column.getName());
                }
            }
        }
        return columns;
    }

    private List<String> resolveComparisonColumns(TableMetadataDO tableMetadata, ValidationRequestDTO request) {
        if (request.getHashColumns() != null && !request.getHashColumns().isEmpty()) {
            return new ArrayList<String>(request.getHashColumns());
        }
        return resolveSelectColumns(tableMetadata);
    }

    private List<String> resolvePrimaryKeyColumns(TableMetadataDO sourceTable, TableMetadataDO targetTable) {
        List<String> result = SchemaTableUtils.primaryKeyColumns(sourceTable);
        if (result.isEmpty()) {
            result = SchemaTableUtils.primaryKeyColumns(targetTable);
        }
        if (result.isEmpty() && sourceTable != null && sourceTable.getColumns() != null && !sourceTable.getColumns().isEmpty()) {
            result.add(sourceTable.getColumns().get(0).getName());
        }
        return result;
    }

    private String buildWhereClause(ValidationRequestDTO request) {
        List<String> clauses = new ArrayList<String>();
        String whereClause = trimToNull(request.getWhereClause());
        if (whereClause != null) {
            clauses.add(whereClause);
        }
        String incrementalCondition = trimToNull(request.getIncrementalCondition());
        if (incrementalCondition != null) {
            clauses.add(incrementalCondition);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                builder.append(" AND ");
            }
            builder.append("(").append(clauses.get(i)).append(")");
        }
        return builder.toString();
    }

    private String joinQuotedColumns(SchemaSqlDialect dialect, List<String> columns) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(dialect.quoteIdentifier(columns.get(i)));
        }
        return builder.toString();
    }

    private Map<String, Object> extractPrimaryKey(Map<String, Object> row, List<String> primaryKeyColumns) {
        Map<String, Object> primaryKey = new LinkedHashMap<String, Object>();
        for (String column : primaryKeyColumns) {
            primaryKey.put(column, lookupValue(row, column));
        }
        return primaryKey;
    }

    private Object lookupValue(Map<String, Object> row, String columnName) {
        if (row == null || columnName == null) {
            return null;
        }
        if (row.containsKey(columnName)) {
            return row.get(columnName);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(columnName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String toJson(Object value) throws SQLException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new SQLException("Failed to serialize validation payload", ex);
        }
    }

    private int countDifferences(List<ValidationDifferenceDO> differences, String type) {
        int count = 0;
        for (ValidationDifferenceDO difference : differences) {
            if (difference != null && type.equalsIgnoreCase(difference.getDifferenceType())) {
                count++;
            }
        }
        return count;
    }

    private String normalizeRunId(String runId, Long taskId) {
        String text = trimToNull(runId);
        if (text != null) {
            return text;
        }
        long now = System.currentTimeMillis();
        return "validation-" + (taskId == null ? "task" : String.valueOf(taskId.longValue())) + "-" + now + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void validateRequest(ValidationRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Validation request must not be null");
        }
        if (request.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        if (request.getSourceDatasource() == null || request.getTargetDatasource() == null) {
            throw new IllegalArgumentException("Source and target datasource must not be null");
        }
        if (trimToNull(request.getSourceTableName()) == null || trimToNull(request.getTargetTableName()) == null) {
            throw new IllegalArgumentException("Source and target table names must not be blank");
        }
        if (request.getValidationMode() == null) {
            throw new IllegalArgumentException("Validation mode must not be null");
        }
    }

    private TableMetadataDO findTable(List<SchemaMetadataDO> schemas, String schemaName, String tableName) {
        for (SchemaMetadataDO schemaMetadata : schemas) {
            if (schemaName != null && schemaName.trim().length() > 0
                    && !schemaName.equalsIgnoreCase(schemaMetadata.getSchemaName())) {
                continue;
            }
            if (schemaMetadata.getTables() == null) {
                continue;
            }
            for (TableMetadataDO tableMetadata : schemaMetadata.getTables()) {
                if (tableName.equalsIgnoreCase(tableMetadata.getTableName())) {
                    return tableMetadata;
                }
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
