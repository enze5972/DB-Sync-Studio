package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class DefaultDatasourceConnectionOpener implements DatasourceConnectionOpener {

    @Override
    public Connection open(DatasourceConfig config) throws SQLException {
        return JdbcConnectionSupport.openConnection(config);
    }
}
