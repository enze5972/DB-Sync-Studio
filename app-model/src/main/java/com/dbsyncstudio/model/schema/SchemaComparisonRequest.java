package com.dbsyncstudio.model.schema;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

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
public class SchemaComparisonRequest {

    private DatasourceConfig sourceDatasource;
    private DatasourceConfig targetDatasource;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
}
