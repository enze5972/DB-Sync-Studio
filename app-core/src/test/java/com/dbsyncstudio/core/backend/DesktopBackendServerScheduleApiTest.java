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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class DesktopBackendServerScheduleApiTest {

    @Test
    public void shouldPersistScheduleStateThroughHttpApi() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-schedule-api", ".sqlite");
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
                .taskName("schedule-http-task")
                .sourceDatasourceId(Long.valueOf(sourceDatasourceId))
                .targetDatasourceId(Long.valueOf(targetDatasourceId))
                .syncMode(SyncMode.FULL)
                .taskStatus(SyncTaskStatus.PENDING)
                .sourceTableName("source_table")
                .targetTableName("target_table")
                .scheduleEnabled(Boolean.FALSE)
                .scheduleType("MANUAL")
                .build();
        long taskId = taskRepository.save(task);

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

        DesktopBackendServer server = new DesktopBackendServer(service);
        int port = server.start(0);
        try {
            HttpResponse response = request(
                    "http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/schedule",
                    "PUT",
                    "{\"enabled\":true,\"scheduleType\":\"CRON\",\"cronExpression\":\"0 0 * * *\",\"intervalSeconds\":null}");
            Assert.assertEquals(200, response.statusCode);

            HttpResponse historyResponse = request(
                    "http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/schedule/history",
                    "GET",
                    null);
            Assert.assertEquals(200, historyResponse.statusCode);

            Optional<SyncTask> reloaded = service.findTaskById(taskId);
            Assert.assertTrue(reloaded.isPresent());
            Assert.assertTrue(Boolean.TRUE.equals(reloaded.get().getScheduleEnabled()));
            Assert.assertEquals("CRON", reloaded.get().getScheduleType());
            Assert.assertEquals("0 0 * * *", reloaded.get().getScheduleCronExpression());
            Assert.assertNull(reloaded.get().getScheduleIntervalSeconds());
            Assert.assertNotNull(reloaded.get().getScheduleNextRunAt());
        } finally {
            server.stop();
        }
    }

    private DatasourceConfig createDatasource(String name) {
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
}
