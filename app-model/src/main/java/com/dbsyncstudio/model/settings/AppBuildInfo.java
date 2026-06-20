package com.dbsyncstudio.model.settings;

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
public class AppBuildInfo {

    private String productName;
    private String appVersion;
    private String frontendVersion;
    private String javaCoreVersion;
    private String tauriVersion;
    private Integer sqliteSchemaVersion;
    private String buildTime;
    private String gitCommit;
}
