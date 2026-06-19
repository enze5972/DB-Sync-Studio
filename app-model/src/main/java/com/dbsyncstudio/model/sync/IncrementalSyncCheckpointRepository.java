package com.dbsyncstudio.model.sync;

import java.util.Optional;

public interface IncrementalSyncCheckpointRepository {

    long save(IncrementalSyncCheckpointEntry entry);

    Optional<IncrementalSyncCheckpointEntry> findByTaskId(long taskId);
}
