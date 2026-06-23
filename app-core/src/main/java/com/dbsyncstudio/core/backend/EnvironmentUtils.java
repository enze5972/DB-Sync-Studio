package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.store.sqlite.DatabasePaths;

import java.io.File;

public final class EnvironmentUtils {

    private EnvironmentUtils() {
    }

    public static File appDirectory() {
        return DatabasePaths.appDirectory();
    }

    public static File logsDirectory() {
        return new File(DatabasePaths.appDirectory(), "logs");
    }
}
