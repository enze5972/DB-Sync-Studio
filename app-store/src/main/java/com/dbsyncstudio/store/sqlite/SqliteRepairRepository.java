package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.validation.RepairDetail;
import com.dbsyncstudio.model.validation.RepairRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteRepairRepository {

    private static final String INSERT_RUN_SQL =
            "INSERT INTO sync_repair_runs (validation_run_id, task_id, run_id, table_name, repair_type, status, repair_count, success_count, failed_count, start_time, end_time, error_message, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RUN_SQL =
            "UPDATE sync_repair_runs SET validation_run_id = ?, task_id = ?, run_id = ?, table_name = ?, repair_type = ?, status = ?, repair_count = ?, success_count = ?, failed_count = ?, start_time = ?, end_time = ?, error_message = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_RECENT_RUNS_SQL =
            "SELECT id, validation_run_id, task_id, run_id, table_name, repair_type, status, repair_count, success_count, failed_count, start_time, end_time, error_message, created_at, updated_at FROM sync_repair_runs ORDER BY id DESC LIMIT ?";
    private static final String FIND_RECENT_RUNS_BY_VALIDATION_RUN_SQL =
            "SELECT id, validation_run_id, task_id, run_id, table_name, repair_type, status, repair_count, success_count, failed_count, start_time, end_time, error_message, created_at, updated_at FROM sync_repair_runs WHERE validation_run_id = ? ORDER BY id DESC LIMIT ?";
    private static final String FIND_RUN_BY_ID_SQL =
            "SELECT id, validation_run_id, task_id, run_id, table_name, repair_type, status, repair_count, success_count, failed_count, start_time, end_time, error_message, created_at, updated_at FROM sync_repair_runs WHERE id = ?";
    private static final String INSERT_DETAIL_SQL =
            "INSERT INTO sync_repair_details (repair_run_id, validation_difference_id, task_id, repair_type, primary_key_json, sql_preview, parameter_json, status, error_message, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_DETAILS_BY_RUN_SQL =
            "SELECT id, repair_run_id, validation_difference_id, task_id, repair_type, primary_key_json, sql_preview, parameter_json, status, error_message, created_at, updated_at FROM sync_repair_details WHERE repair_run_id = ? ORDER BY id ASC";

    private final SqliteConnectionFactory connectionFactory;

    public SqliteRepairRepository(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    public long saveRun(RepairRun run) {
        if (run == null) {
            throw new IllegalArgumentException("Repair run must not be null");
        }
        long now = System.currentTimeMillis();
        if (run.getCreatedAt() == null) {
            run.setCreatedAt(Long.valueOf(now));
        }
        if (run.getUpdatedAt() == null) {
            run.setUpdatedAt(Long.valueOf(now));
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            connection = connectionFactory.openConnection();
            if (run.getId() == null) {
                statement = connection.prepareStatement(INSERT_RUN_SQL, Statement.RETURN_GENERATED_KEYS);
                bindRunParameters(statement, run, false);
                statement.executeUpdate();
                generatedKeys = statement.getGeneratedKeys();
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    run.setId(Long.valueOf(id));
                    return id;
                }
                throw new SQLException("Insert repair run failed, no generated key returned");
            }
            statement = connection.prepareStatement(UPDATE_RUN_SQL);
            bindRunParameters(statement, run, true);
            statement.executeUpdate();
            return run.getId().longValue();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save repair run", ex);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public List<RepairRun> findRecentRuns(int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RECENT_RUNS_SQL);
            statement.setInt(1, safeLimit);
            resultSet = statement.executeQuery();
            List<RepairRun> result = new ArrayList<RepairRun>();
            while (resultSet.next()) {
                result.add(mapRun(resultSet));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load repair runs", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public List<RepairRun> findRecentRunsByValidationRunId(long validationRunId, int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RECENT_RUNS_BY_VALIDATION_RUN_SQL);
            statement.setLong(1, validationRunId);
            statement.setInt(2, safeLimit);
            resultSet = statement.executeQuery();
            List<RepairRun> result = new ArrayList<RepairRun>();
            while (resultSet.next()) {
                result.add(mapRun(resultSet));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load repair runs by validation run id", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public RepairRun findRunById(long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RUN_BY_ID_SQL);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapRun(resultSet);
            }
            return null;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load repair run", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public long saveDetail(RepairDetail detail) {
        if (detail == null) {
            throw new IllegalArgumentException("Repair detail must not be null");
        }
        long now = System.currentTimeMillis();
        if (detail.getCreatedAt() == null) {
            detail.setCreatedAt(Long.valueOf(now));
        }
        if (detail.getUpdatedAt() == null) {
            detail.setUpdatedAt(Long.valueOf(now));
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(INSERT_DETAIL_SQL, Statement.RETURN_GENERATED_KEYS);
            bindDetailParameters(statement, detail);
            statement.executeUpdate();
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                detail.setId(Long.valueOf(id));
                return id;
            }
            throw new SQLException("Insert repair detail failed, no generated key returned");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save repair detail", ex);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public List<RepairDetail> findDetailsByRunId(long repairRunId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_DETAILS_BY_RUN_SQL);
            statement.setLong(1, repairRunId);
            resultSet = statement.executeQuery();
            List<RepairDetail> result = new ArrayList<RepairDetail>();
            while (resultSet.next()) {
                result.add(mapDetail(resultSet));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load repair details", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void bindRunParameters(PreparedStatement statement, RepairRun run, boolean update) throws SQLException {
        bindNullableLong(statement, 1, run.getValidationRunId());
        bindNullableLong(statement, 2, run.getTaskId());
        statement.setString(3, run.getRunId());
        statement.setString(4, run.getTableName());
        statement.setString(5, run.getRepairType());
        statement.setString(6, run.getStatus());
        bindNullableLong(statement, 7, run.getRepairCount());
        bindNullableLong(statement, 8, run.getSuccessCount());
        bindNullableLong(statement, 9, run.getFailedCount());
        bindNullableLong(statement, 10, run.getStartTime());
        bindNullableLong(statement, 11, run.getEndTime());
        statement.setString(12, run.getErrorMessage());
        statement.setLong(13, run.getCreatedAt().longValue());
        statement.setLong(14, run.getUpdatedAt().longValue());
        if (update) {
            statement.setLong(15, run.getId().longValue());
        }
    }

    private void bindDetailParameters(PreparedStatement statement, RepairDetail detail) throws SQLException {
        bindNullableLong(statement, 1, detail.getRepairRunId());
        bindNullableLong(statement, 2, detail.getValidationDifferenceId());
        bindNullableLong(statement, 3, detail.getTaskId());
        statement.setString(4, detail.getRepairType());
        statement.setString(5, detail.getPrimaryKeyJson());
        statement.setString(6, detail.getSqlPreview());
        statement.setString(7, detail.getParameterJson());
        statement.setString(8, detail.getStatus());
        statement.setString(9, detail.getErrorMessage());
        statement.setLong(10, detail.getCreatedAt().longValue());
        statement.setLong(11, detail.getUpdatedAt().longValue());
    }

    private RepairRun mapRun(ResultSet resultSet) throws SQLException {
        RepairRun run = new RepairRun();
        run.setId(Long.valueOf(resultSet.getLong("id")));
        run.setValidationRunId(toLong(resultSet.getObject("validation_run_id")));
        run.setTaskId(toLong(resultSet.getObject("task_id")));
        run.setRunId(resultSet.getString("run_id"));
        run.setTableName(resultSet.getString("table_name"));
        run.setRepairType(resultSet.getString("repair_type"));
        run.setStatus(resultSet.getString("status"));
        run.setRepairCount(toLong(resultSet.getObject("repair_count")));
        run.setSuccessCount(toLong(resultSet.getObject("success_count")));
        run.setFailedCount(toLong(resultSet.getObject("failed_count")));
        run.setStartTime(toLong(resultSet.getObject("start_time")));
        run.setEndTime(toLong(resultSet.getObject("end_time")));
        run.setErrorMessage(resultSet.getString("error_message"));
        run.setCreatedAt(toLong(resultSet.getObject("created_at")));
        run.setUpdatedAt(toLong(resultSet.getObject("updated_at")));
        return run;
    }

    private RepairDetail mapDetail(ResultSet resultSet) throws SQLException {
        RepairDetail detail = new RepairDetail();
        detail.setId(Long.valueOf(resultSet.getLong("id")));
        detail.setRepairRunId(toLong(resultSet.getObject("repair_run_id")));
        detail.setValidationDifferenceId(toLong(resultSet.getObject("validation_difference_id")));
        detail.setTaskId(toLong(resultSet.getObject("task_id")));
        detail.setRepairType(resultSet.getString("repair_type"));
        detail.setPrimaryKeyJson(resultSet.getString("primary_key_json"));
        detail.setSqlPreview(resultSet.getString("sql_preview"));
        detail.setParameterJson(resultSet.getString("parameter_json"));
        detail.setStatus(resultSet.getString("status"));
        detail.setErrorMessage(resultSet.getString("error_message"));
        detail.setCreatedAt(toLong(resultSet.getObject("created_at")));
        detail.setUpdatedAt(toLong(resultSet.getObject("updated_at")));
        return detail;
    }

    private void bindNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value.longValue());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        return Long.valueOf(String.valueOf(value));
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
