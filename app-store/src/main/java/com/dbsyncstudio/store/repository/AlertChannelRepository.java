package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.alert.entity.AlertChannelDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlertChannelRepository {

    void initialize() throws SQLException;

    long save(AlertChannelDO channel) throws SQLException;

    Optional<AlertChannelDO> findById(long id) throws SQLException;

    List<AlertChannelDO> findAll() throws SQLException;

    List<AlertChannelDO> findEnabled() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
