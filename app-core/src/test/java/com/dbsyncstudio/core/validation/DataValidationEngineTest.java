package com.dbsyncstudio.core.validation;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.validation.ValidationDifference;
import com.dbsyncstudio.model.validation.ValidationMode;
import com.dbsyncstudio.model.validation.ValidationRequest;
import com.dbsyncstudio.model.validation.ValidationResult;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataValidationEngineTest {

    @Test
    public void shouldDetectMissingRowsWithCompositePrimaryKey() throws Exception {
        Map<String, String> connections = createH2Connections("validation_missing");
        try (Connection source = openConnection(connections.get("source"));
             Connection target = openConnection(connections.get("target"));
             Statement statement = source.createStatement();
             Statement targetStatement = target.createStatement()) {
            statement.execute("CREATE TABLE users (tenant_id BIGINT NOT NULL, id BIGINT NOT NULL, name VARCHAR(64), PRIMARY KEY (tenant_id, id))");
            targetStatement.execute("CREATE TABLE users (tenant_id BIGINT NOT NULL, id BIGINT NOT NULL, name VARCHAR(64), PRIMARY KEY (tenant_id, id))");
            statement.execute("INSERT INTO users (tenant_id, id, name) VALUES (1, 10, 'Alice'), (1, 11, 'Bob')");
            targetStatement.execute("INSERT INTO users (tenant_id, id, name) VALUES (1, 10, 'Alice')");
        }

        DataValidationEngine engine = new DataValidationEngine(new JdbcDatabaseMetadataScanner(), h2Opener(connections));
        ValidationRequest request = baseRequest("users", "users", ValidationMode.PRIMARY_KEY_EXISTS);
        ValidationResult result = engine.validate(request);

        Assert.assertEquals("SUCCESS", result.getRun().getStatus());
        Assert.assertEquals(Long.valueOf(2L), result.getRun().getSourceRowCount());
        Assert.assertEquals(Long.valueOf(1L), result.getRun().getTargetRowCount());
        Assert.assertEquals(Long.valueOf(1L), result.getRun().getMissingCount());
        Assert.assertEquals(1, result.getDifferences().size());
        ValidationDifference difference = result.getDifferences().get(0);
        Assert.assertEquals("MISSING_TARGET", difference.getDifferenceType());
        String primaryKeyJson = difference.getPrimaryKeyJson().toLowerCase();
        Assert.assertTrue(primaryKeyJson.contains("\"tenant_id\":1"));
        Assert.assertTrue(primaryKeyJson.contains("\"id\":11"));
    }

    @Test
    public void shouldCompareRowCountsAndReportMismatch() throws Exception {
        Map<String, String> connections = createH2Connections("validation_count");
        try (Connection source = openConnection(connections.get("source"));
             Connection target = openConnection(connections.get("target"));
             Statement statement = source.createStatement();
             Statement targetStatement = target.createStatement()) {
            statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(64))");
            targetStatement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(64))");
            statement.execute("INSERT INTO users (id, name) VALUES (1, 'Alice'), (2, 'Bob')");
            targetStatement.execute("INSERT INTO users (id, name) VALUES (1, 'Alice')");
        }

        DataValidationEngine engine = new DataValidationEngine(new JdbcDatabaseMetadataScanner(), h2Opener(connections));
        ValidationRequest request = baseRequest("users", "users", ValidationMode.ROW_COUNT);
        ValidationResult result = engine.validate(request);

        Assert.assertEquals(Long.valueOf(2L), result.getRun().getSourceRowCount());
        Assert.assertEquals(Long.valueOf(1L), result.getRun().getTargetRowCount());
        Assert.assertEquals(Long.valueOf(1L), result.getRun().getMissingCount());
        Assert.assertEquals(1, result.getDifferences().size());
        Assert.assertEquals("ROW_COUNT_MISMATCH", result.getDifferences().get(0).getDifferenceType());
    }

    @Test
    public void shouldNormalizeNumericValuesDuringHashValidation() throws Exception {
        Map<String, String> connections = createH2Connections("validation_hash");
        try (Connection source = openConnection(connections.get("source"));
             Connection target = openConnection(connections.get("target"));
             Statement statement = source.createStatement();
             Statement targetStatement = target.createStatement()) {
            statement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, amount DECIMAL(10, 4), updated_at TIMESTAMP, name VARCHAR(64))");
            targetStatement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, amount DECIMAL(10, 4), updated_at TIMESTAMP, name VARCHAR(64))");
            statement.execute("INSERT INTO users (id, amount, updated_at, name) VALUES (1, 1.0, TIMESTAMP '2026-06-19 10:00:00', 'Alice')");
            targetStatement.execute("INSERT INTO users (id, amount, updated_at, name) VALUES (1, 1.00, TIMESTAMP '2026-06-19 10:00:00', 'Alice')");
        }

        DataValidationEngine engine = new DataValidationEngine(new JdbcDatabaseMetadataScanner(), h2Opener(connections));
        ValidationRequest request = baseRequest("users", "users", ValidationMode.HASH);
        request.setHashColumns(java.util.Arrays.asList("amount", "updated_at", "name"));
        ValidationResult result = engine.validate(request);

        Assert.assertEquals(Long.valueOf(0L), result.getRun().getInconsistentCount());
        Assert.assertEquals(0, result.getDifferences().size());
    }

    private ValidationRequest baseRequest(String sourceTableName, String targetTableName, ValidationMode mode) {
        DatasourceConfig source = DatasourceConfig.builder()
                .type(DatasourceType.MYSQL)
                .databaseName("source")
                .build();
        DatasourceConfig target = DatasourceConfig.builder()
                .type(DatasourceType.MYSQL)
                .databaseName("target")
                .build();
        return ValidationRequest.builder()
                .taskId(Long.valueOf(101L))
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceTableName(sourceTableName)
                .targetTableName(targetTableName)
                .validationMode(mode)
                .sampleCount(Integer.valueOf(2))
                .build();
    }

    private Map<String, String> createH2Connections(String name) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("source", "jdbc:h2:mem:" + name + "_source;MODE=MySQL;DB_CLOSE_DELAY=-1");
        result.put("target", "jdbc:h2:mem:" + name + "_target;MODE=MySQL;DB_CLOSE_DELAY=-1");
        return result;
    }

    private Connection openConnection(String jdbcUrl) throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private DatasourceConnectionOpener h2Opener(final Map<String, String> connections) {
        return new DatasourceConnectionOpener() {
            @Override
            public Connection open(DatasourceConfig config) throws java.sql.SQLException {
                try {
                    return DataValidationEngineTest.this.openConnection(connections.get(config.getDatabaseName()));
                } catch (Exception ex) {
                    if (ex instanceof java.sql.SQLException) {
                        throw (java.sql.SQLException) ex;
                    }
                    throw new java.sql.SQLException("Failed to open H2 connection", ex);
                }
            }
        };
    }
}
