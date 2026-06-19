ALTER TABLE sync_task ADD COLUMN incremental_mode TEXT;
ALTER TABLE sync_task ADD COLUMN incremental_column_name TEXT;
ALTER TABLE sync_task ADD COLUMN incremental_tie_breaker_column_name TEXT;
ALTER TABLE sync_task ADD COLUMN incremental_composite_column_name TEXT;
ALTER TABLE sync_task ADD COLUMN schedule_enabled INTEGER;
ALTER TABLE sync_task ADD COLUMN schedule_type TEXT;
ALTER TABLE sync_task ADD COLUMN schedule_cron_expression TEXT;
ALTER TABLE sync_task ADD COLUMN schedule_interval_seconds INTEGER;
ALTER TABLE sync_task ADD COLUMN schedule_last_run_at INTEGER;
ALTER TABLE sync_task ADD COLUMN schedule_next_run_at INTEGER;
ALTER TABLE sync_task ADD COLUMN schedule_last_result TEXT;
ALTER TABLE sync_task ADD COLUMN schedule_last_message TEXT;

CREATE TABLE IF NOT EXISTS incremental_sync_checkpoint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id INTEGER NOT NULL UNIQUE,
    checkpoint_mode TEXT NOT NULL,
    checkpoint_value TEXT,
    checkpoint_tie_breaker_value TEXT,
    checkpoint_composite_value TEXT,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_incremental_sync_checkpoint_task_id ON incremental_sync_checkpoint(task_id);
