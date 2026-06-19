package com.dbsyncstudio.core.metadata;

import com.dbsyncstudio.core.connection.JdbcConnectionDescriptor;
import com.dbsyncstudio.core.connection.JdbcConnectionSupport;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.metadata.IndexMetadata;
import com.dbsyncstudio.model.metadata.SchemaMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class JdbcDatabaseMetadataScanner implements DatabaseMetadataScanner {

    @Override
    public List<SchemaMetadata> scan(DatasourceConfig config) throws SQLException {
        if (config == null) {
            throw new SQLException("Datasource config must not be null");
        }
        JdbcConnectionDescriptor descriptor = JdbcConnectionSupport.resolve(config);
        try {
            Class.forName(descriptor.getDriverClassName());
        } catch (ClassNotFoundException ex) {
            throw new SQLException("JDBC driver not found: " + descriptor.getDriverClassName(), ex);
        }

        try (Connection connection = openConnection(descriptor.getJdbcUrl(), config)) {
            return scan(connection);
        }
    }

    @Override
    public List<SchemaMetadata> scan(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = safeCatalog(connection);
        Map<String, SchemaMetadata> schemaMap = new LinkedHashMap<String, SchemaMetadata>();

        try (ResultSet tables = metaData.getTables(catalog, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String schemaName = normalizeSchemaName(tables.getString("TABLE_SCHEM"));
                String tableName = tables.getString("TABLE_NAME");
                if (isSystemSchema(schemaName)) {
                    continue;
                }
                TableMetadata tableMetadata = new TableMetadata();
                tableMetadata.setSchemaName(schemaName);
                tableMetadata.setTableName(tableName);
                tableMetadata.setColumns(loadColumns(metaData, catalog, schemaName, tableName));
                tableMetadata.setIndexes(loadIndexes(metaData, catalog, schemaName, tableName));

                SchemaMetadata schemaMetadata = schemaMap.get(schemaName);
                if (schemaMetadata == null) {
                    schemaMetadata = new SchemaMetadata();
                    schemaMetadata.setSchemaName(schemaName);
                    schemaMetadata.setTables(new ArrayList<TableMetadata>());
                    schemaMap.put(schemaName, schemaMetadata);
                }
                schemaMetadata.getTables().add(tableMetadata);
            }
        }

        return new ArrayList<SchemaMetadata>(schemaMap.values());
    }

    private List<ColumnMetadata> loadColumns(DatabaseMetaData metaData, String catalog, String schemaName, String tableName)
            throws SQLException {
        Map<String, ColumnMetadata> columnMap = new LinkedHashMap<String, ColumnMetadata>();
        try (ResultSet columns = metaData.getColumns(catalog, schemaName, tableName, "%")) {
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                ColumnMetadata columnMetadata = new ColumnMetadata();
                columnMetadata.setName(columnName);
                columnMetadata.setDataType(columns.getString("TYPE_NAME"));
                columnMetadata.setColumnSize(Integer.valueOf(columns.getInt("COLUMN_SIZE")));
                columnMetadata.setDecimalDigits(Integer.valueOf(columns.getInt("DECIMAL_DIGITS")));
                columnMetadata.setNullable("YES".equalsIgnoreCase(columns.getString("IS_NULLABLE")));
                columnMetadata.setAutoIncrement("YES".equalsIgnoreCase(columns.getString("IS_AUTOINCREMENT")));
                columnMetadata.setDefaultValue(columns.getString("COLUMN_DEF"));
                columnMap.put(columnName, columnMetadata);
            }
        }

        Set<String> primaryKeyColumns = loadPrimaryKeys(metaData, catalog, schemaName, tableName);
        for (Map.Entry<String, ColumnMetadata> entry : columnMap.entrySet()) {
            if (primaryKeyColumns.contains(entry.getKey())) {
                entry.getValue().setPrimaryKey(true);
            }
        }
        return new ArrayList<ColumnMetadata>(columnMap.values());
    }

    private Set<String> loadPrimaryKeys(DatabaseMetaData metaData, String catalog, String schemaName, String tableName)
            throws SQLException {
        Set<String> primaryKeyColumns = new LinkedHashSet<String>();
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schemaName, tableName)) {
            while (primaryKeys.next()) {
                primaryKeyColumns.add(primaryKeys.getString("COLUMN_NAME"));
            }
        }
        return primaryKeyColumns;
    }

    private List<IndexMetadata> loadIndexes(DatabaseMetaData metaData, String catalog, String schemaName, String tableName)
            throws SQLException {
        Map<String, IndexMetadata> indexMap = new LinkedHashMap<String, IndexMetadata>();
        try (ResultSet indexInfo = metaData.getIndexInfo(catalog, schemaName, tableName, false, false)) {
            while (indexInfo.next()) {
                String indexName = indexInfo.getString("INDEX_NAME");
                String columnName = indexInfo.getString("COLUMN_NAME");
                if (indexName == null || columnName == null) {
                    continue;
                }
                if (indexInfo.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }
                IndexMetadata indexMetadata = indexMap.get(indexName);
                if (indexMetadata == null) {
                    indexMetadata = new IndexMetadata();
                    indexMetadata.setName(indexName);
                    indexMetadata.setUnique(!indexInfo.getBoolean("NON_UNIQUE"));
                    indexMetadata.setColumnNames(new ArrayList<String>());
                    indexMap.put(indexName, indexMetadata);
                }
                indexMetadata.getColumnNames().add(columnName);
            }
        }
        return new ArrayList<IndexMetadata>(indexMap.values());
    }

    private Connection openConnection(String jdbcUrl, DatasourceConfig config) throws SQLException {
        if (config.getUsername() == null && config.getPassword() == null) {
            return DriverManager.getConnection(jdbcUrl);
        }

        Properties properties = new Properties();
        if (config.getUsername() != null) {
            properties.setProperty("user", config.getUsername());
        }
        if (config.getPassword() != null) {
            properties.setProperty("password", config.getPassword());
        }
        return DriverManager.getConnection(jdbcUrl, properties);
    }

    private String safeCatalog(Connection connection) throws SQLException {
        String catalog = connection.getCatalog();
        if (catalog == null) {
            return null;
        }
        if (catalog.trim().length() == 0) {
            return null;
        }
        return catalog;
    }

    private String normalizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.trim().length() == 0) {
            return "default";
        }
        return schemaName.trim();
    }

    private boolean isSystemSchema(String schemaName) {
        if (schemaName == null) {
            return false;
        }
        String lowerCaseSchemaName = schemaName.toLowerCase();
        return "information_schema".equals(lowerCaseSchemaName)
                || "pg_catalog".equals(lowerCaseSchemaName)
                || "pg_toast".equals(lowerCaseSchemaName)
                || "performance_schema".equals(lowerCaseSchemaName)
                || "sys".equals(lowerCaseSchemaName)
                || "system".equals(lowerCaseSchemaName)
                || "system_lobs".equals(lowerCaseSchemaName);
    }
}
