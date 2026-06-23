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
public class IncrementalSyncCheckpointEntryDO {

    private Long id;
    private Long taskId;
    private String checkpointMode;
    private String checkpointValue;
    private String checkpointTieBreakerValue;
    private String checkpointCompositeValue;
    private Long updatedAt;
}
