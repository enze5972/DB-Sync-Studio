package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.SyncTaskDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncTaskRepository {

    void initialize() throws SQLException;

    long save(SyncTaskDO task) throws SQLException;

    Optional<SyncTaskDO> findById(long id) throws SQLException;

    List<SyncTaskDO> findAll() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
