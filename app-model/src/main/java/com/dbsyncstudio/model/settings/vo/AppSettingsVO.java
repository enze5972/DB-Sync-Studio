package com.dbsyncstudio.model.settings.vo;

import com.dbsyncstudio.model.settings.vo.AppBuildInfoVO;
import com.dbsyncstudio.model.settings.entity.AppSettingsDO;

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
public class AppSettingsVO {

    private AppBuildInfoVO buildInfo;
    private AppSettingsDO settings;
    private String applicationDirectory;
    private String logsDirectory;
    private String databaseFilePath;
    private int databaseUserVersion;
    private int migrationEntryCount;
    private int schemaVersion;
}
