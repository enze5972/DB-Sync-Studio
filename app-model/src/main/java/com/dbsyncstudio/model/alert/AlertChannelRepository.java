package com.dbsyncstudio.model.alert;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlertChannelRepository {

    void initialize() throws SQLException;

    long save(AlertChannel channel) throws SQLException;

    Optional<AlertChannel> findById(long id) throws SQLException;

    List<AlertChannel> findAll() throws SQLException;

    List<AlertChannel> findEnabled() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
