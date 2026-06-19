package com.dbsyncstudio.model.validation;

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
public class RepairResult {

    private RepairRun run;
    @Builder.Default
    private List<RepairDetail> details = new ArrayList<RepairDetail>();
}
