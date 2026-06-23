package com.dbsyncstudio.model.sql.dto;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

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
public class SqlExecutionRequestDTO {

    private DatasourceConfigDO datasource;
    private String sql;
    @Builder.Default
    private boolean allowDangerousSql = false;
}
