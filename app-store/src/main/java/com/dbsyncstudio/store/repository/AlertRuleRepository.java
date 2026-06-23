package com.dbsyncstudio.store.repository;

import com.dbsyncstudio.model.alert.entity.AlertRuleDO;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository {

    void initialize() throws SQLException;

    long save(AlertRuleDO rule) throws SQLException;

    Optional<AlertRuleDO> findById(long id) throws SQLException;

    List<AlertRuleDO> findAll() throws SQLException;

    List<AlertRuleDO> findEnabled() throws SQLException;

    boolean deleteById(long id) throws SQLException;
}
