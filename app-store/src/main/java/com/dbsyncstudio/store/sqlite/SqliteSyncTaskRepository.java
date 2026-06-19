package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskRepository;
import com.dbsyncstudio.model.sync.SyncTaskStatus;

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
public class SqliteSyncTaskRepository implements SyncTaskRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sync_task (task_name, source_datasource_id, target_datasource_id, sync_mode, task_status, source_schema_name, source_table_name, target_schema_name, target_table_name, incremental_mode, incremental_column_name, incremental_tie_breaker_column_name, incremental_composite_column_name, incremental_checkpoint_mode, incremental_checkpoint_value, incremental_checkpoint_updated_at, schedule_enabled, schedule_type, schedule_cron_expression, schedule_interval_seconds, schedule_last_run_at, schedule_next_run_at, schedule_last_result, schedule_last_message, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE sync_task SET task_name = ?, source_datasource_id = ?, target_datasource_id = ?, sync_mode = ?, task_status = ?, source_schema_name = ?, source_table_name = ?, target_schema_name = ?, target_table_name = ?, incremental_mode = ?, incremental_column_name = ?, incremental_tie_breaker_column_name = ?, incremental_composite_column_name = ?, incremental_checkpoint_mode = ?, incremental_checkpoint_value = ?, incremental_checkpoint_updated_at = ?, schedule_enabled = ?, schedule_type = ?, schedule_cron_expression = ?, schedule_interval_seconds = ?, schedule_last_run_at = ?, schedule_next_run_at = ?, schedule_last_result = ?, schedule_last_message = ?, total_row_count = ?, synced_row_count = ?, success_row_count = ?, failed_row_count = ?, speed_rows_per_second = ?, started_at = ?, ended_at = ?, duration_millis = ?, progress_message = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, task_name, source_datasource_id, target_datasource_id, sync_mode, task_status, source_schema_name, source_table_name, target_schema_name, target_table_name, incremental_mode, incremental_column_name, incremental_tie_breaker_column_name, incremental_composite_column_name, incremental_checkpoint_mode, incremental_checkpoint_value, incremental_checkpoint_updated_at, schedule_enabled, schedule_type, schedule_cron_expression, schedule_interval_seconds, schedule_last_run_at, schedule_next_run_at, schedule_last_result, schedule_last_message, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, created_at, updated_at FROM sync_task WHERE id = ?";
    private static final String FIND_ALL_SQL =
            "SELECT id, task_name, source_datasource_id, target_datasource_id, sync_mode, task_status, source_schema_name, source_table_name, target_schema_name, target_table_name, incremental_mode, incremental_column_name, incremental_tie_breaker_column_name, incremental_composite_column_name, incremental_checkpoint_mode, incremental_checkpoint_value, incremental_checkpoint_updated_at, schedule_enabled, schedule_type, schedule_cron_expression, schedule_interval_seconds, schedule_last_run_at, schedule_next_run_at, schedule_last_result, schedule_last_message, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, created_at, updated_at FROM sync_task ORDER BY id DESC";
    private static final String DELETE_SQL =
            "DELETE FROM sync_task WHERE id = ?";

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
    public long save(SyncTask task) throws SQLException {
        long now = System.currentTimeMillis();
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(Long.valueOf(now));
        }
        task.setUpdatedAt(Long.valueOf(now));

        try (Connection connection = connectionFactory.openConnection()) {
            if (task.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, task);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            task.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert sync task failed, no generated key returned");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, task);
                statement.executeUpdate();
                return task.getId().longValue();
            }
        }
    }

    @Override
    public Optional<SyncTask> findById(long id) throws SQLException {
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
    public List<SyncTask> findAll() throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<SyncTask> result = new ArrayList<SyncTask>();
            while (resultSet.next()) {
                result.add(mapRow(resultSet));
            }
            return result;
        }
    }

    @Override
    public boolean deleteById(long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void bindInsertParameters(PreparedStatement statement, SyncTask task) throws SQLException {
        statement.setString(1, task.getTaskName());
        statement.setLong(2, task.getSourceDatasourceId().longValue());
        statement.setLong(3, task.getTargetDatasourceId().longValue());
        statement.setString(4, task.getSyncMode().name());
        statement.setString(5, task.getTaskStatus().name());
        statement.setString(6, task.getSourceSchemaName());
        statement.setString(7, task.getSourceTableName());
        statement.setString(8, task.getTargetSchemaName());
        statement.setString(9, task.getTargetTableName());
        statement.setString(10, task.getIncrementalMode() == null ? null : task.getIncrementalMode().name());
        statement.setString(11, task.getIncrementalColumnName());
        statement.setString(12, task.getIncrementalTieBreakerColumnName());
        statement.setString(13, task.getIncrementalCompositeColumnName());
        statement.setString(14, task.getIncrementalCheckpointMode());
        statement.setString(15, task.getIncrementalCheckpointValue());
        bindNullableLong(statement, 16, task.getIncrementalCheckpointUpdatedAt());
        bindNullableBoolean(statement, 17, task.getScheduleEnabled());
        statement.setString(18, task.getScheduleType());
        statement.setString(19, task.getScheduleCronExpression());
        bindNullableInteger(statement, 20, task.getScheduleIntervalSeconds());
        bindNullableLong(statement, 21, task.getScheduleLastRunAt());
        bindNullableLong(statement, 22, task.getScheduleNextRunAt());
        statement.setString(23, task.getScheduleLastResult());
        statement.setString(24, task.getScheduleLastMessage());
        bindNullableLong(statement, 25, task.getTotalRowCount());
        bindNullableLong(statement, 26, task.getSyncedRowCount());
        bindNullableLong(statement, 27, task.getSuccessRowCount());
        bindNullableLong(statement, 28, task.getFailedRowCount());
        bindNullableDouble(statement, 29, task.getSpeedRowsPerSecond());
        bindNullableLong(statement, 30, task.getStartedAt());
        bindNullableLong(statement, 31, task.getEndedAt());
        bindNullableLong(statement, 32, task.getDurationMillis());
        statement.setString(33, task.getProgressMessage());
        statement.setLong(34, task.getCreatedAt().longValue());
        statement.setLong(35, task.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, SyncTask task) throws SQLException {
        statement.setString(1, task.getTaskName());
        statement.setLong(2, task.getSourceDatasourceId().longValue());
        statement.setLong(3, task.getTargetDatasourceId().longValue());
        statement.setString(4, task.getSyncMode().name());
        statement.setString(5, task.getTaskStatus().name());
        statement.setString(6, task.getSourceSchemaName());
        statement.setString(7, task.getSourceTableName());
        statement.setString(8, task.getTargetSchemaName());
        statement.setString(9, task.getTargetTableName());
        statement.setString(10, task.getIncrementalMode() == null ? null : task.getIncrementalMode().name());
        statement.setString(11, task.getIncrementalColumnName());
        statement.setString(12, task.getIncrementalTieBreakerColumnName());
        statement.setString(13, task.getIncrementalCompositeColumnName());
        statement.setString(14, task.getIncrementalCheckpointMode());
        statement.setString(15, task.getIncrementalCheckpointValue());
        bindNullableLong(statement, 16, task.getIncrementalCheckpointUpdatedAt());
        bindNullableBoolean(statement, 17, task.getScheduleEnabled());
        statement.setString(18, task.getScheduleType());
        statement.setString(19, task.getScheduleCronExpression());
        bindNullableInteger(statement, 20, task.getScheduleIntervalSeconds());
        bindNullableLong(statement, 21, task.getScheduleLastRunAt());
        bindNullableLong(statement, 22, task.getScheduleNextRunAt());
        statement.setString(23, task.getScheduleLastResult());
        statement.setString(24, task.getScheduleLastMessage());
        bindNullableLong(statement, 25, task.getTotalRowCount());
        bindNullableLong(statement, 26, task.getSyncedRowCount());
        bindNullableLong(statement, 27, task.getSuccessRowCount());
        bindNullableLong(statement, 28, task.getFailedRowCount());
        bindNullableDouble(statement, 29, task.getSpeedRowsPerSecond());
        bindNullableLong(statement, 30, task.getStartedAt());
        bindNullableLong(statement, 31, task.getEndedAt());
        bindNullableLong(statement, 32, task.getDurationMillis());
        statement.setString(33, task.getProgressMessage());
        statement.setLong(34, task.getUpdatedAt().longValue());
        statement.setLong(35, task.getId().longValue());
    }

    private SyncTask mapRow(ResultSet resultSet) throws SQLException {
        SyncTask task = new SyncTask();
        task.setId(Long.valueOf(resultSet.getLong("id")));
        task.setTaskName(resultSet.getString("task_name"));
        task.setSourceDatasourceId(Long.valueOf(resultSet.getLong("source_datasource_id")));
        task.setTargetDatasourceId(Long.valueOf(resultSet.getLong("target_datasource_id")));
        task.setSyncMode(SyncMode.valueOf(resultSet.getString("sync_mode")));
        task.setTaskStatus(SyncTaskStatus.valueOf(resultSet.getString("task_status")));
        task.setSourceSchemaName(resultSet.getString("source_schema_name"));
        task.setSourceTableName(resultSet.getString("source_table_name"));
        task.setTargetSchemaName(resultSet.getString("target_schema_name"));
        task.setTargetTableName(resultSet.getString("target_table_name"));
        String incrementalMode = resultSet.getString("incremental_mode");
        if (incrementalMode != null && incrementalMode.trim().length() > 0) {
            task.setIncrementalMode(com.dbsyncstudio.model.sync.IncrementalSyncMode.valueOf(incrementalMode));
        }
        task.setIncrementalColumnName(resultSet.getString("incremental_column_name"));
        task.setIncrementalTieBreakerColumnName(resultSet.getString("incremental_tie_breaker_column_name"));
        task.setIncrementalCompositeColumnName(resultSet.getString("incremental_composite_column_name"));
        task.setIncrementalCheckpointMode(resultSet.getString("incremental_checkpoint_mode"));
        task.setIncrementalCheckpointValue(resultSet.getString("incremental_checkpoint_value"));
        task.setIncrementalCheckpointUpdatedAt(toLong(resultSet.getObject("incremental_checkpoint_updated_at")));
        task.setScheduleEnabled(toBoolean(resultSet.getObject("schedule_enabled")));
        task.setScheduleType(resultSet.getString("schedule_type"));
        task.setScheduleCronExpression(resultSet.getString("schedule_cron_expression"));
        task.setScheduleIntervalSeconds(toInteger(resultSet.getObject("schedule_interval_seconds")));
        task.setScheduleLastRunAt(toLong(resultSet.getObject("schedule_last_run_at")));
        task.setScheduleNextRunAt(toLong(resultSet.getObject("schedule_next_run_at")));
        task.setScheduleLastResult(resultSet.getString("schedule_last_result"));
        task.setScheduleLastMessage(resultSet.getString("schedule_last_message"));
        task.setTotalRowCount(toLong(resultSet.getObject("total_row_count")));
        task.setSyncedRowCount(toLong(resultSet.getObject("synced_row_count")));
        task.setSuccessRowCount(toLong(resultSet.getObject("success_row_count")));
        task.setFailedRowCount(toLong(resultSet.getObject("failed_row_count")));
        task.setSpeedRowsPerSecond(toDouble(resultSet.getObject("speed_rows_per_second")));
        task.setStartedAt(toLong(resultSet.getObject("started_at")));
        task.setEndedAt(toLong(resultSet.getObject("ended_at")));
        task.setDurationMillis(toLong(resultSet.getObject("duration_millis")));
        task.setProgressMessage(resultSet.getString("progress_message"));
        task.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        task.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return task;
    }

    private void bindNullableBoolean(PreparedStatement statement, int index, Boolean value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.booleanValue() ? 1 : 0);
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

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Double.valueOf(((Number) value).doubleValue());
        }
        return Double.valueOf(String.valueOf(value));
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        String text = String.valueOf(value);
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
