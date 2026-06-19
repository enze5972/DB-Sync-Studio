package com.dbsyncstudio.model.sync;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

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
public class FullSyncRequest {

    private DatasourceConfig sourceDatasource;
    private DatasourceConfig targetDatasource;
    private String sourceSchemaName;
    private String targetSchemaName;
    private String sourceTableName;
    private String targetTableName;
    private String checkpointKey;
    private String checkpointValue;
    @Builder.Default
    private int pageSize = 500;
    @Builder.Default
    private int batchSize = 500;
    @Builder.Default
    private boolean replaceTargetData = true;
}
