package com.dbsyncstudio.core.connection;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;

import org.junit.Assert;
import org.junit.Test;

public class JdbcConnectionSupportTest {

    @Test
    public void shouldResolveMysqlUrlAndDriver() {
        DatasourceConfigDO config = new DatasourceConfigDO();
        config.setType(DatasourceType.MYSQL);
        config.setHost("127.0.0.1");
        config.setPort(Integer.valueOf(3306));
        config.setDatabaseName("demo");

        JdbcConnectionDescriptor descriptor = JdbcConnectionSupport.resolve(config);

        Assert.assertEquals("com.mysql.cj.jdbc.Driver", descriptor.getDriverClassName());
        Assert.assertEquals(
                "jdbc:mysql://127.0.0.1:3306/demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                descriptor.getJdbcUrl());
    }

    @Test
    public void shouldResolvePostgresqlUrlAndDriverWithDefaultPort() {
        DatasourceConfigDO config = new DatasourceConfigDO();
        config.setType(DatasourceType.POSTGRESQL);
        config.setHost("127.0.0.1");
        config.setDatabaseName("demo");

        JdbcConnectionDescriptor descriptor = JdbcConnectionSupport.resolve(config);

        Assert.assertEquals("org.postgresql.Driver", descriptor.getDriverClassName());
        Assert.assertEquals("jdbc:postgresql://127.0.0.1:5432/demo", descriptor.getJdbcUrl());
    }

    @Test
    public void shouldResolveDmUrlAndDriverWithDefaultPort() {
        DatasourceConfigDO config = new DatasourceConfigDO();
        config.setType(DatasourceType.DM);
        config.setHost("127.0.0.1");
        config.setDatabaseName("demo");

        JdbcConnectionDescriptor descriptor = JdbcConnectionSupport.resolve(config);

        Assert.assertEquals("dm.jdbc.driver.DmDriver", descriptor.getDriverClassName());
        Assert.assertEquals("jdbc:dm://127.0.0.1:5236/demo", descriptor.getJdbcUrl());
    }
}

