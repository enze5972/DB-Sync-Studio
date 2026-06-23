package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.SyncCheckpointDO;

import java.util.Optional;

public interface SyncCheckpointRepository {

    long save(SyncCheckpointDO checkpoint);

    Optional<SyncCheckpointDO> findByKey(String checkpointKey);

    boolean deleteByKey(String checkpointKey);
}
