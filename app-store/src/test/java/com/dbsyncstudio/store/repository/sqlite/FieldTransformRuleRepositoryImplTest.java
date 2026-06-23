package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;

import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class FieldTransformRuleRepositoryImplTest {

    @Test
    public void shouldPersistLoadAndDeleteTransformRules() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-transform-rule", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        FieldTransformRuleRepositoryImpl repository = new FieldTransformRuleRepositoryImpl(new DatabaseConnectionFactory(tempDatabase));
        repository.initialize();

        TransformRuleDO rule = TransformRuleDO.builder()
                .taskId(Long.valueOf(42L))
                .tableTaskId(Long.valueOf(7L))
                .fieldMappingId(Long.valueOf(99L))
                .sourceField("user_name")
                .targetField("username")
                .transformType("trim")
                .transformConfig("{\"defaultValue\":\"\"}")
                .transformOrder(Integer.valueOf(1))
                .enabled(Boolean.TRUE)
                .onError(TransformErrorStrategy.FAIL)
                .defaultValue("")
                .build();

        long id = repository.save(rule);
        Assert.assertTrue(id > 0L);

        Optional<TransformRuleDO> loaded = repository.findById(id);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals(Long.valueOf(42L), loaded.get().getTaskId());
        Assert.assertEquals(Long.valueOf(7L), loaded.get().getTableTaskId());
        Assert.assertEquals(Long.valueOf(99L), loaded.get().getFieldMappingId());
        Assert.assertEquals("user_name", loaded.get().getSourceField());
        Assert.assertEquals("username", loaded.get().getTargetField());
        Assert.assertEquals("trim", loaded.get().getTransformType());

        List<TransformRuleDO> byTaskId = repository.findByTaskId(42L);
        Assert.assertEquals(1, byTaskId.size());

        Assert.assertTrue(repository.deleteById(id));
        Assert.assertFalse(repository.findById(id).isPresent());
    }
}
