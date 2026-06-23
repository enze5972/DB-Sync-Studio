package com.dbsyncstudio.core.bootstrap;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationBootstrap {

    private static final Logger LOGGER = Logger.getLogger(ApplicationBootstrap.class.getName());

    public void start(String[] args) {
        File databaseFile = AppPaths.databaseFile();
        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(databaseFile);
        DatasourceRepositoryImpl repository = new DatasourceRepositoryImpl(connectionFactory);

        try {
            repository.initialize();
            LOGGER.log(Level.INFO, "SQLite store initialized at {0}", databaseFile.getAbsolutePath());

            if (hasArgument(args, "--demo")) {
                runDatasourceDemo(repository);
            } else {
                LOGGER.info("DB Sync Studio core is ready. Start the desktop shell to open the UI.");
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Failed to bootstrap DB Sync Studio", ex);
            throw new IllegalStateException("Failed to bootstrap DB Sync Studio", ex);
        }
    }

    private void runDatasourceDemo(DatasourceRepositoryImpl repository) throws SQLException {
        DatasourceConfigDO config = new DatasourceConfigDO();
        config.setName("Local MySQL Demo");
        config.setType(DatasourceType.MYSQL);
        config.setHost("127.0.0.1");
        config.setPort(Integer.valueOf(3306));
        config.setDatabaseName("demo_db");
        config.setUsername("root");
        config.setPassword("root");
        config.setRemark("Bootstrap demo datasource");

        long id = repository.save(config);
        LOGGER.log(Level.INFO, "Saved datasource config with id {0}", Long.valueOf(id));

        List<DatasourceConfigDO> datasourceConfigs = repository.findAll();
        LOGGER.log(Level.INFO, "Datasource config count: {0}", Integer.valueOf(datasourceConfigs.size()));
    }

    private boolean hasArgument(String[] args, String expected) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (expected.equals(arg)) {
                return true;
            }
        }
        return false;
    }
}

