package com.dbsyncstudio.model.schema.dto;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

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
public class SchemaComparisonRequestDTO {

    private DatasourceConfigDO sourceDatasource;
    private DatasourceConfigDO targetDatasource;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
}
