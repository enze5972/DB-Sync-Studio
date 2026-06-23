package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;

import com.dbsyncstudio.model.sync.entity.FieldMappingRuleDO;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class FieldMappingRepositoryImplTest {

    @Test
    public void shouldPersistAndLoadFieldMappingRules() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-field-mapping", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        FieldMappingRepositoryImpl repository = new FieldMappingRepositoryImpl(new DatabaseConnectionFactory(tempDatabase));
        repository.initialize();

        FieldMappingRuleDO mappingRule = new FieldMappingRuleDO();
        mappingRule.setTaskId(Long.valueOf(42L));
        mappingRule.setSourceSchemaName("public");
        mappingRule.setTargetSchemaName("archive");
        mappingRule.setSourceTableName("source_table");
        mappingRule.setTargetTableName("target_table");
        mappingRule.setSourceColumnName("source_name");
        mappingRule.setTargetColumnName("target_name");
        mappingRule.setIgnored(false);
        mappingRule.setDefaultValue("N/A");
        mappingRule.setTransformRule("trim()");

        long id = repository.save(mappingRule);
        Assert.assertTrue(id > 0L);

        Optional<FieldMappingRuleDO> loaded = repository.findById(id);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals("public", loaded.get().getSourceSchemaName());
        Assert.assertEquals("archive", loaded.get().getTargetSchemaName());
        Assert.assertEquals("source_name", loaded.get().getSourceColumnName());
        Assert.assertEquals("target_name", loaded.get().getTargetColumnName());

        List<FieldMappingRuleDO> byTaskId = repository.findByTaskId(42L);
        Assert.assertEquals(1, byTaskId.size());

        Assert.assertTrue(repository.deleteById(id));
        Assert.assertFalse(repository.findById(id).isPresent());
    }
}
