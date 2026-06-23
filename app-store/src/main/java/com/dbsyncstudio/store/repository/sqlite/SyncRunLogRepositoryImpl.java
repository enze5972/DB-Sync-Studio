package com.dbsyncstudio.store.repository.sqlite;

import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer;

import com.dbsyncstudio.model.sync.entity.SyncRunLogEntryDO;
import com.dbsyncstudio.store.repository.SyncRunLogRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class SyncRunLogRepositoryImpl implements SyncRunLogRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sync_run_log (task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_TASK_ID_SQL =
            "SELECT id, task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at FROM sync_run_log WHERE task_id = ? ORDER BY id DESC";
    private static final String FIND_BY_RUN_ID_SQL =
            "SELECT id, task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at FROM sync_run_log WHERE run_id = ? ORDER BY id DESC";
    private static final String FIND_BY_SYNC_RUN_ID_SQL =
            "SELECT id, task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at FROM sync_run_log WHERE sync_run_id = ? ORDER BY id DESC";
    private static final String FIND_BY_SYNC_TABLE_RUN_ID_SQL =
            "SELECT id, task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at FROM sync_run_log WHERE sync_table_run_id = ? ORDER BY id DESC";
    private static final String FIND_RECENT_SQL =
            "SELECT id, task_id, sync_run_id, sync_table_run_id, run_id, table_name, log_level, log_message, created_at FROM sync_run_log ORDER BY id DESC LIMIT ?";

    @NonNull
    private final DatabaseConnectionFactory connectionFactory;

    @Override
    public void initialize() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(true);
            DatabaseSchemaInitializer.initialize(connection);
        }
    }

    @Override
    public long append(SyncRunLogEntryDO entry) throws SQLException {
        if (entry == null) {
            throw new IllegalArgumentException("Sync run log entry must not be null");
        }
        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(Long.valueOf(System.currentTimeMillis()));
        }
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, entry.getTaskId() == null ? 0L : entry.getTaskId().longValue());
            bindNullableLong(statement, 2, entry.getSyncRunId());
            bindNullableLong(statement, 3, entry.getSyncTableRunId());
            statement.setString(4, entry.getRunId());
            statement.setString(5, entry.getTableName());
            statement.setString(6, entry.getLogLevel());
            statement.setString(7, entry.getLogMessage());
            statement.setLong(8, entry.getCreatedAt().longValue());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long id = generatedKeys.getLong(1);
                    entry.setId(Long.valueOf(id));
                    return id;
                }
            }
            return 0L;
        }
    }

    @Override
    public List<SyncRunLogEntryDO> findByTaskId(long taskId) throws SQLException {
        return findList(FIND_BY_TASK_ID_SQL, taskId);
    }

    @Override
    public List<SyncRunLogEntryDO> findByRunId(String runId) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_RUN_ID_SQL)) {
            statement.setString(1, runId);
            return readList(statement);
        }
    }

    @Override
    public List<SyncRunLogEntryDO> findBySyncRunId(long syncRunId) throws SQLException {
        return findList(FIND_BY_SYNC_RUN_ID_SQL, syncRunId);
    }

    @Override
    public List<SyncRunLogEntryDO> findBySyncTableRunId(long syncTableRunId) throws SQLException {
        return findList(FIND_BY_SYNC_TABLE_RUN_ID_SQL, syncTableRunId);
    }

    @Override
    public List<SyncRunLogEntryDO> findRecent(int limit) throws SQLException {
        int safeLimit = limit <= 0 ? 20 : limit;
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_RECENT_SQL)) {
            statement.setInt(1, safeLimit);
            return readList(statement);
        }
    }

    @Override
    public int deleteOlderThan(long createdAt, List<Long> excludedTaskIds) throws SQLException {
        StringBuilder sql = new StringBuilder("DELETE FROM sync_run_log WHERE created_at < ?");
        if (excludedTaskIds != null && !excludedTaskIds.isEmpty()) {
            sql.append(" AND task_id NOT IN (");
            for (int i = 0; i < excludedTaskIds.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
            }
            sql.append(")");
        }
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, createdAt);
            if (excludedTaskIds != null) {
                for (int i = 0; i < excludedTaskIds.size(); i++) {
                    statement.setLong(i + 2, excludedTaskIds.get(i).longValue());
                }
            }
            return statement.executeUpdate();
        }
    }

    private List<SyncRunLogEntryDO> findList(String sql, long value) throws SQLException {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, value);
            return readList(statement);
        }
    }

    private List<SyncRunLogEntryDO> readList(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<SyncRunLogEntryDO> result = new ArrayList<SyncRunLogEntryDO>();
            while (resultSet.next()) {
                result.add(mapRow(resultSet));
            }
            return result;
        }
    }

    private SyncRunLogEntryDO mapRow(ResultSet resultSet) throws SQLException {
        SyncRunLogEntryDO entry = new SyncRunLogEntryDO();
        entry.setId(Long.valueOf(resultSet.getLong("id")));
        entry.setTaskId(Long.valueOf(resultSet.getLong("task_id")));
        entry.setSyncRunId(toLong(resultSet.getObject("sync_run_id")));
        entry.setSyncTableRunId(toLong(resultSet.getObject("sync_table_run_id")));
        entry.setRunId(resultSet.getString("run_id"));
        entry.setTableName(resultSet.getString("table_name"));
        entry.setLogLevel(resultSet.getString("log_level"));
        entry.setLogMessage(resultSet.getString("log_message"));
        entry.setCreatedAt(Long.valueOf(resultSet.getLong("created_at")));
        return entry;
    }

    private void bindNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value.longValue());
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(((Number) value).longValue());
    }
}
