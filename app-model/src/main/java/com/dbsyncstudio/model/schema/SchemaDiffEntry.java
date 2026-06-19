package com.dbsyncstudio.model.schema;

import com.dbsyncstudio.model.metadata.ColumnMetadata;

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
public class SchemaDiffEntry {

    private SchemaComparisonType diffType;
    private String sourceColumnName;
    private String targetColumnName;
    private ColumnMetadata sourceColumn;
    private ColumnMetadata targetColumn;
    private String description;
    private String suggestedSql;
}
