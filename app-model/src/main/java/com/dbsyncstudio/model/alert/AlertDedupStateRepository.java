package com.dbsyncstudio.model.alert;

import java.sql.SQLException;
import java.util.Optional;

public interface AlertDedupStateRepository {

    void initialize() throws SQLException;

    long save(AlertDedupState state) throws SQLException;

    Optional<AlertDedupState> findByDedupKey(String dedupKey) throws SQLException;

    boolean deleteByDedupKey(String dedupKey) throws SQLException;
}
