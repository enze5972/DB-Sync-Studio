package com.dbsyncstudio.model.sync.vo;

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
public class TransformStepResultVO {

    private String transformType;
    private Object before;
    private Object after;
    private boolean success;
    private String errorMessage;
}
