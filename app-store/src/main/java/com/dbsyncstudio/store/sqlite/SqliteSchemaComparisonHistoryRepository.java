package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.schema.SchemaComparisonHistoryEntry;
import com.dbsyncstudio.model.schema.SchemaComparisonHistoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqliteSchemaComparisonHistoryRepository implements SchemaComparisonHistoryRepository {

    private static final String INSERT_SQL =
            "INSERT INTO schema_comparison_history (source_datasource_id, target_datasource_id, source_schema_name, source_table_name, target_schema_name, target_table_name, diff_summary, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_RECENT_SQL =
            "SELECT id, source_datasource_id, target_datasource_id, source_schema_name, source_table_name, target_schema_name, target_table_name, diff_summary, created_at "
                    + "FROM schema_comparison_history ORDER BY id DESC LIMIT ?";

    private final SqliteConnectionFactory connectionFactory;

    public SqliteSchemaComparisonHistoryRepository(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(SchemaComparisonHistoryEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Schema comparison history entry must not be null");
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
            statement.setLong(1, entry.getSourceDatasourceId() == null ? 0L : entry.getSourceDatasourceId().longValue());
            statement.setLong(2, entry.getTargetDatasourceId() == null ? 0L : entry.getTargetDatasourceId().longValue());
            statement.setString(3, entry.getSourceSchemaName());
            statement.setString(4, entry.getSourceTableName());
            statement.setString(5, entry.getTargetSchemaName());
            statement.setString(6, entry.getTargetTableName());
            statement.setString(7, entry.getDiffSummary());
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
            throw new IllegalStateException("Failed to save schema comparison history", ex);
        } finally {
            closeQuietly(generatedKeys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public List<SchemaComparisonHistoryEntry> findRecent(int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = connectionFactory.openConnection();
            statement = connection.prepareStatement(FIND_RECENT_SQL);
            statement.setInt(1, safeLimit);
            resultSet = statement.executeQuery();
            List<SchemaComparisonHistoryEntry> result = new ArrayList<SchemaComparisonHistoryEntry>();
            while (resultSet.next()) {
                SchemaComparisonHistoryEntry entry = new SchemaComparisonHistoryEntry();
                entry.setId(Long.valueOf(resultSet.getLong("id")));
                entry.setSourceDatasourceId(Long.valueOf(resultSet.getLong("source_datasource_id")));
                entry.setTargetDatasourceId(Long.valueOf(resultSet.getLong("target_datasource_id")));
                entry.setSourceSchemaName(resultSet.getString("source_schema_name"));
                entry.setSourceTableName(resultSet.getString("source_table_name"));
                entry.setTargetSchemaName(resultSet.getString("target_schema_name"));
                entry.setTargetTableName(resultSet.getString("target_table_name"));
                entry.setDiffSummary(resultSet.getString("diff_summary"));
                entry.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
                result.add(entry);
            }
            return result;
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load schema comparison history", ex);
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
