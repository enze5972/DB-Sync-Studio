package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO;

import java.util.List;

public interface ExecutionLogRepository {

    long append(ExecutionLogEntryDO entry);

    List<ExecutionLogEntryDO> findByTaskId(long taskId);

    int deleteOlderThan(long createdAt, List<Long> excludedTaskIds);
}
