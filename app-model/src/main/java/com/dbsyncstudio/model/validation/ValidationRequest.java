package com.dbsyncstudio.model.validation;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

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
public class ValidationRequest {

    private Long taskId;
    private String runId;
    private DatasourceConfig sourceDatasource;
    private DatasourceConfig targetDatasource;
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
