package com.dbsyncstudio.model.datasource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface DatasourceRepository {

    void initialize() throws SQLException;

    long save(DatasourceConfig config) throws SQLException;

    Optional<DatasourceConfig> findById(long id) throws SQLException;

    List<DatasourceConfig> findAll() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}

