package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.sync.entity.SyncTaskTableDO;
import com.dbsyncstudio.store.repository.SyncTaskTableRepository;

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
public class SyncTaskTableRepositoryImpl implements SyncTaskTableRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sync_task_table (task_id, source_schema_name, source_table_name, target_schema_name, target_table_name, sync_mode, incremental_mode, incremental_column_name, incremental_tie_breaker_column_name, incremental_composite_column_name, batch_size, table_order, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE sync_task_table SET task_id = ?, source_schema_name = ?, source_table_name = ?, target_schema_name = ?, target_table_name = ?, sync_mode = ?, incremental_mode = ?, incremental_column_name = ?, incremental_tie_breaker_column_name = ?, incremental_composite_column_name = ?, batch_size = ?, table_order = ?, enabled = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, task_id, source_schema_name, source_table_name, target_schema_name, target_table_name, sync_mode, incremental_mode, incremental_column_name, incremental_tie_breaker_column_name, incremental_composite_column_name, batch_size, table_order, enabled, created_at, updated_at FROM sync_task_table WHERE id = ?";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, task_id, source_schema_name, source_table_name, target_schema_name, target_table_name, sync_mode, incremental_mode, incremental_column_name, incremental_tie_breaker_column_name, incremental_composite_column_name, batch_size, table_order, enabled, created_at, updated_at FROM sync_task_table WHERE task_id = ? ORDER BY table_order ASC, id ASC";
    private static final String DELETE_SQL =
            "DELETE FROM sync_task_table WHERE id = ?";

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
    public long save(SyncTaskTableDO taskTable) throws SQLException {
        long now = System.currentTimeMillis();
        if (taskTable.getCreatedAt() == null) {
            taskTable.setCreatedAt(Long.valueOf(now));
        }
        taskTable.setUpdatedAt(Long.valueOf(now));
        try (Connection connection = connectionFactory.openConnection()) {
            if (taskTable.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, taskTable);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            taskTable.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert sync task table failed, no generated key returned");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, taskTable);
                statement.executeUpdate();
                return taskTable.getId().longValue();
            }
        }
    }

    @Override
    public Optional<SyncTaskTableDO> findById(long id) throws SQLException {
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
    public List<SyncTaskTableDO> findByTaskId(long taskId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SyncTaskTableDO> result = new ArrayList<SyncTaskTableDO>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
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

    private void bindInsertParameters(PreparedStatement statement, SyncTaskTableDO taskTable) throws SQLException {
        statement.setLong(1, taskTable.getTaskId().longValue());
        statement.setString(2, taskTable.getSourceSchemaName());
        statement.setString(3, taskTable.getSourceTableName());
        statement.setString(4, taskTable.getTargetSchemaName());
        statement.setString(5, taskTable.getTargetTableName());
        statement.setString(6, taskTable.getSyncMode());
        statement.setString(7, taskTable.getIncrementalMode());
        statement.setString(8, taskTable.getIncrementalColumnName());
        statement.setString(9, taskTable.getIncrementalTieBreakerColumnName());
        statement.setString(10, taskTable.getIncrementalCompositeColumnName());
        bindNullableInteger(statement, 11, taskTable.getBatchSize());
        bindNullableInteger(statement, 12, taskTable.getTableOrder());
        bindNullableBoolean(statement, 13, taskTable.getEnabled());
        statement.setLong(14, taskTable.getCreatedAt().longValue());
        statement.setLong(15, taskTable.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, SyncTaskTableDO taskTable) throws SQLException {
        statement.setLong(1, taskTable.getTaskId().longValue());
        statement.setString(2, taskTable.getSourceSchemaName());
        statement.setString(3, taskTable.getSourceTableName());
        statement.setString(4, taskTable.getTargetSchemaName());
        statement.setString(5, taskTable.getTargetTableName());
        statement.setString(6, taskTable.getSyncMode());
        statement.setString(7, taskTable.getIncrementalMode());
        statement.setString(8, taskTable.getIncrementalColumnName());
        statement.setString(9, taskTable.getIncrementalTieBreakerColumnName());
        statement.setString(10, taskTable.getIncrementalCompositeColumnName());
        bindNullableInteger(statement, 11, taskTable.getBatchSize());
        bindNullableInteger(statement, 12, taskTable.getTableOrder());
        bindNullableBoolean(statement, 13, taskTable.getEnabled());
        statement.setLong(14, taskTable.getUpdatedAt().longValue());
        statement.setLong(15, taskTable.getId().longValue());
    }

    private SyncTaskTableDO mapRow(ResultSet resultSet) throws SQLException {
        SyncTaskTableDO taskTable = new SyncTaskTableDO();
        taskTable.setId(Long.valueOf(resultSet.getLong("id")));
        taskTable.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
        taskTable.setSourceSchemaName(resultSet.getString("source_schema_name"));
        taskTable.setSourceTableName(resultSet.getString("source_table_name"));
        taskTable.setTargetSchemaName(resultSet.getString("target_schema_name"));
        taskTable.setTargetTableName(resultSet.getString("target_table_name"));
        taskTable.setSyncMode(resultSet.getString("sync_mode"));
        taskTable.setIncrementalMode(resultSet.getString("incremental_mode"));
        taskTable.setIncrementalColumnName(resultSet.getString("incremental_column_name"));
        taskTable.setIncrementalTieBreakerColumnName(resultSet.getString("incremental_tie_breaker_column_name"));
        taskTable.setIncrementalCompositeColumnName(resultSet.getString("incremental_composite_column_name"));
        taskTable.setBatchSize(toInteger(resultSet.getObject("batch_size")));
        taskTable.setTableOrder(toInteger(resultSet.getObject("table_order")));
        taskTable.setEnabled(toBoolean(resultSet.getObject("enabled")));
        taskTable.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        taskTable.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return taskTable;
    }

    private void bindNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.intValue());
    }

    private void bindNullableBoolean(PreparedStatement statement, int index, Boolean value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.booleanValue() ? 1 : 0);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(((Number) value).intValue());
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        return Boolean.valueOf(((Number) value).intValue() == 1);
    }
}
