package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.DatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.model.datasource.ConnectionTestResult;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.metadata.SchemaMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;
import com.dbsyncstudio.model.sync.FieldMappingSuggestion;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.SqliteConnectionFactory;
import com.dbsyncstudio.store.sqlite.SqliteDatasourceRepository;
import com.dbsyncstudio.store.sqlite.SqliteFieldMappingRepository;
import com.dbsyncstudio.store.sqlite.SqliteIncrementalSyncCheckpointRepository;
import com.dbsyncstudio.store.sqlite.SqliteSchemaComparisonHistoryRepository;
import com.dbsyncstudio.store.sqlite.SqliteSqlExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTaskRepository;
import com.dbsyncstudio.store.sync.SqliteExecutionLogRepository;
import com.dbsyncstudio.store.sync.SqliteSyncCheckpointRepository;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class DesktopBackendServiceFieldMappingTest {

    @Test
    public void shouldSuggestMappingsWhenTaskSchemaNameDoesNotMatchMetadataSchemaName() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-field-mapping-service", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteDatasourceRepository datasourceRepository = new SqliteDatasourceRepository(connectionFactory);
        SqliteSyncTaskRepository taskRepository = new SqliteSyncTaskRepository(connectionFactory);
        SqliteExecutionLogRepository executionLogRepository = new SqliteExecutionLogRepository(connectionFactory);
        SqliteSyncCheckpointRepository checkpointRepository = new SqliteSyncCheckpointRepository(connectionFactory);
        SqliteFieldMappingRepository fieldMappingRepository = new SqliteFieldMappingRepository(connectionFactory);
        SqliteSqlExecutionLogRepository sqlExecutionLogRepository = new SqliteSqlExecutionLogRepository(connectionFactory);
        SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository = new SqliteSchemaComparisonHistoryRepository(connectionFactory);
        SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository = new SqliteIncrementalSyncCheckpointRepository(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        checkpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfig datasource = new DatasourceConfig();
        datasource.setName("field-mapping-source");
        datasource.setType(DatasourceType.MYSQL);
        datasource.setHost("127.0.0.1");
        datasource.setPort(Integer.valueOf(3306));
        datasource.setDatabaseName("field_mapping_db");
        datasource.setUsername("root");
        datasource.setPassword("secret");
        datasourceRepository.save(datasource);

        SyncTask task = new SyncTask();
        task.setTaskName("Field Mapping Demo");
        task.setSourceDatasourceId(datasource.getId());
        task.setTargetDatasourceId(datasource.getId());
        task.setSyncMode(SyncMode.FULL);
        task.setTaskStatus(SyncTaskStatus.PENDING);
        task.setSourceSchemaName("Cs2103");
        task.setSourceTableName("tb_user");
        task.setTargetSchemaName("Cs2103");
        task.setTargetTableName("tb_user_copy");
        taskRepository.save(task);

        DatabaseMetadataScanner scanner = new DatabaseMetadataScanner() {
            @Override
            public List<SchemaMetadata> scan(DatasourceConfig config) {
                return metadata();
            }

            @Override
            public List<SchemaMetadata> scan(Connection connection) {
                return metadata();
            }

            private List<SchemaMetadata> metadata() {
                List<SchemaMetadata> schemas = new ArrayList<SchemaMetadata>();
                SchemaMetadata schema = new SchemaMetadata();
                schema.setSchemaName("default");
                List<TableMetadata> tables = new ArrayList<TableMetadata>();
                tables.add(table("tb_user", "id", "user_name"));
                schema.setTables(tables);
                schemas.add(schema);
                return schemas;
            }
        };

        DatasourceConnectionTester connectionTester = new DatasourceConnectionTester() {
            @Override
            public ConnectionTestResult test(DatasourceConfig config) {
                return ConnectionTestResult.builder()
                        .success(true)
                        .message("ok")
                        .costMillis(1L)
                        .build();
            }
        };

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                taskRepository,
                executionLogRepository,
                checkpointRepository,
                fieldMappingRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                scanner,
                connectionTester,
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                null,
                null);

        List<FieldMappingSuggestion> suggestions = service.suggestFieldMappings(task.getId().longValue());

        Assert.assertEquals(2, suggestions.size());
        Assert.assertEquals("id", suggestions.get(0).getSourceColumnName());
        Assert.assertEquals("user_name", suggestions.get(1).getSourceColumnName());
    }

    private TableMetadata table(String tableName, String... columnNames) {
        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.setTableName(tableName);
        List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
        for (String columnName : columnNames) {
            ColumnMetadata columnMetadata = new ColumnMetadata();
            columnMetadata.setName(columnName);
            columns.add(columnMetadata);
        }
        tableMetadata.setColumns(columns);
        return tableMetadata;
    }
}
