package com.dbsyncstudio.model.monitoring;

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
public class TaskRunMetricSummary {

    private Integer totalTaskCount;
    private Integer successTaskCount;
    private Integer failedTaskCount;
    private Integer latestRunningTaskCount;
}
