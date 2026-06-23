package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.DatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.model.datasource.vo.ConnectionTestResultVO;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;
import com.dbsyncstudio.model.sync.vo.FieldMappingSuggestionVO;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.entity.SyncTaskDO;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.FieldMappingRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.IncrementalSyncCheckpointRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SchemaComparisonHistoryRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SqlExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.ExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncCheckpointRepositoryImpl;
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

        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
        DatasourceRepositoryImpl datasourceRepository = new DatasourceRepositoryImpl(connectionFactory);
        SyncTaskRepositoryImpl taskRepository = new SyncTaskRepositoryImpl(connectionFactory);
        ExecutionLogRepositoryImpl executionLogRepository = new ExecutionLogRepositoryImpl(connectionFactory);
        SyncCheckpointRepositoryImpl checkpointRepository = new SyncCheckpointRepositoryImpl(connectionFactory);
        FieldMappingRepositoryImpl fieldMappingRepository = new FieldMappingRepositoryImpl(connectionFactory);
        SqlExecutionLogRepositoryImpl sqlExecutionLogRepository = new SqlExecutionLogRepositoryImpl(connectionFactory);
        SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository = new SchemaComparisonHistoryRepositoryImpl(connectionFactory);
        IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository = new IncrementalSyncCheckpointRepositoryImpl(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        checkpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfigDO datasource = new DatasourceConfigDO();
        datasource.setName("field-mapping-source");
        datasource.setType(DatasourceType.MYSQL);
        datasource.setHost("127.0.0.1");
        datasource.setPort(Integer.valueOf(3306));
        datasource.setDatabaseName("field_mapping_db");
        datasource.setUsername("root");
        datasource.setPassword("secret");
        datasourceRepository.save(datasource);

        SyncTaskDO task = new SyncTaskDO();
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
            public List<SchemaMetadataDO> scan(DatasourceConfigDO config) {
                return metadata();
            }

            @Override
            public List<SchemaMetadataDO> scan(Connection connection) {
                return metadata();
            }

            private List<SchemaMetadataDO> metadata() {
                List<SchemaMetadataDO> schemas = new ArrayList<SchemaMetadataDO>();
                SchemaMetadataDO schema = new SchemaMetadataDO();
                schema.setSchemaName("default");
                List<TableMetadataDO> tables = new ArrayList<TableMetadataDO>();
                tables.add(table("tb_user", "id", "user_name"));
                schema.setTables(tables);
                schemas.add(schema);
                return schemas;
            }
        };

        DatasourceConnectionTester connectionTester = new DatasourceConnectionTester() {
            @Override
            public ConnectionTestResultVO test(DatasourceConfigDO config) {
                return ConnectionTestResultVO.builder()
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

        List<FieldMappingSuggestionVO> suggestions = service.suggestFieldMappings(task.getId().longValue());

        Assert.assertEquals(2, suggestions.size());
        Assert.assertEquals("id", suggestions.get(0).getSourceColumnName());
        Assert.assertEquals("user_name", suggestions.get(1).getSourceColumnName());
    }

    private TableMetadataDO table(String tableName, String... columnNames) {
        TableMetadataDO tableMetadata = new TableMetadataDO();
        tableMetadata.setTableName(tableName);
        List<ColumnMetadataDO> columns = new ArrayList<ColumnMetadataDO>();
        for (String columnName : columnNames) {
            ColumnMetadataDO columnMetadata = new ColumnMetadataDO();
            columnMetadata.setName(columnName);
            columns.add(columnMetadata);
        }
        tableMetadata.setColumns(columns);
        return tableMetadata;
    }
}
