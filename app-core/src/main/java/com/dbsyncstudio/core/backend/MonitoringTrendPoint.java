package com.dbsyncstudio.core.backend;

public class MonitoringTrendPoint {

    private Long metricTime;
    private Long successRowCount;
    private Long failedRowCount;
    private Double speedRowsPerSecond;
    private Long latencyMillis;
    private Long durationMillis;

    public Long getMetricTime() {
        return metricTime;
    }

    public void setMetricTime(Long metricTime) {
        this.metricTime = metricTime;
    }

    public Long getSuccessRowCount() {
        return successRowCount;
    }

    public void setSuccessRowCount(Long successRowCount) {
        this.successRowCount = successRowCount;
    }

    public Long getFailedRowCount() {
        return failedRowCount;
    }

    public void setFailedRowCount(Long failedRowCount) {
        this.failedRowCount = failedRowCount;
    }

    public Double getSpeedRowsPerSecond() {
        return speedRowsPerSecond;
    }

    public void setSpeedRowsPerSecond(Double speedRowsPerSecond) {
        this.speedRowsPerSecond = speedRowsPerSecond;
    }

    public Long getLatencyMillis() {
        return latencyMillis;
    }

    public void setLatencyMillis(Long latencyMillis) {
        this.latencyMillis = latencyMillis;
    }

    public Long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(Long durationMillis) {
        this.durationMillis = durationMillis;
    }
}
