package com.dbsyncstudio.model.monitoring.vo;

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
public class MonitoringCleanupSummaryVO {

    private Integer retentionDays;
    private Long cutoffTime;
    private Integer taskRunMetricDeletedCount;
    private Integer tableRunMetricDeletedCount;
    private Integer datasourceConnectionMetricDeletedCount;
}
