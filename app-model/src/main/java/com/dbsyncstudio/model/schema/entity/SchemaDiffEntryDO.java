package com.dbsyncstudio.model.schema.entity;
import com.dbsyncstudio.model.schema.SchemaComparisonType;

import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;

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
public class SchemaDiffEntryDO {

    private SchemaComparisonType diffType;
    private String sourceColumnName;
    private String targetColumnName;
    private ColumnMetadataDO sourceColumn;
    private ColumnMetadataDO targetColumn;
    private String description;
    private String suggestedSql;
}
