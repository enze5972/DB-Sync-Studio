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
public class SyncCheckpoint {

    private Long id;
    private String checkpointKey;
    private String checkpointValue;
    private Long updatedAt;
}

