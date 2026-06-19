package com.dbsyncstudio.model.sync;

import java.util.Optional;

public interface SyncCheckpointRepository {

    long save(SyncCheckpoint checkpoint);

    Optional<SyncCheckpoint> findByKey(String checkpointKey);

    boolean deleteByKey(String checkpointKey);
}
