package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;

import com.dbsyncstudio.model.sync.entity.SyncRunDO;
import com.dbsyncstudio.model.sync.entity.SyncRunLogEntryDO;
import com.dbsyncstudio.model.sync.entity.SyncTableRunDO;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SyncRunRepositoryImplTest {

    @Test
    public void shouldPersistRunTableRunAndLogHistory() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-run-history", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(tempDatabase);
        SyncRunRepositoryImpl runRepository = new SyncRunRepositoryImpl(connectionFactory);
        SyncTableRunRepositoryImpl tableRunRepository = new SyncTableRunRepositoryImpl(connectionFactory);
        SyncRunLogRepositoryImpl logRepository = new SyncRunLogRepositoryImpl(connectionFactory);

        runRepository.initialize();
        tableRunRepository.initialize();
        logRepository.initialize();

        SyncRunDO run = new SyncRunDO();
        run.setTaskId(Long.valueOf(42L));
        run.setRunId("run-20260619-001");
        run.setSyncMode("FULL");
        run.setRunStatus("RUNNING");
        run.setTotalTableCount(Integer.valueOf(2));
        run.setCompletedTableCount(Integer.valueOf(1));
        run.setTotalRowCount(Long.valueOf(10L));
        run.setSyncedRowCount(Long.valueOf(8L));
        run.setSuccessRowCount(Long.valueOf(8L));
        run.setFailedRowCount(Long.valueOf(0L));
        run.setSpeedRowsPerSecond(Double.valueOf(12.5d));

        long runId = runRepository.save(run);
        Assert.assertTrue(runId > 0L);

        SyncTableRunDO tableRun = new SyncTableRunDO();
        tableRun.setSyncRunId(Long.valueOf(runId));
        tableRun.setTaskId(Long.valueOf(42L));
        tableRun.setRunId("run-20260619-001");
        tableRun.setTaskTableId(Long.valueOf(7L));
        tableRun.setSourceSchemaName("source_schema");
        tableRun.setSourceTableName("source_table");
        tableRun.setTargetSchemaName("target_schema");
        tableRun.setTargetTableName("target_table");
        tableRun.setTableOrder(Integer.valueOf(1));
        tableRun.setTableStatus("SUCCESS");
        tableRun.setTotalRowCount(Long.valueOf(10L));
        tableRun.setSyncedRowCount(Long.valueOf(10L));
        tableRun.setSuccessRowCount(Long.valueOf(10L));
        tableRun.setFailedRowCount(Long.valueOf(0L));

        long tableRunId = tableRunRepository.save(tableRun);
        Assert.assertTrue(tableRunId > 0L);

        SyncRunLogEntryDO logEntry = new SyncRunLogEntryDO();
        logEntry.setTaskId(Long.valueOf(42L));
        logEntry.setSyncRunId(Long.valueOf(runId));
        logEntry.setSyncTableRunId(Long.valueOf(tableRunId));
        logEntry.setRunId("run-20260619-001");
        logEntry.setTableName("source_table");
        logEntry.setLogLevel("INFO");
        logEntry.setLogMessage("table completed");

        long logId = logRepository.append(logEntry);
        Assert.assertTrue(logId > 0L);

        Optional<SyncRunDO> loadedRun = runRepository.findById(runId);
        Assert.assertTrue(loadedRun.isPresent());
        Assert.assertEquals("run-20260619-001", loadedRun.get().getRunId());

        List<SyncTableRunDO> tableRuns = tableRunRepository.findBySyncRunId(runId);
        Assert.assertEquals(1, tableRuns.size());
        Assert.assertEquals("SUCCESS", tableRuns.get(0).getTableStatus());

        List<SyncRunLogEntryDO> logsByRunId = logRepository.findByRunId("run-20260619-001");
        Assert.assertEquals(1, logsByRunId.size());
        Assert.assertEquals("table completed", logsByRunId.get(0).getLogMessage());

        List<SyncRunLogEntryDO> logsByTaskId = logRepository.findByTaskId(42L);
        Assert.assertEquals(1, logsByTaskId.size());
    }
}
