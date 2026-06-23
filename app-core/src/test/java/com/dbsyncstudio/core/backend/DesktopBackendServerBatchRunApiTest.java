package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.core.sync.SyncTaskProgressListener;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.sync.dto.FullSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.FullSyncResultVO;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Optional;

public class DesktopBackendServerBatchRunApiTest {

    @Test
    public void shouldAcceptBatchRunRequestThroughHttpApi() throws Exception {
        Fixtures fixtures = Fixtures.create("batch-http");
        TrackingFullSyncEngine fullSyncEngine = new TrackingFullSyncEngine();
        DesktopBackendService service = fixtures.createService(fullSyncEngine, new JdbcIncrementalSyncEngine(
                fixtures.executionLogRepository, fixtures.incrementalCheckpointRepository));

        DesktopBackendServer server = new DesktopBackendServer(service);
        int port = server.start(0);
        try {
            HttpResponse response = request("http://127.0.0.1:" + port + "/api/tasks/" + fixtures.taskId + "/run-batch",
                    "POST",
                    "{\"runId\":\"batch-http-001\",\"tables\":[{\"sourceTableName\":\"customer\",\"targetTableName\":\"customer_copy\"},{\"sourceTableName\":\"orders\",\"targetTableName\":\"orders_copy\"}]}");
            Assert.assertEquals(200, response.statusCode);
            Assert.assertTrue(fullSyncEngine.awaitInvocations(2));

            Assert.assertEquals(2, fullSyncEngine.getInvocations().size());
            Assert.assertTrue(containsInvocation(fullSyncEngine.getInvocations(), "customer"));
            Assert.assertTrue(containsInvocation(fullSyncEngine.getInvocations(), "orders"));
            Assert.assertTrue(awaitTerminalTaskStatus(fixtures, SyncTaskStatus.SUCCESS, SyncTaskStatus.PARTIAL_SUCCESS));
        } finally {
            server.stop();
        }
    }

    private boolean awaitTerminalTaskStatus(Fixtures fixtures, SyncTaskStatus... statuses) throws Exception {
        long deadline = System.currentTimeMillis() + 2000L;
        while (System.currentTimeMillis() < deadline) {
            Optional<SyncTaskDO> task = fixtures.taskRepository.findById(fixtures.taskId);
            if (task.isPresent()) {
                SyncTaskStatus currentStatus = task.get().getTaskStatus();
                for (SyncTaskStatus status : statuses) {
                    if (currentStatus == status) {
                        return true;
                    }
                }
            }
            Thread.sleep(50L);
        }
        return false;
    }

    private HttpResponse request(String url, String method, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            OutputStream outputStream = connection.getOutputStream();
            try {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            } finally {
                outputStream.close();
            }
        }
        int statusCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection, statusCode >= 400);
        connection.disconnect();
        return new HttpResponse(statusCode, responseBody);
    }

    private String readResponseBody(HttpURLConnection connection, boolean errorStream) throws Exception {
        InputStream inputStream = errorStream ? connection.getErrorStream() : connection.getInputStream();
        if (inputStream == null) {
            return "";
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            inputStream.close();
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static class HttpResponse {
        private final int statusCode;
        private final String body;

        private HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
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
        private final CountDownLatch invocationsLatch = new CountDownLatch(2);

        private TrackingFullSyncEngine() {
            super(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), null);
        }

        @Override
        public FullSyncResultVO sync(FullSyncRequestDTO request, SyncTaskProgressListener progressListener) {
            invocations.add(new Invocation(request.getSourceTableName(), request.getTargetTableName(), request.getCheckpointKey()));
            invocationsLatch.countDown();
            return FullSyncResultVO.builder().success(true).sourceRowCount(1L).insertedRowCount(1L).build();
        }

        private boolean awaitInvocations(int expectedCalls) throws InterruptedException {
            boolean completed = invocationsLatch.await(2L, TimeUnit.SECONDS);
            return completed && invocations.size() >= expectedCalls;
        }

        private List<Invocation> getInvocations() {
            return invocations;
        }
    }

    private boolean containsInvocation(List<Invocation> invocations, String sourceTableName) {
        for (Invocation invocation : invocations) {
            if (invocation != null && sourceTableName.equals(invocation.sourceTableName)) {
                return true;
            }
        }
        return false;
    }
}
