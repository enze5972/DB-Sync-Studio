package com.dbsyncstudio.model.settings;

public interface AppSettingsRepository {

    AppSettings load();

    void save(AppSettings settings);

    String findRawValue(String settingKey);

    void saveRawValue(String settingKey, String settingValue);
}
