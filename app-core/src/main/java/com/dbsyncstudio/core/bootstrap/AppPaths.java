package com.dbsyncstudio.core.bootstrap;

import com.dbsyncstudio.store.sqlite.DatabasePaths;

import java.io.File;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppPaths {

    public static File databaseFile() {
        return DatabasePaths.defaultDatabaseFile();
    }
}
