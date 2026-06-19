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

CREATE INDEX IF NOT EXISTS idx_sync_validation_runs_task_id ON sync_validation_runs(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_runs_run_id ON sync_validation_runs(run_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_differences_run_id ON sync_validation_differences(validation_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_validation_differences_task_id ON sync_validation_differences(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_runs_validation_run_id ON sync_repair_runs(validation_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_runs_task_id ON sync_repair_runs(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_details_run_id ON sync_repair_details(repair_run_id);
CREATE INDEX IF NOT EXISTS idx_sync_repair_details_difference_id ON sync_repair_details(validation_difference_id);
