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

CREATE INDEX IF NOT EXISTS idx_task_run_metric_run_id ON task_run_metric(run_id);
CREATE INDEX IF NOT EXISTS idx_task_run_metric_task_id ON task_run_metric(task_id);
CREATE INDEX IF NOT EXISTS idx_task_run_metric_metric_time ON task_run_metric(metric_time);
CREATE INDEX IF NOT EXISTS idx_table_run_metric_run_id ON table_run_metric(run_id);
CREATE INDEX IF NOT EXISTS idx_table_run_metric_task_id ON table_run_metric(task_id);
CREATE INDEX IF NOT EXISTS idx_table_run_metric_metric_time ON table_run_metric(metric_time);
CREATE INDEX IF NOT EXISTS idx_datasource_connection_metric_datasource_id ON datasource_connection_metric(datasource_id);
CREATE INDEX IF NOT EXISTS idx_datasource_connection_metric_metric_time ON datasource_connection_metric(metric_time);
