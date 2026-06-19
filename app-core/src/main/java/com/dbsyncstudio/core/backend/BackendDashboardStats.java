package com.dbsyncstudio.core.backend;

public class BackendDashboardStats {

    private int datasourceCount;
    private int taskCount;
    private int logCount;

    public BackendDashboardStats() {
    }

    public BackendDashboardStats(int datasourceCount, int taskCount, int logCount) {
        this.datasourceCount = datasourceCount;
        this.taskCount = taskCount;
        this.logCount = logCount;
    }

    public int getDatasourceCount() {
        return datasourceCount;
    }

    public void setDatasourceCount(int datasourceCount) {
        this.datasourceCount = datasourceCount;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getLogCount() {
        return logCount;
    }

    public void setLogCount(int logCount) {
        this.logCount = logCount;
    }
}
