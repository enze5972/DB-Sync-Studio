package com.dbsyncstudio.core.metadata;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseMetadataScanner {

    List<SchemaMetadataDO> scan(DatasourceConfigDO config) throws SQLException;

    List<SchemaMetadataDO> scan(Connection connection) throws SQLException;
}

