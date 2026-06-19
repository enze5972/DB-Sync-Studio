CREATE TABLE IF NOT EXISTS sync_task_table (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL,
    source_schema_name TEXT,
    source_table_name TEXT NOT NULL,
    target_schema_name TEXT,
    target_table_name TEXT NOT NULL,
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
