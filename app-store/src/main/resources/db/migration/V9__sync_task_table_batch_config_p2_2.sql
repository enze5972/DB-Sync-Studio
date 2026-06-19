ALTER TABLE sync_task_table ADD COLUMN sync_mode TEXT;
ALTER TABLE sync_task_table ADD COLUMN incremental_mode TEXT;
ALTER TABLE sync_task_table ADD COLUMN incremental_column_name TEXT;
ALTER TABLE sync_task_table ADD COLUMN incremental_tie_breaker_column_name TEXT;
ALTER TABLE sync_task_table ADD COLUMN incremental_composite_column_name TEXT;
ALTER TABLE sync_task_table ADD COLUMN batch_size INTEGER;

CREATE INDEX IF NOT EXISTS idx_sync_task_table_task_id ON sync_task_table(task_id);
