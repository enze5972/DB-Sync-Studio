package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.alert.entity.AlertDedupStateDO;

import java.sql.SQLException;
import java.util.Optional;

public interface AlertDedupStateRepository {

    void initialize() throws SQLException;

    long save(AlertDedupStateDO state) throws SQLException;

    Optional<AlertDedupStateDO> findByDedupKey(String dedupKey) throws SQLException;

    boolean deleteByDedupKey(String dedupKey) throws SQLException;
}
