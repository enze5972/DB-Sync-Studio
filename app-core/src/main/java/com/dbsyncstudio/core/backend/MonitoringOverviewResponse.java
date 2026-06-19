package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.monitoring.TaskRunMetric;
import com.dbsyncstudio.model.monitoring.TaskRunMetricSummary;

public class MonitoringOverviewResponse {

    private TaskRunMetricSummary summary;
    private TaskRunMetric latestTaskMetric;

    public TaskRunMetricSummary getSummary() {
        return summary;
    }

    public void setSummary(TaskRunMetricSummary summary) {
        this.summary = summary;
    }

    public TaskRunMetric getLatestTaskMetric() {
        return latestTaskMetric;
    }

    public void setLatestTaskMetric(TaskRunMetric latestTaskMetric) {
        this.latestTaskMetric = latestTaskMetric;
    }
}
