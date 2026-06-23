package com.dbsyncstudio.core.validation;

import com.dbsyncstudio.core.connection.DatasourceConnectionOpener;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.validation.dto.RepairRequestDTO;
import com.dbsyncstudio.model.validation.vo.RepairResultVO;
import com.dbsyncstudio.model.validation.RepairType;
import com.dbsyncstudio.model.validation.entity.ValidationDifferenceDO;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataRepairEngineTest {

    @Test
    public void shouldExecuteInsertAndUpdateRepairs() throws Exception {
        Map<String, String> connections = createH2Connections("repair_execute");
        try (Connection source = open(connections.get("source"));
             Connection target = open(connections.get("target"));
             Statement sourceStatement = source.createStatement();
             Statement targetStatement = target.createStatement()) {
            sourceStatement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(64), amount DECIMAL(10, 2))");
            targetStatement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(64), amount DECIMAL(10, 2))");
            sourceStatement.execute("INSERT INTO users (id, name, amount) VALUES (1, 'Alice', 10.00), (2, 'Bob', 20.00)");
            targetStatement.execute("INSERT INTO users (id, name, amount) VALUES (1, 'Alice', 11.00)");
            DataRepairEngine engine = new DataRepairEngine(h2Opener(connections));

            ValidationDifferenceDO missingDifference = ValidationDifferenceDO.builder()
                    .id(Long.valueOf(1L))
                    .taskId(Long.valueOf(101L))
                    .validationRunId(Long.valueOf(201L))
                    .runId("validation-1")
                    .differenceType("MISSING_TARGET")
                    .primaryKeyJson("{\"id\":2}")
                    .sourceRowJson("{\"id\":2,\"name\":\"Bob\",\"amount\":20.00}")
                    .suggestedRepairType(RepairType.INSERT_MISSING.name())
                    .status("OPEN")
                    .build();
            RepairRequestDTO insertRequest = baseRepairRequest(RepairType.INSERT_MISSING, true);
            RepairResultVO insertResult = engine.repair(insertRequest, java.util.Arrays.asList(missingDifference));
            Assert.assertEquals("SUCCESS", insertResult.getRun().getStatus());
            Assert.assertEquals(1, insertResult.getDetails().size());
            assertRowExists(target, 2L, "Bob", "20.00");

            ValidationDifferenceDO inconsistentDifference = ValidationDifferenceDO.builder()
                    .id(Long.valueOf(2L))
                    .taskId(Long.valueOf(101L))
                    .validationRunId(Long.valueOf(202L))
                    .runId("validation-2")
                    .differenceType("INCONSISTENT_ROW")
                    .primaryKeyJson("{\"id\":1}")
                    .sourceRowJson("{\"id\":1,\"name\":\"Alice\",\"amount\":10.00}")
                    .targetRowJson("{\"id\":1,\"name\":\"Alice\",\"amount\":11.00}")
                    .differingColumnsJson("[\"amount\"]")
                    .suggestedRepairType(RepairType.UPDATE_INCONSISTENT.name())
                    .status("OPEN")
                    .build();
            RepairRequestDTO updateRequest = baseRepairRequest(RepairType.UPDATE_INCONSISTENT, true);
            RepairResultVO updateResult = engine.repair(updateRequest, java.util.Arrays.asList(inconsistentDifference));
            Assert.assertEquals("SUCCESS", updateResult.getRun().getStatus());
            Assert.assertEquals(1, updateResult.getDetails().size());
            assertRowExists(target, 1L, "Alice", "10.00");
        }
    }

    @Test
    public void shouldRequireDeleteConfirmationBeforeExecutingDeleteRepair() throws Exception {
        Map<String, String> connections = createH2Connections("repair_delete");
        try (Connection source = open(connections.get("source"));
             Connection target = open(connections.get("target"));
             Statement sourceStatement = source.createStatement();
             Statement targetStatement = target.createStatement()) {
            sourceStatement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(64))");
            targetStatement.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, name VARCHAR(64))");
            targetStatement.execute("INSERT INTO users (id, name) VALUES (1, 'Alice')");
        }

        DataRepairEngine engine = new DataRepairEngine(h2Opener(connections));
        ValidationDifferenceDO extraDifference = ValidationDifferenceDO.builder()
                .id(Long.valueOf(3L))
                .taskId(Long.valueOf(101L))
                .validationRunId(Long.valueOf(203L))
                .runId("validation-3")
                .differenceType("EXTRA_TARGET")
                .primaryKeyJson("{\"id\":1}")
                .targetRowJson("{\"id\":1,\"name\":\"Alice\"}")
                .suggestedRepairType(RepairType.DELETE_EXTRA.name())
                .status("OPEN")
                .build();

        RepairRequestDTO deleteRequest = baseRepairRequest(RepairType.DELETE_EXTRA, true);
        deleteRequest.setConfirmDelete(false);
        boolean failed = false;
        try {
            engine.repair(deleteRequest, java.util.Arrays.asList(extraDifference));
        } catch (SQLException ex) {
            failed = true;
        }
        Assert.assertTrue(failed);
    }

    private RepairRequestDTO baseRepairRequest(RepairType repairType, boolean execute) {
        DatasourceConfigDO source = DatasourceConfigDO.builder()
                .type(DatasourceType.MYSQL)
                .databaseName("source")
                .build();
        DatasourceConfigDO target = DatasourceConfigDO.builder()
                .type(DatasourceType.MYSQL)
                .databaseName("target")
                .build();
        return RepairRequestDTO.builder()
                .taskId(Long.valueOf(101L))
                .validationRunId(Long.valueOf(201L))
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceTableName("users")
                .targetTableName("users")
                .primaryKeyColumns(java.util.Collections.singletonList("id"))
                .repairType(repairType)
                .execute(execute)
                .confirmDelete(false)
                .build();
    }

    private Map<String, String> createH2Connections(String name) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("source", "jdbc:h2:mem:" + name + "_source;MODE=MySQL;DB_CLOSE_DELAY=-1");
        result.put("target", "jdbc:h2:mem:" + name + "_target;MODE=MySQL;DB_CLOSE_DELAY=-1");
        return result;
    }

    private Connection open(String jdbcUrl) throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(jdbcUrl, "sa", "");
    }

    private DatasourceConnectionOpener h2Opener(final Map<String, String> connections) {
        return new DatasourceConnectionOpener() {
            @Override
            public Connection open(DatasourceConfigDO config) throws java.sql.SQLException {
                try {
                    return DataRepairEngineTest.this.open(connections.get(config.getDatabaseName()));
                } catch (Exception ex) {
                    if (ex instanceof java.sql.SQLException) {
                        throw (java.sql.SQLException) ex;
                    }
                    throw new java.sql.SQLException("Failed to open H2 connection", ex);
                }
            }
        };
    }

    private void assertRowExists(Connection target, long id, String name, String amount) throws Exception {
        try (Statement statement = target.createStatement();
             java.sql.ResultSet resultSet = statement.executeQuery("SELECT id, name, amount FROM users WHERE id = " + id)) {
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(Long.valueOf(id), Long.valueOf(resultSet.getLong("id")));
            Assert.assertEquals(name, resultSet.getString("name"));
            Assert.assertEquals(amount, resultSet.getBigDecimal("amount").toPlainString());
        }
    }
}
