package com.dbsyncstudio.model.sync.dto;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformTestRequestDTO {

    private Long taskId;
    private Long tableTaskId;
    private Long fieldMappingId;
    private String sourceField;
    private String targetField;
    private String runId;
    private Object value;
    private List<TransformRuleDO> rules;
}
