package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.core.backend.BackendDiagnosticsResponse;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointEntry;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.SqliteConnectionFactory;
import com.dbsyncstudio.store.sqlite.SqliteDatasourceRepository;
import com.dbsyncstudio.store.sqlite.SqliteFieldMappingRepository;
import com.dbsyncstudio.store.sqlite.SqliteIncrementalSyncCheckpointRepository;
import com.dbsyncstudio.store.sqlite.SqliteSchemaComparisonHistoryRepository;
import com.dbsyncstudio.store.sqlite.SqliteSqlExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTaskRepository;
import com.dbsyncstudio.store.sync.SqliteExecutionLogRepository;
import com.dbsyncstudio.store.sync.SqliteSyncCheckpointRepository;

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

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteDatasourceRepository datasourceRepository = new SqliteDatasourceRepository(connectionFactory);
        SqliteSyncTaskRepository taskRepository = new SqliteSyncTaskRepository(connectionFactory);
        SqliteExecutionLogRepository executionLogRepository = new SqliteExecutionLogRepository(connectionFactory);
        SqliteSyncCheckpointRepository syncCheckpointRepository = new SqliteSyncCheckpointRepository(connectionFactory);
        SqliteFieldMappingRepository fieldMappingRepository = new SqliteFieldMappingRepository(connectionFactory);
        SqliteSqlExecutionLogRepository sqlExecutionLogRepository = new SqliteSqlExecutionLogRepository(connectionFactory);
        SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository = new SqliteSchemaComparisonHistoryRepository(connectionFactory);
        SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository = new SqliteIncrementalSyncCheckpointRepository(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfig sourceDatasource = new DatasourceConfig();
        sourceDatasource.setName("source");
        sourceDatasource.setType(DatasourceType.MYSQL);
        sourceDatasource.setHost("127.0.0.1");
        sourceDatasource.setPort(Integer.valueOf(3306));
        sourceDatasource.setDatabaseName("source_db");
        sourceDatasource.setUsername("root");
        sourceDatasource.setPassword("secret");
        long sourceDatasourceId = datasourceRepository.save(sourceDatasource);

        DatasourceConfig targetDatasource = new DatasourceConfig();
        targetDatasource.setName("target");
        targetDatasource.setType(DatasourceType.MYSQL);
        targetDatasource.setHost("127.0.0.1");
        targetDatasource.setPort(Integer.valueOf(3306));
        targetDatasource.setDatabaseName("target_db");
        targetDatasource.setUsername("root");
        targetDatasource.setPassword("secret");
        long targetDatasourceId = datasourceRepository.save(targetDatasource);

        SyncTask task = SyncTask.builder()
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

        IncrementalSyncCheckpointEntry checkpointEntry = new IncrementalSyncCheckpointEntry();
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

        Optional<SyncTask> loadedTask = service.findTaskById(taskId);
        Assert.assertTrue(loadedTask.isPresent());
        assertCheckpointState(loadedTask.get(), "TIMESTAMP", "2026-06-18T10:00:00", Long.valueOf(123456789L));

        List<SyncTask> allTasks = service.listTasks();
        Assert.assertEquals(1, allTasks.size());
        assertCheckpointState(allTasks.get(0), "TIMESTAMP", "2026-06-18T10:00:00", Long.valueOf(123456789L));

        SyncTask scheduledTask = service.updateScheduleState(taskId, false, null, null, null);
        assertCheckpointState(scheduledTask, "TIMESTAMP", "2026-06-18T10:00:00", Long.valueOf(123456789L));
    }

    @Test
    public void shouldPersistScheduleStateAndFilterScheduleHistory() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-schedule-state", ".sqlite");
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

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfig sourceDatasource = new DatasourceConfig();
        sourceDatasource.setName("source");
        sourceDatasource.setType(DatasourceType.MYSQL);
        sourceDatasource.setHost("127.0.0.1");
        sourceDatasource.setPort(Integer.valueOf(3306));
        sourceDatasource.setDatabaseName("source_db");
        sourceDatasource.setUsername("root");
        sourceDatasource.setPassword("secret");
        long sourceDatasourceId = datasourceRepository.save(sourceDatasource);

        DatasourceConfig targetDatasource = new DatasourceConfig();
        targetDatasource.setName("target");
        targetDatasource.setType(DatasourceType.MYSQL);
        targetDatasource.setHost("127.0.0.1");
        targetDatasource.setPort(Integer.valueOf(3306));
        targetDatasource.setDatabaseName("target_db");
        targetDatasource.setUsername("root");
        targetDatasource.setPassword("secret");
        long targetDatasourceId = datasourceRepository.save(targetDatasource);

        SyncTask task = SyncTask.builder()
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

        SyncTask updated = service.updateScheduleState(taskId, true, "INTERVAL", null, Integer.valueOf(300));
        Assert.assertTrue(updated.getScheduleEnabled().booleanValue());
        Assert.assertEquals("INTERVAL", updated.getScheduleType());
        Assert.assertEquals(Integer.valueOf(300), updated.getScheduleIntervalSeconds());
        Assert.assertNotNull(updated.getScheduleNextRunAt());

        service.appendTaskLog(taskId, "INFO", "Scheduled execution started");
        service.appendTaskLog(taskId, "INFO", "Task finished successfully");
        service.appendTaskLog(taskId, "WARN", "Scheduled execution skipped because task is already running");

        List<com.dbsyncstudio.model.sync.ExecutionLogEntry> scheduleHistory = service.listTaskScheduleHistory(taskId);
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

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteDatasourceRepository datasourceRepository = new SqliteDatasourceRepository(connectionFactory);
        SqliteSyncTaskRepository taskRepository = new SqliteSyncTaskRepository(connectionFactory);
        SqliteExecutionLogRepository executionLogRepository = new SqliteExecutionLogRepository(connectionFactory);
        SqliteSyncCheckpointRepository syncCheckpointRepository = new SqliteSyncCheckpointRepository(connectionFactory);
        SqliteFieldMappingRepository fieldMappingRepository = new SqliteFieldMappingRepository(connectionFactory);
        SqliteSqlExecutionLogRepository sqlExecutionLogRepository = new SqliteSqlExecutionLogRepository(connectionFactory);
        SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository = new SqliteSchemaComparisonHistoryRepository(connectionFactory);
        SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository = new SqliteIncrementalSyncCheckpointRepository(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfig sourceDatasource = new DatasourceConfig();
        sourceDatasource.setName("source");
        sourceDatasource.setType(DatasourceType.MYSQL);
        sourceDatasource.setHost("127.0.0.1");
        sourceDatasource.setPort(Integer.valueOf(3306));
        sourceDatasource.setDatabaseName("source_db");
        sourceDatasource.setUsername("root");
        sourceDatasource.setPassword("secret");
        long sourceDatasourceId = datasourceRepository.save(sourceDatasource);

        DatasourceConfig targetDatasource = new DatasourceConfig();
        targetDatasource.setName("target");
        targetDatasource.setType(DatasourceType.MYSQL);
        targetDatasource.setHost("127.0.0.1");
        targetDatasource.setPort(Integer.valueOf(3306));
        targetDatasource.setDatabaseName("target_db");
        targetDatasource.setUsername("root");
        targetDatasource.setPassword("secret");
        long targetDatasourceId = datasourceRepository.save(targetDatasource);

        SyncTask task = SyncTask.builder()
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

        SyncTask recoveredTask = service.findTaskById(taskId).orElse(null);
        Assert.assertNotNull(recoveredTask);
        Assert.assertEquals(SyncTaskStatus.PENDING, recoveredTask.getTaskStatus());
        Assert.assertNotNull(recoveredTask.getEndedAt());

        BackendDiagnosticsResponse diagnostics = service.diagnosticsStatus();
        Assert.assertNotNull(diagnostics);
        Assert.assertTrue(diagnostics.getGeneratedAt() > 0L);
        Assert.assertEquals(1, diagnostics.getTotalTaskCount());
        Assert.assertEquals(0, diagnostics.getRunningTaskCount());
        Assert.assertEquals(1, diagnostics.getRecoveredTaskCount());
        Assert.assertTrue(diagnostics.getDatabaseUserVersion() >= 0);
        Assert.assertTrue(diagnostics.getMigrationEntryCount() >= 0);
    }

    private void assertCheckpointState(SyncTask task, String expectedMode, String expectedValue, Long expectedUpdatedAt) {
        Assert.assertEquals(expectedMode, task.getIncrementalCheckpointMode());
        Assert.assertEquals(expectedValue, task.getIncrementalCheckpointValue());
        Assert.assertEquals(expectedUpdatedAt, task.getIncrementalCheckpointUpdatedAt());
    }
}
