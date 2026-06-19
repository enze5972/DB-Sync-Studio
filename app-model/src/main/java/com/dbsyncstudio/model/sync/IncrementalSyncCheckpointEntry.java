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
public class IncrementalSyncCheckpointEntry {

    private Long id;
    private Long taskId;
    private String checkpointMode;
    private String checkpointValue;
    private String checkpointTieBreakerValue;
    private String checkpointCompositeValue;
    private Long updatedAt;
}
