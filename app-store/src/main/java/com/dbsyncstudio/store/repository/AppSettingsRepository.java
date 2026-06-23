package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.settings.entity.AppSettingsDO;

public interface AppSettingsRepository {

    AppSettingsDO load();

    void save(AppSettingsDO settings);

    String findRawValue(String settingKey);

    void saveRawValue(String settingKey, String settingValue);
}
