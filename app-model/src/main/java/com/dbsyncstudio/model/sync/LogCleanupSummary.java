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
public class LogCleanupSummary {

    private Integer retentionDays;
    private Long cutoffTime;
    private Integer executionLogDeletedCount;
    private Integer syncRunLogDeletedCount;
    private Integer sqlExecutionLogDeletedCount;
}
