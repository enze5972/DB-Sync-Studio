package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.alert.AlertChannelType;
import com.dbsyncstudio.model.alert.AlertDedupState;
import com.dbsyncstudio.model.alert.AlertDedupStateRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

@RequiredArgsConstructor
public class SqliteAlertDedupStateRepository implements AlertDedupStateRepository {

    private static final String INSERT_SQL =
            "INSERT INTO alert_dedup_state (dedup_key, rule_id, alert_type, task_id, table_name, channel_type, channel_id, last_alert_id, last_content_hash, last_sent_time, cooldown_until, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE alert_dedup_state SET rule_id = ?, alert_type = ?, task_id = ?, table_name = ?, channel_type = ?, channel_id = ?, last_alert_id = ?, last_content_hash = ?, last_sent_time = ?, cooldown_until = ?, updated_at = ? WHERE dedup_key = ?";
    private static final String FIND_BY_DEDUP_KEY_SQL =
            "SELECT id, dedup_key, rule_id, alert_type, task_id, table_name, channel_type, channel_id, last_alert_id, last_content_hash, last_sent_time, cooldown_until, created_at, updated_at FROM alert_dedup_state WHERE dedup_key = ?";
    private static final String DELETE_SQL =
            "DELETE FROM alert_dedup_state WHERE dedup_key = ?";

    @NonNull
    private final SqliteConnectionFactory connectionFactory;

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(AlertDedupState state) throws SQLException {
        long now = System.currentTimeMillis();
        if (state.getCreatedAt() == null) {
            state.setCreatedAt(Long.valueOf(now));
        }
        state.setUpdatedAt(Long.valueOf(now));
        Optional<AlertDedupState> existing = findByDedupKey(state.getDedupKey());
        try (Connection connection = connectionFactory.openConnection()) {
            if (!existing.isPresent()) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, state);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            state.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert alert dedup state failed, no generated key returned");
                }
            }
            state.setId(existing.get().getId());
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, state);
                statement.executeUpdate();
                return state.getId().longValue();
            }
        }
    }

    @Override
    public Optional<AlertDedupState> findByDedupKey(String dedupKey) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_DEDUP_KEY_SQL)) {
            statement.setString(1, dedupKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    @Override
    public boolean deleteByDedupKey(String dedupKey) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, dedupKey);
            return statement.executeUpdate() > 0;
        }
    }

    private void bindInsertParameters(PreparedStatement statement, AlertDedupState state) throws SQLException {
        statement.setString(1, state.getDedupKey());
        bindNullableLong(statement, 2, state.getRuleId());
        statement.setString(3, state.getAlertType());
        bindNullableLong(statement, 4, state.getTaskId());
        statement.setString(5, state.getTableName());
        statement.setString(6, state.getChannelType() == null ? null : state.getChannelType().name());
        bindNullableLong(statement, 7, state.getChannelId());
        statement.setString(8, state.getLastAlertId());
        statement.setString(9, state.getLastContentHash());
        bindNullableLong(statement, 10, state.getLastSentTime());
        bindNullableLong(statement, 11, state.getCooldownUntil());
        statement.setLong(12, state.getCreatedAt().longValue());
        statement.setLong(13, state.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, AlertDedupState state) throws SQLException {
        bindNullableLong(statement, 1, state.getRuleId());
        statement.setString(2, state.getAlertType());
        bindNullableLong(statement, 3, state.getTaskId());
        statement.setString(4, state.getTableName());
        statement.setString(5, state.getChannelType() == null ? null : state.getChannelType().name());
        bindNullableLong(statement, 6, state.getChannelId());
        statement.setString(7, state.getLastAlertId());
        statement.setString(8, state.getLastContentHash());
        bindNullableLong(statement, 9, state.getLastSentTime());
        bindNullableLong(statement, 10, state.getCooldownUntil());
        statement.setLong(11, state.getUpdatedAt().longValue());
        statement.setString(12, state.getDedupKey());
    }

    private AlertDedupState mapRow(ResultSet resultSet) throws SQLException {
        AlertDedupState state = new AlertDedupState();
        state.setId(Long.valueOf(resultSet.getLong("id")));
        state.setDedupKey(resultSet.getString("dedup_key"));
        state.setRuleId(toLong(resultSet.getObject("rule_id")));
        state.setAlertType(resultSet.getString("alert_type"));
        state.setTaskId(toLong(resultSet.getObject("task_id")));
        state.setTableName(resultSet.getString("table_name"));
        String channelType = resultSet.getString("channel_type");
        state.setChannelType(channelType == null ? null : AlertChannelType.valueOf(channelType));
        state.setChannelId(toLong(resultSet.getObject("channel_id")));
        state.setLastAlertId(resultSet.getString("last_alert_id"));
        state.setLastContentHash(resultSet.getString("last_content_hash"));
        state.setLastSentTime(toLong(resultSet.getObject("last_sent_time")));
        state.setCooldownUntil(toLong(resultSet.getObject("cooldown_until")));
        state.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        state.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return state;
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
