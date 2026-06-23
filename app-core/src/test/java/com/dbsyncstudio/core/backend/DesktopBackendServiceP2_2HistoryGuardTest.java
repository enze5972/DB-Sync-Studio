package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO;
import com.dbsyncstudio.model.sync.vo.LogCleanupSummaryVO;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.entity.SyncRunDO;
import com.dbsyncstudio.model.sync.entity.SyncRunLogEntryDO;
import com.dbsyncstudio.model.sync.entity.SyncTableRunDO;
import com.dbsyncstudio.model.sync.entity.SyncTaskDO;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.FieldMappingRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.IncrementalSyncCheckpointRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SchemaComparisonHistoryRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SqlExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncRunLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncRunRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTableRunRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.ExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncCheckpointRepositoryImpl;

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

        List<SyncRunLogEntryDO> logs = service.listSyncRunLogs(Long.valueOf(fixtures.taskId), "run-001",
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

        LogCleanupSummaryVO summary = service.cleanupLogs(Integer.valueOf(1));

        Assert.assertNotNull(summary);
        Assert.assertTrue(summary.getExecutionLogDeletedCount().intValue() >= 0);
        List<ExecutionLogEntryDO> logs = service.listLogs(Long.valueOf(fixtures.taskId));
        Assert.assertFalse(logs.isEmpty());
    }

    private static final class Fixtures {
        private final DatasourceRepositoryImpl datasourceRepository;
        private final SyncTaskRepositoryImpl taskRepository;
        private final ExecutionLogRepositoryImpl executionLogRepository;
        private final SyncCheckpointRepositoryImpl syncCheckpointRepository;
        private final FieldMappingRepositoryImpl fieldMappingRepository;
        private final SqlExecutionLogRepositoryImpl sqlExecutionLogRepository;
        private final SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository;
        private final IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository;
        private final SyncRunRepositoryImpl syncRunRepository;
        private final SyncTableRunRepositoryImpl syncTableRunRepository;
        private final SyncRunLogRepositoryImpl syncRunLogRepository;
        private final long taskId;
        private final long syncRunId;
        private final long syncTableRunId;

        private Fixtures(DatasourceRepositoryImpl datasourceRepository,
                         SyncTaskRepositoryImpl taskRepository,
                         ExecutionLogRepositoryImpl executionLogRepository,
                         SyncCheckpointRepositoryImpl syncCheckpointRepository,
                         FieldMappingRepositoryImpl fieldMappingRepository,
                         SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                         SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                         IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
                         SyncRunRepositoryImpl syncRunRepository,
                         SyncTableRunRepositoryImpl syncTableRunRepository,
                         SyncRunLogRepositoryImpl syncRunLogRepository,
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

            DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
            DatasourceRepositoryImpl datasourceRepository = new DatasourceRepositoryImpl(connectionFactory);
            SyncTaskRepositoryImpl taskRepository = new SyncTaskRepositoryImpl(connectionFactory);
            ExecutionLogRepositoryImpl executionLogRepository = new ExecutionLogRepositoryImpl(connectionFactory);
            SyncCheckpointRepositoryImpl syncCheckpointRepository = new SyncCheckpointRepositoryImpl(connectionFactory);
            FieldMappingRepositoryImpl fieldMappingRepository = new FieldMappingRepositoryImpl(connectionFactory);
            SqlExecutionLogRepositoryImpl sqlExecutionLogRepository = new SqlExecutionLogRepositoryImpl(connectionFactory);
            SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository = new SchemaComparisonHistoryRepositoryImpl(connectionFactory);
            IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository = new IncrementalSyncCheckpointRepositoryImpl(connectionFactory);
            SyncRunRepositoryImpl syncRunRepository = new SyncRunRepositoryImpl(connectionFactory);
            SyncTableRunRepositoryImpl syncTableRunRepository = new SyncTableRunRepositoryImpl(connectionFactory);
            SyncRunLogRepositoryImpl syncRunLogRepository = new SyncRunLogRepositoryImpl(connectionFactory);

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
            SyncTaskDO task = SyncTaskDO.builder()
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
            SyncRunDO run = SyncRunDO.builder()
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

            SyncTableRunDO tableRun = SyncTableRunDO.builder()
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

            SyncRunLogEntryDO entry = SyncRunLogEntryDO.builder()
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

        private static DatasourceConfigDO createDatasource(String name) {
            DatasourceConfigDO datasourceConfig = new DatasourceConfigDO();
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
