package com.dbsyncstudio.model.sync.dto;

import java.util.List;

public class TaskBatchRunDTO {

    private String runId;
    private List<TaskBatchTableDTO> tables;
    private Integer maxConcurrency;

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public List<TaskBatchTableDTO> getTables() {
        return tables;
    }

    public void setTables(List<TaskBatchTableDTO> tables) {
        this.tables = tables;
    }

    public Integer getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(Integer maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }
}
