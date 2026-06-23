package com.dbsyncstudio.model.validation.dto;
import com.dbsyncstudio.model.validation.ValidationMode;
import com.dbsyncstudio.model.validation.ValidationSampleMode;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

import java.util.ArrayList;
import java.util.List;

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
public class ValidationRequestDTO {

    private Long taskId;
    private String runId;
    private DatasourceConfigDO sourceDatasource;
    private DatasourceConfigDO targetDatasource;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private ValidationMode validationMode;
    private ValidationSampleMode sampleMode;
    private Integer sampleCount;
    private String whereClause;
    private String incrementalCondition;
    private String hashAlgorithm;
    @Builder.Default
    private List<String> hashColumns = new ArrayList<String>();
}
