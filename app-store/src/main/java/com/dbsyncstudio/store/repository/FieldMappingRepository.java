package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.sync.entity.FieldMappingRuleDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface FieldMappingRepository {

    void initialize() throws SQLException;

    long save(FieldMappingRuleDO mappingRule) throws SQLException;

    Optional<FieldMappingRuleDO> findById(long id) throws SQLException;

    List<FieldMappingRuleDO> findByTaskId(long taskId) throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
