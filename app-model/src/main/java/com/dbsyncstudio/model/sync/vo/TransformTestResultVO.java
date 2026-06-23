package com.dbsyncstudio.model.sync.vo;

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
public class TransformTestResultVO {

    private boolean success;
    private Object originalValue;
    private Object resultValue;
    private List<TransformStepResultVO> steps;
    private String errorMessage;
}
