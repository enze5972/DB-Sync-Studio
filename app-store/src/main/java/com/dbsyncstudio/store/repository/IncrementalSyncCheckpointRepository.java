package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.IncrementalSyncCheckpointEntryDO;

import java.util.Optional;

public interface IncrementalSyncCheckpointRepository {

    long save(IncrementalSyncCheckpointEntryDO entry);

    Optional<IncrementalSyncCheckpointEntryDO> findByTaskId(long taskId);
}
