package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.monitoring.vo.BackendDiagnosticsVO;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.entity.IncrementalSyncCheckpointEntryDO;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.entity.SyncTaskDO;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.FieldMappingRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.IncrementalSyncCheckpointRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SchemaComparisonHistoryRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SqlExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.ExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncCheckpointRepositoryImpl;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class DesktopBackendServiceScheduleTest {

    @Test
    public void shouldBackfillIncrementalCheckpointFromCheckpointStore() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-backend", ".sqlite");
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

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfigDO sourceDatasource = new DatasourceConfigDO();
        sourceDatasource.setName("source");
        sourceDatasource.setType(DatasourceType.MYSQL);
        sourceDatasource.setHost("127.0.0.1");
        sourceDatasource.setPort(Integer.valueOf(3306));
        sourceDatasource.setDatabaseName("source_db");
        sourceDatasource.setUsername("root");
        sourceDatasource.setPassword("secret");
        long sourceDatasourceId = datasourceRepository.save(sourceDatasource);

        DatasourceConfigDO targetDatasource = new DatasourceConfigDO();
        targetDatasource.setName("target");
        targetDatasource.setType(DatasourceType.MYSQL);
        targetDatasource.setHost("127.0.0.1");
        targetDatasource.setPort(Integer.valueOf(3306));
        targetDatasource.setDatabaseName("target_db");
        targetDatasource.setUsername("root");
        targetDatasource.setPassword("secret");
        long targetDatasourceId = datasourceRepository.save(targetDatasource);

        SyncTaskDO task = SyncTaskDO.builder()
                .taskName("incremental-task")
                .sourceDatasourceId(Long.valueOf(sourceDatasourceId))
                .targetDatasourceId(Long.valueOf(targetDatasourceId))
                .syncMode(SyncMode.INCREMENTAL)
                .taskStatus(SyncTaskStatus.PENDING)
                .sourceTableName("customer")
                .targetTableName("customer_copy")
                .incrementalMode(IncrementalSyncMode.TIMESTAMP)
                .incrementalCheckpointMode("STALE_MODE")
                .incrementalCheckpointValue("stale-value")
                .incrementalCheckpointUpdatedAt(Long.valueOf(11L))
                .build();
        long taskId = taskRepository.save(task);
        Assert.assertTrue(taskId > 0L);

        IncrementalSyncCheckpointEntryDO checkpointEntry = new IncrementalSyncCheckpointEntryDO();
        checkpointEntry.setTaskId(Long.valueOf(taskId));
        checkpointEntry.setCheckpointMode("TIMESTAMP");
        checkpointEntry.setCheckpointValue("2026-06-18T10:00:00");
        checkpointEntry.setUpdatedAt(Long.valueOf(123456789L));
        incrementalCheckpointRepository.save(checkpointEntry);

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                taskRepository,
                executionLogRepository,
                syncCheckpointRepository,
                fieldMappingRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                new JdbcDatabaseMetadataScanner(),
                new JdbcDatasourceConnectionTester(),
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), syncCheckpointRepository),
                new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository));

        Optional<SyncTaskDO> loadedTask = service.findTaskById(taskId);
        Assert.assertTrue(loadedTask.isPresent());
        assertCheckpointState(loadedTask.get(), "TIMESTAMP", "2026-06-18T10:00:00", Long.valueOf(123456789L));

        List<SyncTaskDO> allTasks = service.listTasks();
        Assert.assertEquals(1, allTasks.size());
        assertCheckpointState(allTasks.get(0), "TIMESTAMP", "2026-06-18T10:00:00", Long.valueOf(123456789L));

        SyncTaskDO scheduledTask = service.updateScheduleState(taskId, false, null, null, null);
        assertCheckpointState(scheduledTask, "TIMESTAMP", "2026-06-18T10:00:00", Long.valueOf(123456789L));
    }

    @Test
    public void shouldPersistScheduleStateAndFilterScheduleHistory() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-schedule-state", ".sqlite");
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

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfigDO sourceDatasource = new DatasourceConfigDO();
        sourceDatasource.setName("source");
        sourceDatasource.setType(DatasourceType.MYSQL);
        sourceDatasource.setHost("127.0.0.1");
        sourceDatasource.setPort(Integer.valueOf(3306));
        sourceDatasource.setDatabaseName("source_db");
        sourceDatasource.setUsername("root");
        sourceDatasource.setPassword("secret");
        long sourceDatasourceId = datasourceRepository.save(sourceDatasource);

        DatasourceConfigDO targetDatasource = new DatasourceConfigDO();
        targetDatasource.setName("target");
        targetDatasource.setType(DatasourceType.MYSQL);
        targetDatasource.setHost("127.0.0.1");
        targetDatasource.setPort(Integer.valueOf(3306));
        targetDatasource.setDatabaseName("target_db");
        targetDatasource.setUsername("root");
        targetDatasource.setPassword("secret");
        long targetDatasourceId = datasourceRepository.save(targetDatasource);

        SyncTaskDO task = SyncTaskDO.builder()
                .taskName("schedule-task")
                .sourceDatasourceId(Long.valueOf(sourceDatasourceId))
                .targetDatasourceId(Long.valueOf(targetDatasourceId))
                .syncMode(SyncMode.FULL)
                .taskStatus(SyncTaskStatus.PENDING)
                .sourceTableName("customer")
                .targetTableName("customer_copy")
                .build();
        long taskId = taskRepository.save(task);
        Assert.assertTrue(taskId > 0L);

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                taskRepository,
                executionLogRepository,
                syncCheckpointRepository,
                fieldMappingRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                new JdbcDatabaseMetadataScanner(),
                new JdbcDatasourceConnectionTester(),
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), syncCheckpointRepository),
                new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository));

        SyncTaskDO updated = service.updateScheduleState(taskId, true, "INTERVAL", null, Integer.valueOf(300));
        Assert.assertTrue(updated.getScheduleEnabled().booleanValue());
        Assert.assertEquals("INTERVAL", updated.getScheduleType());
        Assert.assertEquals(Integer.valueOf(300), updated.getScheduleIntervalSeconds());
        Assert.assertNotNull(updated.getScheduleNextRunAt());

        service.appendTaskLog(taskId, "INFO", "Scheduled execution started");
        service.appendTaskLog(taskId, "INFO", "Task finished successfully");
        service.appendTaskLog(taskId, "WARN", "Scheduled execution skipped because task is already running");

        List<com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO> scheduleHistory = service.listTaskScheduleHistory(taskId);
        Assert.assertEquals(2, scheduleHistory.size());
        Assert.assertTrue(scheduleHistory.get(0).getLogMessage().toLowerCase().contains("scheduled execution"));
    }

    @Test
    public void shouldRecoverUnfinishedTasksAndExposeDiagnostics() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-diagnostics", ".sqlite");
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

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfigDO sourceDatasource = new DatasourceConfigDO();
        sourceDatasource.setName("source");
        sourceDatasource.setType(DatasourceType.MYSQL);
        sourceDatasource.setHost("127.0.0.1");
        sourceDatasource.setPort(Integer.valueOf(3306));
        sourceDatasource.setDatabaseName("source_db");
        sourceDatasource.setUsername("root");
        sourceDatasource.setPassword("secret");
        long sourceDatasourceId = datasourceRepository.save(sourceDatasource);

        DatasourceConfigDO targetDatasource = new DatasourceConfigDO();
        targetDatasource.setName("target");
        targetDatasource.setType(DatasourceType.MYSQL);
        targetDatasource.setHost("127.0.0.1");
        targetDatasource.setPort(Integer.valueOf(3306));
        targetDatasource.setDatabaseName("target_db");
        targetDatasource.setUsername("root");
        targetDatasource.setPassword("secret");
        long targetDatasourceId = datasourceRepository.save(targetDatasource);

        SyncTaskDO task = SyncTaskDO.builder()
                .taskName("running-task")
                .sourceDatasourceId(Long.valueOf(sourceDatasourceId))
                .targetDatasourceId(Long.valueOf(targetDatasourceId))
                .syncMode(SyncMode.FULL)
                .taskStatus(SyncTaskStatus.RUNNING)
                .sourceTableName("customer")
                .targetTableName("customer_copy")
                .build();
        long taskId = taskRepository.save(task);
        Assert.assertTrue(taskId > 0L);

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                taskRepository,
                executionLogRepository,
                syncCheckpointRepository,
                fieldMappingRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                new JdbcDatabaseMetadataScanner(),
                new JdbcDatasourceConnectionTester(),
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), syncCheckpointRepository),
                new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository));

        int recovered = service.recoverUnfinishedTasks();
        Assert.assertEquals(1, recovered);

        SyncTaskDO recoveredTask = service.findTaskById(taskId).orElse(null);
        Assert.assertNotNull(recoveredTask);
        Assert.assertEquals(SyncTaskStatus.PENDING, recoveredTask.getTaskStatus());
        Assert.assertNotNull(recoveredTask.getEndedAt());

        BackendDiagnosticsVO diagnostics = service.diagnosticsStatus();
        Assert.assertNotNull(diagnostics);
        Assert.assertTrue(diagnostics.getGeneratedAt() > 0L);
        Assert.assertEquals(1, diagnostics.getTotalTaskCount());
        Assert.assertEquals(0, diagnostics.getRunningTaskCount());
        Assert.assertEquals(1, diagnostics.getRecoveredTaskCount());
        Assert.assertTrue(diagnostics.getDatabaseUserVersion() >= 0);
        Assert.assertTrue(diagnostics.getMigrationEntryCount() >= 0);
    }

    private void assertCheckpointState(SyncTaskDO task, String expectedMode, String expectedValue, Long expectedUpdatedAt) {
        Assert.assertEquals(expectedMode, task.getIncrementalCheckpointMode());
        Assert.assertEquals(expectedValue, task.getIncrementalCheckpointValue());
        Assert.assertEquals(expectedUpdatedAt, task.getIncrementalCheckpointUpdatedAt());
    }
}
