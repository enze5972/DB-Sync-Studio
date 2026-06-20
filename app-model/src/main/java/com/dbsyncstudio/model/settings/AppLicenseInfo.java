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
public class AppLicenseInfo {

    private AppLicenseStatus status;
    private String maskedLicenseKey;
    private String machineCode;
    private String licensedTo;
    private Long issuedAt;
    private Long expiresAt;
    private String message;
}
