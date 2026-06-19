package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;

import lombok.experimental.UtilityClass;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@UtilityClass
public class JdbcConnectionSupport {

    private static final int MYSQL_DEFAULT_PORT = 3306;
    private static final int POSTGRESQL_DEFAULT_PORT = 5432;
    private static final int DM_DEFAULT_PORT = 5236;

    public static JdbcConnectionDescriptor resolve(DatasourceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Datasource config must not be null");
        }

        DatasourceType type = config.getType();
        if (type == null) {
            throw new IllegalArgumentException("Datasource type must not be null");
        }

        return new JdbcConnectionDescriptor(resolveDriverClassName(type), resolveJdbcUrl(config));
    }

    public static String resolveDriverClassName(DatasourceType type) {
        switch (type) {
            case MYSQL:
                return "com.mysql.cj.jdbc.Driver";
            case POSTGRESQL:
                return "org.postgresql.Driver";
            case DM:
                return "dm.jdbc.driver.DmDriver";
            default:
                throw new IllegalArgumentException("Unsupported datasource type: " + type);
        }
    }

    public static String resolveJdbcUrl(DatasourceConfig config) {
        int port = resolvePort(config);
        String databaseName = normalizeDatabaseName(config.getDatabaseName());
        switch (config.getType()) {
            case MYSQL:
                return "jdbc:mysql://" + config.getHost() + ":" + port + "/" + databaseName
                        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            case POSTGRESQL:
                return "jdbc:postgresql://" + config.getHost() + ":" + port + "/" + databaseName;
            case DM:
                return "jdbc:dm://" + config.getHost() + ":" + port + "/" + databaseName;
            default:
                throw new IllegalArgumentException("Unsupported datasource type: " + config.getType());
        }
    }

    public static Connection openConnection(DatasourceConfig config) throws SQLException {
        JdbcConnectionDescriptor descriptor = resolve(config);
        try {
            Class.forName(descriptor.getDriverClassName());
        } catch (ClassNotFoundException ex) {
            throw new SQLException("JDBC driver not found: " + descriptor.getDriverClassName(), ex);
        }

        if (config.getUsername() == null && config.getPassword() == null) {
            return DriverManager.getConnection(descriptor.getJdbcUrl());
        }

        Properties properties = new Properties();
        if (config.getUsername() != null) {
            properties.setProperty("user", config.getUsername());
        }
        if (config.getPassword() != null) {
            properties.setProperty("password", config.getPassword());
        }
        return DriverManager.getConnection(descriptor.getJdbcUrl(), properties);
    }

    private static int resolvePort(DatasourceConfig config) {
        if (config.getPort() != null) {
            return config.getPort().intValue();
        }
        switch (config.getType()) {
            case MYSQL:
                return MYSQL_DEFAULT_PORT;
            case POSTGRESQL:
                return POSTGRESQL_DEFAULT_PORT;
            case DM:
                return DM_DEFAULT_PORT;
            default:
                throw new IllegalArgumentException("Unsupported datasource type: " + config.getType());
        }
    }

    private static String normalizeDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().length() == 0) {
            return "";
        }
        return databaseName.trim();
    }
}
