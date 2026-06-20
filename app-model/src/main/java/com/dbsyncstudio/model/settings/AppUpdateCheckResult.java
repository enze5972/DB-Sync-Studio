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
public class AppUpdateCheckResult {

    private boolean available;
    private boolean latest;
    private String currentVersion;
    private String latestVersion;
    private String releasedAt;
    private String releaseNotes;
    private String downloadUrl;
    private String message;
}
