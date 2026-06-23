package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.model.sync.entity.SyncCheckpointDO;
import com.dbsyncstudio.store.repository.SyncCheckpointRepository;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@RequiredArgsConstructor
public class SyncCheckpointRepositoryImpl implements SyncCheckpointRepository {

    private static final String UPSERT_SQL =
            "INSERT INTO sync_checkpoint (checkpoint_key, checkpoint_value, updated_at) VALUES (?, ?, ?) " +
            "ON CONFLICT(checkpoint_key) DO UPDATE SET checkpoint_value = excluded.checkpoint_value, updated_at = excluded.updated_at";
    private static final String FIND_SQL =
            "SELECT id, checkpoint_key, checkpoint_value, updated_at FROM sync_checkpoint WHERE checkpoint_key = ?";
    private static final String DELETE_SQL =
            "DELETE FROM sync_checkpoint WHERE checkpoint_key = ?";

    @NonNull
    private final DatabaseConnectionFactory connectionFactory;

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            DatabaseSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(SyncCheckpointDO checkpoint) {
        if (checkpoint == null) {
            throw new IllegalArgumentException("Sync checkpoint must not be null");
        }
        if (checkpoint.getCheckpointKey() == null || checkpoint.getCheckpointKey().trim().length() == 0) {
            throw new IllegalArgumentException("Checkpoint key must not be blank");
        }
        if (checkpoint.getUpdatedAt() == null) {
            checkpoint.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(UPSERT_SQL);
            statement.setString(1, checkpoint.getCheckpointKey());
            statement.setString(2, checkpoint.getCheckpointValue());
            statement.setLong(3, checkpoint.getUpdatedAt().longValue());
            statement.executeUpdate();
            checkpoint.setId(findByKey(checkpoint.getCheckpointKey()).map(new java.util.function.Function<SyncCheckpointDO, Long>() {
                @Override
                public Long apply(SyncCheckpointDO saved) {
                    return saved.getId();
                }
            }).orElse(Long.valueOf(0L)));
            return checkpoint.getId() == null ? 0L : checkpoint.getId().longValue();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save sync checkpoint", ex);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public Optional<SyncCheckpointDO> findByKey(String checkpointKey) {
        if (checkpointKey == null || checkpointKey.trim().length() == 0) {
            return Optional.empty();
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_SQL);
            statement.setString(1, checkpointKey);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                SyncCheckpointDO checkpoint = new SyncCheckpointDO();
                checkpoint.setId(Long.valueOf(resultSet.getLong("id")));
                checkpoint.setCheckpointKey(resultSet.getString("checkpoint_key"));
                checkpoint.setCheckpointValue(resultSet.getString("checkpoint_value"));
                checkpoint.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
                return Optional.of(checkpoint);
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load sync checkpoint", ex);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public boolean deleteByKey(String checkpointKey) {
        if (checkpointKey == null || checkpointKey.trim().length() == 0) {
            return false;
        }

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(DELETE_SQL);
            statement.setString(1, checkpointKey);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete sync checkpoint", ex);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignored) {
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }
}
