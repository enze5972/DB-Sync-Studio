CREATE TABLE IF NOT EXISTS app_setting (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_app_setting_updated_at ON app_setting(updated_at);
