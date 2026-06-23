package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.SyncTableRunDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncTableRunRepository {

    void initialize() throws SQLException;

    long save(SyncTableRunDO tableRun) throws SQLException;

    Optional<SyncTableRunDO> findById(long id) throws SQLException;

    List<SyncTableRunDO> findBySyncRunId(long syncRunId) throws SQLException;

    List<SyncTableRunDO> findByRunId(String runId) throws SQLException;

    List<SyncTableRunDO> findByTaskId(long taskId) throws SQLException;

    List<SyncTableRunDO> findRecent(int limit) throws SQLException;
}
