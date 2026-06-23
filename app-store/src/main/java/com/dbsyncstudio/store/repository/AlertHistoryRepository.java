package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.alert.entity.AlertHistoryEntryDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlertHistoryRepository {

    void initialize() throws SQLException;

    long save(AlertHistoryEntryDO historyEntry) throws SQLException;

    Optional<AlertHistoryEntryDO> findById(long id) throws SQLException;

    List<AlertHistoryEntryDO> findByAlertId(String alertId) throws SQLException;

    List<AlertHistoryEntryDO> findAll() throws SQLException;
}
