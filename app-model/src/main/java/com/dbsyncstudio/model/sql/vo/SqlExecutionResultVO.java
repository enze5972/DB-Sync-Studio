package com.dbsyncstudio.model.sql.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class SqlExecutionResultVO {

    private boolean success;
    private String statementType;
    private String message;
    private long elapsedMillis;
    private long affectedRows;
    @Builder.Default
    private List<String> columns = new ArrayList<String>();
    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    private long logId;
}
