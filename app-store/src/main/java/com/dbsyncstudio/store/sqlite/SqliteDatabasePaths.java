package com.dbsyncstudio.store.sqlite;

import java.io.File;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SqliteDatabasePaths {

    private static final String APP_DIRECTORY_NAME = ".db-sync-studio";
    private static final String DATABASE_FILE_NAME = "db-sync-studio.sqlite";
    private static final String ALERT_SECRET_KEY_FILE_NAME = "alert-secrets.key";

    public static File appDirectory() {
        return new File(baseApplicationDirectory(), APP_DIRECTORY_NAME);
    }

    public static File defaultDatabaseFile() {
        return new File(appDirectory(), DATABASE_FILE_NAME);
    }

    public static File defaultAlertSecretKeyFile() {
        return new File(appDirectory(), ALERT_SECRET_KEY_FILE_NAME);
    }

    private static File baseApplicationDirectory() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && appData.trim().length() > 0) {
                return new File(appData);
            }
            return new File(userHome, "AppData/Roaming");
        }
        if (osName.contains("mac")) {
            return new File(userHome, "Library/Application Support");
        }
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && xdgDataHome.trim().length() > 0) {
            return new File(xdgDataHome);
        }
        return new File(userHome, ".local/share");
    }
}
