package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.ConnectionTestResult;
import com.dbsyncstudio.model.datasource.DatasourceConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcDatasourceConnectionTester implements DatasourceConnectionTester {

    @Override
    public ConnectionTestResult test(DatasourceConfig config) {
        long startTime = System.currentTimeMillis();
        if (config == null) {
            return buildFailure(startTime, "Invalid datasource config: datasource must not be null");
        }
        try {
            try (Connection connection = JdbcConnectionSupport.openConnection(config)) {
                long costMillis = System.currentTimeMillis() - startTime;
                String productName = connection.getMetaData().getDatabaseProductName();
                return ConnectionTestResult.builder()
                        .success(true)
                        .message("Connection successful: " + productName)
                        .costMillis(costMillis)
                        .build();
            }
        } catch (SQLException ex) {
            return buildFailure(startTime, "Connection failed: " + ex.getMessage());
        } catch (RuntimeException ex) {
            return buildFailure(startTime, "Invalid datasource config: " + ex.getMessage());
        }
    }

    private ConnectionTestResult buildFailure(long startTime, String message) {
        long costMillis = System.currentTimeMillis() - startTime;
        return ConnectionTestResult.builder()
                .success(false)
                .message(message)
                .costMillis(costMillis)
                .build();
    }
}
