package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO;
import com.dbsyncstudio.store.repository.ExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ExecutionLogRepositoryImpl implements ExecutionLogRepository {

    private static final String INSERT_SQL =
            "INSERT INTO execution_log (task_id, log_level, log_message, created_at) VALUES (?, ?, ?, ?)";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, task_id, log_level, log_message, created_at FROM execution_log WHERE task_id = ? ORDER BY id DESC";

    @NonNull
    private final DatabaseConnectionFactory connectionFactory;

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            DatabaseSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long append(ExecutionLogEntryDO entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Execution log entry must not be null");
        }

        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(Long.valueOf(System.currentTimeMillis()));
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS);
            statement.setLong(1, entry.getTaskId() == null ? 0L : entry.getTaskId().longValue());
            statement.setString(2, entry.getLogLevel());
            statement.setString(3, entry.getLogMessage());
            statement.setLong(4, entry.getCreatedAt().longValue());
            statement.executeUpdate();
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                entry.setId(Long.valueOf(id));
                return id;
            }
            return 0L;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to append execution log", ex);
        } finally {
            try {
                if (generatedKeys != null) {
                    generatedKeys.close();
                }
            } catch (SQLException ignored) {
                // close best effort
            }
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException ignored) {
                // close best effort
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
                // close best effort
            }
        }
    }

    @Override
    public List<ExecutionLogEntryDO> findByTaskId(long taskId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_BY_TASK_ID_SQL);
            statement.setLong(1, taskId);
            resultSet = statement.executeQuery();
            List<ExecutionLogEntryDO> result = new ArrayList<ExecutionLogEntryDO>();
            while (resultSet.next()) {
                ExecutionLogEntryDO entry = new ExecutionLogEntryDO();
                entry.setId(Long.valueOf(resultSet.getLong("id")));
                entry.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
                entry.setLogLevel(resultSet.getString("log_level"));
                entry.setLogMessage(resultSet.getString("log_message"));
                entry.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
                result.add(entry);
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load execution logs", ex);
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
    public int deleteOlderThan(long createdAt, List<Long> excludedTaskIds) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            StringBuilder sql = new StringBuilder("DELETE FROM execution_log WHERE created_at < ?");
            if (excludedTaskIds != null && !excludedTaskIds.isEmpty()) {
                sql.append(" AND task_id NOT IN (");
                for (int i = 0; i < excludedTaskIds.size(); i++) {
                    if (i > 0) {
                        sql.append(", ");
                    }
                    sql.append("?");
                }
                sql.append(")");
            }
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, createdAt);
            if (excludedTaskIds != null) {
                for (int i = 0; i < excludedTaskIds.size(); i++) {
                    statement.setLong(i + 2, excludedTaskIds.get(i).longValue());
                }
            }
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete execution logs", ex);
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
