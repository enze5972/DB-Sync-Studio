package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.vo.ConnectionTestResultVO;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

public interface DatasourceConnectionTester {

    ConnectionTestResultVO test(DatasourceConfigDO config);
}

