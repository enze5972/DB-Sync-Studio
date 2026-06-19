package com.dbsyncstudio.core.connection;

public class JdbcConnectionDescriptor {

    private final String driverClassName;
    private final String jdbcUrl;

    public JdbcConnectionDescriptor(String driverClassName, String jdbcUrl) {
        this.driverClassName = driverClassName;
        this.jdbcUrl = jdbcUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
