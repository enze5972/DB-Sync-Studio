package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.monitoring.entity.DatasourceConnectionMetricDO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringCleanupSummaryVO;
import com.dbsyncstudio.model.monitoring.entity.TableRunMetricDO;
import com.dbsyncstudio.model.monitoring.entity.TaskRunMetricDO;
import com.dbsyncstudio.model.monitoring.vo.TaskRunMetricSummaryVO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface MonitoringRepository {

    int DEFAULT_RETENTION_DAYS = 30;

    void initialize() throws SQLException;

    long saveTaskRunMetric(TaskRunMetricDO metric) throws SQLException;

    Optional<TaskRunMetricDO> findLatestTaskRunMetricByRunId(String runId) throws SQLException;

    Optional<TaskRunMetricDO> findLatestTaskRunMetricByTaskId(long taskId) throws SQLException;

    List<TaskRunMetricDO> findTaskRunMetrics(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException;

    List<TaskRunMetricDO> findTaskRunMetricsByTaskId(long taskId, int limit) throws SQLException;

    TaskRunMetricSummaryVO summarizeTaskMetricsForToday(long dayStartTime, long dayEndTime) throws SQLException;

    long saveTableRunMetric(TableRunMetricDO metric) throws SQLException;

    List<TableRunMetricDO> findTableRunMetrics(String runId, Long taskId, Long tableTaskId, Long startTime, Long endTime, int limit) throws SQLException;

    List<TableRunMetricDO> findTableRunMetricsByRunId(String runId) throws SQLException;

    List<TableRunMetricDO> findTableRunMetricsByTaskId(long taskId, int limit) throws SQLException;

    long saveDatasourceConnectionMetric(DatasourceConnectionMetricDO metric) throws SQLException;

    Optional<DatasourceConnectionMetricDO> findLatestDatasourceConnectionMetricByDatasourceId(long datasourceId) throws SQLException;

    List<DatasourceConnectionMetricDO> findDatasourceConnectionMetrics(Long datasourceId, Long startTime, Long endTime, int limit) throws SQLException;

    MonitoringCleanupSummaryVO cleanupExpiredMetrics(long currentTime) throws SQLException;

    MonitoringCleanupSummaryVO cleanupExpiredMetrics(int retentionDays, long currentTime) throws SQLException;
}
