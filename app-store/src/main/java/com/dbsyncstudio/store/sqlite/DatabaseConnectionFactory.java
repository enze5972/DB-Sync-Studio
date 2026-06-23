package com.dbsyncstudio.store.sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatabaseConnectionFactory {

    @NonNull
    private final File databaseFile;

    public Connection openConnection() throws SQLException {
        ensureParentDirectoryExists();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public File getDatabaseFile() {
        return databaseFile;
    }

    private void ensureParentDirectoryExists() throws SQLException {
        File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Failed to create database directory: " + parent.getAbsolutePath());
        }
    }
}
