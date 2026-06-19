package com.dbsyncstudio.core.metadata;

import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.metadata.SchemaMetadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DatabaseMetadataScanner {

    List<SchemaMetadata> scan(DatasourceConfig config) throws SQLException;

    List<SchemaMetadata> scan(Connection connection) throws SQLException;
}

