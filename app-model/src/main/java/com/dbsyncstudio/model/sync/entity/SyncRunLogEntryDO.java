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
public class SyncRunLogEntryDO {

    private Long id;
    private Long taskId;
    private Long syncRunId;
    private Long syncTableRunId;
    private String runId;
    private String tableName;
    private String logLevel;
    private String logMessage;
    private Long createdAt;
}
