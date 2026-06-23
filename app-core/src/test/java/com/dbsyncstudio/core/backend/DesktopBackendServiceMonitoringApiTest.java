package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.monitoring.entity.DatasourceConnectionMetricDO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringOverviewVO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringTrendVO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringCleanupSummaryVO;
import com.dbsyncstudio.model.monitoring.entity.TaskRunMetricDO;
import com.dbsyncstudio.model.monitoring.entity.TableRunMetricDO;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.FieldMappingRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.IncrementalSyncCheckpointRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.MonitoringRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SchemaComparisonHistoryRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SqlExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.ExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncCheckpointRepositoryImpl;

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

        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
        DatasourceRepositoryImpl datasourceRepository = new DatasourceRepositoryImpl(connectionFactory);
        SyncTaskRepositoryImpl taskRepository = new SyncTaskRepositoryImpl(connectionFactory);
        ExecutionLogRepositoryImpl executionLogRepository = new ExecutionLogRepositoryImpl(connectionFactory);
        SyncCheckpointRepositoryImpl syncCheckpointRepository = new SyncCheckpointRepositoryImpl(connectionFactory);
        FieldMappingRepositoryImpl fieldMappingRepository = new FieldMappingRepositoryImpl(connectionFactory);
        SqlExecutionLogRepositoryImpl sqlExecutionLogRepository = new SqlExecutionLogRepositoryImpl(connectionFactory);
        SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository = new SchemaComparisonHistoryRepositoryImpl(connectionFactory);
        IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository = new IncrementalSyncCheckpointRepositoryImpl(connectionFactory);
        MonitoringRepositoryImpl monitoringRepository = new MonitoringRepositoryImpl(connectionFactory);

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
        monitoringRepository.saveTaskRunMetric(TaskRunMetricDO.builder()
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
        monitoringRepository.saveTableRunMetric(TableRunMetricDO.builder()
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
        monitoringRepository.saveDatasourceConnectionMetric(DatasourceConnectionMetricDO.builder()
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

        MonitoringOverviewVO overview = service.monitoringOverview();
        Assert.assertNotNull(overview);
        Assert.assertNotNull(overview.getSummary());
        Assert.assertEquals(Integer.valueOf(1), overview.getSummary().getTotalTaskCount());
        Assert.assertNotNull(overview.getLatestTaskMetric());
        Assert.assertEquals("run-metric-001", overview.getLatestTaskMetric().getRunId());

        List<TaskRunMetricDO> taskMetrics = service.listTaskRunMetrics("run-metric-001", Long.valueOf(101L), null, null, 10);
        Assert.assertEquals(1, taskMetrics.size());
        Assert.assertEquals(Long.valueOf(120L), taskMetrics.get(0).getSuccessRowCount());

        List<TableRunMetricDO> tableMetrics = service.listTableRunMetrics("run-metric-001", Long.valueOf(101L), Long.valueOf(201L), null, null, 10);
        Assert.assertEquals(1, tableMetrics.size());
        Assert.assertEquals("orders", tableMetrics.get(0).getTableName());

        List<DatasourceConnectionMetricDO> datasourceMetrics = service.listDatasourceConnectionMetrics(Long.valueOf(301L), null, null, 10);
        Assert.assertEquals(1, datasourceMetrics.size());
        Assert.assertEquals("SUCCESS", datasourceMetrics.get(0).getConnectionStatus());

        MonitoringTrendVO trend = service.taskRunTrend("run-metric-001", Long.valueOf(101L), null, null, 10);
        Assert.assertEquals(1, trend.getTaskRunTrend().size());
        Assert.assertEquals(Long.valueOf(120L), trend.getTaskRunTrend().get(0).getSuccessRowCount());

        MonitoringCleanupSummaryVO cleanupSummary = service.cleanupMonitoringMetrics(Integer.valueOf(365));
        Assert.assertNotNull(cleanupSummary);
        Assert.assertEquals(Integer.valueOf(365), cleanupSummary.getRetentionDays());
    }
}
