package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.store.repository.FieldMappingRepository;
import com.dbsyncstudio.model.sync.entity.FieldMappingRuleDO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FieldMappingRepositoryImpl implements FieldMappingRepository {

    private static final String INSERT_SQL =
            "INSERT INTO field_mapping (task_id, source_schema_name, target_schema_name, source_table_name, target_table_name, source_column_name, target_column_name, ignored, default_value, transform_rule, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE field_mapping SET task_id = ?, source_schema_name = ?, target_schema_name = ?, source_table_name = ?, target_table_name = ?, source_column_name = ?, target_column_name = ?, ignored = ?, default_value = ?, transform_rule = ?, updated_at = ? "
                    + "WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, task_id, source_schema_name, target_schema_name, source_table_name, target_table_name, source_column_name, target_column_name, ignored, default_value, transform_rule, created_at, updated_at "
                    + "FROM field_mapping WHERE id = ?";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, task_id, source_schema_name, target_schema_name, source_table_name, target_table_name, source_column_name, target_column_name, ignored, default_value, transform_rule, created_at, updated_at "
                    + "FROM field_mapping WHERE task_id = ? ORDER BY id DESC";
    private static final String DELETE_SQL =
            "DELETE FROM field_mapping WHERE id = ?";

    private final DatabaseConnectionFactory connectionFactory;

    public FieldMappingRepositoryImpl(DatabaseConnectionFactory connectionFactory) {
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
    public long save(FieldMappingRuleDO mappingRule) throws SQLException {
        long now = System.currentTimeMillis();
        if (mappingRule.getCreatedAt() == null) {
            mappingRule.setCreatedAt(Long.valueOf(now));
        }
        mappingRule.setUpdatedAt(Long.valueOf(now));

        try (Connection connection = connectionFactory.openConnection()) {
            if (mappingRule.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, mappingRule);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            mappingRule.setId(Long.valueOf(id));
                            return id;
                        }
                        throw new SQLException("Insert field mapping failed, no generated key returned");
                    }
                }
            }

            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, mappingRule);
                statement.executeUpdate();
                return mappingRule.getId().longValue();
            }
        }
    }

    @Override
    public Optional<FieldMappingRuleDO> findById(long id) throws SQLException {
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
    public List<FieldMappingRuleDO> findByTaskId(long taskId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TASK_ID_SQL)) {
            statement.setLong(1, taskId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<FieldMappingRuleDO> result = new ArrayList<FieldMappingRuleDO>();
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

    private void bindInsertParameters(PreparedStatement statement, FieldMappingRuleDO mappingRule) throws SQLException {
        statement.setLong(1, mappingRule.getTaskId().longValue());
        statement.setString(2, mappingRule.getSourceSchemaName());
        statement.setString(3, mappingRule.getTargetSchemaName());
        statement.setString(4, mappingRule.getSourceTableName());
        statement.setString(5, mappingRule.getTargetTableName());
        statement.setString(6, mappingRule.getSourceColumnName());
        statement.setString(7, mappingRule.getTargetColumnName());
        statement.setInt(8, mappingRule.isIgnored() ? 1 : 0);
        statement.setString(9, mappingRule.getDefaultValue());
        statement.setString(10, mappingRule.getTransformRule());
        statement.setLong(11, mappingRule.getCreatedAt().longValue());
        statement.setLong(12, mappingRule.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, FieldMappingRuleDO mappingRule) throws SQLException {
        statement.setLong(1, mappingRule.getTaskId().longValue());
        statement.setString(2, mappingRule.getSourceSchemaName());
        statement.setString(3, mappingRule.getTargetSchemaName());
        statement.setString(4, mappingRule.getSourceTableName());
        statement.setString(5, mappingRule.getTargetTableName());
        statement.setString(6, mappingRule.getSourceColumnName());
        statement.setString(7, mappingRule.getTargetColumnName());
        statement.setInt(8, mappingRule.isIgnored() ? 1 : 0);
        statement.setString(9, mappingRule.getDefaultValue());
        statement.setString(10, mappingRule.getTransformRule());
        statement.setLong(11, mappingRule.getUpdatedAt().longValue());
        statement.setLong(12, mappingRule.getId().longValue());
    }

    private FieldMappingRuleDO mapRow(ResultSet resultSet) throws SQLException {
        FieldMappingRuleDO mappingRule = new FieldMappingRuleDO();
        mappingRule.setId(Long.valueOf(resultSet.getLong("id")));
        mappingRule.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
        mappingRule.setSourceSchemaName(resultSet.getString("source_schema_name"));
        mappingRule.setTargetSchemaName(resultSet.getString("target_schema_name"));
        mappingRule.setSourceTableName(resultSet.getString("source_table_name"));
        mappingRule.setTargetTableName(resultSet.getString("target_table_name"));
        mappingRule.setSourceColumnName(resultSet.getString("source_column_name"));
        mappingRule.setTargetColumnName(resultSet.getString("target_column_name"));
        mappingRule.setIgnored(resultSet.getInt("ignored") == 1);
        mappingRule.setDefaultValue(resultSet.getString("default_value"));
        mappingRule.setTransformRule(resultSet.getString("transform_rule"));
        mappingRule.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        mappingRule.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return mappingRule;
    }
}
