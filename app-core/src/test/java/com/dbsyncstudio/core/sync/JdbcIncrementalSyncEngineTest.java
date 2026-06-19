package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.ExecutionLogEntry;
import com.dbsyncstudio.model.sync.ExecutionLogRepository;
import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointEntry;
import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointRepository;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.IncrementalSyncRequest;
import com.dbsyncstudio.model.sync.IncrementalSyncResult;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JdbcIncrementalSyncEngineTest {

    @Test
    public void shouldPerformInitialIncrementalSyncAndPersistCheckpoint() throws Exception {
        JdbcDataSource sourceDataSource = newDataSource("jdbc:h2:mem:incremental_source_1;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcDataSource targetDataSource = newDataSource("jdbc:h2:mem:incremental_target_1;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Map<String, JdbcDataSource> dataSources = new HashMap<String, JdbcDataSource>();
        dataSources.put("source", sourceDataSource);
        dataSources.put("target", targetDataSource);

        createCustomerTable(sourceDataSource);
        insertRows(sourceDataSource, "customer", new Object[][]{
                {1L, "Alice", 10L},
                {2L, "Bob", 20L},
                {3L, "Charlie", 30L}
        });

        InMemoryExecutionLogRepository executionLogRepository = new InMemoryExecutionLogRepository();
        InMemoryIncrementalSyncCheckpointRepository checkpointRepository = new InMemoryIncrementalSyncCheckpointRepository();
        JdbcIncrementalSyncEngine engine = new JdbcIncrementalSyncEngine(new JdbcDatabaseMetadataScanner(),
                new MapBackedConnectionOpener(dataSources), executionLogRepository, checkpointRepository);

        IncrementalSyncResult result = engine.sync(buildTimestampRequest("source", "target", 1L, null));

        Assert.assertTrue(result.isSuccess());
        Assert.assertFalse(result.isResumed());
        Assert.assertEquals(3L, result.getInsertedRowCount());
        Assert.assertEquals("30", result.getCheckpointValue());

        assertRowCount(targetDataSource, "customer_copy", 3);
        assertTargetValue(targetDataSource, "customer_copy", 3L, "Charlie", 30L);
    }

    @Test
    public void shouldResumeFromSavedCheckpointAndOnlySyncNewRows() throws Exception {
        JdbcDataSource sourceDataSource = newDataSource("jdbc:h2:mem:incremental_source_2;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcDataSource targetDataSource = newDataSource("jdbc:h2:mem:incremental_target_2;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Map<String, JdbcDataSource> dataSources = new HashMap<String, JdbcDataSource>();
        dataSources.put("source", sourceDataSource);
        dataSources.put("target", targetDataSource);

        createCustomerTable(sourceDataSource);
        createCustomerCopyTable(targetDataSource);
        insertRows(sourceDataSource, "customer", new Object[][]{
                {1L, "Alice", 10L},
                {2L, "Bob", 20L}
        });

        InMemoryExecutionLogRepository executionLogRepository = new InMemoryExecutionLogRepository();
        InMemoryIncrementalSyncCheckpointRepository checkpointRepository = new InMemoryIncrementalSyncCheckpointRepository();
        JdbcIncrementalSyncEngine engine = new JdbcIncrementalSyncEngine(new JdbcDatabaseMetadataScanner(),
                new MapBackedConnectionOpener(dataSources), executionLogRepository, checkpointRepository);

        IncrementalSyncResult firstResult = engine.sync(buildTimestampRequest("source", "target", 2L, null));
        Assert.assertEquals(2L, firstResult.getInsertedRowCount());

        insertRows(sourceDataSource, "customer", new Object[][]{
                {3L, "Charlie", 30L}
        });

        IncrementalSyncResult secondResult = engine.sync(buildTimestampRequest("source", "target", 2L, null));
        Assert.assertTrue(secondResult.isResumed());
        Assert.assertEquals("30", secondResult.getCheckpointValue());
        Assert.assertEquals(1L, secondResult.getInsertedRowCount());

        assertRowCount(targetDataSource, "customer_copy", 3);
        assertTargetValue(targetDataSource, "customer_copy", 1L, "Alice", 10L);
        assertTargetValue(targetDataSource, "customer_copy", 2L, "Bob", 20L);
        assertTargetValue(targetDataSource, "customer_copy", 3L, "Charlie", 30L);
    }

    @Test
    public void shouldResumeFromSavedAutoIncrementCheckpointAndOnlySyncNewRows() throws Exception {
        JdbcDataSource sourceDataSource = newDataSource("jdbc:h2:mem:incremental_source_3;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcDataSource targetDataSource = newDataSource("jdbc:h2:mem:incremental_target_3;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Map<String, JdbcDataSource> dataSources = new HashMap<String, JdbcDataSource>();
        dataSources.put("source", sourceDataSource);
        dataSources.put("target", targetDataSource);

        createCustomerTable(sourceDataSource);
        createCustomerCopyTable(targetDataSource);
        insertRows(sourceDataSource, "customer", new Object[][]{
                {1L, "Alice", 10L},
                {2L, "Bob", 20L}
        });

        InMemoryExecutionLogRepository executionLogRepository = new InMemoryExecutionLogRepository();
        InMemoryIncrementalSyncCheckpointRepository checkpointRepository = new InMemoryIncrementalSyncCheckpointRepository();
        JdbcIncrementalSyncEngine engine = new JdbcIncrementalSyncEngine(new JdbcDatabaseMetadataScanner(),
                new MapBackedConnectionOpener(dataSources), executionLogRepository, checkpointRepository);

        IncrementalSyncResult firstResult = engine.sync(buildAutoIncrementRequest("source", "target", 3L, null));
        Assert.assertEquals(2L, firstResult.getInsertedRowCount());

        insertRows(sourceDataSource, "customer", new Object[][]{
                {3L, "Charlie", 30L}
        });

        IncrementalSyncResult secondResult = engine.sync(buildAutoIncrementRequest("source", "target", 3L, null));
        Assert.assertTrue(secondResult.isResumed());
        Assert.assertEquals("3", secondResult.getCheckpointValue());
        Assert.assertEquals(1L, secondResult.getInsertedRowCount());

        assertRowCount(targetDataSource, "customer_copy", 3);
        assertTargetValue(targetDataSource, "customer_copy", 1L, "Alice", 10L);
        assertTargetValue(targetDataSource, "customer_copy", 2L, "Bob", 20L);
        assertTargetValue(targetDataSource, "customer_copy", 3L, "Charlie", 30L);
    }

    private IncrementalSyncRequest buildTimestampRequest(String sourceKey, String targetKey, Long taskId, String checkpointValue) {
        return IncrementalSyncRequest.builder()
                .taskId(taskId)
                .sourceDatasource(buildConfig(sourceKey))
                .targetDatasource(buildConfig(targetKey))
                .sourceTableName("customer")
                .targetTableName("customer_copy")
                .incrementalMode(IncrementalSyncMode.TIMESTAMP)
                .watermarkColumnName("updated_at")
                .checkpointKey("customer_sync_" + taskId)
                .checkpointValue(checkpointValue)
                .pageSize(2)
                .batchSize(2)
                .replaceTargetData(false)
                .build();
    }

    private IncrementalSyncRequest buildAutoIncrementRequest(String sourceKey, String targetKey, Long taskId, String checkpointValue) {
        return IncrementalSyncRequest.builder()
                .taskId(taskId)
                .sourceDatasource(buildConfig(sourceKey))
                .targetDatasource(buildConfig(targetKey))
                .sourceTableName("customer")
                .targetTableName("customer_copy")
                .incrementalMode(IncrementalSyncMode.AUTO_INCREMENT_ID)
                .autoIncrementColumnName("id")
                .checkpointKey("customer_sync_" + taskId)
                .checkpointValue(checkpointValue)
                .pageSize(2)
                .batchSize(2)
                .replaceTargetData(false)
                .build();
    }

    private DatasourceConfig buildConfig(String key) {
        DatasourceConfig config = new DatasourceConfig();
        config.setType(DatasourceType.MYSQL);
        config.setRemark(key);
        return config;
    }

    private JdbcDataSource newDataSource(String url) {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void createCustomerTable(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE customer (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, updated_at BIGINT NOT NULL)");
        }
    }

    private void createCustomerCopyTable(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE customer_copy (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, updated_at BIGINT NOT NULL)");
        }
    }

    private void insertRows(JdbcDataSource dataSource, String tableName, Object[][] rows) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (id, name, updated_at) VALUES (?, ?, ?)")) {
            for (Object[] row : rows) {
                statement.setLong(1, ((Long) row[0]).longValue());
                statement.setString(2, String.valueOf(row[1]));
                statement.setLong(3, ((Long) row[2]).longValue());
                statement.executeUpdate();
            }
        }
    }

    private void assertRowCount(JdbcDataSource dataSource, String tableName, int expectedCount) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(expectedCount, resultSet.getInt(1));
        }
    }

    private void assertTargetValue(JdbcDataSource dataSource, String tableName, long id, String name, long updatedAt) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("SELECT id, name, updated_at FROM " + tableName + " WHERE id = " + id)) {
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(id, resultSet.getLong("id"));
            Assert.assertEquals(name, resultSet.getString("name"));
            Assert.assertEquals(updatedAt, resultSet.getLong("updated_at"));
        }
    }

    private static class MapBackedConnectionOpener implements DatasourceConnectionOpener {

        private final Map<String, JdbcDataSource> dataSources;

        private MapBackedConnectionOpener(Map<String, JdbcDataSource> dataSources) {
            this.dataSources = dataSources;
        }

        @Override
        public Connection open(DatasourceConfig config) throws java.sql.SQLException {
            JdbcDataSource dataSource = dataSources.get(config.getRemark());
            if (dataSource == null) {
                throw new java.sql.SQLException("Unknown test data source: " + config.getRemark());
            }
            return dataSource.getConnection();
        }
    }

    private static class InMemoryExecutionLogRepository implements ExecutionLogRepository {

        private final List<ExecutionLogEntry> entries = new ArrayList<ExecutionLogEntry>();

        @Override
        public long append(ExecutionLogEntry entry) {
            entries.add(entry);
            return entries.size();
        }

        @Override
        public List<ExecutionLogEntry> findByTaskId(long taskId) {
            List<ExecutionLogEntry> result = new ArrayList<ExecutionLogEntry>();
            for (ExecutionLogEntry entry : entries) {
                if (entry.getTaskId() != null && entry.getTaskId().longValue() == taskId) {
                    result.add(entry);
                }
            }
            return result;
        }

        @Override
        public int deleteOlderThan(long createdAt, List<Long> excludedTaskIds) {
            return 0;
        }
    }

    private static class InMemoryIncrementalSyncCheckpointRepository implements IncrementalSyncCheckpointRepository {

        private IncrementalSyncCheckpointEntry checkpoint;

        @Override
        public long save(IncrementalSyncCheckpointEntry checkpoint) {
            this.checkpoint = checkpoint;
            this.checkpoint.setId(Long.valueOf(1L));
            return 1L;
        }

        @Override
        public Optional<IncrementalSyncCheckpointEntry> findByTaskId(long taskId) {
            if (checkpoint != null && checkpoint.getTaskId() != null && checkpoint.getTaskId().longValue() == taskId) {
                return Optional.of(checkpoint);
            }
            return Optional.empty();
        }
    }
}
