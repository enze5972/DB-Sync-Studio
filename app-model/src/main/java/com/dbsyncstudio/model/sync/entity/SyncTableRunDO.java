package com.dbsyncstudio.model.sync.entity;

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
public class SyncTableRunDO {

    private Long id;
    private Long syncRunId;
    private Long taskId;
    private String runId;
    private Long taskTableId;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private Integer tableOrder;
    private String tableStatus;
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
    private String checkpointValue;
    private Long createdAt;
    private Long updatedAt;
}
