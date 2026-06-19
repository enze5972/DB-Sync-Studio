package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.ConnectionTestResult;
import com.dbsyncstudio.model.datasource.DatasourceConfig;

public interface DatasourceConnectionTester {

    ConnectionTestResult test(DatasourceConfig config);
}

