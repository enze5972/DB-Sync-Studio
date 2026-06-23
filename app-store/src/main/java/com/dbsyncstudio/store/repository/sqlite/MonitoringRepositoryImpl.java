package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.monitoring.entity.DatasourceConnectionMetricDO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringCleanupSummaryVO;
import com.dbsyncstudio.store.repository.MonitoringRepository;
import com.dbsyncstudio.model.monitoring.entity.TableRunMetricDO;
import com.dbsyncstudio.model.monitoring.entity.TaskRunMetricDO;
import com.dbsyncstudio.model.monitoring.vo.TaskRunMetricSummaryVO;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class MonitoringRepositoryImpl implements MonitoringRepository {

    private static final String INSERT_TASK_RUN_METRIC_SQL =
            "INSERT INTO task_run_metric (run_id, task_id, metric_time, success_row_count, failed_row_count, speed_rows_per_second, latency_millis, duration_millis, error_message, running_task_count, today_task_count, today_success_task_count, today_failed_task_count) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_LATEST_TASK_RUN_METRIC_BY_RUN_ID_SQL =
            "SELECT id, run_id, task_id, metric_time, success_row_count, failed_row_count, speed_rows_per_second, latency_millis, duration_millis, error_message, running_task_count, today_task_count, today_success_task_count, today_failed_task_count FROM task_run_metric WHERE run_id = ? ORDER BY metric_time DESC, id DESC LIMIT 1";
    private static final String FIND_LATEST_TASK_RUN_METRIC_BY_TASK_ID_SQL =
            "SELECT id, run_id, task_id, metric_time, success_row_count, failed_row_count, speed_rows_per_second, latency_millis, duration_millis, error_message, running_task_count, today_task_count, today_success_task_count, today_failed_task_count FROM task_run_metric WHERE task_id = ? ORDER BY metric_time DESC, id DESC LIMIT 1";
    private static final String FIND_TASK_RUN_METRICS_BY_TASK_ID_SQL =
            "SELECT id, run_id, task_id, metric_time, success_row_count, failed_row_count, speed_rows_per_second, latency_millis, duration_millis, error_message, running_task_count, today_task_count, today_success_task_count, today_failed_task_count FROM task_run_metric WHERE task_id = ? ORDER BY metric_time DESC, id DESC LIMIT ?";
    private static final String FIND_TASK_RUN_METRICS_SQL =
            "SELECT id, run_id, task_id, metric_time, success_row_count, failed_row_count, speed_rows_per_second, latency_millis, duration_millis, error_message, running_task_count, today_task_count, today_success_task_count, today_failed_task_count " +
                    "FROM task_run_metric WHERE (? IS NULL OR run_id = ?) AND (? IS NULL OR task_id = ?) AND (? IS NULL OR metric_time >= ?) AND (? IS NULL OR metric_time <= ?) " +
                    "ORDER BY metric_time DESC, id DESC LIMIT ?";
    private static final String SUMMARIZE_TASK_METRICS_FOR_TODAY_SQL =
            "SELECT COUNT(DISTINCT task_id) AS total_task_count, " +
                    "SUM(CASE WHEN failed_row_count > 0 OR error_message IS NOT NULL THEN 1 ELSE 0 END) AS failed_task_count, " +
                    "SUM(CASE WHEN failed_row_count <= 0 AND error_message IS NULL THEN 1 ELSE 0 END) AS success_task_count " +
                    "FROM (" +
                    "SELECT id, task_id, failed_row_count, error_message FROM task_run_metric " +
                    "WHERE metric_time >= ? AND metric_time < ? " +
                    "AND id IN (" +
                    "SELECT MAX(id) FROM task_run_metric WHERE metric_time >= ? AND metric_time < ? GROUP BY task_id" +
                    ")" +
                    ")";
    private static final String FIND_LATEST_RUNNING_TASK_COUNT_SQL =
            "SELECT running_task_count FROM task_run_metric WHERE metric_time >= ? AND metric_time < ? ORDER BY metric_time DESC, id DESC LIMIT 1";
    private static final String INSERT_TABLE_RUN_METRIC_SQL =
            "INSERT INTO table_run_metric (table_task_id, task_id, run_id, table_name, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, batch_count, retry_count, last_checkpoint, last_error, metric_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_TABLE_RUN_METRICS_BY_RUN_ID_SQL =
            "SELECT id, table_task_id, task_id, run_id, table_name, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, batch_count, retry_count, last_checkpoint, last_error, metric_time FROM table_run_metric WHERE run_id = ? ORDER BY metric_time DESC, id DESC";
    private static final String FIND_TABLE_RUN_METRICS_BY_TASK_ID_SQL =
            "SELECT id, table_task_id, task_id, run_id, table_name, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, batch_count, retry_count, last_checkpoint, last_error, metric_time FROM table_run_metric WHERE task_id = ? ORDER BY metric_time DESC, id DESC LIMIT ?";
    private static final String FIND_TABLE_RUN_METRICS_SQL =
            "SELECT id, table_task_id, task_id, run_id, table_name, synced_row_count, success_row_count, failed_row_count, speed_rows_per_second, batch_count, retry_count, last_checkpoint, last_error, metric_time " +
                    "FROM table_run_metric WHERE (? IS NULL OR run_id = ?) AND (? IS NULL OR task_id = ?) AND (? IS NULL OR table_task_id = ?) AND (? IS NULL OR metric_time >= ?) AND (? IS NULL OR metric_time <= ?) " +
                    "ORDER BY metric_time DESC, id DESC LIMIT ?";
    private static final String INSERT_DATASOURCE_CONNECTION_METRIC_SQL =
            "INSERT INTO datasource_connection_metric (datasource_id, connection_status, last_success_time, last_failure_time, failure_reason, average_test_connection_millis, last_test_connection_millis, metric_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_LATEST_DATASOURCE_CONNECTION_METRIC_BY_DATASOURCE_ID_SQL =
            "SELECT id, datasource_id, connection_status, last_success_time, last_failure_time, failure_reason, average_test_connection_millis, last_test_connection_millis, metric_time FROM datasource_connection_metric WHERE datasource_id = ? ORDER BY metric_time DESC, id DESC LIMIT 1";
    private static final String FIND_DATASOURCE_CONNECTION_METRICS_SQL =
            "SELECT id, datasource_id, connection_status, last_success_time, last_failure_time, failure_reason, average_test_connection_millis, last_test_connection_millis, metric_time " +
                    "FROM datasource_connection_metric WHERE (? IS NULL OR datasource_id = ?) AND (? IS NULL OR metric_time >= ?) AND (? IS NULL OR metric_time <= ?) " +
                    "ORDER BY metric_time DESC, id DESC LIMIT ?";
    private static final String DELETE_TASK_RUN_METRIC_SQL =
            "DELETE FROM task_run_metric WHERE metric_time < ?";
    private static final String DELETE_TABLE_RUN_METRIC_SQL =
            "DELETE FROM table_run_metric WHERE metric_time < ?";
    private static final String DELETE_DATASOURCE_CONNECTION_METRIC_SQL =
            "DELETE FROM datasource_connection_metric WHERE metric_time < ?";

    @NonNull
    private final DatabaseConnectionFactory connectionFactory;

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            DatabaseSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long saveTaskRunMetric(TaskRunMetricDO metric) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_TASK_RUN_METRIC_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, metric.getRunId());
            statement.setLong(2, metric.getTaskId().longValue());
            statement.setLong(3, metric.getMetricTime().longValue());
            bindNullableLong(statement, 4, metric.getSuccessRowCount());
            bindNullableLong(statement, 5, metric.getFailedRowCount());
            bindNullableDouble(statement, 6, metric.getSpeedRowsPerSecond());
            bindNullableLong(statement, 7, metric.getLatencyMillis());
            bindNullableLong(statement, 8, metric.getDurationMillis());
            statement.setString(9, metric.getErrorMessage());
            bindNullableInteger(statement, 10, metric.getRunningTaskCount());
            bindNullableInteger(statement, 11, metric.getTodayTaskCount());
            bindNullableInteger(statement, 12, metric.getTodaySuccessTaskCount());
            bindNullableInteger(statement, 13, metric.getTodayFailedTaskCount());
            statement.executeUpdate();
            return readGeneratedId(statement, metric);
        }
    }

    @Override
    public Optional<TaskRunMetricDO> findLatestTaskRunMetricByRunId(String runId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_LATEST_TASK_RUN_METRIC_BY_RUN_ID_SQL)) {
            statement.setString(1, runId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapTaskRunMetric(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public Optional<TaskRunMetricDO> findLatestTaskRunMetricByTaskId(long taskId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_LATEST_TASK_RUN_METRIC_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapTaskRunMetric(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<TaskRunMetricDO> findTaskRunMetricsByTaskId(long taskId, int limit) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TASK_RUN_METRICS_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            statement.setInt(2, safeLimit(limit));
            return readTaskRunMetricList(statement);
        }
    }

    @Override
    public List<TaskRunMetricDO> findTaskRunMetrics(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TASK_RUN_METRICS_SQL)) {
            bindNullableString(statement, 1, runId);
            bindNullableString(statement, 2, runId);
            bindNullableLong(statement, 3, taskId);
            bindNullableLong(statement, 4, taskId);
            bindNullableLong(statement, 5, startTime);
            bindNullableLong(statement, 6, startTime);
            bindNullableLong(statement, 7, endTime);
            bindNullableLong(statement, 8, endTime);
            statement.setInt(9, safeLimit(limit));
            return readTaskRunMetricList(statement);
        }
    }

    @Override
    public TaskRunMetricSummaryVO summarizeTaskMetricsForToday(long dayStartTime, long dayEndTime) throws SQLException {
        TaskRunMetricSummaryVO summary = new TaskRunMetricSummaryVO();
        summary.setTotalTaskCount(Integer.valueOf(0));
        summary.setSuccessTaskCount(Integer.valueOf(0));
        summary.setFailedTaskCount(Integer.valueOf(0));
        summary.setLatestRunningTaskCount(Integer.valueOf(0));
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement summaryStatement = connection.prepareStatement(SUMMARIZE_TASK_METRICS_FOR_TODAY_SQL);
             PreparedStatement runningCountStatement = connection.prepareStatement(FIND_LATEST_RUNNING_TASK_COUNT_SQL)) {
            summaryStatement.setLong(1, dayStartTime);
            summaryStatement.setLong(2, dayEndTime);
            summaryStatement.setLong(3, dayStartTime);
            summaryStatement.setLong(4, dayEndTime);
            try (ResultSet resultSet = summaryStatement.executeQuery()) {
                if (resultSet.next()) {
                    summary.setTotalTaskCount(toIntegerWithDefault(resultSet.getObject("total_task_count"), 0));
                    summary.setFailedTaskCount(toIntegerWithDefault(resultSet.getObject("failed_task_count"), 0));
                    summary.setSuccessTaskCount(toIntegerWithDefault(resultSet.getObject("success_task_count"), 0));
                }
            }
            runningCountStatement.setLong(1, dayStartTime);
            runningCountStatement.setLong(2, dayEndTime);
            try (ResultSet resultSet = runningCountStatement.executeQuery()) {
                if (resultSet.next()) {
                    summary.setLatestRunningTaskCount(toIntegerWithDefault(resultSet.getObject("running_task_count"), 0));
                }
            }
        }
        return summary;
    }

    @Override
    public long saveTableRunMetric(TableRunMetricDO metric) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_TABLE_RUN_METRIC_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, metric.getTableTaskId().longValue());
            statement.setLong(2, metric.getTaskId().longValue());
            statement.setString(3, metric.getRunId());
            statement.setString(4, metric.getTableName());
            bindNullableLong(statement, 5, metric.getSyncedRowCount());
            bindNullableLong(statement, 6, metric.getSuccessRowCount());
            bindNullableLong(statement, 7, metric.getFailedRowCount());
            bindNullableDouble(statement, 8, metric.getSpeedRowsPerSecond());
            bindNullableInteger(statement, 9, metric.getBatchCount());
            bindNullableInteger(statement, 10, metric.getRetryCount());
            statement.setString(11, metric.getLastCheckpoint());
            statement.setString(12, metric.getLastError());
            statement.setLong(13, metric.getMetricTime().longValue());
            statement.executeUpdate();
            return readGeneratedId(statement, metric);
        }
    }

    @Override
    public List<TableRunMetricDO> findTableRunMetricsByRunId(String runId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TABLE_RUN_METRICS_BY_RUN_ID_SQL)) {
            statement.setString(1, runId);
            return readTableRunMetricList(statement);
        }
    }

    @Override
    public List<TableRunMetricDO> findTableRunMetricsByTaskId(long taskId, int limit) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TABLE_RUN_METRICS_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            statement.setInt(2, safeLimit(limit));
            return readTableRunMetricList(statement);
        }
    }

    @Override
    public List<TableRunMetricDO> findTableRunMetrics(String runId, Long taskId, Long tableTaskId, Long startTime,
                                                    Long endTime, int limit) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_TABLE_RUN_METRICS_SQL)) {
            bindNullableString(statement, 1, runId);
            bindNullableString(statement, 2, runId);
            bindNullableLong(statement, 3, taskId);
            bindNullableLong(statement, 4, taskId);
            bindNullableLong(statement, 5, tableTaskId);
            bindNullableLong(statement, 6, tableTaskId);
            bindNullableLong(statement, 7, startTime);
            bindNullableLong(statement, 8, startTime);
            bindNullableLong(statement, 9, endTime);
            bindNullableLong(statement, 10, endTime);
            statement.setInt(11, safeLimit(limit));
            return readTableRunMetricList(statement);
        }
    }

    @Override
    public long saveDatasourceConnectionMetric(DatasourceConnectionMetricDO metric) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_DATASOURCE_CONNECTION_METRIC_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, metric.getDatasourceId().longValue());
            statement.setString(2, metric.getConnectionStatus());
            bindNullableLong(statement, 3, metric.getLastSuccessTime());
            bindNullableLong(statement, 4, metric.getLastFailureTime());
            statement.setString(5, metric.getFailureReason());
            bindNullableDouble(statement, 6, metric.getAverageTestConnectionMillis());
            bindNullableLong(statement, 7, metric.getLastTestConnectionMillis());
            statement.setLong(8, metric.getMetricTime().longValue());
            statement.executeUpdate();
            return readGeneratedId(statement, metric);
        }
    }

    @Override
    public Optional<DatasourceConnectionMetricDO> findLatestDatasourceConnectionMetricByDatasourceId(long datasourceId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_LATEST_DATASOURCE_CONNECTION_METRIC_BY_DATASOURCE_ID_SQL)) {
            statement.setLong(1, datasourceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapDatasourceConnectionMetric(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<DatasourceConnectionMetricDO> findDatasourceConnectionMetrics(Long datasourceId, Long startTime, Long endTime, int limit)
            throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_DATASOURCE_CONNECTION_METRICS_SQL)) {
            bindNullableLong(statement, 1, datasourceId);
            bindNullableLong(statement, 2, datasourceId);
            bindNullableLong(statement, 3, startTime);
            bindNullableLong(statement, 4, startTime);
            bindNullableLong(statement, 5, endTime);
            bindNullableLong(statement, 6, endTime);
            statement.setInt(7, safeLimit(limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<DatasourceConnectionMetricDO> result = new ArrayList<DatasourceConnectionMetricDO>();
                while (resultSet.next()) {
                    result.add(mapDatasourceConnectionMetric(resultSet));
                }
                return result;
            }
        }
    }

    @Override
    public MonitoringCleanupSummaryVO cleanupExpiredMetrics(long currentTime) throws SQLException {
        return cleanupExpiredMetrics(DEFAULT_RETENTION_DAYS, currentTime);
    }

    @Override
    public MonitoringCleanupSummaryVO cleanupExpiredMetrics(int retentionDays, long currentTime) throws SQLException {
        int safeRetentionDays = retentionDays <= 0 ? DEFAULT_RETENTION_DAYS : retentionDays;
        long cutoffTime = currentTime - safeRetentionDays * 24L * 60L * 60L * 1000L;
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);
            try {
                int taskDeleted = deleteByMetricTime(connection, DELETE_TASK_RUN_METRIC_SQL, cutoffTime);
                int tableDeleted = deleteByMetricTime(connection, DELETE_TABLE_RUN_METRIC_SQL, cutoffTime);
                int datasourceDeleted = deleteByMetricTime(connection, DELETE_DATASOURCE_CONNECTION_METRIC_SQL, cutoffTime);
                connection.commit();
                return MonitoringCleanupSummaryVO.builder()
                        .retentionDays(Integer.valueOf(safeRetentionDays))
                        .cutoffTime(Long.valueOf(cutoffTime))
                        .taskRunMetricDeletedCount(Integer.valueOf(taskDeleted))
                        .tableRunMetricDeletedCount(Integer.valueOf(tableDeleted))
                        .datasourceConnectionMetricDeletedCount(Integer.valueOf(datasourceDeleted))
                        .build();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int deleteByMetricTime(Connection connection, String sql, long cutoffTime) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cutoffTime);
            return statement.executeUpdate();
        }
    }

    private List<TaskRunMetricDO> readTaskRunMetricList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<TaskRunMetricDO> result = new ArrayList<TaskRunMetricDO>();
            while (resultSet.next()) {
                result.add(mapTaskRunMetric(resultSet));
            }
            return result;
        }
    }

    private List<TableRunMetricDO> readTableRunMetricList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<TableRunMetricDO> result = new ArrayList<TableRunMetricDO>();
            while (resultSet.next()) {
                result.add(mapTableRunMetric(resultSet));
            }
            return result;
        }
    }

    private TaskRunMetricDO mapTaskRunMetric(ResultSet resultSet) throws SQLException {
        return TaskRunMetricDO.builder()
                .id(Long.valueOf(resultSet.getLong("id")))
                .runId(resultSet.getString("run_id"))
                .taskId(Long.valueOf(resultSet.getLong("task_id")))
                .metricTime(Long.valueOf(resultSet.getLong("metric_time")))
                .successRowCount(toLong(resultSet.getObject("success_row_count")))
                .failedRowCount(toLong(resultSet.getObject("failed_row_count")))
                .speedRowsPerSecond(toDouble(resultSet.getObject("speed_rows_per_second")))
                .latencyMillis(toLong(resultSet.getObject("latency_millis")))
                .durationMillis(toLong(resultSet.getObject("duration_millis")))
                .errorMessage(resultSet.getString("error_message"))
                .runningTaskCount(toInteger(resultSet.getObject("running_task_count")))
                .todayTaskCount(toInteger(resultSet.getObject("today_task_count")))
                .todaySuccessTaskCount(toInteger(resultSet.getObject("today_success_task_count")))
                .todayFailedTaskCount(toInteger(resultSet.getObject("today_failed_task_count")))
                .build();
    }

    private TableRunMetricDO mapTableRunMetric(ResultSet resultSet) throws SQLException {
        return TableRunMetricDO.builder()
                .id(Long.valueOf(resultSet.getLong("id")))
                .tableTaskId(Long.valueOf(resultSet.getLong("table_task_id")))
                .taskId(Long.valueOf(resultSet.getLong("task_id")))
                .runId(resultSet.getString("run_id"))
                .tableName(resultSet.getString("table_name"))
                .syncedRowCount(toLong(resultSet.getObject("synced_row_count")))
                .successRowCount(toLong(resultSet.getObject("success_row_count")))
                .failedRowCount(toLong(resultSet.getObject("failed_row_count")))
                .speedRowsPerSecond(toDouble(resultSet.getObject("speed_rows_per_second")))
                .batchCount(toInteger(resultSet.getObject("batch_count")))
                .retryCount(toInteger(resultSet.getObject("retry_count")))
                .lastCheckpoint(resultSet.getString("last_checkpoint"))
                .lastError(resultSet.getString("last_error"))
                .metricTime(Long.valueOf(resultSet.getLong("metric_time")))
                .build();
    }

    private DatasourceConnectionMetricDO mapDatasourceConnectionMetric(ResultSet resultSet) throws SQLException {
        return DatasourceConnectionMetricDO.builder()
                .id(Long.valueOf(resultSet.getLong("id")))
                .datasourceId(Long.valueOf(resultSet.getLong("datasource_id")))
                .connectionStatus(resultSet.getString("connection_status"))
                .lastSuccessTime(toLong(resultSet.getObject("last_success_time")))
                .lastFailureTime(toLong(resultSet.getObject("last_failure_time")))
                .failureReason(resultSet.getString("failure_reason"))
                .averageTestConnectionMillis(toDouble(resultSet.getObject("average_test_connection_millis")))
                .lastTestConnectionMillis(toLong(resultSet.getObject("last_test_connection_millis")))
                .metricTime(Long.valueOf(resultSet.getLong("metric_time")))
                .build();
    }

    private long readGeneratedId(PreparedStatement statement, TaskRunMetricDO metric) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                metric.setId(Long.valueOf(id));
                return id;
            }
        }
        throw new SQLException("Insert task run metric failed, no generated key returned");
    }

    private long readGeneratedId(PreparedStatement statement, TableRunMetricDO metric) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                metric.setId(Long.valueOf(id));
                return id;
            }
        }
        throw new SQLException("Insert table run metric failed, no generated key returned");
    }

    private long readGeneratedId(PreparedStatement statement, DatasourceConnectionMetricDO metric) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                long id = generatedKeys.getLong(1);
                metric.setId(Long.valueOf(id));
                return id;
            }
        }
        throw new SQLException("Insert datasource connection metric failed, no generated key returned");
    }

    private int safeLimit(int limit) {
        return limit <= 0 ? 20 : limit;
    }


    private void bindNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.intValue());
    }

    private void bindNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value.longValue());
    }

    private void bindNullableDouble(PreparedStatement statement, int index, Double value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DOUBLE);
            return;
        }
        statement.setDouble(index, value.doubleValue());
    }

    private void bindNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
            return;
        }
        statement.setString(index, value);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(((Number) value).intValue());
    }

    private Integer toIntegerWithDefault(Object value, int defaultValue) {
        if (value == null) {
            return Integer.valueOf(defaultValue);
        }
        return Integer.valueOf(((Number) value).intValue());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(((Number) value).longValue());
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        return Double.valueOf(((Number) value).doubleValue());
    }
}
