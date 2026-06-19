package com.dbsyncstudio.store.sqlite;

import java.io.File;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqliteDatabasePaths {

    private static final String APP_DIRECTORY_NAME = ".db-sync-studio";
    private static final String DATABASE_FILE_NAME = "db-sync-studio.sqlite";
    private static final String ALERT_SECRET_KEY_FILE_NAME = "alert-secrets.key";

    public static File appDirectory() {
        return new File(System.getProperty("user.home"), APP_DIRECTORY_NAME);
    }

    public static File defaultDatabaseFile() {
        return new File(appDirectory(), DATABASE_FILE_NAME);
    }

    public static File defaultAlertSecretKeyFile() {
        return new File(appDirectory(), ALERT_SECRET_KEY_FILE_NAME);
    }
}
