package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.SyncTaskTableDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncTaskTableRepository {

    void initialize() throws SQLException;

    long save(SyncTaskTableDO taskTable) throws SQLException;

    Optional<SyncTaskTableDO> findById(long id) throws SQLException;

    List<SyncTaskTableDO> findByTaskId(long taskId) throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
