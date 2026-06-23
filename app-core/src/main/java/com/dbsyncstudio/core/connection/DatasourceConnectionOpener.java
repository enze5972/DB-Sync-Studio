package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatasourceConnectionOpener {

    Connection open(DatasourceConfigDO config) throws SQLException;
}

