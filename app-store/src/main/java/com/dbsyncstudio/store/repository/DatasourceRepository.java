package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface DatasourceRepository {

    void initialize() throws SQLException;

    long save(DatasourceConfigDO config) throws SQLException;

    Optional<DatasourceConfigDO> findById(long id) throws SQLException;

    List<DatasourceConfigDO> findAll() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}

