package com.dbsyncstudio.store.sqlite;

import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertTrue;

public class SqliteSchemaInitializerTest {

    @Test
    public void shouldMigrateMissingSyncTaskColumns() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-schema", ".sqlite");
        tempDatabase.deleteOnExit();

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS sync_task (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_name TEXT NOT NULL, " +
                    "source_datasource_id INTEGER NOT NULL, " +
                    "target_datasource_id INTEGER NOT NULL, " +
                    "sync_mode TEXT NOT NULL, " +
                    "task_status TEXT NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "updated_at INTEGER NOT NULL" +
                    ")");

            SqliteSchemaInitializer.initialize(connection);

            assertTrue(hasColumn(connection, "sync_task", "source_schema_name"));
            assertTrue(hasColumn(connection, "sync_task", "source_table_name"));
            assertTrue(hasColumn(connection, "sync_task", "target_schema_name"));
            assertTrue(hasColumn(connection, "sync_task", "target_table_name"));
            assertTrue(hasTable(connection, "sync_task_table"));
            assertTrue(hasTable(connection, "sync_run"));
            assertTrue(hasTable(connection, "sync_table_run"));
            assertTrue(hasTable(connection, "sync_run_log"));
            assertTrue(hasTable(connection, "task_run_metric"));
            assertTrue(hasTable(connection, "table_run_metric"));
            assertTrue(hasTable(connection, "datasource_connection_metric"));
            assertTrue(hasTable(connection, "alert_rules"));
            assertTrue(hasTable(connection, "alert_channels"));
            assertTrue(hasTable(connection, "alert_history"));
            assertTrue(hasTable(connection, "alert_dedup_state"));
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'")) {
            return resultSet.next();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
