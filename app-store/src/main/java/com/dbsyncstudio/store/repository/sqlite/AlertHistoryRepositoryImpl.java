package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.alert.AlertChannelType;
import com.dbsyncstudio.model.alert.entity.AlertHistoryEntryDO;
import com.dbsyncstudio.store.repository.AlertHistoryRepository;

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
public class AlertHistoryRepositoryImpl implements AlertHistoryRepository {

    private static final String INSERT_SQL =
            "INSERT INTO alert_history (alert_id, rule_id, alert_type, task_id, run_id, table_name, alert_level, alert_content, channel_type, channel_id, send_status, error_message, created_time, sent_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, alert_id, rule_id, alert_type, task_id, run_id, table_name, alert_level, alert_content, channel_type, channel_id, send_status, error_message, created_time, sent_time FROM alert_history WHERE id = ?";
    private static final String FIND_BY_ALERT_ID_SQL =
            "SELECT id, alert_id, rule_id, alert_type, task_id, run_id, table_name, alert_level, alert_content, channel_type, channel_id, send_status, error_message, created_time, sent_time FROM alert_history WHERE alert_id = ? ORDER BY id DESC";
    private static final String FIND_ALL_SQL =
            "SELECT id, alert_id, rule_id, alert_type, task_id, run_id, table_name, alert_level, alert_content, channel_type, channel_id, send_status, error_message, created_time, sent_time FROM alert_history ORDER BY id DESC";

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
    public long save(AlertHistoryEntryDO historyEntry) throws SQLException {
        if (historyEntry.getCreatedTime() == null) {
            historyEntry.setCreatedTime(Long.valueOf(System.currentTimeMillis()));
        }
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bindInsertParameters(statement, historyEntry);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    historyEntry.setId(Long.valueOf(id));
                    return id;
                }
            }
            throw new SQLException("Insert alert history failed, no generated key returned");
        }
    }

    @Override
    public Optional<AlertHistoryEntryDO> findById(long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public List<AlertHistoryEntryDO> findByAlertId(String alertId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ALERT_ID_SQL)) {
            statement.setString(1, alertId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AlertHistoryEntryDO> result = new ArrayList<AlertHistoryEntryDO>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    @Override
    public List<AlertHistoryEntryDO> findAll() throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<AlertHistoryEntryDO> result = new ArrayList<AlertHistoryEntryDO>();
            while (resultSet.next()) {
                result.add(mapRow(resultSet));
            }
            return result;
        }
    }

    private void bindInsertParameters(PreparedStatement statement, AlertHistoryEntryDO historyEntry) throws SQLException {
        statement.setString(1, historyEntry.getAlertId());
        bindNullableLong(statement, 2, historyEntry.getRuleId());
        statement.setString(3, historyEntry.getAlertType());
        bindNullableLong(statement, 4, historyEntry.getTaskId());
        statement.setString(5, historyEntry.getRunId());
        statement.setString(6, historyEntry.getTableName());
        statement.setString(7, historyEntry.getAlertLevel());
        statement.setString(8, historyEntry.getAlertContent());
        statement.setString(9, historyEntry.getChannelType() == null ? null : historyEntry.getChannelType().name());
        bindNullableLong(statement, 10, historyEntry.getChannelId());
        statement.setString(11, historyEntry.getSendStatus());
        statement.setString(12, historyEntry.getErrorMessage());
        statement.setLong(13, historyEntry.getCreatedTime().longValue());
        bindNullableLong(statement, 14, historyEntry.getSentTime());
    }

    private AlertHistoryEntryDO mapRow(ResultSet resultSet) throws SQLException {
        AlertHistoryEntryDO historyEntry = new AlertHistoryEntryDO();
        historyEntry.setId(Long.valueOf(resultSet.getLong("id")));
        historyEntry.setAlertId(resultSet.getString("alert_id"));
        historyEntry.setRuleId(toLong(resultSet.getObject("rule_id")));
        historyEntry.setAlertType(resultSet.getString("alert_type"));
        historyEntry.setTaskId(toLong(resultSet.getObject("task_id")));
        historyEntry.setRunId(resultSet.getString("run_id"));
        historyEntry.setTableName(resultSet.getString("table_name"));
        historyEntry.setAlertLevel(resultSet.getString("alert_level"));
        historyEntry.setAlertContent(resultSet.getString("alert_content"));
        String channelType = resultSet.getString("channel_type");
        historyEntry.setChannelType(channelType == null ? null : AlertChannelType.valueOf(channelType));
        historyEntry.setChannelId(toLong(resultSet.getObject("channel_id")));
        historyEntry.setSendStatus(resultSet.getString("send_status"));
        historyEntry.setErrorMessage(resultSet.getString("error_message"));
        historyEntry.setCreatedTime(Long.valueOf(resultSet.getLong("created_time")));
        historyEntry.setSentTime(toLong(resultSet.getObject("sent_time")));
        return historyEntry;
    }

    private void bindNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value.longValue());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(((Number) value).longValue());
    }
}
