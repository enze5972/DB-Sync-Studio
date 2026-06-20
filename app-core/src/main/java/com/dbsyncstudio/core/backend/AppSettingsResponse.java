package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.settings.AppBuildInfo;
import com.dbsyncstudio.model.settings.AppSettings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppSettingsResponse {

    private AppBuildInfo buildInfo;
    private AppSettings settings;
    private String applicationDirectory;
    private String logsDirectory;
    private String databaseFilePath;
    private int databaseUserVersion;
    private int migrationEntryCount;
    private int schemaVersion;
}
