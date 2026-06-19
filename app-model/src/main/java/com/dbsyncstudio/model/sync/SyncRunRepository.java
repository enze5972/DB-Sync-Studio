package com.dbsyncstudio.model.sync;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncRunRepository {

    void initialize() throws SQLException;

    long save(SyncRun run) throws SQLException;

    Optional<SyncRun> findById(long id) throws SQLException;

    Optional<SyncRun> findByRunId(String runId) throws SQLException;

    List<SyncRun> findByTaskId(long taskId) throws SQLException;

    List<SyncRun> findRecent(int limit) throws SQLException;
}
