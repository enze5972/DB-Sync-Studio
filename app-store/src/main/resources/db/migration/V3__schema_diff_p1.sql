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

CREATE INDEX IF NOT EXISTS idx_schema_comparison_history_source ON schema_comparison_history(source_datasource_id);
CREATE INDEX IF NOT EXISTS idx_schema_comparison_history_target ON schema_comparison_history(target_datasource_id);
