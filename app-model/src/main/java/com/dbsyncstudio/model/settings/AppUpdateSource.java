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
public class AppUpdateSource {

    private String latestVersion;
    private String releasedAt;
    private String releaseNotes;
    private String downloadUrl;
}
