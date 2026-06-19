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

CREATE INDEX IF NOT EXISTS idx_sql_execution_log_datasource_id ON sql_execution_log(datasource_id);
