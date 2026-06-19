package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.sync.SyncRun;
import com.dbsyncstudio.model.sync.SyncRunRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class SqliteSyncRunRepository implements SyncRunRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sync_run (task_id, run_id, sync_mode, run_status, total_table_count, completed_table_count, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE sync_run SET task_id = ?, run_id = ?, sync_mode = ?, run_status = ?, total_table_count = ?, completed_table_count = ?, total_row_count = ?, synced_row_count = ?, success_row_count = ?, failed_row_count = ?, speed_rows_per_second = ?, started_at = ?, ended_at = ?, duration_millis = ?, progress_message = ?, error_message = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, task_id, run_id, sync_mode, run_status, total_table_count, completed_table_count, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, created_at, updated_at FROM sync_run WHERE id = ?";
    private static final String FIND_BY_RUN_ID_SQL =
            "SELECT id, task_id, run_id, sync_mode, run_status, total_table_count, completed_table_count, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, created_at, updated_at FROM sync_run WHERE run_id = ?";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, task_id, run_id, sync_mode, run_status, total_table_count, completed_table_count, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, created_at, updated_at FROM sync_run WHERE task_id = ? ORDER BY id DESC";
    private static final String FIND_RECENT_SQL =
            "SELECT id, task_id, run_id, sync_mode, run_status, total_table_count, completed_table_count, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, created_at, updated_at FROM sync_run ORDER BY id DESC LIMIT ?";

    @NonNull
    private final SqliteConnectionFactory connectionFactory;

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(SyncRun run) throws SQLException {
        long now = System.currentTimeMillis();
        if (run.getCreatedAt() == null) {
            run.setCreatedAt(Long.valueOf(now));
        }
        run.setUpdatedAt(Long.valueOf(now));
        try (Connection connection = connectionFactory.openConnection()) {
            if (run.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, run);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            run.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert sync run failed, no generated key returned");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, run);
                statement.executeUpdate();
                return run.getId().longValue();
            }
        }
    }

    @Override
    public Optional<SyncRun> findById(long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public Optional<SyncRun> findByRunId(String runId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_RUN_ID_SQL)) {
            statement.setString(1, runId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<SyncRun> findByTaskId(long taskId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SyncRun> result = new ArrayList<SyncRun>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    @Override
    public List<SyncRun> findRecent(int limit) throws SQLException {
        int safeLimit = limit <= 0 ? 20 : limit;
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_RECENT_SQL)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SyncRun> result = new ArrayList<SyncRun>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    private void bindInsertParameters(PreparedStatement statement, SyncRun run) throws SQLException {
        statement.setLong(1, run.getTaskId().longValue());
        statement.setString(2, run.getRunId());
        statement.setString(3, run.getSyncMode());
        statement.setString(4, run.getRunStatus());
        bindNullableInteger(statement, 5, run.getTotalTableCount());
        bindNullableInteger(statement, 6, run.getCompletedTableCount());
        bindNullableLong(statement, 7, run.getTotalRowCount());
        bindNullableLong(statement, 8, run.getSyncedRowCount());
        bindNullableLong(statement, 9, run.getSuccessRowCount());
        bindNullableLong(statement, 10, run.getFailedRowCount());
        bindNullableDouble(statement, 11, run.getSpeedRowsPerSecond());
        bindNullableLong(statement, 12, run.getStartedAt());
        bindNullableLong(statement, 13, run.getEndedAt());
        bindNullableLong(statement, 14, run.getDurationMillis());
        statement.setString(15, run.getProgressMessage());
        statement.setString(16, run.getErrorMessage());
        statement.setLong(17, run.getCreatedAt().longValue());
        statement.setLong(18, run.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, SyncRun run) throws SQLException {
        statement.setLong(1, run.getTaskId().longValue());
        statement.setString(2, run.getRunId());
        statement.setString(3, run.getSyncMode());
        statement.setString(4, run.getRunStatus());
        bindNullableInteger(statement, 5, run.getTotalTableCount());
        bindNullableInteger(statement, 6, run.getCompletedTableCount());
        bindNullableLong(statement, 7, run.getTotalRowCount());
        bindNullableLong(statement, 8, run.getSyncedRowCount());
        bindNullableLong(statement, 9, run.getSuccessRowCount());
        bindNullableLong(statement, 10, run.getFailedRowCount());
        bindNullableDouble(statement, 11, run.getSpeedRowsPerSecond());
        bindNullableLong(statement, 12, run.getStartedAt());
        bindNullableLong(statement, 13, run.getEndedAt());
        bindNullableLong(statement, 14, run.getDurationMillis());
        statement.setString(15, run.getProgressMessage());
        statement.setString(16, run.getErrorMessage());
        statement.setLong(17, run.getUpdatedAt().longValue());
        statement.setLong(18, run.getId().longValue());
    }

    private SyncRun mapRow(ResultSet resultSet) throws SQLException {
        SyncRun run = new SyncRun();
        run.setId(Long.valueOf(resultSet.getLong("id")));
        run.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
        run.setRunId(resultSet.getString("run_id"));
        run.setSyncMode(resultSet.getString("sync_mode"));
        run.setRunStatus(resultSet.getString("run_status"));
        run.setTotalTableCount(toInteger(resultSet.getObject("total_table_count")));
        run.setCompletedTableCount(toInteger(resultSet.getObject("completed_table_count")));
        run.setTotalRowCount(toLong(resultSet.getObject("total_row_count")));
        run.setSyncedRowCount(toLong(resultSet.getObject("synced_row_count")));
        run.setSuccessRowCount(toLong(resultSet.getObject("success_row_count")));
        run.setFailedRowCount(toLong(resultSet.getObject("failed_row_count")));
        run.setSpeedRowsPerSecond(toDouble(resultSet.getObject("speed_rows_per_second")));
        run.setStartedAt(toLong(resultSet.getObject("started_at")));
        run.setEndedAt(toLong(resultSet.getObject("ended_at")));
        run.setDurationMillis(toLong(resultSet.getObject("duration_millis")));
        run.setProgressMessage(resultSet.getString("progress_message"));
        run.setErrorMessage(resultSet.getString("error_message"));
        run.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        run.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return run;
    }

    private void bindNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.intValue());
    }

    private void bindNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value.longValue());
    }

    private void bindNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DOUBLE);
            return;
        }
        statement.setDouble(index, value.doubleValue());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(((Number) value).intValue());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(((Number) value).longValue());
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        return Double.valueOf(((Number) value).doubleValue());
    }
}
