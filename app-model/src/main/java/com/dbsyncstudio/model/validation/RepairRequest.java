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
public class RepairRequest {

    private Long taskId;
    private Long validationRunId;
    private String runId;
    private DatasourceConfig sourceDatasource;
    private DatasourceConfig targetDatasource;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    @Builder.Default
    private List<String> primaryKeyColumns = new ArrayList<String>();
    private RepairType repairType;
    @Builder.Default
    private List<Long> validationDifferenceIds = new ArrayList<Long>();
    @Builder.Default
    private boolean execute = false;
    @Builder.Default
    private boolean confirmDelete = false;
}
