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
public class SchemaComparisonHistoryEntry {

    private Long id;
    private Long sourceDatasourceId;
    private Long targetDatasourceId;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private String diffSummary;
    private Long createdAt;
}
