package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sql.entity.SqlExecutionLogEntryDO;

import java.util.List;

public interface SqlExecutionLogRepository {

    long append(SqlExecutionLogEntryDO entry);

    List<SqlExecutionLogEntryDO> findRecent(int limit);

    int deleteOlderThan(long createdAt);
}
