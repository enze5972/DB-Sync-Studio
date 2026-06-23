package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;

import com.dbsyncstudio.model.schema.entity.SchemaComparisonHistoryEntryDO;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class SchemaComparisonHistoryRepositoryImplTest {

    @Test
    public void shouldPersistAndLoadRecentSchemaComparisonHistory() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-history", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        SchemaComparisonHistoryRepositoryImpl repository =
                new SchemaComparisonHistoryRepositoryImpl(new DatabaseConnectionFactory(tempDatabase));
        repository.initialize();

        SchemaComparisonHistoryEntryDO firstEntry = new SchemaComparisonHistoryEntryDO();
        firstEntry.setSourceDatasourceId(Long.valueOf(1L));
        firstEntry.setTargetDatasourceId(Long.valueOf(2L));
        firstEntry.setSourceSchemaName("source_schema");
        firstEntry.setSourceTableName("source_table");
        firstEntry.setTargetSchemaName("target_schema");
        firstEntry.setTargetTableName("target_table");
        firstEntry.setDiffSummary("2");
        firstEntry.setCreatedAt(Long.valueOf(111L));

        SchemaComparisonHistoryEntryDO secondEntry = new SchemaComparisonHistoryEntryDO();
        secondEntry.setSourceDatasourceId(Long.valueOf(3L));
        secondEntry.setTargetDatasourceId(Long.valueOf(4L));
        secondEntry.setSourceSchemaName("source_schema_2");
        secondEntry.setSourceTableName("source_table_2");
        secondEntry.setTargetSchemaName("target_schema_2");
        secondEntry.setTargetTableName("target_table_2");
        secondEntry.setDiffSummary("5");
        secondEntry.setCreatedAt(Long.valueOf(222L));

        long firstId = repository.save(firstEntry);
        long secondId = repository.save(secondEntry);

        Assert.assertTrue(firstId > 0L);
        Assert.assertTrue(secondId > firstId);

        List<SchemaComparisonHistoryEntryDO> recent = repository.findRecent(20);
        Assert.assertEquals(2, recent.size());

        Assert.assertEquals(Long.valueOf(secondId), recent.get(0).getId());
        Assert.assertEquals("source_table_2", recent.get(0).getSourceTableName());
        Assert.assertEquals("target_table_2", recent.get(0).getTargetTableName());
        Assert.assertEquals("5", recent.get(0).getDiffSummary());

        Assert.assertEquals(Long.valueOf(firstId), recent.get(1).getId());
        Assert.assertEquals("source_table", recent.get(1).getSourceTableName());
        Assert.assertEquals("target_table", recent.get(1).getTargetTableName());
        Assert.assertEquals("2", recent.get(1).getDiffSummary());
    }
}
