package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.entity.SyncCheckpointDO;
import com.dbsyncstudio.model.sync.entity.SyncTaskDO;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskRepositoryImpl;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class SyncRepositoriesTest {

    @Test
    public void shouldPersistAndLoadExecutionLogAndCheckpoint() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-sync", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
        new DatasourceRepositoryImpl(connectionFactory).initialize();
        ExecutionLogRepositoryImpl executionLogRepository = new ExecutionLogRepositoryImpl(connectionFactory);
        SyncCheckpointRepositoryImpl checkpointRepository = new SyncCheckpointRepositoryImpl(connectionFactory);
        executionLogRepository.initialize();
        checkpointRepository.initialize();

        ExecutionLogEntryDO logEntry = ExecutionLogEntryDO.builder()
                .taskId(Long.valueOf(123L))
                .logLevel("INFO")
                .logMessage("sync started")
                .createdAt(Long.valueOf(System.currentTimeMillis()))
                .build();
        long logId = executionLogRepository.append(logEntry);
        Assert.assertTrue(logId > 0L);
        Assert.assertEquals(1, executionLogRepository.findByTaskId(123L).size());
        Assert.assertEquals("sync started", executionLogRepository.findByTaskId(123L).get(0).getLogMessage());

        SyncCheckpointDO checkpoint = SyncCheckpointDO.builder()
                .checkpointKey("customer_sync")
                .checkpointValue("100")
                .updatedAt(Long.valueOf(System.currentTimeMillis()))
                .build();
        long checkpointId = checkpointRepository.save(checkpoint);
        Assert.assertTrue(checkpointId > 0L);
        Assert.assertTrue(checkpointRepository.findByKey("customer_sync").isPresent());
        Assert.assertEquals("100", checkpointRepository.findByKey("customer_sync").get().getCheckpointValue());

        Assert.assertTrue(checkpointRepository.deleteByKey("customer_sync"));
        Assert.assertFalse(checkpointRepository.findByKey("customer_sync").isPresent());
    }

    @Test
    public void shouldPersistIncrementalTaskFields() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-task", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
        DatasourceRepositoryImpl datasourceRepository = new DatasourceRepositoryImpl(connectionFactory);
        SyncTaskRepositoryImpl taskRepository = new SyncTaskRepositoryImpl(connectionFactory);
        datasourceRepository.initialize();
        taskRepository.initialize();

        SyncTaskDO task = SyncTaskDO.builder()
                .taskName("incremental-sync")
                .sourceDatasourceId(Long.valueOf(1L))
                .targetDatasourceId(Long.valueOf(2L))
                .syncMode(SyncMode.INCREMENTAL)
                .taskStatus(SyncTaskStatus.PENDING)
                .sourceTableName("customer")
                .targetTableName("customer_copy")
                .incrementalMode(IncrementalSyncMode.COMPOSITE)
                .incrementalColumnName("updated_at")
                .incrementalTieBreakerColumnName("id")
                .incrementalCompositeColumnName("updated_at,id")
                .build();

        long taskId = taskRepository.save(task);
        Assert.assertTrue(taskId > 0L);

        SyncTaskDO loadedTask = taskRepository.findById(taskId).get();
        Assert.assertEquals(IncrementalSyncMode.COMPOSITE, loadedTask.getIncrementalMode());
        Assert.assertEquals("updated_at", loadedTask.getIncrementalColumnName());
        Assert.assertEquals("id", loadedTask.getIncrementalTieBreakerColumnName());
        Assert.assertEquals("updated_at,id", loadedTask.getIncrementalCompositeColumnName());
    }
}
