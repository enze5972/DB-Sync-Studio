package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.TransformRuleDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface TransformRuleRepository {

    void initialize() throws SQLException;

    long save(TransformRuleDO rule) throws SQLException;

    Optional<TransformRuleDO> findById(long id) throws SQLException;

    List<TransformRuleDO> findByTaskId(long taskId) throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
