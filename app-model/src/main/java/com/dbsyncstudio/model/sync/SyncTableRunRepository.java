package com.dbsyncstudio.model.sync;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncTableRunRepository {

    void initialize() throws SQLException;

    long save(SyncTableRun tableRun) throws SQLException;

    Optional<SyncTableRun> findById(long id) throws SQLException;

    List<SyncTableRun> findBySyncRunId(long syncRunId) throws SQLException;

    List<SyncTableRun> findByRunId(String runId) throws SQLException;

    List<SyncTableRun> findByTaskId(long taskId) throws SQLException;

    List<SyncTableRun> findRecent(int limit) throws SQLException;
}
