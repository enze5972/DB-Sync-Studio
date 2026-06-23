package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.schema.dto.SchemaSqlPreviewRequestDTO;
import com.dbsyncstudio.model.schema.vo.SchemaSqlPreviewResultVO;
import com.dbsyncstudio.model.sql.vo.SqlExecutionResultVO;
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

public class DesktopBackendServiceSchemaSqlPreviewTest {

    @Test
    public void shouldAllowBatchSchemaSqlPreviewAndExecution() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-schema-sql", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
        DatasourceRepositoryImpl datasourceRepository = new DatasourceRepositoryImpl(connectionFactory);
        SyncTaskRepositoryImpl taskRepository = new SyncTaskRepositoryImpl(connectionFactory);
        ExecutionLogRepositoryImpl executionLogRepository = new ExecutionLogRepositoryImpl(connectionFactory);
        SyncCheckpointRepositoryImpl syncCheckpointRepository = new SyncCheckpointRepositoryImpl(connectionFactory);
        FieldMappingRepositoryImpl fieldMappingRepository = new FieldMappingRepositoryImpl(connectionFactory);
        SqlExecutionLogRepositoryImpl sqlExecutionLogRepository = new SqlExecutionLogRepositoryImpl(connectionFactory);
        SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository = new SchemaComparisonHistoryRepositoryImpl(connectionFactory);
        IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository = new IncrementalSyncCheckpointRepositoryImpl(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfigDO datasource = new DatasourceConfigDO();
        datasource.setName("schema-sql-preview");
        datasource.setType(DatasourceType.MYSQL);
        datasource.setHost("127.0.0.1");
        datasource.setPort(Integer.valueOf(3306));
        datasource.setDatabaseName("schema_sql_preview_db");
        datasource.setUsername("root");
        datasource.setPassword("secret");
        datasourceRepository.save(datasource);

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                taskRepository,
                executionLogRepository,
                syncCheckpointRepository,
                fieldMappingRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                new JdbcDatabaseMetadataScanner(),
                new JdbcDatasourceConnectionTester(),
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                new JdbcFullSyncEngine(new JdbcDatabaseMetadataScanner(), new DefaultDatasourceConnectionOpener(), syncCheckpointRepository),
                new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository));

        SchemaSqlPreviewRequestDTO previewRequest = SchemaSqlPreviewRequestDTO.builder()
                .datasource(datasource)
                .sql("CREATE TABLE t1 (id INTEGER); CREATE TABLE t2 (id INTEGER)")
                .allowDangerousSql(true)
                .build();
        SchemaSqlPreviewResultVO previewResult = service.previewSchemaSql(previewRequest);
        Assert.assertTrue(previewResult.isExecutable());
        Assert.assertEquals("CREATE", previewResult.getStatementType());

        SqlExecutionResultVO executionResult = service.executeSchemaSql(previewRequest);
        Assert.assertTrue(executionResult.isSuccess());
        Assert.assertEquals(0L, executionResult.getAffectedRows());
        Assert.assertEquals(2, com.dbsyncstudio.core.test.stub.mysql.Driver.EXECUTED_SQL.size());
        Assert.assertEquals("CREATE TABLE t1 (id INTEGER)", com.dbsyncstudio.core.test.stub.mysql.Driver.EXECUTED_SQL.get(0));
        Assert.assertEquals("CREATE TABLE t2 (id INTEGER)", com.dbsyncstudio.core.test.stub.mysql.Driver.EXECUTED_SQL.get(1));
    }
}
