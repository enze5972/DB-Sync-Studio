package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.monitoring.DatasourceConnectionMetric;
import com.dbsyncstudio.model.monitoring.MonitoringCleanupSummary;
import com.dbsyncstudio.model.monitoring.TaskRunMetric;
import com.dbsyncstudio.model.monitoring.TableRunMetric;
import com.dbsyncstudio.store.sqlite.SqliteConnectionFactory;
import com.dbsyncstudio.store.sqlite.SqliteDatasourceRepository;
import com.dbsyncstudio.store.sqlite.SqliteFieldMappingRepository;
import com.dbsyncstudio.store.sqlite.SqliteIncrementalSyncCheckpointRepository;
import com.dbsyncstudio.store.sqlite.SqliteMonitoringRepository;
import com.dbsyncstudio.store.sqlite.SqliteSchemaComparisonHistoryRepository;
import com.dbsyncstudio.store.sqlite.SqliteSqlExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTaskRepository;
import com.dbsyncstudio.store.sync.SqliteExecutionLogRepository;
import com.dbsyncstudio.store.sync.SqliteSyncCheckpointRepository;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class DesktopBackendServiceMonitoringApiTest {

    @Test
    public void shouldReturnMonitoringOverviewAndMetricLists() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-monitoring-service", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteDatasourceRepository datasourceRepository = new SqliteDatasourceRepository(connectionFactory);
        SqliteSyncTaskRepository taskRepository = new SqliteSyncTaskRepository(connectionFactory);
        SqliteExecutionLogRepository executionLogRepository = new SqliteExecutionLogRepository(connectionFactory);
        SqliteSyncCheckpointRepository syncCheckpointRepository = new SqliteSyncCheckpointRepository(connectionFactory);
        SqliteFieldMappingRepository fieldMappingRepository = new SqliteFieldMappingRepository(connectionFactory);
        SqliteSqlExecutionLogRepository sqlExecutionLogRepository = new SqliteSqlExecutionLogRepository(connectionFactory);
        SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository = new SqliteSchemaComparisonHistoryRepository(connectionFactory);
        SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository = new SqliteIncrementalSyncCheckpointRepository(connectionFactory);
        SqliteMonitoringRepository monitoringRepository = new SqliteMonitoringRepository(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();
        monitoringRepository.initialize();

        long now = System.currentTimeMillis();
        monitoringRepository.saveTaskRunMetric(TaskRunMetric.builder()
                .runId("run-metric-001")
                .taskId(Long.valueOf(101L))
                .metricTime(Long.valueOf(now))
                .successRowCount(Long.valueOf(120L))
                .failedRowCount(Long.valueOf(2L))
                .speedRowsPerSecond(Double.valueOf(33.5d))
                .latencyMillis(Long.valueOf(15L))
                .durationMillis(Long.valueOf(3500L))
                .errorMessage("minor retry")
                .runningTaskCount(Integer.valueOf(1))
                .todayTaskCount(Integer.valueOf(3))
                .todaySuccessTaskCount(Integer.valueOf(2))
                .todayFailedTaskCount(Integer.valueOf(1))
                .build());
        monitoringRepository.saveTableRunMetric(TableRunMetric.builder()
                .tableTaskId(Long.valueOf(201L))
                .taskId(Long.valueOf(101L))
                .runId("run-metric-001")
                .tableName("orders")
                .syncedRowCount(Long.valueOf(120L))
                .successRowCount(Long.valueOf(118L))
                .failedRowCount(Long.valueOf(2L))
                .speedRowsPerSecond(Double.valueOf(30.0d))
                .batchCount(Integer.valueOf(4))
                .retryCount(Integer.valueOf(1))
                .lastCheckpoint("offset:120")
                .lastError("minor retry")
                .metricTime(Long.valueOf(now))
                .build());
        monitoringRepository.saveDatasourceConnectionMetric(DatasourceConnectionMetric.builder()
                .datasourceId(Long.valueOf(301L))
                .connectionStatus("SUCCESS")
                .lastSuccessTime(Long.valueOf(now))
                .averageTestConnectionMillis(Double.valueOf(18.0d))
                .lastTestConnectionMillis(Long.valueOf(18L))
                .metricTime(Long.valueOf(now))
                .build());

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                taskRepository,
                executionLogRepository,
                syncCheckpointRepository,
                fieldMappingRepository,
                null,
                null,
                null,
                null,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                null,
                null,
                monitoringRepository,
                new JdbcDatabaseMetadataScanner(),
                new JdbcDatasourceConnectionTester(),
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                null,
                null,
                new JdbcFullSyncEngine(),
                new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository));

        MonitoringOverviewResponse overview = service.monitoringOverview();
        Assert.assertNotNull(overview);
        Assert.assertNotNull(overview.getSummary());
        Assert.assertEquals(Integer.valueOf(1), overview.getSummary().getTotalTaskCount());
        Assert.assertNotNull(overview.getLatestTaskMetric());
        Assert.assertEquals("run-metric-001", overview.getLatestTaskMetric().getRunId());

        List<TaskRunMetric> taskMetrics = service.listTaskRunMetrics("run-metric-001", Long.valueOf(101L), null, null, 10);
        Assert.assertEquals(1, taskMetrics.size());
        Assert.assertEquals(Long.valueOf(120L), taskMetrics.get(0).getSuccessRowCount());

        List<TableRunMetric> tableMetrics = service.listTableRunMetrics("run-metric-001", Long.valueOf(101L), Long.valueOf(201L), null, null, 10);
        Assert.assertEquals(1, tableMetrics.size());
        Assert.assertEquals("orders", tableMetrics.get(0).getTableName());

        List<DatasourceConnectionMetric> datasourceMetrics = service.listDatasourceConnectionMetrics(Long.valueOf(301L), null, null, 10);
        Assert.assertEquals(1, datasourceMetrics.size());
        Assert.assertEquals("SUCCESS", datasourceMetrics.get(0).getConnectionStatus());

        MonitoringTrendResponse trend = service.taskRunTrend("run-metric-001", Long.valueOf(101L), null, null, 10);
        Assert.assertEquals(1, trend.getTaskRunTrend().size());
        Assert.assertEquals(Long.valueOf(120L), trend.getTaskRunTrend().get(0).getSuccessRowCount());

        MonitoringCleanupSummary cleanupSummary = service.cleanupMonitoringMetrics(Integer.valueOf(365));
        Assert.assertNotNull(cleanupSummary);
        Assert.assertEquals(Integer.valueOf(365), cleanupSummary.getRetentionDays());
    }
}
