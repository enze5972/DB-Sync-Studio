package com.dbsyncstudio.model.settings.vo;
import com.dbsyncstudio.model.settings.AppLicenseStatus;

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
public class AppLicenseInfoVO {

    private AppLicenseStatus status;
    private String maskedLicenseKey;
    private String machineCode;
    private String licensedTo;
    private Long issuedAt;
    private Long expiresAt;
    private String message;
}
