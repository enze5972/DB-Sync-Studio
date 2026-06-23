package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.SyncRunDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncRunRepository {

    void initialize() throws SQLException;

    long save(SyncRunDO run) throws SQLException;

    Optional<SyncRunDO> findById(long id) throws SQLException;

    Optional<SyncRunDO> findByRunId(String runId) throws SQLException;

    List<SyncRunDO> findByTaskId(long taskId) throws SQLException;

    List<SyncRunDO> findRecent(int limit) throws SQLException;
}
