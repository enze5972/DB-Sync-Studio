package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class DatasourceRepositoryImplTest {

    @Test
    public void shouldSaveAndLoadDatasourceConfig() throws Exception {
        File tempDatabase = File.createTempFile("db-sync-studio-test", ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();

        DatasourceRepositoryImpl repository = new DatasourceRepositoryImpl(new DatabaseConnectionFactory(tempDatabase));
        repository.initialize();

        DatasourceConfigDO config = new DatasourceConfigDO();
        config.setName("Test MySQL");
        config.setType(DatasourceType.MYSQL);
        config.setHost("127.0.0.1");
        config.setPort(Integer.valueOf(3306));
        config.setDatabaseName("demo");
        config.setUsername("root");
        config.setPassword("secret");
        config.setRemark("roundtrip");

        long id = repository.save(config);
        Assert.assertTrue(id > 0L);

        Optional<DatasourceConfigDO> loaded = repository.findById(id);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals("Test MySQL", loaded.get().getName());
        Assert.assertEquals(DatasourceType.MYSQL, loaded.get().getType());

        List<DatasourceConfigDO> all = repository.findAll();
        Assert.assertEquals(1, all.size());

        boolean deleted = repository.deleteById(id);
        Assert.assertTrue(deleted);
        Assert.assertFalse(repository.findById(id).isPresent());
    }
}

