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
public class FullSyncResult {

    private boolean success;
    private String message;
    private long sourceRowCount;
    private long insertedRowCount;
    private long durationMillis;
    private boolean createdTargetTable;
    private String checkpointValue;
    private boolean resumed;
}
