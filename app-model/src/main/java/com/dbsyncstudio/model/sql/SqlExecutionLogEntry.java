package com.dbsyncstudio.model.sql;

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
public class SqlExecutionLogEntry {

    private Long id;
    private Long datasourceId;
    private String sqlText;
    private String statementType;
    private boolean success;
    private Long affectedRows;
    private Long elapsedMillis;
    private String errorMessage;
    private Long createdAt;
}
