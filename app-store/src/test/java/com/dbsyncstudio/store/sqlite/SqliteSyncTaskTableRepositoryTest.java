package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.sync.SyncTaskTable;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class SqliteSyncTaskTableRepositoryTest {

    @Test
    public void shouldPersistAndLoadTaskTables() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-task-table", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        SqliteSyncTaskTableRepository repository =
                new SqliteSyncTaskTableRepository(new SqliteConnectionFactory(tempDatabase));
        repository.initialize();

        SyncTaskTable table = new SyncTaskTable();
        table.setTaskId(Long.valueOf(42L));
        table.setSourceSchemaName("source_schema");
        table.setSourceTableName("source_table");
        table.setTargetSchemaName("target_schema");
        table.setTargetTableName("target_table");
        table.setTableOrder(Integer.valueOf(3));
        table.setEnabled(Boolean.TRUE);

        long id = repository.save(table);
        Assert.assertTrue(id > 0L);

        Optional<SyncTaskTable> loaded = repository.findById(id);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals("source_table", loaded.get().getSourceTableName());
        Assert.assertEquals("target_table", loaded.get().getTargetTableName());

        List<SyncTaskTable> byTaskId = repository.findByTaskId(42L);
        Assert.assertEquals(1, byTaskId.size());
        Assert.assertEquals(Integer.valueOf(3), byTaskId.get(0).getTableOrder());

        Assert.assertTrue(repository.deleteById(id));
        Assert.assertFalse(repository.findById(id).isPresent());
    }
}
