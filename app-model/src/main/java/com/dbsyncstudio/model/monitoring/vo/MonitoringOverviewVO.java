package com.dbsyncstudio.model.monitoring.vo;

import com.dbsyncstudio.model.monitoring.entity.TaskRunMetricDO;
import com.dbsyncstudio.model.monitoring.vo.TaskRunMetricSummaryVO;

public class MonitoringOverviewVO {

    private TaskRunMetricSummaryVO summary;
    private TaskRunMetricDO latestTaskMetric;

    public TaskRunMetricSummaryVO getSummary() {
        return summary;
    }

    public void setSummary(TaskRunMetricSummaryVO summary) {
        this.summary = summary;
    }

    public TaskRunMetricDO getLatestTaskMetric() {
        return latestTaskMetric;
    }

    public void setLatestTaskMetric(TaskRunMetricDO latestTaskMetric) {
        this.latestTaskMetric = latestTaskMetric;
    }
}
