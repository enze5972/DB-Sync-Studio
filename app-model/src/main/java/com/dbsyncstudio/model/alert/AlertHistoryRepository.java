package com.dbsyncstudio.model.alert;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlertHistoryRepository {

    void initialize() throws SQLException;

    long save(AlertHistoryEntry historyEntry) throws SQLException;

    Optional<AlertHistoryEntry> findById(long id) throws SQLException;

    List<AlertHistoryEntry> findByAlertId(String alertId) throws SQLException;

    List<AlertHistoryEntry> findAll() throws SQLException;
}
