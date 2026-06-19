package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.monitoring.DatasourceConnectionMetric;
import com.dbsyncstudio.model.monitoring.MonitoringCleanupSummary;
import com.dbsyncstudio.model.monitoring.TaskRunMetric;
import com.dbsyncstudio.model.monitoring.TaskRunMetricSummary;
import com.dbsyncstudio.model.monitoring.TableRunMetric;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

public class SqliteMonitoringRepositoryTest {

    @Test
    public void shouldPersistAndQueryMonitoringMetrics() throws Exception {
        File tempDatabase = resetTempDatabase("db-sync-studio-monitoring");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteMonitoringRepository repository = new SqliteMonitoringRepository(connectionFactory);
        repository.initialize();

        TaskRunMetric taskMetric = TaskRunMetric.builder()
                .runId("run-20260619-001")
                .taskId(Long.valueOf(101L))
                .metricTime(Long.valueOf(1_720_000_000_000L))
                .successRowCount(Long.valueOf(900L))
                .failedRowCount(Long.valueOf(5L))
                .speedRowsPerSecond(Double.valueOf(320.5d))
                .latencyMillis(Long.valueOf(120L))
                .durationMillis(Long.valueOf(5600L))
                .errorMessage("partial retry")
                .runningTaskCount(Integer.valueOf(3))
                .todayTaskCount(Integer.valueOf(7))
                .todaySuccessTaskCount(Integer.valueOf(6))
                .todayFailedTaskCount(Integer.valueOf(1))
                .build();
        long taskMetricId = repository.saveTaskRunMetric(taskMetric);
        Assert.assertTrue(taskMetricId > 0L);

        TableRunMetric tableMetric = TableRunMetric.builder()
                .tableTaskId(Long.valueOf(301L))
                .taskId(Long.valueOf(101L))
                .runId("run-20260619-001")
                .tableName("orders")
                .syncedRowCount(Long.valueOf(800L))
                .successRowCount(Long.valueOf(795L))
                .failedRowCount(Long.valueOf(5L))
                .speedRowsPerSecond(Double.valueOf(200.0d))
                .batchCount(Integer.valueOf(16))
                .retryCount(Integer.valueOf(1))
                .lastCheckpoint("offset:800")
                .lastError("one row failed")
                .metricTime(Long.valueOf(1_720_000_000_050L))
                .build();
        long tableMetricId = repository.saveTableRunMetric(tableMetric);
        Assert.assertTrue(tableMetricId > 0L);

        DatasourceConnectionMetric datasourceMetric = DatasourceConnectionMetric.builder()
                .datasourceId(Long.valueOf(501L))
                .connectionStatus("FAILED")
                .lastSuccessTime(Long.valueOf(1_719_999_999_000L))
                .lastFailureTime(Long.valueOf(1_720_000_000_100L))
                .failureReason("timeout")
                .averageTestConnectionMillis(Double.valueOf(88.5d))
                .lastTestConnectionMillis(Long.valueOf(120L))
                .metricTime(Long.valueOf(1_720_000_000_100L))
                .build();
        long datasourceMetricId = repository.saveDatasourceConnectionMetric(datasourceMetric);
        Assert.assertTrue(datasourceMetricId > 0L);

        Optional<TaskRunMetric> loadedTaskMetric = repository.findLatestTaskRunMetricByRunId("run-20260619-001");
        Assert.assertTrue(loadedTaskMetric.isPresent());
        Assert.assertEquals(Long.valueOf(taskMetricId), loadedTaskMetric.get().getId());
        Assert.assertEquals(Long.valueOf(900L), loadedTaskMetric.get().getSuccessRowCount());

        List<TableRunMetric> tableMetrics = repository.findTableRunMetricsByRunId("run-20260619-001");
        Assert.assertEquals(1, tableMetrics.size());
        Assert.assertEquals(Long.valueOf(tableMetricId), tableMetrics.get(0).getId());
        Assert.assertEquals("orders", tableMetrics.get(0).getTableName());

        Optional<DatasourceConnectionMetric> loadedDatasourceMetric =
                repository.findLatestDatasourceConnectionMetricByDatasourceId(501L);
        Assert.assertTrue(loadedDatasourceMetric.isPresent());
        Assert.assertEquals(Long.valueOf(datasourceMetricId), loadedDatasourceMetric.get().getId());
        Assert.assertEquals("FAILED", loadedDatasourceMetric.get().getConnectionStatus());

        TaskRunMetricSummary summary = repository.summarizeTaskMetricsForToday(1_719_993_600_000L, 1_720_080_000_000L);
        Assert.assertEquals(Integer.valueOf(1), summary.getTotalTaskCount());
        Assert.assertEquals(Integer.valueOf(0), summary.getSuccessTaskCount());
        Assert.assertEquals(Integer.valueOf(1), summary.getFailedTaskCount());
        Assert.assertEquals(Integer.valueOf(3), summary.getLatestRunningTaskCount());
    }

