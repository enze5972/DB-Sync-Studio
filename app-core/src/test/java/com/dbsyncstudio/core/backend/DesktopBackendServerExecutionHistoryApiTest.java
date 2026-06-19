package com.dbsyncstudio.core.backend;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

public class DesktopBackendServerExecutionHistoryApiTest {

    @Test
    public void shouldExposeRunHistoryAndTaskTableApisThroughHttp() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-history-api", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DesktopBackendService service = DesktopBackendService.createDefault(tempDatabase);
        long taskId = seedTaskAndRunHistory(tempDatabase);

        DesktopBackendServer server = new DesktopBackendServer(service);
        int port = server.start(0);
        try {
            HttpResponse runsResponse = request("http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/runs", "GET", null);
            Assert.assertEquals(200, runsResponse.statusCode);
            Assert.assertTrue(runsResponse.body.contains("batch-001"));

            HttpResponse detailResponse = request("http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/runs/batch-001", "GET", null);
            Assert.assertEquals(200, detailResponse.statusCode);
            Assert.assertTrue(detailResponse.body.contains("SUCCESS"));
            Assert.assertTrue(detailResponse.body.contains("batch-001"));

            HttpResponse tablesResponse = request("http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/runs/batch-001/tables", "GET", null);
            Assert.assertEquals(200, tablesResponse.statusCode);
            Assert.assertTrue(tablesResponse.body.contains("customer"));

            HttpResponse logsResponse = request("http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/runs/batch-001/logs", "GET", null);
            Assert.assertEquals(200, logsResponse.statusCode);
            Assert.assertTrue(logsResponse.body.contains("table completed"));

            HttpResponse taskTablesResponse = request("http://127.0.0.1:" + port + "/api/tasks/" + taskId + "/tables", "GET", null);
            Assert.assertEquals(200, taskTablesResponse.statusCode);
            Assert.assertTrue(taskTablesResponse.body.contains("customer"));
        } finally {
            server.stop();
        }
    }

    private long seedTaskAndRunHistory(File databaseFile) throws Exception {
        try (Connection connection = openConnection(databaseFile)) {
            long now = System.currentTimeMillis();
            long sourceDatasourceId = insertDatasource(connection, "source-db", now);
            long targetDatasourceId = insertDatasource(connection, "target-db", now);
            long taskId = insertTask(connection, sourceDatasourceId, targetDatasourceId, now);
            long taskTableId = insertTaskTable(connection, taskId, now);
            long syncRunId = insertSyncRun(connection, taskId, now);
            long syncTableRunId = insertSyncTableRun(connection, syncRunId, taskId, taskTableId, now);
            insertSyncRunLog(connection, taskId, syncRunId, syncTableRunId, "batch-001", "customer", now);
            return taskId;
        }
    }

    private Connection openConnection(File databaseFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    private long insertDatasource(Connection connection, String name, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO datasource_config (name, type, host, port, database_name, username, password, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, name);
            statement.setString(2, "MYSQL");
            statement.setString(3, "127.0.0.1");
            statement.setInt(4, 3306);
            statement.setString(5, name.replace('-', '_'));
            statement.setString(6, "root");
            statement.setString(7, "secret");
            statement.setString(8, null);
            statement.setLong(9, now);
            statement.setLong(10, now);
            statement.executeUpdate();
        }
        return queryLastInsertRowId(connection);
    }

    private long insertTask(Connection connection, long sourceDatasourceId, long targetDatasourceId, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sync_task (task_name, source_datasource_id, target_datasource_id, sync_mode, task_status, source_schema_name, source_table_name, target_schema_name, target_table_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, "history-task");
            statement.setLong(2, sourceDatasourceId);
            statement.setLong(3, targetDatasourceId);
            statement.setString(4, "FULL");
            statement.setString(5, "SUCCESS");
            statement.setString(6, "source_schema");
            statement.setString(7, "customer");
            statement.setString(8, "target_schema");
            statement.setString(9, "customer_copy");
            statement.setLong(10, now);
            statement.setLong(11, now);
            statement.executeUpdate();
        }
        return queryLastInsertRowId(connection);
    }

    private long insertTaskTable(Connection connection, long taskId, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sync_task_table (task_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, enabled, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, taskId);
            statement.setString(2, "source_schema");
            statement.setString(3, "customer");
            statement.setString(4, "target_schema");
            statement.setString(5, "customer_copy");
            statement.setInt(6, 1);
            statement.setInt(7, 1);
            statement.setLong(8, now);
            statement.setLong(9, now);
            statement.executeUpdate();
        }
        return queryLastInsertRowId(connection);
    }

    private long insertSyncRun(Connection connection, long taskId, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sync_run (task_id, run_id, sync_mode, run_status, total_table_count, completed_table_count, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, taskId);
            statement.setString(2, "batch-001");
            statement.setString(3, "FULL");
            statement.setString(4, "SUCCESS");
            statement.setInt(5, 1);
            statement.setInt(6, 1);
            statement.setLong(7, 12L);
            statement.setLong(8, 12L);
            statement.setLong(9, 12L);
            statement.setLong(10, 0L);
            statement.setDouble(11, 12.5d);
            statement.setLong(12, now);
            statement.setLong(13, now + TimeUnit.SECONDS.toMillis(2));
            statement.setLong(14, TimeUnit.SECONDS.toMillis(2));
            statement.setString(15, "Batch sync completed");
            statement.setString(16, null);
            statement.setLong(17, now);
            statement.setLong(18, now);
            statement.executeUpdate();
        }
        return queryLastInsertRowId(connection);
    }

    private long insertSyncTableRun(Connection connection, long syncRunId, long taskId, long taskTableId, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sync_table_run (sync_run_id, task_id, run_id, task_table_id, source_schema_name, source_table_name, target_schema_name, target_table_name, table_order, table_status, total_row_count, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, started_at, ended_at, duration_millis, progress_message, error_message, checkpoint_value, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, syncRunId);
            statement.setLong(2, taskId);
            statement.setString(3, "batch-001");
            statement.setLong(4, taskTableId);
            statement.setString(5, "source_schema");
            statement.setString(6, "customer");
            statement.setString(7, "target_schema");
            statement.setString(8, "customer_copy");
            statement.setInt(9, 1);
            statement.setString(10, "SUCCESS");
            statement.setLong(11, 12L);
            statement.setLong(12, 12L);
            statement.setLong(13, 12L);
            statement.setLong(14, 0L);
            statement.setDouble(15, 12.5d);
            statement.setLong(16, now);
            statement.setLong(17, now + TimeUnit.SECONDS.toMillis(2));
            statement.setLong(18, TimeUnit.SECONDS.toMillis(2));
            statement.setString(19, "Table completed");
            statement.setString(20, null);
            statement.setString(21, "checkpoint-001");
            statement.setLong(22, now);
            statement.setLong(23, now);
            statement.executeUpdate();
        }
        return queryLastInsertRowId(connection);
    }

    private void insertSyncRunLog(Connection connection, long taskId, long syncRunId, long syncTableRunId,
                                  String runId, String tableName, long now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO sync_run_log (task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setLong(1, taskId);
            statement.setLong(2, syncRunId);
            statement.setLong(3, syncTableRunId);
            statement.setString(4, runId);
            statement.setString(5, tableName);
            statement.setString(6, "INFO");
            statement.setString(7, "table completed");
            statement.setLong(8, now);
            statement.executeUpdate();
        }
    }

    private long queryLastInsertRowId(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT last_insert_rowid()");
             java.sql.ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getLong(1);
            }
            throw new IllegalStateException("Failed to read last_insert_rowid");
        }
    }

    private HttpResponse request(String url, String method, String body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        if (body != null) {
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
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
