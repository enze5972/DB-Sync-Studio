package com.dbsyncstudio.model.sync.entity;
import com.dbsyncstudio.model.sync.TransformErrorStrategy;

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
public class TransformRuleDO {

    private Long id;
    private Long taskId;
    private Long tableTaskId;
    private Long fieldMappingId;
    private String sourceField;
    private String targetField;
    private String transformType;
    private String transformConfig;
    private Integer transformOrder;
    private Boolean enabled;
    private TransformErrorStrategy onError;
    private String defaultValue;
    private Long createdAt;
    private Long updatedAt;
}
