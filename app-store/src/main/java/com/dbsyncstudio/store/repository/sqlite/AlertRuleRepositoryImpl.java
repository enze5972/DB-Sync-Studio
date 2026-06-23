package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.alert.entity.AlertRuleDO;
import com.dbsyncstudio.store.repository.AlertRuleRepository;

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
public class AlertRuleRepositoryImpl implements AlertRuleRepository {

    private static final String INSERT_SQL =
            "INSERT INTO alert_rules (rule_name, alert_type, task_id, table_name, alert_level, alert_content_template, channel_ids_json, enabled, cooldown_seconds, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE alert_rules SET rule_name = ?, alert_type = ?, task_id = ?, table_name = ?, alert_level = ?, alert_content_template = ?, channel_ids_json = ?, enabled = ?, cooldown_seconds = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, rule_name, alert_type, task_id, table_name, alert_level, alert_content_template, channel_ids_json, enabled, cooldown_seconds, created_at, updated_at FROM alert_rules WHERE id = ?";
    private static final String FIND_ALL_SQL =
            "SELECT id, rule_name, alert_type, task_id, table_name, alert_level, alert_content_template, channel_ids_json, enabled, cooldown_seconds, created_at, updated_at FROM alert_rules ORDER BY id DESC";
    private static final String FIND_ENABLED_SQL =
            "SELECT id, rule_name, alert_type, task_id, table_name, alert_level, alert_content_template, channel_ids_json, enabled, cooldown_seconds, created_at, updated_at FROM alert_rules WHERE enabled = 1 ORDER BY id DESC";
    private static final String DELETE_SQL =
            "DELETE FROM alert_rules WHERE id = ?";

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
    public long save(AlertRuleDO rule) throws SQLException {
        long now = System.currentTimeMillis();
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(Long.valueOf(now));
        }
        rule.setUpdatedAt(Long.valueOf(now));
        try (Connection connection = connectionFactory.openConnection()) {
            if (rule.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, rule);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            rule.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert alert rule failed, no generated key returned");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, rule);
                statement.executeUpdate();
                return rule.getId().longValue();
            }
        }
    }

    @Override
    public Optional<AlertRuleDO> findById(long id) throws SQLException {
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
    public List<AlertRuleDO> findAll() throws SQLException {
        return readList(FIND_ALL_SQL);
    }

    @Override
    public List<AlertRuleDO> findEnabled() throws SQLException {
        return readList(FIND_ENABLED_SQL);
    }

    @Override
    public boolean deleteById(long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private List<AlertRuleDO> readList(String sql) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<AlertRuleDO> result = new ArrayList<AlertRuleDO>();
            while (resultSet.next()) {
                result.add(mapRow(resultSet));
            }
            return result;
        }
    }

    private void bindInsertParameters(PreparedStatement statement, AlertRuleDO rule) throws SQLException {
        statement.setString(1, rule.getRuleName());
        statement.setString(2, rule.getAlertType());
        bindNullableLong(statement, 3, rule.getTaskId());
        statement.setString(4, rule.getTableName());
        statement.setString(5, rule.getAlertLevel());
        statement.setString(6, rule.getAlertContentTemplate());
        statement.setString(7, rule.getChannelIdsJson());
        statement.setInt(8, toSqlBoolean(rule.getEnabled()));
        bindNullableInteger(statement, 9, rule.getCooldownSeconds());
        statement.setLong(10, rule.getCreatedAt().longValue());
        statement.setLong(11, rule.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, AlertRuleDO rule) throws SQLException {
        statement.setString(1, rule.getRuleName());
        statement.setString(2, rule.getAlertType());
        bindNullableLong(statement, 3, rule.getTaskId());
        statement.setString(4, rule.getTableName());
        statement.setString(5, rule.getAlertLevel());
        statement.setString(6, rule.getAlertContentTemplate());
        statement.setString(7, rule.getChannelIdsJson());
        statement.setInt(8, toSqlBoolean(rule.getEnabled()));
        bindNullableInteger(statement, 9, rule.getCooldownSeconds());
        statement.setLong(10, rule.getUpdatedAt().longValue());
        statement.setLong(11, rule.getId().longValue());
    }

    private AlertRuleDO mapRow(ResultSet resultSet) throws SQLException {
        AlertRuleDO rule = new AlertRuleDO();
        rule.setId(Long.valueOf(resultSet.getLong("id")));
        rule.setRuleName(resultSet.getString("rule_name"));
        rule.setAlertType(resultSet.getString("alert_type"));
        rule.setTaskId(toLong(resultSet.getObject("task_id")));
        rule.setTableName(resultSet.getString("table_name"));
        rule.setAlertLevel(resultSet.getString("alert_level"));
        rule.setAlertContentTemplate(resultSet.getString("alert_content_template"));
        rule.setChannelIdsJson(resultSet.getString("channel_ids_json"));
        rule.setEnabled(toBoolean(resultSet.getObject("enabled")));
        rule.setCooldownSeconds(toInteger(resultSet.getObject("cooldown_seconds")));
        rule.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        rule.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return rule;
    }

    private void bindNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value.longValue());
    }

    private void bindNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.intValue());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(((Number) value).longValue());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(((Number) value).intValue());
    }

    private Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue() != 0;
    }

    private int toSqlBoolean(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }
}
