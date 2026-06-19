package com.dbsyncstudio.model.alert;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository {

    void initialize() throws SQLException;

    long save(AlertRule rule) throws SQLException;

    Optional<AlertRule> findById(long id) throws SQLException;

    List<AlertRule> findAll() throws SQLException;

    List<AlertRule> findEnabled() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
