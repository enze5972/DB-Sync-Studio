package com.dbsyncstudio.model.sync;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncTaskRepository {

    void initialize() throws SQLException;

    long save(SyncTask task) throws SQLException;

    Optional<SyncTask> findById(long id) throws SQLException;

    List<SyncTask> findAll() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
