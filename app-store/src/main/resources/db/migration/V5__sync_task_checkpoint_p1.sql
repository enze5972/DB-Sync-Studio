ALTER TABLE sync_task ADD COLUMN incremental_checkpoint_mode TEXT;
ALTER TABLE sync_task ADD COLUMN incremental_checkpoint_value TEXT;
ALTER TABLE sync_task ADD COLUMN incremental_checkpoint_updated_at INTEGER;
