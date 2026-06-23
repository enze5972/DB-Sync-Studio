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
public class ExecutionLogEntryDO {

    private Long id;
    private Long taskId;
    private String logLevel;
    private String logMessage;
    private Long createdAt;
}

