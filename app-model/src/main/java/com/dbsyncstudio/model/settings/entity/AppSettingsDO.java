package com.dbsyncstudio.model.settings.entity;

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
public class AppSettingsDO {

    private Integer logRetentionDays;
    private Integer monitoringRetentionDays;
    private Integer defaultPageSize;
    private Integer defaultSyncBatchSize;
    private Integer defaultMaxConcurrency;
    private String updateSourceUrl;
    private Boolean allowDangerousSql;
    private Boolean restartScheduledTasksOnStartup;
    private Boolean autoCheckUpdatesOnStartup;
    private Boolean onboardingGuideEnabled;
}
