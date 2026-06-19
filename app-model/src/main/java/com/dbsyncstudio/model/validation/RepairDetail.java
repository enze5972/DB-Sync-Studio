package com.dbsyncstudio.model.validation;

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
public class RepairDetail {

    private Long id;
    private Long repairRunId;
    private Long validationDifferenceId;
    private Long taskId;
    private String repairType;
    private String primaryKeyJson;
    private String sqlPreview;
    private String parameterJson;
    private String status;
    private String errorMessage;
    private Long createdAt;
    private Long updatedAt;
}
