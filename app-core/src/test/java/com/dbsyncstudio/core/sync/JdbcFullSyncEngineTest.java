package com.dbsyncstudio.core.sync;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.FullSyncRequest;
import com.dbsyncstudio.model.sync.FullSyncResult;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class JdbcFullSyncEngineTest {

    @Test
    public void shouldAutoCreateTargetTableAndSyncRowsByPages() throws Exception {
        JdbcDataSource sourceDataSource = newDataSource("jdbc:h2:mem:full_sync_source_1;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcDataSource targetDataSource = newDataSource("jdbc:h2:mem:full_sync_target_1;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Map<String, JdbcDataSource> dataSources = new HashMap<String, JdbcDataSource>();
        dataSources.put("source", sourceDataSource);
        dataSources.put("target", targetDataSource);

        createCustomerTable(sourceDataSource);
        insertRows(sourceDataSource, "customer", new Object[][]{
                {1L, "Alice", 18},
                {2L, "Bob", 20},
                {3L, "Charlie", 22}
        });

        JdbcFullSyncEngine engine = new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new MapBackedConnectionOpener(dataSources));
        FullSyncResult result = engine.sync(buildRequest("source", "target", 2, 2, true, "customer_copy"));

        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.isCreatedTargetTable());
        Assert.assertEquals(3L, result.getSourceRowCount());
        Assert.assertEquals(3L, result.getInsertedRowCount());

        assertTargetRowCount(targetDataSource, "customer_copy", 3);
        assertTargetValue(targetDataSource, "customer_copy", 2L, "Bob", 20);
    }

    @Test
    public void shouldReplaceExistingTargetRowsBeforeSync() throws Exception {
        JdbcDataSource sourceDataSource = newDataSource("jdbc:h2:mem:full_sync_source_2;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcDataSource targetDataSource = newDataSource("jdbc:h2:mem:full_sync_target_2;MODE=MySQL;DB_CLOSE_DELAY=-1");
        Map<String, JdbcDataSource> dataSources = new HashMap<String, JdbcDataSource>();
        dataSources.put("source", sourceDataSource);
        dataSources.put("target", targetDataSource);

        createCustomerTable(sourceDataSource);
        insertRows(sourceDataSource, "customer", new Object[][]{
                {1L, "Alice", 18},
                {2L, "Bob", 20}
        });

        createTargetTable(targetDataSource);
        insertRows(targetDataSource, "customer_copy", new Object[][]{
                {99L, "Stale", 99}
        });

        JdbcFullSyncEngine engine = new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new MapBackedConnectionOpener(dataSources));
        FullSyncResult result = engine.sync(buildRequest("source", "target", 1, 1, true, "customer_copy"));

        Assert.assertTrue(result.isSuccess());
        Assert.assertFalse(result.isCreatedTargetTable());
        Assert.assertEquals(2L, result.getInsertedRowCount());

        assertTargetRowCount(targetDataSource, "customer_copy", 2);
        assertTargetValue(targetDataSource, "customer_copy", 1L, "Alice", 18);
        assertTargetValue(targetDataSource, "customer_copy", 2L, "Bob", 20);
        Assert.assertFalse(targetContainsId(targetDataSource, "customer_copy", 99L));
    }

    private FullSyncRequest buildRequest(String sourceKey, String targetKey, int pageSize, int batchSize,
                                         boolean replaceTargetData, String targetTableName) {
        return FullSyncRequest.builder()
                .sourceDatasource(buildConfig(sourceKey))
                .targetDatasource(buildConfig(targetKey))
                .sourceTableName("customer")
                .targetTableName(targetTableName)
                .pageSize(pageSize)
                .batchSize(batchSize)
                .replaceTargetData(replaceTargetData)
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
            statement.execute("CREATE TABLE customer (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, age INT)");
        }
    }

    private void createTargetTable(JdbcDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE customer_copy (id BIGINT PRIMARY KEY, name VARCHAR(64) NOT NULL, age INT)");
        }
    }

    private void insertRows(JdbcDataSource dataSource, String tableName, Object[][] rows) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (id, name, age) VALUES (?, ?, ?)")) {
            for (Object[] row : rows) {
                statement.setLong(1, ((Long) row[0]).longValue());
                statement.setString(2, String.valueOf(row[1]));
                statement.setInt(3, ((Integer) row[2]).intValue());
                statement.executeUpdate();
            }
        }
    }

    private void assertTargetRowCount(JdbcDataSource dataSource, String tableName, int expectedCount) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(expectedCount, resultSet.getInt(1));
        }
    }

    private void assertTargetValue(JdbcDataSource dataSource, String tableName, long id, String name, int age) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("SELECT id, name, age FROM " + tableName + " WHERE id = " + id)) {
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(id, resultSet.getLong("id"));
            Assert.assertEquals(name, resultSet.getString("name"));
            Assert.assertEquals(age, resultSet.getInt("age"));
        }
    }

    private boolean targetContainsId(JdbcDataSource dataSource, String tableName, long id) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName + " WHERE id = " + id)) {
            Assert.assertTrue(resultSet.next());
            return resultSet.getInt(1) > 0;
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
}
