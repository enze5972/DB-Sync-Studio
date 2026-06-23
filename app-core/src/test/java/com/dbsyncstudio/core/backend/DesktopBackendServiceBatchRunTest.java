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
import com.dbsyncstudio.model.sync.dto.TaskBatchRunDTO;
import com.dbsyncstudio.model.sync.dto.TaskBatchTableDTO;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.dto.FullSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.FullSyncResultVO;
import com.dbsyncstudio.model.sync.vo.IncrementalSyncResultVO;
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

        TaskBatchRunDTO request = new TaskBatchRunDTO();
        request.setRunId("batch-run-001");
        List<TaskBatchTableDTO> tables = new ArrayList<TaskBatchTableDTO>();
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

        TaskBatchRunDTO request = new TaskBatchRunDTO();
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

    private TaskBatchTableDTO table(String sourceTableName, String targetTableName) {
        TaskBatchTableDTO table = new TaskBatchTableDTO();
        table.setSourceTableName(sourceTableName);
        table.setTargetTableName(targetTableName);
        return table;
    }

    private List<TaskBatchTableDTO> singleTableList(TaskBatchTableDTO first, TaskBatchTableDTO second) {
        List<TaskBatchTableDTO> tables = new ArrayList<TaskBatchTableDTO>();
        tables.add(first);
        tables.add(second);
        return tables;
    }

    private static class Fixtures {

        private final DatasourceRepositoryImpl datasourceRepository;
        private final SyncTaskRepositoryImpl taskRepository;
        private final ExecutionLogRepositoryImpl executionLogRepository;
        private final SyncCheckpointRepositoryImpl syncCheckpointRepository;
        private final FieldMappingRepositoryImpl fieldMappingRepository;
        private final SqlExecutionLogRepositoryImpl sqlExecutionLogRepository;
        private final SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository;
        private final IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository;
        private final long taskId;

        private Fixtures(DatasourceRepositoryImpl datasourceRepository,
                         SyncTaskRepositoryImpl taskRepository,
                         ExecutionLogRepositoryImpl executionLogRepository,
                         SyncCheckpointRepositoryImpl syncCheckpointRepository,
                         FieldMappingRepositoryImpl fieldMappingRepository,
                         SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                         SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                         IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
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

    private boolean awaitTaskStatus(Fixtures fixtures, SyncTaskStatus status) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            Optional<SyncTaskDO> task = fixtures.taskRepository.findById(fixtures.taskId);
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
        public FullSyncResultVO sync(FullSyncRequestDTO request, SyncTaskProgressListener progressListener) {
            invocations.add(new Invocation(request.getSourceTableName(), request.getTargetTableName(), request.getCheckpointKey()));
            callsLatch.countDown();
            try {
                return FullSyncResultVO.builder().success(true).sourceRowCount(1L).insertedRowCount(1L).build();
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
        public FullSyncResultVO sync(FullSyncRequestDTO request, SyncTaskProgressListener progressListener) {
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
                return FullSyncResultVO.builder().success(true).sourceRowCount(1L).insertedRowCount(1L).build();
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
