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

public class DesktopBackendServerMonitoringApiTest {

    @Test
    public void shouldExposeMonitoringApisThroughHttp() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-monitoring-api", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DesktopBackendService service = DesktopBackendService.createDefault(tempDatabase);
        seedMonitoringData(tempDatabase);

        DesktopBackendServer server = new DesktopBackendServer(service);
        int port = server.start(0);
        try {
            HttpResponse overview = request("http://127.0.0.1:" + port + "/api/monitoring/overview", "GET", null);
            Assert.assertEquals(200, overview.statusCode);
            Assert.assertTrue(overview.body.contains("\"summary\""));

            HttpResponse taskMetrics = request("http://127.0.0.1:" + port + "/api/monitoring/task-metrics?taskId=101", "GET", null);
            Assert.assertEquals(200, taskMetrics.statusCode);
            Assert.assertTrue(taskMetrics.body.contains("run-api-001"));

            HttpResponse datasourceMetrics = request("http://127.0.0.1:" + port + "/api/monitoring/datasource-metrics?datasourceId=301", "GET", null);
            Assert.assertEquals(200, datasourceMetrics.statusCode);
            Assert.assertTrue(datasourceMetrics.body.contains("SUCCESS"));
        } finally {
            server.stop();
        }
    }

    private void seedMonitoringData(File databaseFile) throws Exception {
        try (Connection connection = openConnection(databaseFile)) {
            long now = System.currentTimeMillis();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO task_run_metric (run_id, task_id, metric_time, success_row_count, failed_row_count, speed_rows_per_second, latency_millis, duration_millis, error_message, running_task_count, today_task_count, today_success_task_count, today_failed_task_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                statement.setString(1, "run-api-001");
                statement.setLong(2, 101L);
                statement.setLong(3, now);
                statement.setLong(4, 88L);
                statement.setLong(5, 1L);
                statement.setDouble(6, 22.0d);
                statement.setLong(7, 18L);
                statement.setLong(8, 4000L);
                statement.setString(9, "minor failure");
                statement.setInt(10, 1);
                statement.setInt(11, 1);
                statement.setInt(12, 0);
                statement.setInt(13, 1);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO datasource_connection_metric (datasource_id, connection_status, last_success_time, last_failure_time, failure_reason, average_test_connection_millis, last_test_connection_millis, metric_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                statement.setLong(1, 301L);
                statement.setString(2, "SUCCESS");
                statement.setLong(3, now);
                statement.setNull(4, java.sql.Types.BIGINT);
                statement.setString(5, null);
                statement.setDouble(6, 16.0d);
                statement.setLong(7, 16L);
                statement.setLong(8, now);
                statement.executeUpdate();
            }
        }
    }

    private Connection openConnection(File databaseFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
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
