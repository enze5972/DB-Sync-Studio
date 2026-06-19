package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceRepository;
import com.dbsyncstudio.model.datasource.DatasourceType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqliteDatasourceRepository implements DatasourceRepository {

    private static final String INSERT_SQL =
            "INSERT INTO datasource_config (name, type, host, port, database_name, username, password, remark, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE datasource_config SET name = ?, type = ?, host = ?, port = ?, database_name = ?, username = ?, password = ?, remark = ?, updated_at = ? " +
            "WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, name, type, host, port, database_name, username, password, remark, created_at, updated_at FROM datasource_config WHERE id = ?";
    private static final String FIND_ALL_SQL =
            "SELECT id, name, type, host, port, database_name, username, password, remark, created_at, updated_at FROM datasource_config ORDER BY id DESC";
    private static final String DELETE_SQL =
            "DELETE FROM datasource_config WHERE id = ?";

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
    public long save(DatasourceConfig config) throws SQLException {
        long now = System.currentTimeMillis();
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(Long.valueOf(now));
        }
        config.setUpdatedAt(Long.valueOf(now));

        try (Connection connection = connectionFactory.openConnection()) {
            if (config.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, config);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            config.setId(Long.valueOf(id));
                            return id;
                        }
                        throw new SQLException("Insert datasource config failed, no generated key returned");
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, config);
                statement.executeUpdate();
                return config.getId().longValue();
            }
        }
    }

    @Override
    public Optional<DatasourceConfig> findById(long id) throws SQLException {
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
    public List<DatasourceConfig> findAll() throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            List<DatasourceConfig> result = new ArrayList<DatasourceConfig>();
            while (resultSet.next()) {
                result.add(mapRow(resultSet));
            }
            return result;
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

    private void bindInsertParameters(PreparedStatement statement, DatasourceConfig config) throws SQLException {
        statement.setString(1, config.getName());
        statement.setString(2, config.getType().name());
        statement.setString(3, config.getHost());
        statement.setInt(4, config.getPort().intValue());
        statement.setString(5, config.getDatabaseName());
        statement.setString(6, config.getUsername());
        statement.setString(7, config.getPassword());
        statement.setString(8, config.getRemark());
        statement.setLong(9, config.getCreatedAt().longValue());
        statement.setLong(10, config.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, DatasourceConfig config) throws SQLException {
        statement.setString(1, config.getName());
        statement.setString(2, config.getType().name());
        statement.setString(3, config.getHost());
        statement.setInt(4, config.getPort().intValue());
        statement.setString(5, config.getDatabaseName());
        statement.setString(6, config.getUsername());
        statement.setString(7, config.getPassword());
        statement.setString(8, config.getRemark());
        statement.setLong(9, config.getUpdatedAt().longValue());
        statement.setLong(10, config.getId().longValue());
    }

    private DatasourceConfig mapRow(ResultSet resultSet) throws SQLException {
        DatasourceConfig config = new DatasourceConfig();
        config.setId(Long.valueOf(resultSet.getLong("id")));
        config.setName(resultSet.getString("name"));
        config.setType(DatasourceType.valueOf(resultSet.getString("type")));
        config.setHost(resultSet.getString("host"));
        config.setPort(Integer.valueOf(resultSet.getInt("port")));
        config.setDatabaseName(resultSet.getString("database_name"));
        config.setUsername(resultSet.getString("username"));
        config.setPassword(resultSet.getString("password"));
        config.setRemark(resultSet.getString("remark"));
        config.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        config.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return config;
    }
}
