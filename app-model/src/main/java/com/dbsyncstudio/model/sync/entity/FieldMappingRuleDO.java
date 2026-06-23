package com.dbsyncstudio.model.sync.entity;

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
public class FieldMappingRuleDO {

    private Long id;
    private Long taskId;
    private String sourceSchemaName;
    private String targetSchemaName;
    private String sourceTableName;
    private String targetTableName;
    private String sourceColumnName;
    private String targetColumnName;
    private boolean ignored;
    private String defaultValue;
    private String transformRule;
    private Long createdAt;
    private Long updatedAt;
}
