package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.store.sqlite.SqliteDatabasePaths;

import java.io.File;

public final class AppEnvironmentInfo {

    private AppEnvironmentInfo() {
    }

    public static File appDirectory() {
        return SqliteDatabasePaths.appDirectory();
    }

    public static File logsDirectory() {
        return new File(SqliteDatabasePaths.appDirectory(), "logs");
    }
}
