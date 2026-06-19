package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.validation.ValidationDifference;
import com.dbsyncstudio.model.validation.ValidationRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteValidationRepository {

    private static final String INSERT_RUN_SQL =
            "INSERT INTO sync_validation_runs (task_id, run_id, validation_method, source_table_name, target_table_name, where_clause, incremental_condition, source_row_count, target_row_count, missing_count, inconsistent_count, sample_count, status, error_message, started_at, ended_at, elapsed_millis, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RUN_SQL =
            "UPDATE sync_validation_runs SET task_id = ?, run_id = ?, validation_method = ?, source_table_name = ?, target_table_name = ?, where_clause = ?, incremental_condition = ?, source_row_count = ?, target_row_count = ?, missing_count = ?, inconsistent_count = ?, sample_count = ?, status = ?, error_message = ?, started_at = ?, ended_at = ?, elapsed_millis = ? WHERE id = ?";
    private static final String FIND_RUN_BY_ID_SQL =
            "SELECT id, task_id, run_id, validation_method, source_table_name, target_table_name, where_clause, incremental_condition, source_row_count, target_row_count, missing_count, inconsistent_count, sample_count, status, error_message, started_at, ended_at, elapsed_millis, created_at FROM sync_validation_runs WHERE id = ?";
    private static final String FIND_RECENT_RUNS_SQL =
            "SELECT id, task_id, run_id, validation_method, source_table_name, target_table_name, where_clause, incremental_condition, source_row_count, target_row_count, missing_count, inconsistent_count, sample_count, status, error_message, started_at, ended_at, elapsed_millis, created_at FROM sync_validation_runs ORDER BY id DESC LIMIT ?";
    private static final String FIND_RECENT_RUNS_BY_TASK_SQL =
            "SELECT id, task_id, run_id, validation_method, source_table_name, target_table_name, where_clause, incremental_condition, source_row_count, target_row_count, missing_count, inconsistent_count, sample_count, status, error_message, started_at, ended_at, elapsed_millis, created_at FROM sync_validation_runs WHERE task_id = ? ORDER BY id DESC LIMIT ?";
    private static final String INSERT_DIFFERENCE_SQL =
            "INSERT INTO sync_validation_differences (validation_run_id, task_id, run_id, difference_type, primary_key_json, source_row_json, target_row_json, differing_columns_json, suggested_repair_type, status, error_message, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_DIFFERENCES_BY_RUN_SQL =
            "SELECT id, validation_run_id, task_id, run_id, difference_type, primary_key_json, source_row_json, target_row_json, differing_columns_json, suggested_repair_type, status, error_message, created_at FROM sync_validation_differences WHERE validation_run_id = ? ORDER BY id ASC";

    private final SqliteConnectionFactory connectionFactory;

    public SqliteValidationRepository(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    public long saveRun(ValidationRun run) {
        if (run == null) {
            throw new IllegalArgumentException("Validation run must not be null");
        }
        long now = System.currentTimeMillis();
        if (run.getCreatedAt() == null) {
            run.setCreatedAt(Long.valueOf(now));
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
                throw new SQLException("Insert validation run failed, no generated key returned");
            }
            statement = connection.prepareStatement(UPDATE_RUN_SQL);
            bindRunParameters(statement, run, true);
            statement.executeUpdate();
            return run.getId().longValue();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save validation run", ex);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public Optional<ValidationRun> findRunById(long id) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RUN_BY_ID_SQL);
            statement.setLong(1, id);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return Optional.of(mapRun(resultSet));
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load validation run", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public List<ValidationRun> findRecentRuns(int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RECENT_RUNS_SQL);
            statement.setInt(1, safeLimit);
            resultSet = statement.executeQuery();
            List<ValidationRun> result = new ArrayList<ValidationRun>();
            while (resultSet.next()) {
                result.add(mapRun(resultSet));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load recent validation runs", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public List<ValidationRun> findRecentRunsByTaskId(long taskId, int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RECENT_RUNS_BY_TASK_SQL);
            statement.setLong(1, taskId);
            statement.setInt(2, safeLimit);
            resultSet = statement.executeQuery();
            List<ValidationRun> result = new ArrayList<ValidationRun>();
            while (resultSet.next()) {
                result.add(mapRun(resultSet));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load validation runs by task id", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public long saveDifference(ValidationDifference difference) {
        if (difference == null) {
            throw new IllegalArgumentException("Validation difference must not be null");
        }
        if (difference.getCreatedAt() == null) {
            difference.setCreatedAt(Long.valueOf(System.currentTimeMillis()));
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(INSERT_DIFFERENCE_SQL, Statement.RETURN_GENERATED_KEYS);
            bindDifferenceParameters(statement, difference);
            statement.executeUpdate();
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                difference.setId(Long.valueOf(id));
                return id;
            }
            throw new SQLException("Insert validation difference failed, no generated key returned");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save validation difference", ex);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    public List<ValidationDifference> findDifferencesByRunId(long validationRunId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_DIFFERENCES_BY_RUN_SQL);
            statement.setLong(1, validationRunId);
            resultSet = statement.executeQuery();
            List<ValidationDifference> result = new ArrayList<ValidationDifference>();
            while (resultSet.next()) {
                result.add(mapDifference(resultSet));
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load validation differences", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    private void bindRunParameters(PreparedStatement statement, ValidationRun run, boolean update) throws SQLException {
        statement.setLong(1, run.getTaskId() == null ? 0L : run.getTaskId().longValue());
        statement.setString(2, run.getRunId());
        statement.setString(3, run.getValidationMethod());
        statement.setString(4, run.getSourceTableName());
        statement.setString(5, run.getTargetTableName());
        statement.setString(6, run.getWhereClause());
        statement.setString(7, run.getIncrementalCondition());
        bindNullableLong(statement, 8, run.getSourceRowCount());
        bindNullableLong(statement, 9, run.getTargetRowCount());
        bindNullableLong(statement, 10, run.getMissingCount());
        bindNullableLong(statement, 11, run.getInconsistentCount());
        bindNullableLong(statement, 12, run.getSampleCount());
        statement.setString(13, run.getStatus());
        statement.setString(14, run.getErrorMessage());
        bindNullableLong(statement, 15, run.getStartedAt());
        bindNullableLong(statement, 16, run.getEndedAt());
        bindNullableLong(statement, 17, run.getElapsedMillis());
        statement.setLong(18, run.getCreatedAt().longValue());
        if (update) {
            statement.setLong(19, run.getId().longValue());
        }
    }

    private void bindDifferenceParameters(PreparedStatement statement, ValidationDifference difference) throws SQLException {
        bindNullableLong(statement, 1, difference.getValidationRunId());
        bindNullableLong(statement, 2, difference.getTaskId());
        statement.setString(3, difference.getRunId());
        statement.setString(4, difference.getDifferenceType());
        statement.setString(5, difference.getPrimaryKeyJson());
        statement.setString(6, difference.getSourceRowJson());
        statement.setString(7, difference.getTargetRowJson());
        statement.setString(8, difference.getDifferingColumnsJson());
        statement.setString(9, difference.getSuggestedRepairType());
        statement.setString(10, difference.getStatus());
        statement.setString(11, difference.getErrorMessage());
        statement.setLong(12, difference.getCreatedAt().longValue());
    }

    private ValidationRun mapRun(ResultSet resultSet) throws SQLException {
        ValidationRun run = new ValidationRun();
        run.setId(Long.valueOf(resultSet.getLong("id")));
        run.setTaskId(toLong(resultSet.getObject("task_id")));
        run.setRunId(resultSet.getString("run_id"));
        run.setValidationMethod(resultSet.getString("validation_method"));
        run.setSourceTableName(resultSet.getString("source_table_name"));
        run.setTargetTableName(resultSet.getString("target_table_name"));
        run.setWhereClause(resultSet.getString("where_clause"));
        run.setIncrementalCondition(resultSet.getString("incremental_condition"));
        run.setSourceRowCount(toLong(resultSet.getObject("source_row_count")));
        run.setTargetRowCount(toLong(resultSet.getObject("target_row_count")));
        run.setMissingCount(toLong(resultSet.getObject("missing_count")));
        run.setInconsistentCount(toLong(resultSet.getObject("inconsistent_count")));
        run.setSampleCount(toLong(resultSet.getObject("sample_count")));
        run.setStatus(resultSet.getString("status"));
        run.setErrorMessage(resultSet.getString("error_message"));
        run.setStartedAt(toLong(resultSet.getObject("started_at")));
        run.setEndedAt(toLong(resultSet.getObject("ended_at")));
        run.setElapsedMillis(toLong(resultSet.getObject("elapsed_millis")));
        run.setCreatedAt(toLong(resultSet.getObject("created_at")));
        return run;
    }

    private ValidationDifference mapDifference(ResultSet resultSet) throws SQLException {
        ValidationDifference difference = new ValidationDifference();
        difference.setId(Long.valueOf(resultSet.getLong("id")));
        difference.setValidationRunId(toLong(resultSet.getObject("validation_run_id")));
        difference.setTaskId(toLong(resultSet.getObject("task_id")));
        difference.setRunId(resultSet.getString("run_id"));
        difference.setDifferenceType(resultSet.getString("difference_type"));
        difference.setPrimaryKeyJson(resultSet.getString("primary_key_json"));
        difference.setSourceRowJson(resultSet.getString("source_row_json"));
        difference.setTargetRowJson(resultSet.getString("target_row_json"));
        difference.setDifferingColumnsJson(resultSet.getString("differing_columns_json"));
        difference.setSuggestedRepairType(resultSet.getString("suggested_repair_type"));
        difference.setStatus(resultSet.getString("status"));
        difference.setErrorMessage(resultSet.getString("error_message"));
        difference.setCreatedAt(toLong(resultSet.getObject("created_at")));
        return difference;
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
