package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatasourceConnectionOpener {

    Connection open(DatasourceConfig config) throws SQLException;
}

