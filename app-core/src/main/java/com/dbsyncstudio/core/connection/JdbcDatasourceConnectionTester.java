package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.vo.ConnectionTestResultVO;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcDatasourceConnectionTester implements DatasourceConnectionTester {

    @Override
    public ConnectionTestResultVO test(DatasourceConfigDO config) {
        long startTime = System.currentTimeMillis();
        if (config == null) {
            return buildFailure(startTime, "Invalid datasource config: datasource must not be null");
        }
        try {
            try (Connection connection = JdbcConnectionSupport.openConnection(config)) {
                long costMillis = System.currentTimeMillis() - startTime;
                String productName = connection.getMetaData().getDatabaseProductName();
                return ConnectionTestResultVO.builder()
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

    private ConnectionTestResultVO buildFailure(long startTime, String message) {
        long costMillis = System.currentTimeMillis() - startTime;
        return ConnectionTestResultVO.builder()
                .success(false)
                .message(message)
                .costMillis(costMillis)
                .build();
    }
}
