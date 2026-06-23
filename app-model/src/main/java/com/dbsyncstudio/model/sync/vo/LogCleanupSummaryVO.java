package com.dbsyncstudio.model.sync.vo;

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
public class LogCleanupSummaryVO {

    private Integer retentionDays;
    private Long cutoffTime;
    private Integer executionLogDeletedCount;
    private Integer syncRunLogDeletedCount;
    private Integer sqlExecutionLogDeletedCount;
}
