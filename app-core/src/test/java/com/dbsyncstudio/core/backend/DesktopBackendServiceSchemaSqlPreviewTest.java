package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.schema.SchemaSqlPreviewRequest;
import com.dbsyncstudio.model.schema.SchemaSqlPreviewResult;
import com.dbsyncstudio.model.sql.SqlExecutionResult;
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

public class DesktopBackendServiceSchemaSqlPreviewTest {

    @Test
    public void shouldAllowBatchSchemaSqlPreviewAndExecution() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-schema-sql", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteDatasourceRepository datasourceRepository = new SqliteDatasourceRepository(connectionFactory);
        SqliteSyncTaskRepository taskRepository = new SqliteSyncTaskRepository(connectionFactory);
        SqliteExecutionLogRepository executionLogRepository = new SqliteExecutionLogRepository(connectionFactory);
        SqliteSyncCheckpointRepository syncCheckpointRepository = new SqliteSyncCheckpointRepository(connectionFactory);
        SqliteFieldMappingRepository fieldMappingRepository = new SqliteFieldMappingRepository(connectionFactory);
        SqliteSqlExecutionLogRepository sqlExecutionLogRepository = new SqliteSqlExecutionLogRepository(connectionFactory);
        SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository = new SqliteSchemaComparisonHistoryRepository(connectionFactory);
        SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository = new SqliteIncrementalSyncCheckpointRepository(connectionFactory);

        datasourceRepository.initialize();
        taskRepository.initialize();
        executionLogRepository.initialize();
        syncCheckpointRepository.initialize();
        fieldMappingRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();

        DatasourceConfig datasource = new DatasourceConfig();
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

        SchemaSqlPreviewRequest previewRequest = SchemaSqlPreviewRequest.builder()
                .datasource(datasource)
                .sql("CREATE TABLE t1 (id INTEGER); CREATE TABLE t2 (id INTEGER)")
                .allowDangerousSql(true)
                .build();
        SchemaSqlPreviewResult previewResult = service.previewSchemaSql(previewRequest);
        Assert.assertTrue(previewResult.isExecutable());
        Assert.assertEquals("CREATE", previewResult.getStatementType());

        SqlExecutionResult executionResult = service.executeSchemaSql(previewRequest);
        Assert.assertTrue(executionResult.isSuccess());
        Assert.assertEquals(0L, executionResult.getAffectedRows());
        Assert.assertEquals(2, com.mysql.cj.jdbc.Driver.EXECUTED_SQL.size());
        Assert.assertEquals("CREATE TABLE t1 (id INTEGER)", com.mysql.cj.jdbc.Driver.EXECUTED_SQL.get(0));
        Assert.assertEquals("CREATE TABLE t2 (id INTEGER)", com.mysql.cj.jdbc.Driver.EXECUTED_SQL.get(1));
    }
}
