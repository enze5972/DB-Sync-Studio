package com.dbsyncstudio.core.backend;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTrendResponse {

    private List<MonitoringTrendPoint> taskRunTrend = new ArrayList<MonitoringTrendPoint>();

    public List<MonitoringTrendPoint> getTaskRunTrend() {
        return taskRunTrend;
    }

    public void setTaskRunTrend(List<MonitoringTrendPoint> taskRunTrend) {
        this.taskRunTrend = taskRunTrend;
    }
}
