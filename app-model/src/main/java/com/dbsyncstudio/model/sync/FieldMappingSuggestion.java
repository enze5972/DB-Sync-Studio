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
public class FieldMappingSuggestion {

    private String sourceColumnName;
    private String targetColumnName;
    private double confidence;
    private String matchReason;
    private boolean ignored;
}
