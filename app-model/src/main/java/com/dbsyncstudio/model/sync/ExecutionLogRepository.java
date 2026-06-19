package com.dbsyncstudio.model.sync;

import java.util.List;

public interface ExecutionLogRepository {

    long append(ExecutionLogEntry entry);

    List<ExecutionLogEntry> findByTaskId(long taskId);

    int deleteOlderThan(long createdAt, List<Long> excludedTaskIds);
}
