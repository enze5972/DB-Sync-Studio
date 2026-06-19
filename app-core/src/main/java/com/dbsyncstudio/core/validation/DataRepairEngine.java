package com.dbsyncstudio.core.validation;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.schema.DatabaseDialect;
import com.dbsyncstudio.core.schema.SchemaSqlDialect;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.validation.RepairDetail;
import com.dbsyncstudio.model.validation.RepairRequest;
import com.dbsyncstudio.model.validation.RepairResult;
import com.dbsyncstudio.model.validation.RepairRun;
import com.dbsyncstudio.model.validation.RepairType;
import com.dbsyncstudio.model.validation.ValidationDifference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataRepairEngine {

    private final DatasourceConnectionOpener connectionOpener;
    private final ObjectMapper objectMapper;

    public DataRepairEngine(DatasourceConnectionOpener connectionOpener) {
        this.connectionOpener = connectionOpener;
        this.objectMapper = new ObjectMapper();
    }

    public RepairResult repair(RepairRequest request, List<ValidationDifference> differences) throws SQLException {
        validateRequest(request);
        String runId = normalizeRunId(request.getRunId(), request.getTaskId());
        request.setRunId(runId);
        long startedAt = System.currentTimeMillis();
        RepairRun run = RepairRun.builder()
                .validationRunId(request.getValidationRunId())
                .taskId(request.getTaskId())
                .runId(runId)
                .tableName(resolveTargetTableName(request))
                .repairType(request.getRepairType().name())
                .status(request.isExecute() ? "RUNNING" : "PREVIEWED")
                .repairCount(Long.valueOf(differences == null ? 0 : differences.size()))
                .successCount(Long.valueOf(0L))
                .failedCount(Long.valueOf(0L))
                .startTime(Long.valueOf(startedAt))
                .createdAt(Long.valueOf(startedAt))
                .updatedAt(Long.valueOf(startedAt))
                .build();

        RepairResult result = RepairResult.builder()
                .run(run)
                .build();

        if (differences == null || differences.isEmpty()) {
            run.setEndTime(Long.valueOf(System.currentTimeMillis()));
            run.setUpdatedAt(run.getEndTime());
            run.setStatus("PREVIEWED");
            return result;
        }

        try (Connection targetConnection = connectionOpener.open(request.getTargetDatasource())) {
            SchemaSqlDialect targetDialect = new SchemaSqlDialect(DatabaseDialect.from(request.getTargetDatasource().getType()));
            List<RepairDetail> details = new ArrayList<RepairDetail>();
            long successCount = 0L;
            long failedCount = 0L;
            for (ValidationDifference difference : differences) {
                RepairDetail detail = buildDetail(request, difference, targetDialect);
                if (request.isExecute()) {
                    if (request.getRepairType() == RepairType.DELETE_EXTRA && !request.isConfirmDelete()) {
                        throw new SQLException("Delete repair requires explicit confirmation");
                    }
                    try {
                        executeDetail(targetConnection, detail);
                        detail.setStatus("SUCCESS");
                        successCount++;
                    } catch (SQLException ex) {
                        detail.setStatus("FAILED");
                        detail.setErrorMessage(ex.getMessage());
                        failedCount++;
                    }
                } else {
                    detail.setStatus("PREVIEWED");
                }
                details.add(detail);
            }
            run.setSuccessCount(Long.valueOf(successCount));
            run.setFailedCount(Long.valueOf(failedCount));
            run.setEndTime(Long.valueOf(System.currentTimeMillis()));
            run.setUpdatedAt(run.getEndTime());
            run.setStatus(request.isExecute() ? (failedCount > 0L ? "FAILED" : "SUCCESS") : "PREVIEWED");
            result.setRun(run);
            result.setDetails(details);
            return result;
        } catch (SQLException ex) {
            run.setEndTime(Long.valueOf(System.currentTimeMillis()));
            run.setUpdatedAt(run.getEndTime());
            run.setStatus("FAILED");
            run.setErrorMessage(ex.getMessage());
            result.setRun(run);
            throw ex;
        }
    }

    private RepairDetail buildDetail(RepairRequest request, ValidationDifference difference, SchemaSqlDialect targetDialect) throws SQLException {
        Map<String, Object> sourceRow = readJsonMap(difference.getSourceRowJson());
        Map<String, Object> targetRow = readJsonMap(difference.getTargetRowJson());
        List<String> primaryKeyColumns = resolvePrimaryKeyColumns(request, difference, sourceRow);
        List<String> repairColumns = resolveRepairColumns(request, difference, sourceRow, targetRow, primaryKeyColumns);

        String sql;
        List<Object> parameters = new ArrayList<Object>();
        if (request.getRepairType() == RepairType.INSERT_MISSING) {
            sql = buildInsertSql(request, targetDialect, sourceRow, parameters);
        } else if (request.getRepairType() == RepairType.UPDATE_INCONSISTENT) {
            sql = buildUpdateSql(request, targetDialect, sourceRow, targetRow, primaryKeyColumns, repairColumns, parameters);
        } else if (request.getRepairType() == RepairType.DELETE_EXTRA) {
            sql = buildDeleteSql(request, targetDialect, sourceRow, targetRow, primaryKeyColumns, parameters);
        } else {
            throw new SQLException("Unsupported repair type: " + request.getRepairType());
        }

        return RepairDetail.builder()
                .repairRunId(request.getValidationRunId())
                .validationDifferenceId(difference.getId())
                .taskId(request.getTaskId())
                .repairType(request.getRepairType().name())
                .primaryKeyJson(difference.getPrimaryKeyJson())
                .sqlPreview(sql)
                .parameterJson(toJson(parameters))
                .status(request.isExecute() ? "PENDING" : "PREVIEWED")
                .createdAt(Long.valueOf(System.currentTimeMillis()))
                .updatedAt(Long.valueOf(System.currentTimeMillis()))
                .build();
    }

    private String buildInsertSql(RepairRequest request, SchemaSqlDialect targetDialect, Map<String, Object> sourceRow,
                                  List<Object> parameters) {
        List<String> columns = new ArrayList<String>(sourceRow.keySet());
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(targetDialect.qualifiedTableName(request.getTargetSchemaName(), request.getTargetTableName())).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(targetDialect.quoteIdentifier(columns.get(i)));
            parameters.add(sourceRow.get(columns.get(i)));
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

    private String buildUpdateSql(RepairRequest request, SchemaSqlDialect targetDialect, Map<String, Object> sourceRow,
                                  Map<String, Object> targetRow, List<String> primaryKeyColumns, List<String> repairColumns,
                                  List<Object> parameters) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(targetDialect.qualifiedTableName(request.getTargetSchemaName(), request.getTargetTableName())).append(" SET ");
        for (int i = 0; i < repairColumns.size(); i++) {
            String column = repairColumns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(targetDialect.quoteIdentifier(column)).append(" = ?");
            parameters.add(sourceRow.get(column));
        }
        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            String column = primaryKeyColumns.get(i);
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(targetDialect.quoteIdentifier(column)).append(" = ?");
            parameters.add(lookupValue(sourceRow, column) != null ? lookupValue(sourceRow, column) : lookupValue(targetRow, column));
        }
        return sql.toString();
    }

    private String buildDeleteSql(RepairRequest request, SchemaSqlDialect targetDialect, Map<String, Object> sourceRow,
                                  Map<String, Object> targetRow, List<String> primaryKeyColumns, List<Object> parameters) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(targetDialect.qualifiedTableName(request.getTargetSchemaName(), request.getTargetTableName()));
        sql.append(" WHERE ");
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            String column = primaryKeyColumns.get(i);
            if (i > 0) {
                sql.append(" AND ");
            }
            sql.append(targetDialect.quoteIdentifier(column)).append(" = ?");
            parameters.add(lookupValue(targetRow, column) != null ? lookupValue(targetRow, column) : lookupValue(sourceRow, column));
        }
        return sql.toString();
    }

    private void executeDetail(Connection connection, RepairDetail detail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(detail.getSqlPreview())) {
            List<Object> parameters = readJsonList(detail.getParameterJson());
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            statement.executeUpdate();
        }
    }

    private List<String> resolvePrimaryKeyColumns(RepairRequest request, ValidationDifference difference, Map<String, Object> sourceRow) throws SQLException {
        if (request.getPrimaryKeyColumns() != null && !request.getPrimaryKeyColumns().isEmpty()) {
            return new ArrayList<String>(request.getPrimaryKeyColumns());
        }
        Map<String, Object> primaryKey = readJsonMap(difference.getPrimaryKeyJson());
        if (primaryKey != null && !primaryKey.isEmpty()) {
            return new ArrayList<String>(primaryKey.keySet());
        }
        return new ArrayList<String>(sourceRow.keySet());
    }

    private List<String> resolveRepairColumns(RepairRequest request, ValidationDifference difference,
                                              Map<String, Object> sourceRow, Map<String, Object> targetRow,
                                              List<String> primaryKeyColumns) throws SQLException {
        if (request.getRepairType() == RepairType.INSERT_MISSING) {
            return new ArrayList<String>(sourceRow.keySet());
        }
        if (request.getRepairType() == RepairType.DELETE_EXTRA) {
            return new ArrayList<String>(primaryKeyColumns);
        }
        List<String> differingColumns = readJsonListOfStrings(difference.getDifferingColumnsJson());
        if (!differingColumns.isEmpty()) {
            return differingColumns;
        }
        List<String> result = new ArrayList<String>();
        for (String column : sourceRow.keySet()) {
            if (primaryKeyColumns.contains(column)) {
                continue;
            }
            Object left = lookupValue(sourceRow, column);
            Object right = lookupValue(targetRow, column);
            if (left == null && right == null) {
                continue;
            }
            if (left == null || right == null || !String.valueOf(left).equals(String.valueOf(right))) {
                result.add(column);
            }
        }
        return result;
    }

    private Map<String, Object> readJsonMap(String json) throws SQLException {
        if (json == null || json.trim().length() == 0) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new SQLException("Failed to parse JSON payload", ex);
        }
    }

    private List<Object> readJsonList(String json) throws SQLException {
        if (json == null || json.trim().length() == 0) {
            return new ArrayList<Object>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception ex) {
            throw new SQLException("Failed to parse JSON payload", ex);
        }
    }

    private List<String> readJsonListOfStrings(String json) throws SQLException {
        if (json == null || json.trim().length() == 0) {
            return new ArrayList<String>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            throw new SQLException("Failed to parse JSON payload", ex);
        }
    }

    private String toJson(Object value) throws SQLException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new SQLException("Failed to serialize repair payload", ex);
        }
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

    private void validateRequest(RepairRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Repair request must not be null");
        }
        if (request.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        if (request.getTargetDatasource() == null) {
            throw new IllegalArgumentException("Target datasource must not be null");
        }
        if (request.getRepairType() == null) {
            throw new IllegalArgumentException("Repair type must not be null");
        }
        if (request.getTargetTableName() == null || request.getTargetTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Target table name must not be blank");
        }
    }

    private String resolveTargetTableName(RepairRequest request) {
        if (request.getTargetTableName() != null && request.getTargetTableName().trim().length() > 0) {
            return request.getTargetTableName();
        }
        return request.getSourceTableName();
    }

    private String normalizeRunId(String runId, Long taskId) {
        if (runId != null && runId.trim().length() > 0) {
            return runId.trim();
        }
        return "repair-" + (taskId == null ? "task" : String.valueOf(taskId.longValue())) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
