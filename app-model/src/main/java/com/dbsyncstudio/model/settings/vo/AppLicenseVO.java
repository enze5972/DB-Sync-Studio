package com.dbsyncstudio.model.settings.vo;

import com.dbsyncstudio.model.settings.vo.AppLicenseInfoVO;
import com.dbsyncstudio.model.settings.vo.AppUpdateCheckResultVO;

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
public class AppLicenseVO {

    private AppLicenseInfoVO license;
    private AppUpdateCheckResultVO update;
    private AppSettingsVO appSettings;
}
