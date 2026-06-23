package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;
import com.dbsyncstudio.store.repository.TransformRuleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FieldTransformRuleRepositoryImpl implements TransformRuleRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sync_field_transform_rules (task_id, table_task_id, field_mapping_id, source_field, target_field, transform_type, transform_config, transform_order, enabled, on_error, default_value, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE sync_field_transform_rules SET task_id = ?, table_task_id = ?, field_mapping_id = ?, source_field = ?, target_field = ?, transform_type = ?, transform_config = ?, transform_order = ?, enabled = ?, on_error = ?, default_value = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, task_id, table_task_id, field_mapping_id, source_field, target_field, transform_type, transform_config, transform_order, enabled, on_error, default_value, created_at, updated_at FROM sync_field_transform_rules WHERE id = ?";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, task_id, table_task_id, field_mapping_id, source_field, target_field, transform_type, transform_config, transform_order, enabled, on_error, default_value, created_at, updated_at FROM sync_field_transform_rules WHERE task_id = ? ORDER BY COALESCE(table_task_id, 0), COALESCE(field_mapping_id, 0), transform_order, id";
    private static final String DELETE_SQL = "DELETE FROM sync_field_transform_rules WHERE id = ?";

    private final DatabaseConnectionFactory connectionFactory;

    public FieldTransformRuleRepositoryImpl(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            DatabaseSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(TransformRuleDO rule) throws SQLException {
        long now = System.currentTimeMillis();
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(Long.valueOf(now));
        }
        rule.setUpdatedAt(Long.valueOf(now));
        if (rule.getTransformOrder() == null) {
            rule.setTransformOrder(Integer.valueOf(0));
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(Boolean.TRUE);
        }
        if (rule.getOnError() == null) {
            rule.setOnError(TransformErrorStrategy.FAIL);
        }

        try (Connection connection = connectionFactory.openConnection()) {
            if (rule.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsert(statement, rule);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            rule.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert transform rule failed, no generated key returned");
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdate(statement, rule);
                statement.executeUpdate();
                return rule.getId().longValue();
            }
        }
    }

    @Override
    public Optional<TransformRuleDO> findById(long id) throws SQLException {
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
    public List<TransformRuleDO> findByTaskId(long taskId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TransformRuleDO> result = new ArrayList<TransformRuleDO>();
                while (resultSet.next()) {
                    result.add(mapRow(resultSet));
                }
                return result;
            }
        }
    }

    @Override
    public boolean deleteById(long id) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void bindInsert(PreparedStatement statement, TransformRuleDO rule) throws SQLException {
        statement.setLong(1, rule.getTaskId().longValue());
        bindNullableLong(statement, 2, rule.getTableTaskId());
        bindNullableLong(statement, 3, rule.getFieldMappingId());
        statement.setString(4, rule.getSourceField());
        statement.setString(5, rule.getTargetField());
        statement.setString(6, rule.getTransformType());
        statement.setString(7, rule.getTransformConfig());
        statement.setInt(8, rule.getTransformOrder().intValue());
        statement.setInt(9, rule.getEnabled().booleanValue() ? 1 : 0);
        statement.setString(10, rule.getOnError().name());
        statement.setString(11, rule.getDefaultValue());
        statement.setLong(12, rule.getCreatedAt().longValue());
        statement.setLong(13, rule.getUpdatedAt().longValue());
    }

    private void bindUpdate(PreparedStatement statement, TransformRuleDO rule) throws SQLException {
        statement.setLong(1, rule.getTaskId().longValue());
        bindNullableLong(statement, 2, rule.getTableTaskId());
        bindNullableLong(statement, 3, rule.getFieldMappingId());
        statement.setString(4, rule.getSourceField());
        statement.setString(5, rule.getTargetField());
        statement.setString(6, rule.getTransformType());
        statement.setString(7, rule.getTransformConfig());
        statement.setInt(8, rule.getTransformOrder().intValue());
        statement.setInt(9, rule.getEnabled().booleanValue() ? 1 : 0);
        statement.setString(10, rule.getOnError().name());
        statement.setString(11, rule.getDefaultValue());
        statement.setLong(12, rule.getUpdatedAt().longValue());
        statement.setLong(13, rule.getId().longValue());
    }

    private TransformRuleDO mapRow(ResultSet resultSet) throws SQLException {
        TransformRuleDO rule = new TransformRuleDO();
        rule.setId(Long.valueOf(resultSet.getLong("id")));
        rule.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
        rule.setTableTaskId(toLong(resultSet.getObject("table_task_id")));
        rule.setFieldMappingId(toLong(resultSet.getObject("field_mapping_id")));
        rule.setSourceField(resultSet.getString("source_field"));
        rule.setTargetField(resultSet.getString("target_field"));
        rule.setTransformType(resultSet.getString("transform_type"));
        rule.setTransformConfig(resultSet.getString("transform_config"));
        rule.setTransformOrder(Integer.valueOf(resultSet.getInt("transform_order")));
        rule.setEnabled(resultSet.getInt("enabled") == 1);
        rule.setOnError(TransformErrorStrategy.fromValue(resultSet.getString("on_error")));
        rule.setDefaultValue(resultSet.getString("default_value"));
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

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(((Number) value).longValue());
    }
}
