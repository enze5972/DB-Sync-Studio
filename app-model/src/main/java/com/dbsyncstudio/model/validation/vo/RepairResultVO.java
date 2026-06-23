package com.dbsyncstudio.model.validation.vo;
import com.dbsyncstudio.model.validation.entity.RepairDetailDO;
import com.dbsyncstudio.model.validation.entity.RepairRunDO;

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
public class RepairResultVO {

    private RepairRunDO run;
    @Builder.Default
    private List<RepairDetailDO> details = new ArrayList<RepairDetailDO>();
}
