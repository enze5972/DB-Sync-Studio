package com.dbsyncstudio.store.sqlite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseSchemaInitializer {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static final int CURRENT_SCHEMA_VERSION = 13;

    private DatabaseSchemaInitializer() {
    }

    public static void initialize(Connection connection) throws SQLException {
        String schemaSql = readSchemaSql();
        try (Statement statement = connection.createStatement()) {
            String[] statements = schemaSql.split(";");
            for (String rawStatement : statements) {
                String sql = rawStatement.trim();
                if (sql.length() > 0) {
                    statement.execute(sql);
                }
            }
        }
        ensureColumn(connection, "sync_task", "source_schema_name", "TEXT");
        ensureColumn(connection, "sync_task", "source_table_name", "TEXT");
        ensureColumn(connection, "sync_task", "target_schema_name", "TEXT");
        ensureColumn(connection, "sync_task", "target_table_name", "TEXT");
        ensureColumn(connection, "sync_task", "total_row_count", "INTEGER");
        ensureColumn(connection, "sync_task", "synced_row_count", "INTEGER");
        ensureColumn(connection, "sync_task", "success_row_count", "INTEGER");
        ensureColumn(connection, "sync_task", "failed_row_count", "INTEGER");
        ensureColumn(connection, "sync_task", "speed_rows_per_second", "REAL");
        ensureColumn(connection, "sync_task", "started_at", "INTEGER");
        ensureColumn(connection, "sync_task", "ended_at", "INTEGER");
        ensureColumn(connection, "sync_task", "duration_millis", "INTEGER");
        ensureColumn(connection, "sync_task", "progress_message", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_mode", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_column_name", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_tie_breaker_column_name", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_composite_column_name", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_checkpoint_mode", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_checkpoint_value", "TEXT");
        ensureColumn(connection, "sync_task", "incremental_checkpoint_updated_at", "INTEGER");
        ensureColumn(connection, "sync_task", "schedule_enabled", "INTEGER");
        ensureColumn(connection, "sync_task", "schedule_type", "TEXT");
        ensureColumn(connection, "sync_task", "schedule_cron_expression", "TEXT");
        ensureColumn(connection, "sync_task", "schedule_interval_seconds", "INTEGER");
        ensureColumn(connection, "sync_task", "schedule_last_run_at", "INTEGER");
        ensureColumn(connection, "sync_task", "schedule_next_run_at", "INTEGER");
        ensureColumn(connection, "sync_task", "schedule_last_result", "TEXT");
        ensureColumn(connection, "sync_task", "schedule_last_message", "TEXT");
        ensureColumn(connection, "field_mapping", "source_schema_name", "TEXT");
        ensureColumn(connection, "field_mapping", "target_schema_name", "TEXT");
        ensureColumn(connection, "sync_task_table", "sync_mode", "TEXT");
        ensureColumn(connection, "sync_task_table", "incremental_mode", "TEXT");
        ensureColumn(connection, "sync_task_table", "incremental_column_name", "TEXT");
        ensureColumn(connection, "sync_task_table", "incremental_tie_breaker_column_name", "TEXT");
        ensureColumn(connection, "sync_task_table", "incremental_composite_column_name", "TEXT");
        ensureColumn(connection, "sync_task_table", "batch_size", "INTEGER");
        ensureTable(connection, "sync_task_table", "CREATE TABLE IF NOT EXISTS sync_task_table (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER NOT NULL, " +
                "source_schema_name TEXT, " +
                "source_table_name TEXT NOT NULL, " +
                "target_schema_name TEXT, " +
                "target_table_name TEXT NOT NULL, " +
                "sync_mode TEXT, " +
                "incremental_mode TEXT, " +
                "incremental_column_name TEXT, " +
                "incremental_tie_breaker_column_name TEXT, " +
                "incremental_composite_column_name TEXT, " +
                "batch_size INTEGER, " +
                "table_order INTEGER, " +
                "enabled INTEGER, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureTable(connection, "sync_run", "CREATE TABLE IF NOT EXISTS sync_run (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER NOT NULL, " +
                "run_id TEXT NOT NULL, " +
                "sync_mode TEXT NOT NULL, " +
                "run_status TEXT NOT NULL, " +
                "total_table_count INTEGER, " +
                "completed_table_count INTEGER, " +
                "total_row_count INTEGER, " +
                "synced_row_count INTEGER, " +
                "success_row_count INTEGER, " +
                "failed_row_count INTEGER, " +
                "speed_rows_per_second REAL, " +
                "started_at INTEGER, " +
                "ended_at INTEGER, " +
                "duration_millis INTEGER, " +
                "progress_message TEXT, " +
                "error_message TEXT, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureTable(connection, "sync_table_run", "CREATE TABLE IF NOT EXISTS sync_table_run (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sync_run_id INTEGER NOT NULL, " +
                "task_id INTEGER NOT NULL, " +
                "run_id TEXT NOT NULL, " +
                "task_table_id INTEGER, " +
                "source_schema_name TEXT, " +
                "source_table_name TEXT NOT NULL, " +
                "target_schema_name TEXT, " +
                "target_table_name TEXT NOT NULL, " +
                "table_order INTEGER, " +
                "table_status TEXT NOT NULL, " +
                "total_row_count INTEGER, " +
                "synced_row_count INTEGER, " +
                "success_row_count INTEGER, " +
                "failed_row_count INTEGER, " +
                "speed_rows_per_second REAL, " +
                "started_at INTEGER, " +
                "ended_at INTEGER, " +
                "duration_millis INTEGER, " +
                "progress_message TEXT, " +
                "error_message TEXT, " +
                "checkpoint_value TEXT, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureTable(connection, "sync_run_log", "CREATE TABLE IF NOT EXISTS sync_run_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id INTEGER NOT NULL, " +
                "sync_run_id INTEGER, " +
                "sync_table_run_id INTEGER, " +
                "run_id TEXT, " +
                "table_name TEXT, " +
                "log_level TEXT NOT NULL, " +
                "log_message TEXT NOT NULL, " +
                "created_at INTEGER NOT NULL" +
                ")");
        ensureIndex(connection, "idx_sync_task_table_task_id", "sync_task_table", "task_id");
        ensureIndex(connection, "idx_sync_field_transform_rules_task_id", "sync_field_transform_rules", "task_id");
        ensureIndex(connection, "idx_sync_field_transform_rules_table_task_id", "sync_field_transform_rules", "table_task_id");
        ensureIndex(connection, "idx_sync_field_transform_rules_field_mapping_id", "sync_field_transform_rules", "field_mapping_id");
        ensureIndex(connection, "idx_sync_field_transform_rules_enabled", "sync_field_transform_rules", "enabled");
        ensureCompositeIndex(connection, "idx_sync_field_transform_rules_order", "sync_field_transform_rules",
                "task_id", "table_task_id", "field_mapping_id", "transform_order");
        ensureIndex(connection, "idx_sync_run_task_id", "sync_run", "task_id");
        ensureIndex(connection, "idx_sync_run_run_id", "sync_run", "run_id");
        ensureIndex(connection, "idx_sync_table_run_sync_run_id", "sync_table_run", "sync_run_id");
        ensureIndex(connection, "idx_sync_table_run_task_id", "sync_table_run", "task_id");
        ensureIndex(connection, "idx_sync_table_run_run_id", "sync_table_run", "run_id");
        ensureIndex(connection, "idx_sync_run_log_task_id", "sync_run_log", "task_id");
        ensureIndex(connection, "idx_sync_run_log_run_id", "sync_run_log", "run_id");
        ensureIndex(connection, "idx_sync_run_log_sync_run_id", "sync_run_log", "sync_run_id");
        ensureIndex(connection, "idx_sync_run_log_sync_table_run_id", "sync_run_log", "sync_table_run_id");
        ensureTable(connection, "task_run_metric", "CREATE TABLE IF NOT EXISTS task_run_metric (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "run_id TEXT NOT NULL, " +
                "task_id INTEGER NOT NULL, " +
                "metric_time INTEGER NOT NULL, " +
                "success_row_count INTEGER, " +
                "failed_row_count INTEGER, " +
                "speed_rows_per_second REAL, " +
                "latency_millis INTEGER, " +
                "duration_millis INTEGER, " +
                "error_message TEXT, " +
                "running_task_count INTEGER, " +
                "today_task_count INTEGER, " +
                "today_success_task_count INTEGER, " +
                "today_failed_task_count INTEGER" +
                ")");
        ensureTable(connection, "table_run_metric", "CREATE TABLE IF NOT EXISTS table_run_metric (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "table_task_id INTEGER NOT NULL, " +
                "task_id INTEGER NOT NULL, " +
                "run_id TEXT NOT NULL, " +
                "table_name TEXT NOT NULL, " +
                "synced_row_count INTEGER, " +
                "success_row_count INTEGER, " +
                "failed_row_count INTEGER, " +
                "speed_rows_per_second REAL, " +
                "batch_count INTEGER, " +
                "retry_count INTEGER, " +
                "last_checkpoint TEXT, " +
                "last_error TEXT, " +
                "metric_time INTEGER NOT NULL" +
                ")");
        ensureTable(connection, "datasource_connection_metric", "CREATE TABLE IF NOT EXISTS datasource_connection_metric (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "datasource_id INTEGER NOT NULL, " +
                "connection_status TEXT NOT NULL, " +
                "last_success_time INTEGER, " +
                "last_failure_time INTEGER, " +
                "failure_reason TEXT, " +
                "average_test_connection_millis REAL, " +
                "last_test_connection_millis INTEGER, " +
                "metric_time INTEGER NOT NULL" +
                ")");
        ensureIndex(connection, "idx_task_run_metric_run_id", "task_run_metric", "run_id");
        ensureIndex(connection, "idx_task_run_metric_task_id", "task_run_metric", "task_id");
        ensureIndex(connection, "idx_task_run_metric_metric_time", "task_run_metric", "metric_time");
        ensureIndex(connection, "idx_table_run_metric_run_id", "table_run_metric", "run_id");
        ensureIndex(connection, "idx_table_run_metric_task_id", "table_run_metric", "task_id");
        ensureIndex(connection, "idx_table_run_metric_metric_time", "table_run_metric", "metric_time");
        ensureIndex(connection, "idx_datasource_connection_metric_datasource_id", "datasource_connection_metric", "datasource_id");
        ensureIndex(connection, "idx_datasource_connection_metric_metric_time", "datasource_connection_metric", "metric_time");
        ensureTable(connection, "alert_rules", "CREATE TABLE IF NOT EXISTS alert_rules (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "rule_name TEXT NOT NULL, " +
                "alert_type TEXT NOT NULL, " +
                "task_id INTEGER, " +
                "table_name TEXT, " +
                "alert_level TEXT NOT NULL, " +
                "alert_content_template TEXT NOT NULL, " +
                "channel_ids_json TEXT, " +
                "enabled INTEGER NOT NULL DEFAULT 1, " +
                "cooldown_seconds INTEGER, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureTable(connection, "alert_channels", "CREATE TABLE IF NOT EXISTS alert_channels (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "channel_name TEXT NOT NULL, " +
                "channel_type TEXT NOT NULL, " +
                "enabled INTEGER NOT NULL DEFAULT 1, " +
                "smtp_host TEXT, " +
                "smtp_port INTEGER, " +
                "smtp_username TEXT, " +
                "smtp_password_encrypted TEXT, " +
                "smtp_to_address TEXT, " +
                "smtp_from_address TEXT, " +
                "webhook_url TEXT, " +
                "webhook_token_encrypted TEXT, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureTable(connection, "alert_history", "CREATE TABLE IF NOT EXISTS alert_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "alert_id TEXT NOT NULL, " +
                "rule_id INTEGER, " +
                "alert_type TEXT NOT NULL, " +
                "task_id INTEGER, " +
                "run_id TEXT, " +
                "table_name TEXT, " +
                "alert_level TEXT NOT NULL, " +
                "alert_content TEXT NOT NULL, " +
                "channel_type TEXT NOT NULL, " +
                "channel_id INTEGER, " +
                "send_status TEXT NOT NULL, " +
                "error_message TEXT, " +
                "created_time INTEGER NOT NULL, " +
                "sent_time INTEGER" +
                ")");
        ensureTable(connection, "alert_dedup_state", "CREATE TABLE IF NOT EXISTS alert_dedup_state (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "dedup_key TEXT NOT NULL UNIQUE, " +
                "rule_id INTEGER, " +
                "alert_type TEXT NOT NULL, " +
                "task_id INTEGER, " +
                "table_name TEXT, " +
                "channel_type TEXT, " +
                "channel_id INTEGER, " +
                "last_alert_id TEXT, " +
                "last_content_hash TEXT, " +
                "last_sent_time INTEGER, " +
                "cooldown_until INTEGER, " +
                "created_at INTEGER NOT NULL, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureIndex(connection, "idx_alert_rules_enabled", "alert_rules", "enabled");
        ensureIndex(connection, "idx_alert_rules_task_id", "alert_rules", "task_id");
        ensureIndex(connection, "idx_alert_channels_enabled", "alert_channels", "enabled");
        ensureIndex(connection, "idx_alert_history_alert_id", "alert_history", "alert_id");
        ensureIndex(connection, "idx_alert_history_task_id", "alert_history", "task_id");
        ensureIndex(connection, "idx_alert_history_run_id", "alert_history", "run_id");
        ensureIndex(connection, "idx_alert_dedup_state_key", "alert_dedup_state", "dedup_key");
        ensureIndex(connection, "idx_alert_dedup_state_cooldown_until", "alert_dedup_state", "cooldown_until");
        ensureTable(connection, "app_setting", "CREATE TABLE IF NOT EXISTS app_setting (" +
                "setting_key TEXT PRIMARY KEY, " +
                "setting_value TEXT, " +
                "updated_at INTEGER NOT NULL" +
                ")");
        ensureIndex(connection, "idx_app_setting_updated_at", "app_setting", "updated_at");
        ensureTable(connection, "schema_migration_entry", "CREATE TABLE IF NOT EXISTS schema_migration_entry (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "migration_version INTEGER NOT NULL UNIQUE, " +
                "migration_name TEXT NOT NULL, " +
                "migration_status TEXT NOT NULL, " +
                "applied_at INTEGER NOT NULL, " +
                "message TEXT" +
                ")");
        ensureIndex(connection, "idx_schema_migration_entry_version", "schema_migration_entry", "migration_version");
        ensureMigrationEntry(connection);
        setUserVersion(connection, CURRENT_SCHEMA_VERSION);
    }

    public static int currentSchemaVersion() {
        return CURRENT_SCHEMA_VERSION;
    }

    private static void ensureColumn(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        if (hasColumn(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private static boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                if (columnName.equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void ensureTable(Connection connection, String tableName, String createSql) throws SQLException {
        if (hasTable(connection, tableName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
    }

    private static void ensureIndex(Connection connection, String indexName, String tableName, String columnName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + "(" + columnName + ")");
        }
    }

    private static void ensureCompositeIndex(Connection connection, String indexName, String tableName, String... columnNames) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX IF NOT EXISTS ").append(indexName).append(" ON ").append(tableName).append("(");
        for (int i = 0; i < columnNames.length; i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(columnNames[i]);
        }
        sql.append(")");
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql.toString());
        }
    }

    private static void ensureMigrationEntry(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id FROM schema_migration_entry WHERE migration_version = " + CURRENT_SCHEMA_VERSION)) {
            if (resultSet.next()) {
                return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO schema_migration_entry (migration_version, migration_name, migration_status, applied_at, message) " +
                    "VALUES (" + CURRENT_SCHEMA_VERSION + ", 'V13__schema_baseline', 'APPLIED', " + System.currentTimeMillis() + ", 'SQLite schema initialized')");
        }
    }

    private static void setUserVersion(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = " + version);
        }
    }

    private static boolean hasTable(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '" + tableName + "'")) {
            return resultSet.next();
        }
    }

    private static String readSchemaSql() throws SQLException {
        InputStream inputStream = DatabaseSchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE);
        if (inputStream == null) {
            throw new SQLException("Schema resource not found: " + SCHEMA_RESOURCE);
        }

        try (InputStream source = inputStream;
             BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new SQLException("Failed to read SQLite schema", ex);
        }
    }
}
