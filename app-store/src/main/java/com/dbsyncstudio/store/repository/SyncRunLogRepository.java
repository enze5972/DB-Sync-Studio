package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.SyncRunLogEntryDO;

import java.sql.SQLException;
import java.util.List;

public interface SyncRunLogRepository {

    void initialize() throws SQLException;

    long append(SyncRunLogEntryDO entry) throws SQLException;

    List<SyncRunLogEntryDO> findByTaskId(long taskId) throws SQLException;

    List<SyncRunLogEntryDO> findByRunId(String runId) throws SQLException;

    List<SyncRunLogEntryDO> findBySyncRunId(long syncRunId) throws SQLException;

    List<SyncRunLogEntryDO> findBySyncTableRunId(long syncTableRunId) throws SQLException;

    List<SyncRunLogEntryDO> findRecent(int limit) throws SQLException;

    int deleteOlderThan(long createdAt, List<Long> excludedTaskIds) throws SQLException;
}
