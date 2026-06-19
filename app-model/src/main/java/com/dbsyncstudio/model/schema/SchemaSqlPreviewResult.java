package com.dbsyncstudio.model.schema;

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
public class SchemaSqlPreviewResult {

    private boolean executable;
    private String statementType;
    private String sql;
    private String message;
}
