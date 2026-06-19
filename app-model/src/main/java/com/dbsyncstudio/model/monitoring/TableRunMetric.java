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
public class TableRunMetric {

    private Long id;
    private Long tableTaskId;
    private Long taskId;
    private String runId;
    private String tableName;
    private Long syncedRowCount;
    private Long successRowCount;
    private Long failedRowCount;
    private Double speedRowsPerSecond;
    private Integer batchCount;
    private Integer retryCount;
    private String lastCheckpoint;
    private String lastError;
    private Long metricTime;
}
