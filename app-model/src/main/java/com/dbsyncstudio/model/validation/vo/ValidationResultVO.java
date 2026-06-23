package com.dbsyncstudio.model.validation.vo;
import com.dbsyncstudio.model.validation.entity.ValidationDifferenceDO;
import com.dbsyncstudio.model.validation.entity.ValidationRunDO;

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
public class ValidationResultVO {

    private ValidationRunDO run;
    @Builder.Default
    private List<ValidationDifferenceDO> differences = new ArrayList<ValidationDifferenceDO>();
}
