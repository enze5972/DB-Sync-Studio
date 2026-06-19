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
public class SyncTaskTable {

    private Long id;
    private Long taskId;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private String syncMode;
    private String incrementalMode;
    private String incrementalColumnName;
    private String incrementalTieBreakerColumnName;
    private String incrementalCompositeColumnName;
    private Integer batchSize;
    private Integer tableOrder;
    private Boolean enabled;
    private Long createdAt;
    private Long updatedAt;
}