    @Test
    public void shouldCleanupExpiredMonitoringMetricsAndRespectRetentionDays() throws Exception {
        File tempDatabase = resetTempDatabase("db-sync-studio-monitoring-cleanup");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteMonitoringRepository repository = new SqliteMonitoringRepository(connectionFactory);
        repository.initialize();

        repository.saveTaskRunMetric(TaskRunMetric.builder()
                .runId("run-old")
                .taskId(Long.valueOf(1L))
                .metricTime(Long.valueOf(1000L))
                .successRowCount(Long.valueOf(10L))
                .failedRowCount(Long.valueOf(0L))
                .runningTaskCount(Integer.valueOf(1))
                .todayTaskCount(Integer.valueOf(1))
                .todaySuccessTaskCount(Integer.valueOf(1))
                .todayFailedTaskCount(Integer.valueOf(0))
                .build());
        repository.saveTableRunMetric(TableRunMetric.builder()
                .tableTaskId(Long.valueOf(11L))
                .taskId(Long.valueOf(1L))
                .runId("run-old")
                .tableName("t_old")
                .metricTime(Long.valueOf(1000L))
                .build());
        repository.saveDatasourceConnectionMetric(DatasourceConnectionMetric.builder()
                .datasourceId(Long.valueOf(21L))
                .connectionStatus("SUCCESS")
                .metricTime(Long.valueOf(1000L))
                .build());

        long freshMetricTime = 2_592_000_001L;
        repository.saveTaskRunMetric(TaskRunMetric.builder()
                .runId("run-new")
                .taskId(Long.valueOf(2L))
                .metricTime(Long.valueOf(freshMetricTime))
                .successRowCount(Long.valueOf(20L))
                .failedRowCount(Long.valueOf(0L))
                .runningTaskCount(Integer.valueOf(1))
                .todayTaskCount(Integer.valueOf(1))
                .todaySuccessTaskCount(Integer.valueOf(1))
                .todayFailedTaskCount(Integer.valueOf(0))
                .build());
        repository.saveTableRunMetric(TableRunMetric.builder()
                .tableTaskId(Long.valueOf(12L))
                .taskId(Long.valueOf(2L))
                .runId("run-new")
                .tableName("t_new")
                .metricTime(Long.valueOf(freshMetricTime))
                .build());
        repository.saveDatasourceConnectionMetric(DatasourceConnectionMetric.builder()
                .datasourceId(Long.valueOf(22L))
                .connectionStatus("SUCCESS")
                .metricTime(Long.valueOf(freshMetricTime))
                .build());

        MonitoringCleanupSummary summary = repository.cleanupExpiredMetrics(30, 5_184_000_000L);
        Assert.assertEquals(Integer.valueOf(30), summary.getRetentionDays());
        Assert.assertEquals(Long.valueOf(2_592_000_000L), summary.getCutoffTime());
        Assert.assertEquals(Integer.valueOf(1), summary.getTaskRunMetricDeletedCount());
        Assert.assertEquals(Integer.valueOf(1), summary.getTableRunMetricDeletedCount());
        Assert.assertEquals(Integer.valueOf(1), summary.getDatasourceConnectionMetricDeletedCount());

        List<TaskRunMetric> taskMetrics = repository.findTaskRunMetricsByTaskId(2L, 10);
        Assert.assertEquals(1, taskMetrics.size());
        Assert.assertEquals("run-new", taskMetrics.get(0).getRunId());

        Assert.assertFalse(repository.findLatestTaskRunMetricByRunId("run-old").isPresent());
        Assert.assertEquals(1, repository.findTableRunMetricsByRunId("run-new").size());
        Assert.assertFalse(repository.findLatestDatasourceConnectionMetricByDatasourceId(21L).isPresent());
    }

    @Test
    public void shouldInitializeMonitoringTablesForExistingDatabase() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-monitoring-schema", ".sqlite");
        tempDatabase.deleteOnExit();

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS sync_task (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_name TEXT NOT NULL, " +
                    "source_datasource_id INTEGER NOT NULL, " +
                    "target_datasource_id INTEGER NOT NULL, " +
                    "sync_mode TEXT NOT NULL, " +
                    "task_status TEXT NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL" +
                    ")");

            SqliteSchemaInitializer.initialize(connection);

            Assert.assertTrue(hasTable(connection, "task_run_metric"));
            Assert.assertTrue(hasTable(connection, "table_run_metric"));
            Assert.assertTrue(hasTable(connection, "datasource_connection_metric"));
            Assert.assertTrue(hasIndex(connection, "idx_task_run_metric_run_id"));
            Assert.assertTrue(hasIndex(connection, "idx_table_run_metric_run_id"));
            Assert.assertTrue(hasIndex(connection, "idx_datasource_connection_metric_datasource_id"));
        }
    }

    private File resetTempDatabase(String prefix) throws Exception {
        File tempDatabase = File.createTempFile(prefix, ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();
        return tempDatabase;
    }

    private boolean hasTable(Connection connection, String tableName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'")) {
            return resultSet.next();
        }
    }

    private boolean hasIndex(Connection connection, String indexName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'index' AND name = '" + indexName + "'")) {
            return resultSet.next();
        }
    }
}
