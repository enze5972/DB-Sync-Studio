package com.dbsyncstudio.core.metadata;

import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

public class JdbcDatabaseMetadataScannerTest {

    @Test
    public void shouldScanSchemaTableColumnsAndPrimaryKey() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:dbsyncstudio_scan;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE customer (" +
                    "id BIGINT PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "age INT" +
                    ")");
        }

        JdbcDatabaseMetadataScanner scanner = new JdbcDatabaseMetadataScanner();
        List<SchemaMetadataDO> schemas;
        try (Connection connection = dataSource.getConnection()) {
            schemas = scanner.scan(connection);
        }

        Assert.assertFalse(schemas.isEmpty());

        TableMetadataDO tableMetadata = findTable(schemas, "customer");
        Assert.assertNotNull(tableMetadata);
        Assert.assertEquals(3, tableMetadata.getColumns().size());

        ColumnMetadataDO idColumn = findColumn(tableMetadata, "id");
        ColumnMetadataDO nameColumn = findColumn(tableMetadata, "name");
        Assert.assertNotNull(idColumn);
        Assert.assertNotNull(nameColumn);
        Assert.assertEquals("ID", idColumn.getName().toUpperCase());
        Assert.assertTrue(idColumn.isPrimaryKey());
        Assert.assertFalse(nameColumn.isPrimaryKey());
    }

    private ColumnMetadataDO findColumn(TableMetadataDO tableMetadata, String columnName) {
        for (ColumnMetadataDO columnMetadata : tableMetadata.getColumns()) {
            if (columnMetadata.getName() != null && columnName.equalsIgnoreCase(columnMetadata.getName())) {
                return columnMetadata;
            }
        }
        return null;
    }

    private TableMetadataDO findTable(List<SchemaMetadataDO> schemas, String tableName) {
        for (SchemaMetadataDO schemaMetadata : schemas) {
            if (schemaMetadata.getTables() == null) {
                continue;
            }
            for (TableMetadataDO tableMetadata : schemaMetadata.getTables()) {
                if (tableMetadata.getTableName() != null
                        && tableName.equalsIgnoreCase(tableMetadata.getTableName())) {
                    return tableMetadata;
                }
            }
        }
        return null;
    }
}
