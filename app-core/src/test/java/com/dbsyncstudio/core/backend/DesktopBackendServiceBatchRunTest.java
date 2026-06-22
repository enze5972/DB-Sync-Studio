package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.core.sync.SyncTaskPausedException;
import com.dbsyncstudio.core.sync.SyncTaskProgressListener;
import com.dbsyncstudio.core.sync.SyncTaskStoppedException;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.FullSyncRequest;
import com.dbsyncstudio.model.sync.FullSyncResult;
import com.dbsyncstudio.model.sync.IncrementalSyncResult;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class DesktopBackendServiceBatchRunTest {

    @Test
    public void shouldRunBatchTablesForAllConfiguredTables() throws Exception {
        Fixtures fixtures = Fixtures.create("batch-order");
        TrackingFullSyncEngine fullSyncEngine = new TrackingFullSyncEngine();
        DesktopBackendService service = fixtures.createService(fullSyncEngine, new JdbcIncrementalSyncEngine(
                fixtures.executionLogRepository, fixtures.incrementalCheckpointRepository));

        TaskBatchRunRequest request = new TaskBatchRunRequest();
        request.setRunId("batch-run-001");
        List<TaskBatchTableRequest> tables = new ArrayList<TaskBatchTableRequest>();
        tables.add(table("customer", "customer_copy"));
        tables.add(table("orders", "orders_copy"));
        request.setTables(tables);

        service.runBatchTask(fixtures.taskId, request);
        Assert.assertTrue(fullSyncEngine.awaitCalls(2));
        Assert.assertTrue(awaitTaskStatus(fixtures, SyncTaskStatus.SUCCESS));
        Assert.assertTrue(fullSyncEngine.awaitCompleted());

        Assert.assertEquals(2, fullSyncEngine.getInvocations().size());
        Assert.assertTrue(containsInvocation(fullSyncEngine.getInvocations(), "customer"));
        Assert.assertTrue(containsInvocation(fullSyncEngine.getInvocations(), "orders"));
        Assert.assertTrue(containsCheckpointKey(fullSyncEngine.getInvocations(), "batch-run-001-1"));
        Assert.assertTrue(containsCheckpointKey(fullSyncEngine.getInvocations(), "batch-run-001-2"));
    }

    @Test
    public void shouldStopBatchBeforeSecondTable() throws Exception {
        Fixtures fixtures = Fixtures.create("batch-stop");
        BlockingFullSyncEngine fullSyncEngine = new BlockingFullSyncEngine();
        DesktopBackendService service = fixtures.createService(fullSyncEngine, new JdbcIncrementalSyncEngine(
                fixtures.executionLogRepository, fixtures.incrementalCheckpointRepository));

        TaskBatchRunRequest request = new TaskBatchRunRequest();
        request.setTables(singleTableList(table("customer", "customer_copy"), table("orders", "orders_copy")));

        service.runBatchTask(fixtures.taskId, request);
        Assert.assertTrue(fullSyncEngine.awaitFirstCall());
        service.stopTask(fixtures.taskId);
        fullSyncEngine.releaseFirstCall();
        Assert.assertTrue(fullSyncEngine.awaitFinished());
        Assert.assertTrue(awaitTaskStatus(fixtures, SyncTaskStatus.STOPPED));

        Assert.assertTrue(fullSyncEngine.getInvocations().size() >= 1);
        Assert.assertTrue(fullSyncEngine.getInvocations().size() <= 2);
    }

    private boolean containsInvocation(List<Invocation> invocations, String sourceTableName) {
        for (Invocation invocation : invocations) {
            if (invocation != null && sourceTableName.equals(invocation.sourceTableName)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsCheckpointKey(List<Invocation> invocations, String checkpointKey) {
        for (Invocation invocation : invocations) {
            if (invocation != null && checkpointKey.equals(invocation.checkpointKey)) {
                return true;
            }
        }
        return false;
    }

    private TaskBatchTableRequest table(String sourceTableName, String targetTableName) {
        TaskBatchTableRequest table = new TaskBatchTableRequest();
        table.setSourceTableName(sourceTableName);
        table.setTargetTableName(targetTableName);
        return table;
    }

    private List<TaskBatchTableRequest> singleTableList(TaskBatchTableRequest first, TaskBatchTableRequest second) {
        List<TaskBatchTableRequest> tables = new ArrayList<TaskBatchTableRequest>();
        tables.add(first);
        tables.add(second);
        return tables;
    }

    private static class Fixtures {

        private final SqliteDatasourceRepository datasourceRepository;
        private final SqliteSyncTaskRepository taskRepository;
        private final SqliteExecutionLogRepository executionLogRepository;
        private final SqliteSyncCheckpointRepository syncCheckpointRepository;
        private final SqliteFieldMappingRepository fieldMappingRepository;
        private final SqliteSqlExecutionLogRepository sqlExecutionLogRepository;
        private final SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository;
        private final SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository;
        private final long taskId;

        private Fixtures(SqliteDatasourceRepository datasourceRepository,
                         SqliteSyncTaskRepository taskRepository,
                         SqliteExecutionLogRepository executionLogRepository,
                         SqliteSyncCheckpointRepository syncCheckpointRepository,
                         SqliteFieldMappingRepository fieldMappingRepository,
                         SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                         SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                         SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                         long taskId) {
            this.datasourceRepository = datasourceRepository;
            this.taskRepository = taskRepository;
            this.executionLogRepository = executionLogRepository;
            this.syncCheckpointRepository = syncCheckpointRepository;
            this.fieldMappingRepository = fieldMappingRepository;
            this.sqlExecutionLogRepository = sqlExecutionLogRepository;
            this.schemaComparisonHistoryRepository = schemaComparisonHistoryRepository;
            this.incrementalCheckpointRepository = incrementalCheckpointRepository;
            this.taskId = taskId;
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

            datasourceRepository.initialize();
            taskRepository.initialize();
            executionLogRepository.initialize();
            syncCheckpointRepository.initialize();
            fieldMappingRepository.initialize();
            sqlExecutionLogRepository.initialize();
            schemaComparisonHistoryRepository.initialize();
            incrementalCheckpointRepository.initialize();

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
            return new Fixtures(datasourceRepository, taskRepository, executionLogRepository, syncCheckpointRepository,
                    fieldMappingRepository, sqlExecutionLogRepository, schemaComparisonHistoryRepository,
                    incrementalCheckpointRepository, taskId);
        }

        private DesktopBackendService createService(JdbcFullSyncEngine fullSyncEngine, JdbcIncrementalSyncEngine incrementalSyncEngine) {
            return new DesktopBackendService(
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
                    fullSyncEngine,
                    incrementalSyncEngine);
        }
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

    private boolean awaitTaskStatus(Fixtures fixtures, SyncTaskStatus status) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            Optional<SyncTask> task = fixtures.taskRepository.findById(fixtures.taskId);
            if (task.isPresent() && task.get().getTaskStatus() == status) {
                return true;
            }
            Thread.sleep(50L);
        }
        return false;
    }

    private static class Invocation {
        private final String sourceTableName;
        private final String targetTableName;
        private final String checkpointKey;

        private Invocation(String sourceTableName, String targetTableName, String checkpointKey) {
            this.sourceTableName = sourceTableName;
            this.targetTableName = targetTableName;
            this.checkpointKey = checkpointKey;
        }
    }

    private static class TrackingFullSyncEngine extends JdbcFullSyncEngine {
        private final List<Invocation> invocations = new CopyOnWriteArrayList<Invocation>();
        private final CountDownLatch callsLatch = new CountDownLatch(2);
        private final CountDownLatch completedLatch = new CountDownLatch(1);

        private TrackingFullSyncEngine() {
            super(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), null);
        }

        @Override
        public FullSyncResult sync(FullSyncRequest request, SyncTaskProgressListener progressListener) {
            invocations.add(new Invocation(request.getSourceTableName(), request.getTargetTableName(), request.getCheckpointKey()));
            callsLatch.countDown();
            try {
                return FullSyncResult.builder().success(true).sourceRowCount(1L).insertedRowCount(1L).build();
            } finally {
                completedLatch.countDown();
            }
        }

        private boolean awaitCalls(int expectedCalls) throws InterruptedException {
            boolean completed = callsLatch.await(2L, TimeUnit.SECONDS);
            return completed && invocations.size() >= expectedCalls;
        }

        private boolean awaitCompleted() throws InterruptedException {
            return completedLatch.await(2L, TimeUnit.SECONDS);
        }

        private List<Invocation> getInvocations() {
            return invocations;
        }
    }

    private static class BlockingFullSyncEngine extends JdbcFullSyncEngine {
        private final List<Invocation> invocations = new CopyOnWriteArrayList<Invocation>();
        private final CountDownLatch firstCallStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirstCall = new CountDownLatch(1);
        private final CountDownLatch finished = new CountDownLatch(1);

        private BlockingFullSyncEngine() {
            super(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), null);
        }

        @Override
        public FullSyncResult sync(FullSyncRequest request, SyncTaskProgressListener progressListener) {
            invocations.add(new Invocation(request.getSourceTableName(), request.getTargetTableName(), request.getCheckpointKey()));
            firstCallStarted.countDown();
            try {
                releaseFirstCall.await(2L, TimeUnit.SECONDS);
                if (progressListener != null && progressListener.isStopRequested()) {
                    throw new SyncTaskStoppedException("stopped");
                }
                if (progressListener != null && progressListener.isPauseRequested()) {
                    throw new SyncTaskPausedException("paused");
                }
                return FullSyncResult.builder().success(true).sourceRowCount(1L).insertedRowCount(1L).build();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            } finally {
                finished.countDown();
            }
        }

        private boolean awaitFirstCall() throws InterruptedException {
            return firstCallStarted.await(2L, TimeUnit.SECONDS);
        }

        private void releaseFirstCall() {
            releaseFirstCall.countDown();
        }

        private boolean awaitFinished() throws InterruptedException {
            return finished.await(2L, TimeUnit.SECONDS);
        }

        private List<Invocation> getInvocations() {
            return invocations;
        }
    }
}
