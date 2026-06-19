package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.sql.SqlExecutionLogEntry;
import com.dbsyncstudio.model.sql.SqlExecutionLogRepository;

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
public class SqliteSqlExecutionLogRepository implements SqlExecutionLogRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sql_execution_log (datasource_id, sql_text, statement_type, success, affected_rows, elapsed_millis, error_message, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_RECENT_SQL =
            "SELECT id, datasource_id, sql_text, statement_type, success, affected_rows, elapsed_millis, error_message, created_at " +
            "FROM sql_execution_log ORDER BY id DESC LIMIT ?";

    @NonNull
    private final SqliteConnectionFactory connectionFactory;

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long append(SqlExecutionLogEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("SQL execution log entry must not be null");
        }
        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(Long.valueOf(System.currentTimeMillis()));
        }

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, entry.getDatasourceId() == null ? 0L : entry.getDatasourceId().longValue());
            statement.setString(2, entry.getSqlText());
            statement.setString(3, entry.getStatementType());
            statement.setInt(4, entry.isSuccess() ? 1 : 0);
            if (entry.getAffectedRows() == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, entry.getAffectedRows().longValue());
            }
            if (entry.getElapsedMillis() == null) {
                statement.setNull(6, java.sql.Types.BIGINT);
            } else {
                statement.setLong(6, entry.getElapsedMillis().longValue());
            }
            statement.setString(7, entry.getErrorMessage());
            statement.setLong(8, entry.getCreatedAt().longValue());
            statement.executeUpdate();
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                entry.setId(Long.valueOf(id));
                return id;
            }
            return 0L;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to append SQL execution log", ex);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public List<SqlExecutionLogEntry> findRecent(int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RECENT_SQL);
            statement.setInt(1, safeLimit);
            resultSet = statement.executeQuery();
            List<SqlExecutionLogEntry> result = new ArrayList<SqlExecutionLogEntry>();
            while (resultSet.next()) {
                SqlExecutionLogEntry entry = new SqlExecutionLogEntry();
                entry.setId(Long.valueOf(resultSet.getLong("id")));
                entry.setDatasourceId(Long.valueOf(resultSet.getLong("datasource_id")));
                entry.setSqlText(resultSet.getString("sql_text"));
                entry.setStatementType(resultSet.getString("statement_type"));
                entry.setSuccess(resultSet.getInt("success") == 1);
                entry.setAffectedRows(Long.valueOf(resultSet.getLong("affected_rows")));
                entry.setElapsedMillis(Long.valueOf(resultSet.getLong("elapsed_millis")));
                entry.setErrorMessage(resultSet.getString("error_message"));
                entry.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
                result.add(entry);
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load SQL execution logs", ex);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public int deleteOlderThan(long createdAt) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement("DELETE FROM sql_execution_log WHERE created_at < ?");
            statement.setLong(1, createdAt);
            return statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete SQL execution logs", ex);
        } finally {
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
            // best effort
        }
    }
}
