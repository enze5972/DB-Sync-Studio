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
public class ValidationRunDO {

    private Long id;
    private Long taskId;
    private String runId;
    private String validationMethod;
    private String sourceTableName;
    private String targetTableName;
    private String whereClause;
    private String incrementalCondition;
    private Long sourceRowCount;
    private Long targetRowCount;
    private Long missingCount;
    private Long inconsistentCount;
    private Long sampleCount;
    private String status;
    private String errorMessage;
    private Long startedAt;
    private Long endedAt;
    private Long elapsedMillis;
    private Long createdAt;
}
