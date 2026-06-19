package com.dbsyncstudio.model.sync;

import java.sql.SQLException;
import java.util.List;

public interface SyncRunLogRepository {

    void initialize() throws SQLException;

    long append(SyncRunLogEntry entry) throws SQLException;

    List<SyncRunLogEntry> findByTaskId(long taskId) throws SQLException;

    List<SyncRunLogEntry> findByRunId(String runId) throws SQLException;

    List<SyncRunLogEntry> findBySyncRunId(long syncRunId) throws SQLException;

    List<SyncRunLogEntry> findBySyncTableRunId(long syncTableRunId) throws SQLException;

    List<SyncRunLogEntry> findRecent(int limit) throws SQLException;

    int deleteOlderThan(long createdAt, List<Long> excludedTaskIds) throws SQLException;
}
