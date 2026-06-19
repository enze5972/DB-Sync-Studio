package com.dbsyncstudio.model.sync;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface SyncTaskTableRepository {

    void initialize() throws SQLException;

    long save(SyncTaskTable taskTable) throws SQLException;

    Optional<SyncTaskTable> findById(long id) throws SQLException;

    List<SyncTaskTable> findByTaskId(long taskId) throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
