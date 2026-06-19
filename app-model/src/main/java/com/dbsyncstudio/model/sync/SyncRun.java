package com.dbsyncstudio.model.sync;

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
public class SyncRun {

    private Long id;
    private Long taskId;
    private String runId;
    private String syncMode;
    private String runStatus;
    private Integer totalTableCount;
    private Integer completedTableCount;
    private Long totalRowCount;
    private Long syncedRowCount;
    private Long successRowCount;
    private Long failedRowCount;
    private Double speedRowsPerSecond;
    private Long startedAt;
    private Long endedAt;
    private Long durationMillis;
    private String progressMessage;
    private String errorMessage;
    private Long createdAt;
    private Long updatedAt;
}
