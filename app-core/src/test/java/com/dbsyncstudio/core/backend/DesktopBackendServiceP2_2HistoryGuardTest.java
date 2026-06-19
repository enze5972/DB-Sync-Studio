package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.ExecutionLogEntry;
import com.dbsyncstudio.model.sync.LogCleanupSummary;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.SyncRun;
import com.dbsyncstudio.model.sync.SyncRunLogEntry;
import com.dbsyncstudio.model.sync.SyncTableRun;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.SqliteConnectionFactory;
import com.dbsyncstudio.store.sqlite.SqliteDatasourceRepository;
import com.dbsyncstudio.store.sqlite.SqliteFieldMappingRepository;
import com.dbsyncstudio.store.sqlite.SqliteIncrementalSyncCheckpointRepository;
import com.dbsyncstudio.store.sqlite.SqliteSchemaComparisonHistoryRepository;
import com.dbsyncstudio.store.sqlite.SqliteSqlExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncRunLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncRunRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTableRunRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTaskRepository;
import com.dbsyncstudio.store.sync.SqliteExecutionLogRepository;
import com.dbsyncstudio.store.sync.SqliteSyncCheckpointRepository;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DesktopBackendServiceP2_2HistoryGuardTest {

    @Test
    public void shouldFilterSyncRunLogsByTaskRunAndTableTogether() throws Exception {
        Fixtures fixtures = Fixtures.create("history-guard-filter");
        DesktopBackendService service = fixtures.createService();

        List<SyncRunLogEntry> logs = service.listSyncRunLogs(Long.valueOf(fixtures.taskId), "run-001",
                Long.valueOf(fixtures.syncRunId), Long.valueOf(fixtures.syncTableRunId),
                "customer", "INFO", "table completed", null, null, 20);

        Assert.assertEquals(1, logs.size());
        Assert.assertEquals("table completed", logs.get(0).getLogMessage());
    }

    @Test
    public void shouldKeepRunningTaskLogsDuringCleanup() throws Exception {
        Fixtures fixtures = Fixtures.create("history-guard-cleanup");
        DesktopBackendService service = fixtures.createService();
        service.startTask(fixtures.taskId);

        LogCleanupSummary summary = service.cleanupLogs(Integer.valueOf(1));

        Assert.assertNotNull(summary);
        Assert.assertTrue(summary.getExecutionLogDeletedCount().intValue() >= 0);
        List<ExecutionLogEntry> logs = service.listLogs(Long.valueOf(fixtures.taskId));
        Assert.assertFalse(logs.isEmpty());
    }

    private static final class Fixtures {
        private final SqliteDatasourceRepository datasourceRepository;
        private final SqliteSyncTaskRepository taskRepository;
        private final SqliteExecutionLogRepository executionLogRepository;
        private final SqliteSyncCheckpointRepository syncCheckpointRepository;
        private final SqliteFieldMappingRepository fieldMappingRepository;
        private final SqliteSqlExecutionLogRepository sqlExecutionLogRepository;
        private final SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository;
        private final SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository;
        private final SqliteSyncRunRepository syncRunRepository;
        private final SqliteSyncTableRunRepository syncTableRunRepository;
        private final SqliteSyncRunLogRepository syncRunLogRepository;
        private final long taskId;
        private final long syncRunId;
        private final long syncTableRunId;

        private Fixtures(SqliteDatasourceRepository datasourceRepository,
                         SqliteSyncTaskRepository taskRepository,
                         SqliteExecutionLogRepository executionLogRepository,
                         SqliteSyncCheckpointRepository syncCheckpointRepository,
                         SqliteFieldMappingRepository fieldMappingRepository,
                         SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                         SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                         SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                         SqliteSyncRunRepository syncRunRepository,
                         SqliteSyncTableRunRepository syncTableRunRepository,
                         SqliteSyncRunLogRepository syncRunLogRepository,
                         long taskId,
                         long syncRunId,
                         long syncTableRunId) {
            this.datasourceRepository = datasourceRepository;
            this.taskRepository = taskRepository;
            this.executionLogRepository = executionLogRepository;
            this.syncCheckpointRepository = syncCheckpointRepository;
            this.fieldMappingRepository = fieldMappingRepository;
            this.sqlExecutionLogRepository = sqlExecutionLogRepository;
            this.schemaComparisonHistoryRepository = schemaComparisonHistoryRepository;
            this.incrementalCheckpointRepository = incrementalCheckpointRepository;
            this.syncRunRepository = syncRunRepository;
            this.syncTableRunRepository = syncTableRunRepository;
            this.syncRunLogRepository = syncRunLogRepository;
            this.taskId = taskId;
            this.syncRunId = syncRunId;
            this.syncTableRunId = syncTableRunId;
        }

        private static Fixtures create(String prefix) throws Exception {
            File tempDatabase = File.createTempFile(prefix, ".sqlite");
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
            SqliteSyncRunRepository syncRunRepository = new SqliteSyncRunRepository(connectionFactory);
            SqliteSyncTableRunRepository syncTableRunRepository = new SqliteSyncTableRunRepository(connectionFactory);
            SqliteSyncRunLogRepository syncRunLogRepository = new SqliteSyncRunLogRepository(connectionFactory);

            datasourceRepository.initialize();
            taskRepository.initialize();
            executionLogRepository.initialize();
            syncCheckpointRepository.initialize();
            fieldMappingRepository.initialize();
            sqlExecutionLogRepository.initialize();
            schemaComparisonHistoryRepository.initialize();
            incrementalCheckpointRepository.initialize();
            syncRunRepository.initialize();
            syncTableRunRepository.initialize();
            syncRunLogRepository.initialize();

            long sourceDatasourceId = datasourceRepository.save(createDatasource("source-db"));
            long targetDatasourceId = datasourceRepository.save(createDatasource("target-db"));
            SyncTask task = SyncTask.builder()
                    .taskName(prefix + "-task")
                    .sourceDatasourceId(Long.valueOf(sourceDatasourceId))
                    .targetDatasourceId(Long.valueOf(targetDatasourceId))
                    .syncMode(SyncMode.FULL)
                    .taskStatus(SyncTaskStatus.PENDING)
                    .sourceTableName("customer")
                    .targetTableName("customer_copy")
                    .build();
            long taskId = taskRepository.save(task);

            long now = System.currentTimeMillis();
            SyncRun run = SyncRun.builder()
                    .taskId(Long.valueOf(taskId))
                    .runId("run-001")
                    .syncMode("FULL")
                    .runStatus("SUCCESS")
                    .totalTableCount(Integer.valueOf(1))
                    .completedTableCount(Integer.valueOf(1))
                    .totalRowCount(Long.valueOf(1L))
                    .syncedRowCount(Long.valueOf(1L))
                    .successRowCount(Long.valueOf(1L))
                    .failedRowCount(Long.valueOf(0L))
                    .startedAt(Long.valueOf(now))
                    .endedAt(Long.valueOf(now))
                    .durationMillis(Long.valueOf(1L))
                    .createdAt(Long.valueOf(now))
                    .updatedAt(Long.valueOf(now))
                    .build();
            long syncRunId = syncRunRepository.save(run);

            SyncTableRun tableRun = SyncTableRun.builder()
                    .syncRunId(Long.valueOf(syncRunId))
                    .taskId(Long.valueOf(taskId))
                    .runId("run-001")
                    .sourceTableName("customer")
                    .targetTableName("customer_copy")
                    .tableOrder(Integer.valueOf(1))
                    .tableStatus("SUCCESS")
                    .startedAt(Long.valueOf(now))
                    .endedAt(Long.valueOf(now))
                    .durationMillis(Long.valueOf(1L))
                    .createdAt(Long.valueOf(now))
                    .updatedAt(Long.valueOf(now))
                    .build();
            long syncTableRunId = syncTableRunRepository.save(tableRun);

            SyncRunLogEntry entry = SyncRunLogEntry.builder()
                    .taskId(Long.valueOf(taskId))
                    .syncRunId(Long.valueOf(syncRunId))
                    .syncTableRunId(Long.valueOf(syncTableRunId))
                    .runId("run-001")
                    .tableName("customer")
                    .logLevel("INFO")
                    .logMessage("table completed")
                    .createdAt(Long.valueOf(now))
                    .build();
            syncRunLogRepository.append(entry);

            return new Fixtures(datasourceRepository, taskRepository, executionLogRepository, syncCheckpointRepository,
                    fieldMappingRepository, sqlExecutionLogRepository, schemaComparisonHistoryRepository,
                    incrementalCheckpointRepository, syncRunRepository, syncTableRunRepository, syncRunLogRepository,
                    taskId, syncRunId, syncTableRunId);
        }

        private DesktopBackendService createService() {
            return new DesktopBackendService(
                    datasourceRepository,
                    taskRepository,
                    executionLogRepository,
                    syncCheckpointRepository,
                    fieldMappingRepository,
                    null,
                    syncRunRepository,
                    syncTableRunRepository,
                    syncRunLogRepository,
                    sqlExecutionLogRepository,
                    schemaComparisonHistoryRepository,
                    incrementalCheckpointRepository,
                    null,
                    null,
                    new JdbcDatabaseMetadataScanner(),
                    new JdbcDatasourceConnectionTester(),
                    new FieldMappingSuggestionMatcher(),
                    new SchemaComparisonEngine(),
                    null,
                    null,
                    new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), syncCheckpointRepository),
                    new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository));
        }

        private static DatasourceConfig createDatasource(String name) {
            DatasourceConfig datasourceConfig = new DatasourceConfig();
            datasourceConfig.setName(name);
            datasourceConfig.setType(DatasourceType.MYSQL);
            datasourceConfig.setHost("127.0.0.1");
            datasourceConfig.setPort(Integer.valueOf(3306));
            datasourceConfig.setDatabaseName(name.replace('-', '_'));
            datasourceConfig.setUsername("root");
            datasourceConfig.setPassword("secret");
            return datasourceConfig;
        }
    }
}
