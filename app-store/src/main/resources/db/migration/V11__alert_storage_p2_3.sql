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

CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled ON alert_rules(enabled);
CREATE INDEX IF NOT EXISTS idx_alert_rules_task_id ON alert_rules(task_id);
CREATE INDEX IF NOT EXISTS idx_alert_channels_enabled ON alert_channels(enabled);
CREATE INDEX IF NOT EXISTS idx_alert_history_alert_id ON alert_history(alert_id);
CREATE INDEX IF NOT EXISTS idx_alert_history_task_id ON alert_history(task_id);
CREATE INDEX IF NOT EXISTS idx_alert_history_run_id ON alert_history(run_id);
CREATE INDEX IF NOT EXISTS idx_alert_dedup_state_key ON alert_dedup_state(dedup_key);
CREATE INDEX IF NOT EXISTS idx_alert_dedup_state_cooldown_until ON alert_dedup_state(cooldown_until);
