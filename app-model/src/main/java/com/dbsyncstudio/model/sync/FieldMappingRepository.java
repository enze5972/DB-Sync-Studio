package com.dbsyncstudio.model.sync;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface FieldMappingRepository {

    void initialize() throws SQLException;

    long save(FieldMappingRule mappingRule) throws SQLException;

    Optional<FieldMappingRule> findById(long id) throws SQLException;

    List<FieldMappingRule> findByTaskId(long taskId) throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
