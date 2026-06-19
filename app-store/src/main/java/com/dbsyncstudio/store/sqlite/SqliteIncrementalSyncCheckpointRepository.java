package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointEntry;
import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SqliteIncrementalSyncCheckpointRepository implements IncrementalSyncCheckpointRepository {

    private static final String UPSERT_SQL =
            "INSERT INTO incremental_sync_checkpoint (task_id, checkpoint_mode, checkpoint_value, checkpoint_tie_breaker_value, checkpoint_composite_value, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(task_id) DO UPDATE SET checkpoint_mode = excluded.checkpoint_mode, checkpoint_value = excluded.checkpoint_value, checkpoint_tie_breaker_value = excluded.checkpoint_tie_breaker_value, checkpoint_composite_value = excluded.checkpoint_composite_value, updated_at = excluded.updated_at";
    private static final String FIND_SQL =
            "SELECT id, task_id, checkpoint_mode, checkpoint_value, checkpoint_tie_breaker_value, checkpoint_composite_value, updated_at FROM incremental_sync_checkpoint WHERE task_id = ?";

    private final SqliteConnectionFactory connectionFactory;

    public SqliteIncrementalSyncCheckpointRepository(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(IncrementalSyncCheckpointEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Incremental sync checkpoint entry must not be null");
        }
        if (entry.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        if (entry.getUpdatedAt() == null) {
            entry.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        }
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(UPSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, entry.getTaskId().longValue());
            statement.setString(2, entry.getCheckpointMode());
            statement.setString(3, entry.getCheckpointValue());
            statement.setString(4, entry.getCheckpointTieBreakerValue());
            statement.setString(5, entry.getCheckpointCompositeValue());
            statement.setLong(6, entry.getUpdatedAt().longValue());
            statement.executeUpdate();
            return entry.getTaskId().longValue();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save incremental sync checkpoint", ex);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public Optional<IncrementalSyncCheckpointEntry> findByTaskId(long taskId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_SQL);
            statement.setLong(1, taskId);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                IncrementalSyncCheckpointEntry entry = new IncrementalSyncCheckpointEntry();
                entry.setId(Long.valueOf(resultSet.getLong("id")));
                entry.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
                entry.setCheckpointMode(resultSet.getString("checkpoint_mode"));
                entry.setCheckpointValue(resultSet.getString("checkpoint_value"));
                entry.setCheckpointTieBreakerValue(resultSet.getString("checkpoint_tie_breaker_value"));
                entry.setCheckpointCompositeValue(resultSet.getString("checkpoint_composite_value"));
                entry.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
                return Optional.of(entry);
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load incremental sync checkpoint", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
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
