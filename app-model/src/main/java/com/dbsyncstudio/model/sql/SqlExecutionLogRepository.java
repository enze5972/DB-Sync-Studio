package com.dbsyncstudio.model.sql;

import java.util.List;

public interface SqlExecutionLogRepository {

    long append(SqlExecutionLogEntry entry);

    List<SqlExecutionLogEntry> findRecent(int limit);

    int deleteOlderThan(long createdAt);
}
