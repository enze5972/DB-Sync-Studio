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
public class TaskRunMetric {

    private Long id;
    private String runId;
    private Long taskId;
    private Long metricTime;
    private Long successRowCount;
    private Long failedRowCount;
    private Double speedRowsPerSecond;
    private Long latencyMillis;
    private Long durationMillis;
    private String errorMessage;
    private Integer runningTaskCount;
    private Integer todayTaskCount;
    private Integer todaySuccessTaskCount;
    private Integer todayFailedTaskCount;
}
