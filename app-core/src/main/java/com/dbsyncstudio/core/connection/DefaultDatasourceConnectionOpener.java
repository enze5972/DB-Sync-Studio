package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

import java.sql.Connection;
import java.sql.SQLException;

public class DefaultDatasourceConnectionOpener implements DatasourceConnectionOpener {

    @Override
    public Connection open(DatasourceConfigDO config) throws SQLException {
        return JdbcConnectionSupport.openConnection(config);
    }
}
