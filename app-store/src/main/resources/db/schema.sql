CREATE TABLE IF NOT EXISTS datasource_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL,
    host TEXT NOT NULL,
    port INTEGER NOT NULL,
    database_name TEXT NOT NULL,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    remark TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_task (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_name TEXT NOT NULL,
    source_datasource_id INTEGER NOT NULL,
    target_datasource_id INTEGER NOT NULL,
    sync_mode TEXT NOT NULL,
    task_status TEXT NOT NULL,
    source_schema_name TEXT,
    source_table_name TEXT NOT NULL,
    target_schema_name TEXT,
    target_table_name TEXT NOT NULL,
    incremental_mode TEXT,
    incremental_column_name TEXT,
    incremental_tie_breaker_column_name TEXT,
    incremental_composite_column_name TEXT,
    schedule_enabled INTEGER,
    schedule_type TEXT,
    schedule_cron_expression TEXT,
    schedule_interval_seconds INTEGER,
    schedule_last_run_at INTEGER,
    schedule_next_run_at INTEGER,
    schedule_last_result TEXT,
    schedule_last_message TEXT,
    total_row_count INTEGER,
    synced_row_count INTEGER,
    success_row_count INTEGER,
    failed_row_count INTEGER,
    speed_rows_per_second REAL,
    started_at INTEGER,
    ended_at INTEGER,
    duration_millis INTEGER,
    progress_message TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS field_mapping (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    source_schema_name TEXT,
    target_schema_name TEXT,
    source_table_name TEXT NOT NULL,
    target_table_name TEXT NOT NULL,
    source_column_name TEXT NOT NULL,
    target_column_name TEXT NOT NULL,
    ignored INTEGER NOT NULL DEFAULT 0,
    default_value TEXT,
    transform_rule TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_field_transform_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    table_task_id INTEGER,
    field_mapping_id INTEGER,
    source_field TEXT NOT NULL,
    target_field TEXT NOT NULL,
    transform_type TEXT NOT NULL,
    transform_config TEXT,
    transform_order INTEGER NOT NULL DEFAULT 0,
    enabled INTEGER NOT NULL DEFAULT 1,
    on_error TEXT NOT NULL DEFAULT 'FAIL',
    default_value TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS execution_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    log_level TEXT NOT NULL,
    log_message TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sql_execution_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    datasource_id INTEGER NOT NULL,
    sql_text TEXT NOT NULL,
    statement_type TEXT NOT NULL,
    success INTEGER NOT NULL,
    affected_rows INTEGER,
    elapsed_millis INTEGER,
    error_message TEXT,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_checkpoint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    checkpoint_key TEXT NOT NULL UNIQUE,
    checkpoint_value TEXT,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS incremental_sync_checkpoint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL UNIQUE,
    checkpoint_mode TEXT NOT NULL,
    checkpoint_value TEXT,
    checkpoint_tie_breaker_value TEXT,
    checkpoint_composite_value TEXT,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_comparison_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_datasource_id INTEGER NOT NULL,
    target_datasource_id INTEGER NOT NULL,
    source_schema_name TEXT,
    source_table_name TEXT NOT NULL,
    target_schema_name TEXT,
    target_table_name TEXT NOT NULL,
    diff_summary TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_validation_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    run_id TEXT NOT NULL,
    validation_method TEXT NOT NULL,
    source_table_name TEXT NOT NULL,
    target_table_name TEXT NOT NULL,
    where_clause TEXT,
    incremental_condition TEXT,
    source_row_count INTEGER,
    target_row_count INTEGER,
    missing_count INTEGER,
    inconsistent_count INTEGER,
    sample_count INTEGER,
    status TEXT NOT NULL,
    error_message TEXT,
    started_at INTEGER,
    ended_at INTEGER,
    elapsed_millis INTEGER,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_validation_differences (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    validation_run_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    run_id TEXT NOT NULL,
    difference_type TEXT NOT NULL,
    primary_key_json TEXT,
    source_row_json TEXT,
    target_row_json TEXT,
    differing_columns_json TEXT,
    suggested_repair_type TEXT,
    status TEXT,
    error_message TEXT,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_repair_runs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    validation_run_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    run_id TEXT NOT NULL,
    table_name TEXT NOT NULL,
    repair_type TEXT NOT NULL,
    status TEXT NOT NULL,
    repair_count INTEGER,
    success_count INTEGER,
    failed_count INTEGER,
    start_time INTEGER,
    end_time INTEGER,
    error_message TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_repair_details (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    repair_run_id INTEGER NOT NULL,
    validation_difference_id INTEGER,
    task_id INTEGER NOT NULL,
    repair_type TEXT NOT NULL,
    primary_key_json TEXT,
    sql_preview TEXT,
    parameter_json TEXT,
    status TEXT,
    error_message TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_task_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    source_schema_name TEXT,
    source_table_name TEXT NOT NULL,
    target_schema_name TEXT,
    target_table_name TEXT NOT NULL,
    sync_mode TEXT,
    incremental_mode TEXT,
    incremental_column_name TEXT,
    incremental_tie_breaker_column_name TEXT,
    incremental_composite_column_name TEXT,
    batch_size INTEGER,
    table_order INTEGER,
    enabled INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    run_id TEXT NOT NULL,
    sync_mode TEXT NOT NULL,
    run_status TEXT NOT NULL,
    total_table_count INTEGER,
    completed_table_count INTEGER,
    total_row_count INTEGER,
    synced_row_count INTEGER,
    success_row_count INTEGER,
    failed_row_count INTEGER,
    speed_rows_per_second REAL,
    started_at INTEGER,
    ended_at INTEGER,
    duration_millis INTEGER,
    progress_message TEXT,
    error_message TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_table_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sync_run_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    run_id TEXT NOT NULL,
    task_table_id INTEGER,
    source_schema_name TEXT,
    source_table_name TEXT NOT NULL,
    target_schema_name TEXT,
    target_table_name TEXT NOT NULL,
    table_order INTEGER,
    table_status TEXT NOT NULL,
    total_row_count INTEGER,
    synced_row_count INTEGER,
    success_row_count INTEGER,
    failed_row_count INTEGER,
    speed_rows_per_second REAL,
    started_at INTEGER,
    ended_at INTEGER,
    duration_millis INTEGER,
    progress_message TEXT,
    error_message TEXT,
    checkpoint_value TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_run_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    sync_run_id INTEGER,
    sync_table_run_id INTEGER,
    run_id TEXT,
    table_name TEXT,
    log_level TEXT NOT NULL,
    log_message TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS task_run_metric (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id TEXT NOT NULL,
    task_id INTEGER NOT NULL,
    metric_time INTEGER NOT NULL,
    success_row_count INTEGER,
    failed_row_count INTEGER,
    speed_rows_per_second REAL,
    latency_millis INTEGER,
    duration_millis INTEGER,
    error_message TEXT,
    running_task_count INTEGER,
    today_task_count INTEGER,
    today_success_task_count INTEGER,
    today_failed_task_count INTEGER
);

CREATE TABLE IF NOT EXISTS table_run_metric (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    table_task_id INTEGER NOT NULL,
    task_id INTEGER NOT NULL,
    run_id TEXT NOT NULL,
    table_name TEXT NOT NULL,
    synced_row_count INTEGER,
    success_row_count INTEGER,
    failed_row_count INTEGER,
    speed_rows_per_second REAL,
    batch_count INTEGER,
    retry_count INTEGER,
    last_checkpoint TEXT,
    last_error TEXT,
    metric_time INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS datasource_connection_metric (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    datasource_id INTEGER NOT NULL,
    connection_status TEXT NOT NULL,
    last_success_time INTEGER,
    last_failure_time INTEGER,
    failure_reason TEXT,
    average_test_connection_millis REAL,
    last_test_connection_millis INTEGER,
    metric_time INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS alert_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_name TEXT NOT NULL,
    alert_type TEXT NOT NULL,
    task_id INTEGER,
    table_name TEXT,
    alert_level TEXT NOT NULL,
    alert_content_template TEXT NOT NULL,
    channel_ids_json TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    cooldown_seconds INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS alert_channels (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    channel_name TEXT NOT NULL,
    channel_type TEXT NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    smtp_host TEXT,
    smtp_port INTEGER,
    smtp_username TEXT,
    smtp_password_encrypted TEXT,
    smtp_to_address TEXT,
    smtp_from_address TEXT,
    webhook_url TEXT,
    webhook_token_encrypted TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS alert_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    alert_id TEXT NOT NULL,
    rule_id INTEGER,
    alert_type TEXT NOT NULL,
    task_id INTEGER,
    run_id TEXT,
    table_name TEXT,
    alert_level TEXT NOT NULL,
    alert_content TEXT NOT NULL,
    channel_type TEXT NOT NULL,
    channel_id INTEGER,
    send_status TEXT NOT NULL,
    error_message TEXT,
    created_time INTEGER NOT NULL,
    sent_time INTEGER
);

CREATE TABLE IF NOT EXISTS alert_dedup_state (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dedup_key TEXT NOT NULL UNIQUE,
    rule_id INTEGER,
    alert_type TEXT NOT NULL,
    task_id INTEGER,
    table_name TEXT,
    channel_type TEXT,
    channel_id INTEGER,
    last_alert_id TEXT,
    last_content_hash TEXT,
    last_sent_time INTEGER,
    cooldown_until INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_datasource_config_type ON datasource_config(type);
CREATE INDEX IF NOT EXISTS idx_sync_task_status ON sync_task(task_status);
CREATE INDEX IF NOT EXISTS idx_sync_task_table_task_id ON sync_task_table(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_task_id ON sync_run(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_run_id ON sync_run(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_table_run_sync_run_id ON sync_table_run(sync_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_table_run_task_id ON sync_table_run(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_table_run_run_id ON sync_table_run(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_task_id ON sync_run_log(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_run_id ON sync_run_log(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_sync_run_id ON sync_run_log(sync_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_sync_table_run_id ON sync_run_log(sync_table_run_id);
CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled ON alert_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_alert_rules_task_id ON alert_rules(task_id);
CREATE INDEX IF NOT EXISTS idx_alert_channels_enabled ON alert_channels(enabled);
CREATE INDEX IF NOT EXISTS idx_alert_history_alert_id ON alert_history(alert_id);
CREATE INDEX IF NOT EXISTS idx_alert_history_task_id ON alert_history(task_id);
CREATE INDEX IF NOT EXISTS idx_alert_history_run_id ON alert_history(run_id);
CREATE INDEX IF NOT EXISTS idx_alert_dedup_state_key ON alert_dedup_state(dedup_key);
CREATE INDEX IF NOT EXISTS idx_alert_dedup_state_cooldown_until ON alert_dedup_state(cooldown_until);
CREATE INDEX IF NOT EXISTS idx_execution_log_task_id ON execution_log(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_checkpoint_key ON sync_checkpoint(checkpoint_key);
CREATE INDEX IF NOT EXISTS idx_sql_execution_log_datasource_id ON sql_execution_log(datasource_id);
CREATE INDEX IF NOT EXISTS idx_schema_comparison_history_source ON schema_comparison_history(source_datasource_id);
CREATE INDEX IF NOT EXISTS idx_schema_comparison_history_target ON schema_comparison_history(target_datasource_id);
CREATE INDEX IF NOT EXISTS idx_incremental_sync_checkpoint_task_id ON incremental_sync_checkpoint(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_runs_task_id ON sync_validation_runs(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_runs_run_id ON sync_validation_runs(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_differences_run_id ON sync_validation_differences(validation_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_differences_task_id ON sync_validation_differences(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_runs_validation_run_id ON sync_repair_runs(validation_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_runs_task_id ON sync_repair_runs(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_details_run_id ON sync_repair_details(repair_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_details_difference_id ON sync_repair_details(validation_difference_id);
CREATE INDEX IF NOT EXISTS idx_sync_task_table_task_id ON sync_task_table(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_task_id ON sync_run(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_run_id ON sync_run(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_table_run_sync_run_id ON sync_table_run(sync_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_table_run_task_id ON sync_table_run(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_table_run_run_id ON sync_table_run(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_task_id ON sync_run_log(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_run_id ON sync_run_log(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_sync_run_id ON sync_run_log(sync_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_run_log_sync_table_run_id ON sync_run_log(sync_table_run_id);
