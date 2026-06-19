package com.dbsyncstudio.core.metadata;

import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.metadata.SchemaMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;

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
        List<SchemaMetadata> schemas;
        try (Connection connection = dataSource.getConnection()) {
            schemas = scanner.scan(connection);
        }

        Assert.assertFalse(schemas.isEmpty());

        TableMetadata tableMetadata = findTable(schemas, "customer");
        Assert.assertNotNull(tableMetadata);
        Assert.assertEquals(3, tableMetadata.getColumns().size());

        ColumnMetadata idColumn = findColumn(tableMetadata, "id");
        ColumnMetadata nameColumn = findColumn(tableMetadata, "name");
        Assert.assertNotNull(idColumn);
        Assert.assertNotNull(nameColumn);
        Assert.assertEquals("ID", idColumn.getName().toUpperCase());
        Assert.assertTrue(idColumn.isPrimaryKey());
        Assert.assertFalse(nameColumn.isPrimaryKey());
    }

    private ColumnMetadata findColumn(TableMetadata tableMetadata, String columnName) {
        for (ColumnMetadata columnMetadata : tableMetadata.getColumns()) {
            if (columnMetadata.getName() != null && columnName.equalsIgnoreCase(columnMetadata.getName())) {
                return columnMetadata;
            }
        }
        return null;
    }

    private TableMetadata findTable(List<SchemaMetadata> schemas, String tableName) {
        for (SchemaMetadata schemaMetadata : schemas) {
            if (schemaMetadata.getTables() == null) {
                continue;
            }
            for (TableMetadata tableMetadata : schemaMetadata.getTables()) {
                if (tableMetadata.getTableName() != null
                        && tableName.equalsIgnoreCase(tableMetadata.getTableName())) {
                    return tableMetadata;
                }
            }
        }
        return null;
    }
}
