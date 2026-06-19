package com.dbsyncstudio.model.monitoring;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MonitoringRepository {

    int DEFAULT_RETENTION_DAYS = 30;

    void initialize() throws SQLException;

    long saveTaskRunMetric(TaskRunMetric metric) throws SQLException;

    Optional<TaskRunMetric> findLatestTaskRunMetricByRunId(String runId) throws SQLException;

    Optional<TaskRunMetric> findLatestTaskRunMetricByTaskId(long taskId) throws SQLException;

    List<TaskRunMetric> findTaskRunMetrics(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException;

    List<TaskRunMetric> findTaskRunMetricsByTaskId(long taskId, int limit) throws SQLException;

    TaskRunMetricSummary summarizeTaskMetricsForToday(long dayStartTime, long dayEndTime) throws SQLException;

    long saveTableRunMetric(TableRunMetric metric) throws SQLException;

    List<TableRunMetric> findTableRunMetrics(String runId, Long taskId, Long tableTaskId, Long startTime, Long endTime, int limit) throws SQLException;

    List<TableRunMetric> findTableRunMetricsByRunId(String runId) throws SQLException;

    List<TableRunMetric> findTableRunMetricsByTaskId(long taskId, int limit) throws SQLException;

    long saveDatasourceConnectionMetric(DatasourceConnectionMetric metric) throws SQLException;

    Optional<DatasourceConnectionMetric> findLatestDatasourceConnectionMetricByDatasourceId(long datasourceId) throws SQLException;

    List<DatasourceConnectionMetric> findDatasourceConnectionMetrics(Long datasourceId, Long startTime, Long endTime, int limit) throws SQLException;

    MonitoringCleanupSummary cleanupExpiredMetrics(long currentTime) throws SQLException;

    MonitoringCleanupSummary cleanupExpiredMetrics(int retentionDays, long currentTime) throws SQLException;
}
