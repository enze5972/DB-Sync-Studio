package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.alert.AlertChannel;
import com.dbsyncstudio.model.alert.AlertChannelRepository;
import com.dbsyncstudio.model.alert.AlertChannelType;

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
public class SqliteAlertChannelRepository implements AlertChannelRepository {

    private static final String INSERT_SQL =
            "INSERT INTO alert_channels (channel_name, channel_type, enabled, smtp_host, smtp_port, smtp_username, smtp_password_encrypted, smtp_to_address, smtp_from_address, webhook_url, webhook_token_encrypted, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE alert_channels SET channel_name = ?, channel_type = ?, enabled = ?, smtp_host = ?, smtp_port = ?, smtp_username = ?, smtp_password_encrypted = ?, smtp_to_address = ?, smtp_from_address = ?, webhook_url = ?, webhook_token_encrypted = ?, updated_at = ? WHERE id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, channel_name, channel_type, enabled, smtp_host, smtp_port, smtp_username, smtp_password_encrypted, smtp_to_address, smtp_from_address, webhook_url, webhook_token_encrypted, created_at, updated_at FROM alert_channels WHERE id = ?";
    private static final String FIND_ALL_SQL =
            "SELECT id, channel_name, channel_type, enabled, smtp_host, smtp_port, smtp_username, smtp_password_encrypted, smtp_to_address, smtp_from_address, webhook_url, webhook_token_encrypted, created_at, updated_at FROM alert_channels ORDER BY id DESC";
    private static final String FIND_ENABLED_SQL =
            "SELECT id, channel_name, channel_type, enabled, smtp_host, smtp_port, smtp_username, smtp_password_encrypted, smtp_to_address, smtp_from_address, webhook_url, webhook_token_encrypted, created_at, updated_at FROM alert_channels WHERE enabled = 1 ORDER BY id DESC";
    private static final String DELETE_SQL =
            "DELETE FROM alert_channels WHERE id = ?";

    @NonNull
    private final SqliteConnectionFactory connectionFactory;
    @NonNull
    private final LocalSecretCryptoService cryptoService;

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            SqliteSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long save(AlertChannel channel) throws SQLException {
        long now = System.currentTimeMillis();
        if (channel.getCreatedAt() == null) {
            channel.setCreatedAt(Long.valueOf(now));
        }
        channel.setUpdatedAt(Long.valueOf(now));
        try (Connection connection = connectionFactory.openConnection()) {
            if (channel.getId() == null) {
                try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                    bindInsertParameters(statement, channel);
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            long id = generatedKeys.getLong(1);
                            channel.setId(Long.valueOf(id));
                            return id;
                        }
                    }
                    throw new SQLException("Insert alert channel failed, no generated key returned");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bindUpdateParameters(statement, channel);
                statement.executeUpdate();
                return channel.getId().longValue();
            }
        }
    }

    @Override
    public Optional<AlertChannel> findById(long id) throws SQLException {
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
    public List<AlertChannel> findAll() throws SQLException {
        return readList(FIND_ALL_SQL);
    }

    @Override
    public List<AlertChannel> findEnabled() throws SQLException {
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

    private List<AlertChannel> readList(String sql) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<AlertChannel> result = new ArrayList<AlertChannel>();
            while (resultSet.next()) {
                result.add(mapRow(resultSet));
            }
            return result;
        }
    }

    private void bindInsertParameters(PreparedStatement statement, AlertChannel channel) throws SQLException {
        statement.setString(1, channel.getChannelName());
        statement.setString(2, channel.getChannelType().name());
        statement.setInt(3, toSqlBoolean(channel.getEnabled()));
        statement.setString(4, channel.getSmtpHost());
        bindNullableInteger(statement, 5, channel.getSmtpPort());
        statement.setString(6, channel.getSmtpUsername());
        statement.setString(7, cryptoService.encrypt(channel.getSmtpPassword()));
        statement.setString(8, channel.getSmtpToAddress());
        statement.setString(9, channel.getSmtpFromAddress());
        statement.setString(10, channel.getWebhookUrl());
        statement.setString(11, cryptoService.encrypt(channel.getWebhookToken()));
        statement.setLong(12, channel.getCreatedAt().longValue());
        statement.setLong(13, channel.getUpdatedAt().longValue());
    }

    private void bindUpdateParameters(PreparedStatement statement, AlertChannel channel) throws SQLException {
        statement.setString(1, channel.getChannelName());
        statement.setString(2, channel.getChannelType().name());
        statement.setInt(3, toSqlBoolean(channel.getEnabled()));
        statement.setString(4, channel.getSmtpHost());
        bindNullableInteger(statement, 5, channel.getSmtpPort());
        statement.setString(6, channel.getSmtpUsername());
        statement.setString(7, cryptoService.encrypt(channel.getSmtpPassword()));
        statement.setString(8, channel.getSmtpToAddress());
        statement.setString(9, channel.getSmtpFromAddress());
        statement.setString(10, channel.getWebhookUrl());
        statement.setString(11, cryptoService.encrypt(channel.getWebhookToken()));
        statement.setLong(12, channel.getUpdatedAt().longValue());
        statement.setLong(13, channel.getId().longValue());
    }

    private AlertChannel mapRow(ResultSet resultSet) throws SQLException {
        AlertChannel channel = new AlertChannel();
        channel.setId(Long.valueOf(resultSet.getLong("id")));
        channel.setChannelName(resultSet.getString("channel_name"));
        channel.setChannelType(AlertChannelType.valueOf(resultSet.getString("channel_type")));
        channel.setEnabled(toBoolean(resultSet.getObject("enabled")));
        channel.setSmtpHost(resultSet.getString("smtp_host"));
        channel.setSmtpPort(toInteger(resultSet.getObject("smtp_port")));
        channel.setSmtpUsername(resultSet.getString("smtp_username"));
        channel.setSmtpPassword(cryptoService.decrypt(resultSet.getString("smtp_password_encrypted")));
        channel.setSmtpToAddress(resultSet.getString("smtp_to_address"));
        channel.setSmtpFromAddress(resultSet.getString("smtp_from_address"));
        channel.setWebhookUrl(resultSet.getString("webhook_url"));
        channel.setWebhookToken(cryptoService.decrypt(resultSet.getString("webhook_token_encrypted")));
        channel.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        channel.setUpdatedAt(Long.valueOf(resultSet.getLong("updated_at")));
        return channel;
    }

    private void bindNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.intValue());
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
