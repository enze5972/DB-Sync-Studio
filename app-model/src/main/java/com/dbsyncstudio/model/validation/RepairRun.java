package com.dbsyncstudio.model.validation;

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
public class RepairRun {

    private Long id;
    private Long validationRunId;
    private Long taskId;
    private String runId;
    private String tableName;
    private String repairType;
    private String status;
    private Long repairCount;
    private Long successCount;
    private Long failedCount;
    private Long startTime;
    private Long endTime;
    private String errorMessage;
    private Long createdAt;
    private Long updatedAt;
}
