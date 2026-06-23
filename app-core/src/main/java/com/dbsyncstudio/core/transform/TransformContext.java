package com.dbsyncstudio.core.transform;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TransformContext {

    private Long taskId;
    private String runId;
    private Long tableTaskId;
    private Long fieldMappingId;
    private String sourceField;
    private String targetField;
    private String transformType;
    private Map<String, Object> sourceRow;
    private Map<String, Object> targetRow;
    private Object currentValue;
    private Map<String, Object> ruleConfig;
}
