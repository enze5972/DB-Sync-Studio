package com.dbsyncstudio.model.sql;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

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
public class SqlExecutionRequest {

    private DatasourceConfig datasource;
    private String sql;
    @Builder.Default
    private boolean allowDangerousSql = false;
}
