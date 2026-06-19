package com.dbsyncstudio.core.backend;

import java.util.List;

public class TaskBatchRunRequest {

    private String runId;
    private List<TaskBatchTableRequest> tables;
    private Integer maxConcurrency;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public List<TaskBatchTableRequest> getTables() {
        return tables;
    }

    public void setTables(List<TaskBatchTableRequest> tables) {
        this.tables = tables;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
}
