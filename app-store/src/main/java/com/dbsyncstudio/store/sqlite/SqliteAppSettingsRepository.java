package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.settings.AppSettings;
import com.dbsyncstudio.model.settings.AppSettingsRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
public class SqliteAppSettingsRepository implements AppSettingsRepository {

    private static final String UPSERT_SQL =
            "INSERT INTO app_setting (setting_key, setting_value, updated_at) VALUES (?, ?, ?) " +
                    "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value, updated_at = excluded.updated_at";
    private static final String FIND_ALL_SQL =
            "SELECT setting_key, setting_value FROM app_setting";
    private static final String FIND_SQL =
            "SELECT setting_value FROM app_setting WHERE setting_key = ?";
    private static final String APP_SETTINGS_KEY = "app_settings";
    private static final String DEFAULT_UPDATE_SOURCE_URL = "";

    @NonNull
    private final SqliteConnectionFactory connectionFactory;

    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public AppSettings load() {
        Map<String, String> values = new LinkedHashMap<String, String>();
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                values.put(resultSet.getString("setting_key"), resultSet.getString("setting_value"));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load app settings", ex);
        }
        return AppSettings.builder()
                .logRetentionDays(parseInteger(values.get("log_retention_days")))
                .monitoringRetentionDays(parseInteger(values.get("monitoring_retention_days")))
                .defaultPageSize(parseInteger(values.get("default_page_size")))
                .defaultSyncBatchSize(parseInteger(values.get("default_sync_batch_size")))
                .defaultMaxConcurrency(parseInteger(values.get("default_max_concurrency")))
                .updateSourceUrl(defaultString(values.get("update_source_url"), DEFAULT_UPDATE_SOURCE_URL))
                .allowDangerousSql(parseBoolean(values.get("allow_dangerous_sql")))
                .restartScheduledTasksOnStartup(parseBoolean(values.get("restart_scheduled_tasks_on_startup")))
                .autoCheckUpdatesOnStartup(parseBoolean(values.get("auto_check_updates_on_startup")))
                .onboardingGuideEnabled(parseBoolean(values.get("onboarding_guide_enabled")))
                .build();
    }

    @Override
    public void save(AppSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("App settings must not be null");
        }
        long now = System.currentTimeMillis();
        try (Connection connection = connectionFactory.openConnection()) {
            upsert(connection, "log_retention_days", stringify(settings.getLogRetentionDays()), now);
            upsert(connection, "monitoring_retention_days", stringify(settings.getMonitoringRetentionDays()), now);
            upsert(connection, "default_page_size", stringify(settings.getDefaultPageSize()), now);
            upsert(connection, "default_sync_batch_size", stringify(settings.getDefaultSyncBatchSize()), now);
            upsert(connection, "default_max_concurrency", stringify(settings.getDefaultMaxConcurrency()), now);
            upsert(connection, "update_source_url", stringify(settings.getUpdateSourceUrl()), now);
            upsert(connection, "allow_dangerous_sql", stringify(settings.getAllowDangerousSql()), now);
            upsert(connection, "restart_scheduled_tasks_on_startup", stringify(settings.getRestartScheduledTasksOnStartup()), now);
            upsert(connection, "auto_check_updates_on_startup", stringify(settings.getAutoCheckUpdatesOnStartup()), now);
            upsert(connection, "onboarding_guide_enabled", stringify(settings.getOnboardingGuideEnabled()), now);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save app settings", ex);
        }
    }

    public String findRawValue(String settingKey) {
        if (settingKey == null || settingKey.trim().length() == 0) {
            return null;
        }
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_SQL)) {
            statement.setString(1, settingKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("setting_value");
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load app setting", ex);
        }
        return null;
    }

    public void saveRawValue(String settingKey, String settingValue) {
        if (settingKey == null || settingKey.trim().length() == 0) {
            throw new IllegalArgumentException("Setting key must not be blank");
        }
        long now = System.currentTimeMillis();
        try (Connection connection = connectionFactory.openConnection()) {
            upsert(connection, settingKey.trim(), settingValue, now);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to save app setting", ex);
        }
    }

    private void upsert(Connection connection, String key, String value, long updatedAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.setLong(3, updatedAt);
            statement.executeUpdate();
        }
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return Boolean.valueOf("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value.trim()));
    }

    private String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
