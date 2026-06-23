package com.dbsyncstudio.model.sync.dto;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

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
public class IncrementalSyncRequestDTO {

    private Long taskId;
    private Long tableTaskId;
    private String runId;
    private DatasourceConfigDO sourceDatasource;
    private DatasourceConfigDO targetDatasource;
    private String sourceSchemaName;
    private String targetSchemaName;
    private String sourceTableName;
    private String targetTableName;
    @Builder.Default
    private IncrementalSyncMode incrementalMode = IncrementalSyncMode.TIMESTAMP;
    private String watermarkColumnName;
    private String autoIncrementColumnName;
    private String compositeWatermarkColumnName;
    private String compositeTieBreakerColumnName;
    private String checkpointKey;
    private String checkpointValue;
    @Builder.Default
    private int pageSize = 500;
    @Builder.Default
    private int batchSize = 500;
    @Builder.Default
    private boolean replaceTargetData = false;
}
