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

CREATE INDEX IF NOT EXISTS idx_sync_field_transform_rules_task_id
    ON sync_field_transform_rules(task_id);
CREATE INDEX IF NOT EXISTS idx_sync_field_transform_rules_table_task_id
    ON sync_field_transform_rules(table_task_id);
CREATE INDEX IF NOT EXISTS idx_sync_field_transform_rules_field_mapping_id
    ON sync_field_transform_rules(field_mapping_id);
CREATE INDEX IF NOT EXISTS idx_sync_field_transform_rules_enabled
    ON sync_field_transform_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_sync_field_transform_rules_order
    ON sync_field_transform_rules(task_id, table_task_id, field_mapping_id, transform_order);
