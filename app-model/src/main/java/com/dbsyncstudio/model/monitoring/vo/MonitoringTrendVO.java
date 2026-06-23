package com.dbsyncstudio.model.monitoring.vo;

import java.util.ArrayList;
import java.util.List;

public class MonitoringTrendVO {

    private List<MonitoringTrendPointVO> taskRunTrend = new ArrayList<MonitoringTrendPointVO>();

    public List<MonitoringTrendPointVO> getTaskRunTrend() {
        return taskRunTrend;
    }

    public void setTaskRunTrend(List<MonitoringTrendPointVO> taskRunTrend) {
        this.taskRunTrend = taskRunTrend;
    }
}
