package com.dbsyncstudio.model.sync.entity;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.SyncTaskStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncTaskDO {

    private Long id;
    private String taskName;
    private Long sourceDatasourceId;
    private Long targetDatasourceId;
    private SyncMode syncMode;
    private SyncTaskStatus taskStatus;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private IncrementalSyncMode incrementalMode;
    private String incrementalColumnName;
    private String incrementalTieBreakerColumnName;
    private String incrementalCompositeColumnName;
    private String incrementalCheckpointMode;
    private String incrementalCheckpointValue;
    private Long incrementalCheckpointUpdatedAt;
    private Boolean scheduleEnabled;
    private String scheduleType;
    private String scheduleCronExpression;
    private Integer scheduleIntervalSeconds;
    private Long scheduleLastRunAt;
    private Long scheduleNextRunAt;
    private String scheduleLastResult;
    private String scheduleLastMessage;
    private List<SyncTaskTableDO> taskTables;
    private Long totalRowCount;
    private Long syncedRowCount;
    private Long successRowCount;
    private Long failedRowCount;
    private Double speedRowsPerSecond;
    private Long startedAt;
    private Long endedAt;
    private Long durationMillis;
    private String progressMessage;
    private Long createdAt;
    private Long updatedAt;
}
