package com.dbsyncstudio.core.backend;

public class BackendDiagnosticsResponse {

    private String applicationDirectory;
    private String databaseFilePath;
    private long generatedAt;
    private int schemaVersion;
    private int databaseUserVersion;
    private int migrationEntryCount;
    private int totalTaskCount;
    private int unfinishedTaskCount;
    private int runningTaskCount;
    private int recoveredTaskCount;

    public String getApplicationDirectory() {
        return applicationDirectory;
    }

    public void setApplicationDirectory(String applicationDirectory) {
        this.applicationDirectory = applicationDirectory;
    }

    public String getDatabaseFilePath() {
        return databaseFilePath;
    }

    public void setDatabaseFilePath(String databaseFilePath) {
        this.databaseFilePath = databaseFilePath;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public int getMigrationEntryCount() {
        return migrationEntryCount;
    }

    public void setMigrationEntryCount(int migrationEntryCount) {
        this.migrationEntryCount = migrationEntryCount;
    }

    public int getTotalTaskCount() {
        return totalTaskCount;
    }

    public void setTotalTaskCount(int totalTaskCount) {
        this.totalTaskCount = totalTaskCount;
    }

    public int getDatabaseUserVersion() {
        return databaseUserVersion;
    }

    public void setDatabaseUserVersion(int databaseUserVersion) {
        this.databaseUserVersion = databaseUserVersion;
    }

    public int getUnfinishedTaskCount() {
        return unfinishedTaskCount;
    }

    public void setUnfinishedTaskCount(int unfinishedTaskCount) {
        this.unfinishedTaskCount = unfinishedTaskCount;
    }

    public int getRunningTaskCount() {
        return runningTaskCount;
    }

    public void setRunningTaskCount(int runningTaskCount) {
        this.runningTaskCount = runningTaskCount;
    }

    public int getRecoveredTaskCount() {
        return recoveredTaskCount;
    }

    public void setRecoveredTaskCount(int recoveredTaskCount) {
        this.recoveredTaskCount = recoveredTaskCount;
    }
}
