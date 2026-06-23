package com.dbsyncstudio.model.validation.entity;

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
public class ValidationDifferenceDO {

    private Long id;
    private Long validationRunId;
    private Long taskId;
    private String runId;
    private String differenceType;
    private String primaryKeyJson;
    private String sourceRowJson;
    private String targetRowJson;
    private String differingColumnsJson;
    private String suggestedRepairType;
    private String status;
    private String errorMessage;
    private Long createdAt;
}
