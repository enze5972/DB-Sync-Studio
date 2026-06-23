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
public class IncrementalSyncCheckpointStateDO {

    private String watermarkValue;
    private String tieBreakerValue;
    private String compositeValue;
}
