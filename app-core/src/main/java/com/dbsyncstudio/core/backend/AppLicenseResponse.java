package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.settings.AppLicenseInfo;
import com.dbsyncstudio.model.settings.AppUpdateCheckResult;

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
public class AppLicenseResponse {

    private AppLicenseInfo license;
    private AppUpdateCheckResult update;
    private AppSettingsResponse appSettings;
}
