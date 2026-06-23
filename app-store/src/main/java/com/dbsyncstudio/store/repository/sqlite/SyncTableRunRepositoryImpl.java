package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.sync.entity.SyncTableRunDO;
import com.dbsyncstudio.store.repository.SyncTableRunRepository;

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
public class SyncTableRunRepositoryImpl implements SyncTableRunRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sync_table_run (sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE sync_table_run SET sync_run_id = ?, task_id = ?, run_id = ?, task_table_id = ?, source_schema_name = ?, source_table_name = ?, target_schema_name = ?, target_table_name = ?, table_order = ?, table_status = ?, total_row_count = ?, synced_row_count = ?, success_row_count = ?, failed_row_count = ?, speed_rows_per_second = ?, started_at = ?, ended_at = ?, duration_millis = ?, progress_message = ?, error_message = ?, checkpoint_value = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at FROM sync_table_run WHERE id = ?";
    private static final String FIND_BY_SYNC_RUN_ID_SQL =
            "SELECT id, sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at FROM sync_table_run WHERE sync_run_id = ? ORDER BY table_order ASC, id ASC";
    private static final String FIND_BY_RUN_ID_SQL =
            "SELECT id, sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at FROM sync_table_run WHERE run_id = ? ORDER BY table_order ASC, id ASC";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at FROM sync_table_run WHERE task_id = ? ORDER BY id DESC";
    private static final String FIND_RECENT_SQL =
            "SELECT id, sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at FROM sync_table_run ORDER BY id DESC LIMIT ?";

    @NonNull
    private final DatabaseConnectionFactory connectionFactory;

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            DatabaseSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(SyncTableRunDO tableRun) throws SQLException {
        long now = System.currentTimeMillis();
        if (tableRun.getCreatedAt() == null) {
            tableRun.setCreatedAt(Long.valueOf(now));
        }
        tableRun.setUpdatedAt(Long.valueOf(now));
        try (Connection connection = connectionFactory.openConnection()) {
            if (tableRun.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, tableRun);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            tableRun.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert sync table run failed, no generated key returned");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, tableRun);
                statement.executeUpdate();
                return tableRun.getId().longValue();
            }
        }
    }

    @Override
    public Optional<SyncTableRunDO> findById(long id) throws SQLException {
        return findOne(FIND_BY_ID_SQL, id);
    }

    @Override
    public List<SyncTableRunDO> findBySyncRunId(long syncRunId) throws SQLException {
        return findList(FIND_BY_SYNC_RUN_ID_SQL, syncRunId);
    }

    @Override
    public List<SyncTableRunDO> findByRunId(String runId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_RUN_ID_SQL)) {
            statement.setString(1, runId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SyncTableRunDO> result = new ArrayList<SyncTableRunDO>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    @Override
    public List<SyncTableRunDO> findByTaskId(long taskId) throws SQLException {
        return findList(FIND_BY_TASK_ID_SQL, taskId);
    }

    @Override
    public List<SyncTableRunDO> findRecent(int limit) throws SQLException {
        int safeLimit = limit <= 0 ? 20 : limit;
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_RECENT_SQL)) {
            statement.setInt(1, safeLimit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SyncTableRunDO> result = new ArrayList<SyncTableRunDO>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    private Optional<SyncTableRunDO> findOne(String sql, long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    private List<SyncTableRunDO> findList(String sql, long value) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SyncTableRunDO> result = new ArrayList<SyncTableRunDO>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    private void bindInsertParameters(PreparedStatement statement, SyncTableRunDO tableRun) throws SQLException {
        bindNullableLong(statement, 1, tableRun.getSyncRunId());
        bindNullableLong(statement, 2, tableRun.getTaskId());
        statement.setString(3, tableRun.getRunId());
        bindNullableLong(statement, 4, tableRun.getTaskTableId());
        statement.setString(5, tableRun.getSourceSchemaName());
        statement.setString(6, tableRun.getSourceTableName());
        statement.setString(7, tableRun.getTargetSchemaName());
        statement.setString(8, tableRun.getTargetTableName());
        bindNullableInteger(statement, 9, tableRun.getTableOrder());
        statement.setString(10, tableRun.getTableStatus());
        bindNullableLong(statement, 11, tableRun.getTotalRowCount());
        bindNullableLong(statement, 12, tableRun.getSyncedRowCount());
        bindNullableLong(statement, 13, tableRun.getSuccessRowCount());
        bindNullableLong(statement, 14, tableRun.getFailedRowCount());
        bindNullableDouble(statement, 15, tableRun.getSpeedRowsPerSecond());
        bindNullableLong(statement, 16, tableRun.getStartedAt());
        bindNullableLong(statement, 17, tableRun.getEndedAt());
        bindNullableLong(statement, 18, tableRun.getDurationMillis());
        statement.setString(19, tableRun.getProgressMessage());
        statement.setString(20, tableRun.getErrorMessage());
        statement.setString(21, tableRun.getCheckpointValue());
        statement.setLong(22, tableRun.getCreatedAt().longValue());
        statement.setLong(23, tableRun.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, SyncTableRunDO tableRun) throws SQLException {
        bindNullableLong(statement, 1, tableRun.getSyncRunId());
        bindNullableLong(statement, 2, tableRun.getTaskId());
        statement.setString(3, tableRun.getRunId());
        bindNullableLong(statement, 4, tableRun.getTaskTableId());
        statement.setString(5, tableRun.getSourceSchemaName());
        statement.setString(6, tableRun.getSourceTableName());
        statement.setString(7, tableRun.getTargetSchemaName());
        statement.setString(8, tableRun.getTargetTableName());
        bindNullableInteger(statement, 9, tableRun.getTableOrder());
        statement.setString(10, tableRun.getTableStatus());
        bindNullableLong(statement, 11, tableRun.getTotalRowCount());
        bindNullableLong(statement, 12, tableRun.getSyncedRowCount());
        bindNullableLong(statement, 13, tableRun.getSuccessRowCount());
        bindNullableLong(statement, 14, tableRun.getFailedRowCount());
        bindNullableDouble(statement, 15, tableRun.getSpeedRowsPerSecond());
        bindNullableLong(statement, 16, tableRun.getStartedAt());
        bindNullableLong(statement, 17, tableRun.getEndedAt());
        bindNullableLong(statement, 18, tableRun.getDurationMillis());
        statement.setString(19, tableRun.getProgressMessage());
        statement.setString(20, tableRun.getErrorMessage());
        statement.setString(21, tableRun.getCheckpointValue());
        statement.setLong(22, tableRun.getUpdatedAt().longValue());
        statement.setLong(23, tableRun.getId().longValue());
    }

    private SyncTableRunDO mapRow(ResultSet resultSet) throws SQLException {
        SyncTableRunDO tableRun = new SyncTableRunDO();
        tableRun.setId(Long.valueOf(resultSet.getLong("id")));
        tableRun.setSyncRunId(toLong(resultSet.getObject("sync_run_id")));
        tableRun.setTaskId(toLong(resultSet.getObject("task_id")));
        tableRun.setRunId(resultSet.getString("run_id"));
        tableRun.setTaskTableId(toLong(resultSet.getObject("task_table_id")));
        tableRun.setSourceSchemaName(resultSet.getString("source_schema_name"));
        tableRun.setSourceTableName(resultSet.getString("source_table_name"));
        tableRun.setTargetSchemaName(resultSet.getString("target_schema_name"));
        tableRun.setTargetTableName(resultSet.getString("target_table_name"));
        tableRun.setTableOrder(toInteger(resultSet.getObject("table_order")));
        tableRun.setTableStatus(resultSet.getString("table_status"));
        tableRun.setTotalRowCount(toLong(resultSet.getObject("total_row_count")));
        tableRun.setSyncedRowCount(toLong(resultSet.getObject("synced_row_count")));
        tableRun.setSuccessRowCount(toLong(resultSet.getObject("success_row_count")));
        tableRun.setFailedRowCount(toLong(resultSet.getObject("failed_row_count")));
        tableRun.setSpeedRowsPerSecond(toDouble(resultSet.getObject("speed_rows_per_second")));
        tableRun.setStartedAt(toLong(resultSet.getObject("started_at")));
        tableRun.setEndedAt(toLong(resultSet.getObject("ended_at")));
        tableRun.setDurationMillis(toLong(resultSet.getObject("duration_millis")));
        tableRun.setProgressMessage(resultSet.getString("progress_message"));
        tableRun.setErrorMessage(resultSet.getString("error_message"));
        tableRun.setCheckpointValue(resultSet.getString("checkpoint_value"));
        tableRun.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        tableRun.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return tableRun;
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
