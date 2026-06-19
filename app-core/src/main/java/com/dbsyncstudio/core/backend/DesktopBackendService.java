package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.connection.DatasourceConnectionTester;
import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.core.alert.AlertSendResult;
import com.dbsyncstudio.core.alert.AlertSenderService;
import com.dbsyncstudio.core.metadata.DatabaseMetadataScanner;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.schema.DatabaseDialect;
import com.dbsyncstudio.core.schema.SchemaComparisonEngine;
import com.dbsyncstudio.core.schema.SchemaSqlDialect;
import com.dbsyncstudio.core.validation.DataRepairEngine;
import com.dbsyncstudio.core.validation.DataValidationEngine;
import com.dbsyncstudio.core.sync.JdbcFullSyncEngine;
import com.dbsyncstudio.core.sync.JdbcIncrementalSyncEngine;
import com.dbsyncstudio.core.sync.SyncTaskPausedException;
import com.dbsyncstudio.core.sync.SyncTaskProgressListener;
import com.dbsyncstudio.core.sync.SyncTaskStoppedException;
import com.dbsyncstudio.core.scheduler.TaskScheduleCalculator;
import com.dbsyncstudio.model.datasource.ConnectionTestResult;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.datasource.DatasourceRepository;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.alert.AlertChannel;
import com.dbsyncstudio.model.alert.AlertChannelRepository;
import com.dbsyncstudio.model.alert.AlertChannelType;
import com.dbsyncstudio.model.alert.AlertDedupState;
import com.dbsyncstudio.model.alert.AlertDedupStateRepository;
import com.dbsyncstudio.model.alert.AlertHistoryEntry;
import com.dbsyncstudio.model.alert.AlertHistoryRepository;
import com.dbsyncstudio.model.alert.AlertRule;
import com.dbsyncstudio.model.alert.AlertRuleRepository;
import com.dbsyncstudio.model.monitoring.DatasourceConnectionMetric;
import com.dbsyncstudio.model.monitoring.MonitoringCleanupSummary;
import com.dbsyncstudio.model.monitoring.TaskRunMetric;
import com.dbsyncstudio.model.monitoring.TaskRunMetricSummary;
import com.dbsyncstudio.model.monitoring.TableRunMetric;
import com.dbsyncstudio.model.preview.DataPreviewFilter;
import com.dbsyncstudio.model.preview.DataPreviewFilterOperator;
import com.dbsyncstudio.model.preview.DataPreviewRequest;
import com.dbsyncstudio.model.preview.DataPreviewResult;
import com.dbsyncstudio.model.sync.FieldMappingRepository;
import com.dbsyncstudio.model.sync.FieldMappingRule;
import com.dbsyncstudio.model.sync.FieldMappingSuggestion;
import com.dbsyncstudio.model.sync.ExecutionLogEntry;
import com.dbsyncstudio.model.sync.ExecutionLogRepository;
import com.dbsyncstudio.model.sync.SyncRun;
import com.dbsyncstudio.model.sync.SyncRunLogEntry;
import com.dbsyncstudio.model.sync.SyncRunLogRepository;
import com.dbsyncstudio.model.sync.SyncRunRepository;
import com.dbsyncstudio.model.sync.FullSyncRequest;
import com.dbsyncstudio.model.sync.FullSyncResult;
import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointEntry;
import com.dbsyncstudio.model.sync.IncrementalSyncRequest;
import com.dbsyncstudio.model.sync.IncrementalSyncResult;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.SyncTableRun;
import com.dbsyncstudio.model.sync.SyncTableRunRepository;
import com.dbsyncstudio.model.sync.SyncTaskTable;
import com.dbsyncstudio.model.sync.SyncTaskTableRepository;
import com.dbsyncstudio.model.metadata.SchemaMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;
import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.schema.SchemaComparisonHistoryEntry;
import com.dbsyncstudio.model.schema.SchemaComparisonHistoryRepository;
import com.dbsyncstudio.model.schema.SchemaComparisonRequest;
import com.dbsyncstudio.model.schema.SchemaComparisonResult;
import com.dbsyncstudio.model.schema.SchemaDiffEntry;
import com.dbsyncstudio.model.schema.SchemaSqlPreviewRequest;
import com.dbsyncstudio.model.schema.SchemaSqlPreviewResult;
import com.dbsyncstudio.model.validation.RepairDetail;
import com.dbsyncstudio.model.validation.RepairRequest;
import com.dbsyncstudio.model.validation.RepairResult;
import com.dbsyncstudio.model.validation.RepairRun;
import com.dbsyncstudio.model.validation.RepairType;
import com.dbsyncstudio.model.validation.ValidationDifference;
import com.dbsyncstudio.model.validation.ValidationMode;
import com.dbsyncstudio.model.validation.ValidationRequest;
import com.dbsyncstudio.model.validation.ValidationResult;
import com.dbsyncstudio.model.validation.ValidationRun;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskRepository;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.model.sync.IncrementalSyncCheckpointRepository;
import com.dbsyncstudio.model.sync.LogCleanupSummary;
import com.dbsyncstudio.model.sync.SyncCheckpoint;
import com.dbsyncstudio.model.sql.SqlExecutionLogEntry;
import com.dbsyncstudio.model.sql.SqlExecutionLogRepository;
import com.dbsyncstudio.model.sql.SqlExecutionRequest;
import com.dbsyncstudio.model.sql.SqlExecutionResult;
import com.dbsyncstudio.store.sqlite.SqliteConnectionFactory;
import com.dbsyncstudio.store.sqlite.LocalSecretCryptoService;
import com.dbsyncstudio.store.sqlite.LocalSecretKeyProvider;
import com.dbsyncstudio.store.sqlite.SqliteDatasourceRepository;
import com.dbsyncstudio.store.sqlite.SqliteFieldMappingRepository;
import com.dbsyncstudio.store.sqlite.SqliteAlertChannelRepository;
import com.dbsyncstudio.store.sqlite.SqliteAlertDedupStateRepository;
import com.dbsyncstudio.store.sqlite.SqliteAlertHistoryRepository;
import com.dbsyncstudio.store.sqlite.SqliteAlertRuleRepository;
import com.dbsyncstudio.store.sqlite.SqliteDatabasePaths;
import com.dbsyncstudio.store.sqlite.SqliteRepairRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncRunLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncRunRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTableRunRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTaskTableRepository;
import com.dbsyncstudio.store.sqlite.SqliteMonitoringRepository;
import com.dbsyncstudio.store.sqlite.SqliteSqlExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteValidationRepository;
import com.dbsyncstudio.store.sqlite.SqliteSchemaComparisonHistoryRepository;
import com.dbsyncstudio.store.sqlite.SqliteSyncTaskRepository;
import com.dbsyncstudio.store.sync.SqliteExecutionLogRepository;
import com.dbsyncstudio.store.sqlite.SqliteIncrementalSyncCheckpointRepository;
import com.dbsyncstudio.store.sync.SqliteSyncCheckpointRepository;
import com.dbsyncstudio.store.sqlite.SqliteDatabasePaths;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DesktopBackendService {

    private static final Logger LOGGER = Logger.getLogger(DesktopBackendService.class.getName());
    private static final String LOG_RETENTION_DAYS_KEY = "log_retention_days";
    private static final int DEFAULT_LOG_RETENTION_DAYS = 30;
    private static final int DEFAULT_MONITORING_RETENTION_DAYS = 30;
    private static final int DEFAULT_ALERT_COOLDOWN_SECONDS = 600;

    private final SqliteDatasourceRepository datasourceRepository;
    private final SqliteSyncTaskRepository syncTaskRepository;
    private final SqliteExecutionLogRepository executionLogRepository;
    private final SqliteSyncCheckpointRepository checkpointRepository;
    private final SqliteFieldMappingRepository fieldMappingRepository;
    private final SqliteSyncTaskTableRepository syncTaskTableRepository;
    private final SqliteSyncRunRepository syncRunRepository;
    private final SqliteSyncTableRunRepository syncTableRunRepository;
    private final SqliteSyncRunLogRepository syncRunLogRepository;
    private final SqliteSqlExecutionLogRepository sqlExecutionLogRepository;
    private final SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository;
    private final SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository;
    private final SqliteValidationRepository validationRepository;
    private final SqliteRepairRepository repairRepository;
    private final SqliteMonitoringRepository monitoringRepository;
    private final SqliteAlertRuleRepository alertRuleRepository;
    private final SqliteAlertChannelRepository alertChannelRepository;
    private final SqliteAlertHistoryRepository alertHistoryRepository;
    private final SqliteAlertDedupStateRepository alertDedupStateRepository;
    private final LocalSecretCryptoService alertCryptoService;
    private final AlertSenderService alertSenderService;
    private final DatabaseMetadataScanner metadataScanner;
    private final DatasourceConnectionTester connectionTester;
    private final FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher;
    private final SchemaComparisonEngine schemaComparisonEngine;
    private final DataValidationEngine dataValidationEngine;
    private final DataRepairEngine dataRepairEngine;
    private final JdbcFullSyncEngine fullSyncEngine;
    private final JdbcIncrementalSyncEngine incrementalSyncEngine;
    private final ExecutorService taskExecutor;
    private final Map<Long, TaskExecutionState> taskExecutionStates;
    private SqliteConnectionFactory connectionFactory;
    private volatile int lastRecoveredTaskCount;

    public DesktopBackendService(SqliteDatasourceRepository datasourceRepository,
                                 SqliteSyncTaskRepository syncTaskRepository,
                                 SqliteExecutionLogRepository executionLogRepository,
                                 SqliteSyncCheckpointRepository checkpointRepository,
                                 SqliteFieldMappingRepository fieldMappingRepository,
                                 SqliteSyncTaskTableRepository syncTaskTableRepository,
                                 SqliteSyncRunRepository syncRunRepository,
                                 SqliteSyncTableRunRepository syncTableRunRepository,
                                 SqliteSyncRunLogRepository syncRunLogRepository,
                                 SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                                 SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                                 SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                                 SqliteValidationRepository validationRepository,
                                 SqliteRepairRepository repairRepository,
                                 SqliteMonitoringRepository monitoringRepository,
                                 SqliteAlertRuleRepository alertRuleRepository,
                                 SqliteAlertChannelRepository alertChannelRepository,
                                 SqliteAlertHistoryRepository alertHistoryRepository,
                                 SqliteAlertDedupStateRepository alertDedupStateRepository,
                                 LocalSecretCryptoService alertCryptoService,
                                 DatabaseMetadataScanner metadataScanner,
                                 DatasourceConnectionTester connectionTester,
                                 FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher,
                                 SchemaComparisonEngine schemaComparisonEngine,
                                 DataValidationEngine dataValidationEngine,
                                 DataRepairEngine dataRepairEngine,
                                 JdbcFullSyncEngine fullSyncEngine,
                                 JdbcIncrementalSyncEngine incrementalSyncEngine) {
        this.datasourceRepository = datasourceRepository;
        this.syncTaskRepository = syncTaskRepository;
        this.executionLogRepository = executionLogRepository;
        this.checkpointRepository = checkpointRepository;
        this.fieldMappingRepository = fieldMappingRepository;
        this.syncTaskTableRepository = syncTaskTableRepository;
        this.syncRunRepository = syncRunRepository;
        this.syncTableRunRepository = syncTableRunRepository;
        this.syncRunLogRepository = syncRunLogRepository;
        this.sqlExecutionLogRepository = sqlExecutionLogRepository;
        this.schemaComparisonHistoryRepository = schemaComparisonHistoryRepository;
        this.incrementalCheckpointRepository = incrementalCheckpointRepository;
        this.validationRepository = validationRepository;
        this.repairRepository = repairRepository;
        this.monitoringRepository = monitoringRepository;
        this.alertRuleRepository = alertRuleRepository;
        this.alertChannelRepository = alertChannelRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.alertDedupStateRepository = alertDedupStateRepository;
        this.alertCryptoService = alertCryptoService;
        this.alertSenderService = new AlertSenderService();
        this.metadataScanner = metadataScanner;
        this.connectionTester = connectionTester;
        this.fieldMappingSuggestionMatcher = fieldMappingSuggestionMatcher;
        this.schemaComparisonEngine = schemaComparisonEngine;
        this.dataValidationEngine = dataValidationEngine;
        this.dataRepairEngine = dataRepairEngine;
        this.fullSyncEngine = fullSyncEngine;
        this.incrementalSyncEngine = incrementalSyncEngine;
        this.taskExecutor = Executors.newCachedThreadPool();
        this.taskExecutionStates = new ConcurrentHashMap<Long, TaskExecutionState>();
        this.connectionFactory = null;
        this.lastRecoveredTaskCount = 0;
    }

    public DesktopBackendService(SqliteDatasourceRepository datasourceRepository,
                                 SqliteSyncTaskRepository syncTaskRepository,
                                 SqliteExecutionLogRepository executionLogRepository,
                                 SqliteSyncCheckpointRepository checkpointRepository,
                                 SqliteFieldMappingRepository fieldMappingRepository,
                                 SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                                 SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                                 SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                                 DatabaseMetadataScanner metadataScanner,
                                 DatasourceConnectionTester connectionTester,
                                 FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher,
                                 SchemaComparisonEngine schemaComparisonEngine,
                                 JdbcFullSyncEngine fullSyncEngine,
                                 JdbcIncrementalSyncEngine incrementalSyncEngine) {
        this(datasourceRepository,
                syncTaskRepository,
                executionLogRepository,
                checkpointRepository,
                fieldMappingRepository,
                null,
                null,
                null,
                null,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                metadataScanner,
                connectionTester,
                fieldMappingSuggestionMatcher,
                schemaComparisonEngine,
                null,
                null,
                fullSyncEngine,
                incrementalSyncEngine);
    }

    public DesktopBackendService(SqliteDatasourceRepository datasourceRepository,
                                 SqliteSyncTaskRepository syncTaskRepository,
                                 SqliteExecutionLogRepository executionLogRepository,
                                 SqliteSyncCheckpointRepository checkpointRepository,
                                 SqliteFieldMappingRepository fieldMappingRepository,
                                 SqliteSyncTaskTableRepository syncTaskTableRepository,
                                 SqliteSyncRunRepository syncRunRepository,
                                 SqliteSyncTableRunRepository syncTableRunRepository,
                                 SqliteSyncRunLogRepository syncRunLogRepository,
                                 SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                                 SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                                 SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                                 SqliteValidationRepository validationRepository,
                                 SqliteRepairRepository repairRepository,
                                 DatabaseMetadataScanner metadataScanner,
                                 DatasourceConnectionTester connectionTester,
                                 FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher,
                                 SchemaComparisonEngine schemaComparisonEngine,
                                 DataValidationEngine dataValidationEngine,
                                 DataRepairEngine dataRepairEngine,
                                 JdbcFullSyncEngine fullSyncEngine,
                                 JdbcIncrementalSyncEngine incrementalSyncEngine) {
        this(datasourceRepository,
                syncTaskRepository,
                executionLogRepository,
                checkpointRepository,
                fieldMappingRepository,
                syncTaskTableRepository,
                syncRunRepository,
                syncTableRunRepository,
                syncRunLogRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                validationRepository,
                repairRepository,
                null,
                null,
                null,
                null,
                null,
                null,
                metadataScanner,
                connectionTester,
                fieldMappingSuggestionMatcher,
                schemaComparisonEngine,
                dataValidationEngine,
                dataRepairEngine,
                fullSyncEngine,
                incrementalSyncEngine);
    }

    public DesktopBackendService(SqliteDatasourceRepository datasourceRepository,
                                 SqliteSyncTaskRepository syncTaskRepository,
                                 SqliteExecutionLogRepository executionLogRepository,
                                 SqliteSyncCheckpointRepository checkpointRepository,
                                 SqliteFieldMappingRepository fieldMappingRepository,
                                 SqliteSyncTaskTableRepository syncTaskTableRepository,
                                 SqliteSyncRunRepository syncRunRepository,
                                 SqliteSyncTableRunRepository syncTableRunRepository,
                                 SqliteSyncRunLogRepository syncRunLogRepository,
                                 SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                                 SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                                 SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                                 SqliteValidationRepository validationRepository,
                                 SqliteRepairRepository repairRepository,
                                 SqliteMonitoringRepository monitoringRepository,
                                 SqliteAlertRuleRepository alertRuleRepository,
                                 SqliteAlertChannelRepository alertChannelRepository,
                                 SqliteAlertHistoryRepository alertHistoryRepository,
                                 SqliteAlertDedupStateRepository alertDedupStateRepository,
                                 LocalSecretCryptoService alertCryptoService,
                                 DatabaseMetadataScanner metadataScanner,
                                 DatasourceConnectionTester connectionTester,
                                 FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher,
                                 SchemaComparisonEngine schemaComparisonEngine,
                                 DataValidationEngine dataValidationEngine,
                                 DataRepairEngine dataRepairEngine,
                                 JdbcFullSyncEngine fullSyncEngine,
                                 JdbcIncrementalSyncEngine incrementalSyncEngine,
                                 SqliteConnectionFactory connectionFactory) {
        this(datasourceRepository,
                syncTaskRepository,
                executionLogRepository,
                checkpointRepository,
                fieldMappingRepository,
                syncTaskTableRepository,
                syncRunRepository,
                syncTableRunRepository,
                syncRunLogRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                validationRepository,
                repairRepository,
                monitoringRepository,
                alertRuleRepository,
                alertChannelRepository,
                alertHistoryRepository,
                alertDedupStateRepository,
                alertCryptoService,
                metadataScanner,
                connectionTester,
                fieldMappingSuggestionMatcher,
                schemaComparisonEngine,
                dataValidationEngine,
                dataRepairEngine,
                fullSyncEngine,
                incrementalSyncEngine);
        this.connectionFactory = connectionFactory;
        this.lastRecoveredTaskCount = 0;
    }

    public DesktopBackendService(SqliteDatasourceRepository datasourceRepository,
                                 SqliteSyncTaskRepository syncTaskRepository,
                                 SqliteExecutionLogRepository executionLogRepository,
                                 SqliteSyncCheckpointRepository checkpointRepository,
                                 SqliteFieldMappingRepository fieldMappingRepository,
                                 SqliteSyncTaskTableRepository syncTaskTableRepository,
                                 SqliteSyncRunRepository syncRunRepository,
                                 SqliteSyncTableRunRepository syncTableRunRepository,
                                 SqliteSyncRunLogRepository syncRunLogRepository,
                                 SqliteSqlExecutionLogRepository sqlExecutionLogRepository,
                                 SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository,
                                 SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository,
                                 SqliteValidationRepository validationRepository,
                                 SqliteRepairRepository repairRepository,
                                 SqliteMonitoringRepository monitoringRepository,
                                 DatabaseMetadataScanner metadataScanner,
                                 DatasourceConnectionTester connectionTester,
                                 FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher,
                                 SchemaComparisonEngine schemaComparisonEngine,
                                 DataValidationEngine dataValidationEngine,
                                 DataRepairEngine dataRepairEngine,
                                 JdbcFullSyncEngine fullSyncEngine,
                                 JdbcIncrementalSyncEngine incrementalSyncEngine) {
        this.datasourceRepository = datasourceRepository;
        this.syncTaskRepository = syncTaskRepository;
        this.executionLogRepository = executionLogRepository;
        this.checkpointRepository = checkpointRepository;
        this.fieldMappingRepository = fieldMappingRepository;
        this.syncTaskTableRepository = syncTaskTableRepository;
        this.syncRunRepository = syncRunRepository;
        this.syncTableRunRepository = syncTableRunRepository;
        this.syncRunLogRepository = syncRunLogRepository;
        this.sqlExecutionLogRepository = sqlExecutionLogRepository;
        this.schemaComparisonHistoryRepository = schemaComparisonHistoryRepository;
        this.incrementalCheckpointRepository = incrementalCheckpointRepository;
        this.validationRepository = validationRepository;
        this.repairRepository = repairRepository;
        this.monitoringRepository = monitoringRepository;
        this.alertRuleRepository = null;
        this.alertChannelRepository = null;
        this.alertHistoryRepository = null;
        this.alertDedupStateRepository = null;
        this.alertCryptoService = null;
        this.alertSenderService = new AlertSenderService();
        this.metadataScanner = metadataScanner;
        this.connectionTester = connectionTester;
        this.fieldMappingSuggestionMatcher = fieldMappingSuggestionMatcher;
        this.schemaComparisonEngine = schemaComparisonEngine;
        this.dataValidationEngine = dataValidationEngine;
        this.dataRepairEngine = dataRepairEngine;
        this.fullSyncEngine = fullSyncEngine;
        this.incrementalSyncEngine = incrementalSyncEngine;
        this.taskExecutor = Executors.newCachedThreadPool();
        this.taskExecutionStates = new ConcurrentHashMap<Long, TaskExecutionState>();
        this.connectionFactory = null;
    }

    public static DesktopBackendService createDefault(File databaseFile) throws SQLException {
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(databaseFile);
        JdbcDatabaseMetadataScanner metadataScanner = new JdbcDatabaseMetadataScanner();
        DefaultDatasourceConnectionOpener connectionOpener = new DefaultDatasourceConnectionOpener();
        SqliteDatasourceRepository datasourceRepository = new SqliteDatasourceRepository(connectionFactory);
        SqliteSyncTaskRepository syncTaskRepository = new SqliteSyncTaskRepository(connectionFactory);
        SqliteExecutionLogRepository executionLogRepository = new SqliteExecutionLogRepository(connectionFactory);
        SqliteSyncCheckpointRepository checkpointRepository = new SqliteSyncCheckpointRepository(connectionFactory);
        SqliteFieldMappingRepository fieldMappingRepository = new SqliteFieldMappingRepository(connectionFactory);
        SqliteSyncTaskTableRepository syncTaskTableRepository = new SqliteSyncTaskTableRepository(connectionFactory);
        SqliteSyncRunRepository syncRunRepository = new SqliteSyncRunRepository(connectionFactory);
        SqliteSyncTableRunRepository syncTableRunRepository = new SqliteSyncTableRunRepository(connectionFactory);
        SqliteSyncRunLogRepository syncRunLogRepository = new SqliteSyncRunLogRepository(connectionFactory);
        SqliteSqlExecutionLogRepository sqlExecutionLogRepository = new SqliteSqlExecutionLogRepository(connectionFactory);
        SqliteSchemaComparisonHistoryRepository schemaComparisonHistoryRepository = new SqliteSchemaComparisonHistoryRepository(connectionFactory);
        SqliteIncrementalSyncCheckpointRepository incrementalCheckpointRepository = new SqliteIncrementalSyncCheckpointRepository(connectionFactory);
        SqliteValidationRepository validationRepository = new SqliteValidationRepository(connectionFactory);
        SqliteRepairRepository repairRepository = new SqliteRepairRepository(connectionFactory);
        SqliteMonitoringRepository monitoringRepository = new SqliteMonitoringRepository(connectionFactory);
        LocalSecretCryptoService alertCryptoService =
                new LocalSecretCryptoService(new LocalSecretKeyProvider(SqliteDatabasePaths.defaultAlertSecretKeyFile()));
        SqliteAlertRuleRepository alertRuleRepository = new SqliteAlertRuleRepository(connectionFactory);
        SqliteAlertChannelRepository alertChannelRepository = new SqliteAlertChannelRepository(connectionFactory, alertCryptoService);
        SqliteAlertHistoryRepository alertHistoryRepository = new SqliteAlertHistoryRepository(connectionFactory);
        SqliteAlertDedupStateRepository alertDedupStateRepository = new SqliteAlertDedupStateRepository(connectionFactory);

        datasourceRepository.initialize();
        syncTaskRepository.initialize();
        executionLogRepository.initialize();
        checkpointRepository.initialize();
        fieldMappingRepository.initialize();
        syncTaskTableRepository.initialize();
        syncRunRepository.initialize();
        syncTableRunRepository.initialize();
        syncRunLogRepository.initialize();
        sqlExecutionLogRepository.initialize();
        schemaComparisonHistoryRepository.initialize();
        incrementalCheckpointRepository.initialize();
        validationRepository.initialize();
        repairRepository.initialize();
        monitoringRepository.initialize();
        alertRuleRepository.initialize();
        alertChannelRepository.initialize();
        alertHistoryRepository.initialize();
        alertDedupStateRepository.initialize();

        DesktopBackendService service = new DesktopBackendService(
                datasourceRepository,
                syncTaskRepository,
                executionLogRepository,
                checkpointRepository,
                fieldMappingRepository,
                syncTaskTableRepository,
                syncRunRepository,
                syncTableRunRepository,
                syncRunLogRepository,
                sqlExecutionLogRepository,
                schemaComparisonHistoryRepository,
                incrementalCheckpointRepository,
                validationRepository,
                repairRepository,
                monitoringRepository,
                alertRuleRepository,
                alertChannelRepository,
                alertHistoryRepository,
                alertDedupStateRepository,
                alertCryptoService,
                metadataScanner,
                new JdbcDatasourceConnectionTester(),
                new FieldMappingSuggestionMatcher(),
                new SchemaComparisonEngine(),
                new DataValidationEngine(metadataScanner, connectionOpener),
                new DataRepairEngine(connectionOpener),
                new JdbcFullSyncEngine(metadataScanner, connectionOpener, checkpointRepository),
                new JdbcIncrementalSyncEngine(executionLogRepository, incrementalCheckpointRepository),
                connectionFactory);
        service.lastRecoveredTaskCount = service.recoverUnfinishedTasks();
        return service;
    }

    public List<DatasourceConfig> listDatasources() throws SQLException {
        return datasourceRepository.findAll();
    }

    public DatasourceConfig saveDatasource(DatasourceConfig config) throws SQLException {
        validateDatasource(config);
        datasourceRepository.save(config);
        return config;
    }

    public boolean deleteDatasource(long id) throws SQLException {
        return datasourceRepository.deleteById(id);
    }

    public Optional<DatasourceConfig> findDatasourceById(long id) throws SQLException {
        return datasourceRepository.findById(id);
    }

    public ConnectionTestResult testConnection(DatasourceConfig config) {
        validateDatasource(config);
        ConnectionTestResult result = connectionTester.test(config);
        captureDatasourceConnectionMetric(config, result);
        if (!result.isSuccess()) {
            triggerAlert("DATASOURCE_CONNECTION_FAILED", null, null, null, "ERROR",
                    result.getMessage(), null, config.getId());
        }
        return result;
    }

    public List<SyncTask> listTasks() throws SQLException {
        List<SyncTask> tasks = syncTaskRepository.findAll();
        List<SyncTask> result = new ArrayList<SyncTask>();
        for (SyncTask task : tasks) {
            SyncTask loadedTask = applyIncrementalCheckpoint(task);
            if (syncTaskTableRepository != null) {
                loadedTask.setTaskTables(syncTaskTableRepository.findByTaskId(task.getId().longValue()));
            }
            result.add(loadedTask);
        }
        return result;
    }

    public Optional<SyncTask> findTaskById(long taskId) throws SQLException {
        Optional<SyncTask> task = syncTaskRepository.findById(taskId);
        if (!task.isPresent()) {
            return task;
        }
        SyncTask loadedTask = applyIncrementalCheckpoint(task.get());
        if (syncTaskTableRepository != null) {
            loadedTask.setTaskTables(syncTaskTableRepository.findByTaskId(taskId));
        }
        return Optional.of(loadedTask);
    }

    public SyncTask saveTask(SyncTask task) throws SQLException {
        validateTask(task);
        if (task.getTaskStatus() == null) {
            task.setTaskStatus(SyncTaskStatus.PENDING);
        }
        if (task.getSyncMode() == null) {
            task.setSyncMode(SyncMode.FULL);
        }
        normalizeSchedule(task);
        syncTaskRepository.save(task);
        saveTaskTables(task);
        return task;
    }

    public boolean deleteTask(long id) throws SQLException {
        return syncTaskRepository.deleteById(id);
    }

    public List<ExecutionLogEntry> listLogs(Long taskId) {
        if (taskId == null) {
            return new ArrayList<ExecutionLogEntry>();
        }
        return executionLogRepository.findByTaskId(taskId.longValue());
    }

    public void appendTaskLog(long taskId, String logLevel, String message) {
        appendLog(taskId, logLevel, message);
    }

    public List<ExecutionLogEntry> listAllLogs(List<SyncTask> tasks) {
        List<ExecutionLogEntry> result = new ArrayList<ExecutionLogEntry>();
        for (SyncTask task : tasks) {
            result.addAll(executionLogRepository.findByTaskId(task.getId().longValue()));
        }
        return result;
    }

    public List<ExecutionLogEntry> listTaskScheduleHistory(long taskId) {
        List<ExecutionLogEntry> entries = executionLogRepository.findByTaskId(taskId);
        List<ExecutionLogEntry> result = new ArrayList<ExecutionLogEntry>();
        for (ExecutionLogEntry entry : entries) {
            String message = entry.getLogMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("scheduled execution")) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<SyncTaskTable> listTaskTables(long taskId) throws SQLException {
        if (syncTaskTableRepository == null) {
            return new ArrayList<SyncTaskTable>();
        }
        return syncTaskTableRepository.findByTaskId(taskId);
    }

    public SyncTaskTable saveTaskTable(long taskId, SyncTaskTable taskTable) throws SQLException {
        if (taskTable == null) {
            throw new IllegalArgumentException("Task table must not be null");
        }
        if (syncTaskTableRepository == null) {
            throw new IllegalStateException("Task table repository is not configured");
        }
        taskTable.setTaskId(Long.valueOf(taskId));
        if (trimToNull(taskTable.getSourceTableName()) == null) {
            throw new IllegalArgumentException("Source table name must not be blank");
        }
        if (trimToNull(taskTable.getTargetTableName()) == null) {
            throw new IllegalArgumentException("Target table name must not be blank");
        }
        if (taskTable.getEnabled() == null) {
            taskTable.setEnabled(Boolean.TRUE);
        }
        normalizeTaskTableConfig(taskTable, findTaskById(taskId).orElse(null));
        syncTaskTableRepository.save(taskTable);
        return taskTable;
    }

    private void saveTaskTables(SyncTask task) throws SQLException {
        if (syncTaskTableRepository == null || task == null || task.getId() == null) {
            return;
        }
        List<SyncTaskTable> taskTables = task.getTaskTables();
        if (taskTables == null) {
            return;
        }
        List<SyncTaskTable> existingTaskTables = syncTaskTableRepository.findByTaskId(task.getId().longValue());
        for (SyncTaskTable existingTaskTable : existingTaskTables) {
            if (existingTaskTable != null && existingTaskTable.getId() != null) {
                syncTaskTableRepository.deleteById(existingTaskTable.getId().longValue());
            }
        }
        for (int i = 0; i < taskTables.size(); i++) {
            SyncTaskTable taskTable = taskTables.get(i);
            if (taskTable == null) {
                continue;
            }
            taskTable.setTaskId(task.getId());
            normalizeTaskTableConfig(taskTable, task);
            if (taskTable.getTableOrder() == null) {
                taskTable.setTableOrder(Integer.valueOf(i + 1));
            }
            if (taskTable.getEnabled() == null) {
                taskTable.setEnabled(Boolean.TRUE);
            }
            syncTaskTableRepository.save(taskTable);
        }
    }

    public boolean deleteTaskTable(long taskTableId) throws SQLException {
        if (syncTaskTableRepository == null) {
            return false;
        }
        return syncTaskTableRepository.deleteById(taskTableId);
    }

    public List<SyncRun> listSyncRuns(Long taskId, int limit) throws SQLException {
        if (syncRunRepository == null) {
            return new ArrayList<SyncRun>();
        }
        int safeLimit = limit <= 0 ? 20 : limit;
        if (taskId == null) {
            return syncRunRepository.findRecent(safeLimit);
        }
        List<SyncRun> runs = syncRunRepository.findByTaskId(taskId.longValue());
        if (runs.size() <= safeLimit) {
            return runs;
        }
        return new ArrayList<SyncRun>(runs.subList(0, safeLimit));
    }

    public Optional<SyncRun> findSyncRunById(long syncRunId) throws SQLException {
        if (syncRunRepository == null) {
            return Optional.empty();
        }
        return syncRunRepository.findById(syncRunId);
    }

    public Optional<SyncRun> findSyncRunByRunId(String runId) throws SQLException {
        if (syncRunRepository == null) {
            return Optional.empty();
        }
        return syncRunRepository.findByRunId(runId);
    }

    public List<SyncTableRun> listSyncTableRuns(long syncRunId) throws SQLException {
        if (syncTableRunRepository == null) {
            return new ArrayList<SyncTableRun>();
        }
        return syncTableRunRepository.findBySyncRunId(syncRunId);
    }

    public List<SyncRunLogEntry> listSyncRunLogs(Long taskId, String runId, Long syncRunId, Long syncTableRunId,
                                                 String tableName, String logLevel, String keyword,
                                                 Long startTime, Long endTime, int limit) throws SQLException {
        if (syncRunLogRepository == null) {
            return new ArrayList<SyncRunLogEntry>();
        }
        int safeLimit = limit <= 0 ? 100 : limit;
        List<SyncRunLogEntry> candidates = loadSyncRunLogCandidates(taskId, runId, syncRunId, syncTableRunId, safeLimit);
        List<SyncRunLogEntry> result = new ArrayList<SyncRunLogEntry>();
        String normalizedTableName = trimToNull(tableName);
        String normalizedLogLevel = trimToNull(logLevel);
        String normalizedKeyword = trimToNull(keyword);
        for (SyncRunLogEntry entry : candidates) {
            if (entry == null) {
                continue;
            }
            if (!matchesSyncRunLogIdentifiers(entry, taskId, runId, syncRunId, syncTableRunId)) {
                continue;
            }
            if (normalizedTableName != null && (entry.getTableName() == null || !normalizedTableName.equalsIgnoreCase(entry.getTableName()))) {
                continue;
            }
            if (normalizedLogLevel != null && (entry.getLogLevel() == null || !normalizedLogLevel.equalsIgnoreCase(entry.getLogLevel()))) {
                continue;
            }
            if (startTime != null && (entry.getCreatedAt() == null || entry.getCreatedAt().longValue() < startTime.longValue())) {
                continue;
            }
            if (endTime != null && (entry.getCreatedAt() == null || entry.getCreatedAt().longValue() > endTime.longValue())) {
                continue;
            }
            if (normalizedKeyword != null) {
                String message = entry.getLogMessage() == null ? "" : entry.getLogMessage();
                if (!message.toLowerCase(Locale.ROOT).contains(normalizedKeyword.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }
            result.add(entry);
        }
        return result;
    }

    public SyncRunDetailResponse loadSyncRunDetail(long taskId, String runId, int limit) throws SQLException {
        SyncRun run = findSyncRunByRunId(runId).orElse(null);
        if (run == null) {
            throw new SQLException("Sync run not found: " + runId);
        }
        if (run.getTaskId() == null || run.getTaskId().longValue() != taskId) {
            throw new SQLException("Sync run does not belong to task: " + taskId);
        }
        List<SyncTableRun> tableRuns = listSyncTableRuns(run.getId().longValue());
        List<SyncRunLogEntry> logs = listSyncRunLogs(Long.valueOf(taskId), runId, run.getId(), null, null, null, null, null, null, limit);
        SyncRunDetailResponse detail = new SyncRunDetailResponse();
        detail.setRun(run);
        detail.setTableRuns(tableRuns);
        detail.setLogs(logs);
        return detail;
    }

    public List<SqlExecutionLogEntry> listSqlExecutionLogs(int limit) {
        return sqlExecutionLogRepository.findRecent(limit);
    }

    public int getLogRetentionDays() {
        SyncCheckpoint checkpoint = loadOrCreateCheckpoint(LOG_RETENTION_DAYS_KEY, String.valueOf(DEFAULT_LOG_RETENTION_DAYS));
        String value = checkpoint.getCheckpointValue();
        if (value == null || value.trim().length() == 0) {
            return DEFAULT_LOG_RETENTION_DAYS;
        }
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (Exception ex) {
            return DEFAULT_LOG_RETENTION_DAYS;
        }
    }

    public int updateLogRetentionDays(int retentionDays) {
        int safeDays = Math.max(1, retentionDays);
        SyncCheckpoint checkpoint = new SyncCheckpoint();
        checkpoint.setCheckpointKey(LOG_RETENTION_DAYS_KEY);
        checkpoint.setCheckpointValue(String.valueOf(safeDays));
        checkpoint.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        checkpointRepository.save(checkpoint);
        return safeDays;
    }

    public LogCleanupSummary cleanupLogs(Integer retentionDays) throws SQLException {
        int safeDays = retentionDays == null ? getLogRetentionDays() : Math.max(1, retentionDays.intValue());
        long cutoffTime = System.currentTimeMillis() - (long) safeDays * 24L * 60L * 60L * 1000L;
        List<SyncTask> tasks = syncTaskRepository.findAll();
        List<Long> runningTaskIds = new ArrayList<Long>();
        for (SyncTask task : tasks) {
            if (task != null && task.getTaskStatus() == SyncTaskStatus.RUNNING && task.getId() != null) {
                runningTaskIds.add(task.getId());
            }
        }
        for (Long activeTaskId : taskExecutionStates.keySet()) {
            if (activeTaskId != null && !runningTaskIds.contains(activeTaskId)) {
                runningTaskIds.add(activeTaskId);
            }
        }
        int executionDeleted = executionLogRepository == null ? 0 : executionLogRepository.deleteOlderThan(cutoffTime, runningTaskIds);
        int syncRunDeleted = syncRunLogRepository == null ? 0 : syncRunLogRepository.deleteOlderThan(cutoffTime, runningTaskIds);
        int sqlDeleted = sqlExecutionLogRepository == null ? 0 : sqlExecutionLogRepository.deleteOlderThan(cutoffTime);
        updateLogRetentionDays(safeDays);
        return LogCleanupSummary.builder()
                .retentionDays(Integer.valueOf(safeDays))
                .cutoffTime(Long.valueOf(cutoffTime))
                .executionLogDeletedCount(Integer.valueOf(executionDeleted))
                .syncRunLogDeletedCount(Integer.valueOf(syncRunDeleted))
                .sqlExecutionLogDeletedCount(Integer.valueOf(sqlDeleted))
                .build();
    }

    public List<SchemaComparisonHistoryEntry> listSchemaComparisonHistory(int limit) {
        return schemaComparisonHistoryRepository.findRecent(limit);
    }

    public List<AlertRule> listAlertRules() throws SQLException {
        if (alertRuleRepository == null) {
            return new ArrayList<AlertRule>();
        }
        return alertRuleRepository.findAll();
    }

    public List<AlertChannel> listAlertChannels() throws SQLException {
        if (alertChannelRepository == null) {
            return new ArrayList<AlertChannel>();
        }
        List<AlertChannel> channels = alertChannelRepository.findAll();
        List<AlertChannel> result = new ArrayList<AlertChannel>();
        for (AlertChannel channel : channels) {
            result.add(sanitizeAlertChannel(channel));
        }
        return result;
    }

    public List<AlertHistoryEntry> listAlertHistory(int limit) throws SQLException {
        return listAlertHistory(null, null, null, null, null, null, limit);
    }

    public List<AlertHistoryEntry> listAlertHistory(Long taskId, String alertType, String sendStatus,
                                                    Long startTime, Long endTime, String keyword, int limit) throws SQLException {
        if (alertHistoryRepository == null) {
            return new ArrayList<AlertHistoryEntry>();
        }
        List<AlertHistoryEntry> history = alertHistoryRepository.findAll();
        List<AlertHistoryEntry> result = new ArrayList<AlertHistoryEntry>();
        int safeLimit = limit <= 0 ? 50 : limit;
        String normalizedAlertType = trimToNull(alertType);
        String normalizedSendStatus = trimToNull(sendStatus);
        String normalizedKeyword = trimToNull(keyword);
        for (AlertHistoryEntry entry : history) {
            if (entry == null) {
                continue;
            }
            if (taskId != null && (entry.getTaskId() == null || !taskId.equals(entry.getTaskId()))) {
                continue;
            }
            if (normalizedAlertType != null && (entry.getAlertType() == null || !normalizedAlertType.equalsIgnoreCase(entry.getAlertType()))) {
                continue;
            }
            if (normalizedSendStatus != null && (entry.getSendStatus() == null || !normalizedSendStatus.equalsIgnoreCase(entry.getSendStatus()))) {
                continue;
            }
            if (startTime != null && (entry.getCreatedTime() == null || entry.getCreatedTime().longValue() < startTime.longValue())) {
                continue;
            }
            if (endTime != null && (entry.getCreatedTime() == null || entry.getCreatedTime().longValue() > endTime.longValue())) {
                continue;
            }
            if (normalizedKeyword != null) {
                String content = entry.getAlertContent() == null ? "" : entry.getAlertContent();
                String errorMessage = entry.getErrorMessage() == null ? "" : entry.getErrorMessage();
                if (!content.toLowerCase(Locale.ROOT).contains(normalizedKeyword.toLowerCase(Locale.ROOT))
                        && !errorMessage.toLowerCase(Locale.ROOT).contains(normalizedKeyword.toLowerCase(Locale.ROOT))) {
                    continue;
                }
            }
            result.add(entry);
            if (result.size() >= safeLimit) {
                break;
            }
        }
        return result;
    }

    public AlertRule saveAlertRule(AlertRule rule) throws SQLException {
        if (rule == null) {
            throw new IllegalArgumentException("Alert rule must not be null");
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(Boolean.TRUE);
        }
        if (rule.getCooldownSeconds() == null) {
            rule.setCooldownSeconds(Integer.valueOf(DEFAULT_ALERT_COOLDOWN_SECONDS));
        }
        alertRuleRepository.save(rule);
        return rule;
    }

    public AlertChannel saveAlertChannel(AlertChannel channel) throws SQLException {
        if (channel == null) {
            throw new IllegalArgumentException("Alert channel must not be null");
        }
        if (channel.getEnabled() == null) {
            channel.setEnabled(Boolean.TRUE);
        }
        if (channel.getId() != null && alertChannelRepository != null) {
            AlertChannel existing = alertChannelRepository.findById(channel.getId().longValue()).orElse(null);
            if (existing != null) {
                mergeAlertChannelSecrets(channel, existing);
            }
        }
        alertChannelRepository.save(channel);
        return sanitizeAlertChannel(channel);
    }

    public boolean deleteAlertRule(long id) throws SQLException {
        return alertRuleRepository != null && alertRuleRepository.deleteById(id);
    }

    public boolean deleteAlertChannel(long id) throws SQLException {
        return alertChannelRepository != null && alertChannelRepository.deleteById(id);
    }

    public AlertSendResult testAlertChannel(long channelId, String content) throws SQLException {
        if (alertChannelRepository == null) {
            throw new IllegalStateException("Alert channel repository is not configured");
        }
        AlertChannel channel = alertChannelRepository.findById(channelId)
                .orElseThrow(() -> new SQLException("Alert channel not found: " + channelId));
        AlertSendResult result = alertSenderService.send(channel,
                "DB Sync Studio Alert Test",
                content == null ? "This is a test alert from DB Sync Studio." : content);
        AlertHistoryEntry history = AlertHistoryEntry.builder()
                .alertId("test-" + channelId + "-" + System.currentTimeMillis())
                .alertType("CHANNEL_TEST")
                .alertLevel(result.isSuccess() ? "INFO" : "ERROR")
                .alertContent(content == null ? "This is a test alert from DB Sync Studio." : content)
                .channelType(channel.getChannelType())
                .channelId(channel.getId())
                .sendStatus(result.isSuccess() ? "SUCCESS" : "FAILED")
                .errorMessage(result.getMessage())
                .createdTime(Long.valueOf(System.currentTimeMillis()))
                .sentTime(result.isSuccess() ? Long.valueOf(System.currentTimeMillis()) : null)
                .build();
        if (alertHistoryRepository != null) {
            alertHistoryRepository.save(history);
        }
        return result;
    }

    public ValidationResult runValidation(ValidationRequest request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Validation request must not be null");
        }
        if (request.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        SyncTask task = loadTask(request.getTaskId().longValue());
        DatasourceConfig source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
        DatasourceConfig target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
        ValidationRequest effective = ValidationRequest.builder()
                .taskId(task.getId())
                .runId(trimToNull(request.getRunId()))
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceSchemaName(task.getSourceSchemaName())
                .sourceTableName(task.getSourceTableName())
                .targetSchemaName(task.getTargetSchemaName())
                .targetTableName(task.getTargetTableName())
                .validationMode(request.getValidationMode())
                .sampleMode(request.getSampleMode())
                .sampleCount(request.getSampleCount())
                .whereClause(request.getWhereClause())
                .incrementalCondition(request.getIncrementalCondition())
                .hashAlgorithm(request.getHashAlgorithm())
                .hashColumns(request.getHashColumns())
                .build();
        ValidationResult result = dataValidationEngine.validate(effective);
        validationRepository.saveRun(result.getRun());
        for (ValidationDifference difference : result.getDifferences()) {
            validationRepository.saveDifference(difference);
        }
        if (result.getRun() != null && result.getRun().getInconsistentCount() != null
                && result.getRun().getInconsistentCount().longValue() > 0L) {
            triggerAlert("VALIDATION_INCONSISTENT", task.getId(), result.getRun().getRunId(), task.getSourceTableName(),
                    "WARNING", "Validation found inconsistent rows", null, null);
        }
        return result;
    }

    public void notifyValidationInconsistent(ValidationRun run, String tableName) {
        if (run == null) {
            return;
        }
        triggerAlert("VALIDATION_INCONSISTENT", run.getTaskId(), run.getRunId(), tableName,
                "WARNING", "Validation found inconsistent rows", null, null);
    }

    public void handleScheduledSkip(long taskId, String message) {
        try {
            SyncTask task = loadTask(taskId);
            triggerAlert("SCHEDULE_SKIPPED", task.getId(), null, task.getSourceTableName(),
                    "WARNING", message == null ? "Scheduled execution skipped" : message, null, null);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to handle scheduled skip alert", ex);
        }
    }

    public List<ValidationRun> listValidationRuns(Long taskId, int limit) throws SQLException {
        if (taskId == null) {
            return validationRepository.findRecentRuns(limit);
        }
        return validationRepository.findRecentRunsByTaskId(taskId.longValue(), limit);
    }

    public List<ValidationDifference> listValidationDifferences(long validationRunId) {
        return validationRepository.findDifferencesByRunId(validationRunId);
    }

    public RepairResult runRepair(RepairRequest request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Repair request must not be null");
        }
        if (request.getValidationRunId() == null) {
            throw new IllegalArgumentException("Validation run id must not be null");
        }
        ValidationRun validationRun = validationRepository.findRunById(request.getValidationRunId().longValue()).orElse(null);
        if (validationRun == null) {
            throw new SQLException("Validation run not found: " + request.getValidationRunId());
        }
        SyncTask task = loadTask(validationRun.getTaskId().longValue());
        DatasourceConfig source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
        DatasourceConfig target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
        List<ValidationDifference> allDifferences = validationRepository.findDifferencesByRunId(validationRun.getId().longValue());
        List<ValidationDifference> selectedDifferences = filterDifferences(allDifferences, request.getValidationDifferenceIds());
        RepairRequest effective = RepairRequest.builder()
                .taskId(task.getId())
                .validationRunId(validationRun.getId())
                .runId(trimToNull(request.getRunId()))
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceSchemaName(task.getSourceSchemaName())
                .sourceTableName(task.getSourceTableName())
                .targetSchemaName(task.getTargetSchemaName())
                .targetTableName(task.getTargetTableName())
                .primaryKeyColumns(resolvePrimaryKeyColumns(source, task.getSourceSchemaName(), task.getSourceTableName()))
                .repairType(request.getRepairType())
                .validationDifferenceIds(request.getValidationDifferenceIds())
                .execute(request.isExecute())
                .confirmDelete(request.isConfirmDelete())
                .build();
        RepairResult result = dataRepairEngine.repair(effective, selectedDifferences);
        long repairRunId = repairRepository.saveRun(result.getRun());
        List<RepairDetail> persistedDetails = new ArrayList<RepairDetail>();
        for (RepairDetail detail : result.getDetails()) {
            detail.setRepairRunId(Long.valueOf(repairRunId));
            repairRepository.saveDetail(detail);
            persistedDetails.add(detail);
        }
        result.setDetails(persistedDetails);
        if (result.getRun() != null && result.getRun().getStatus() != null
                && !"SUCCESS".equalsIgnoreCase(result.getRun().getStatus())) {
            triggerAlert("REPAIR_FAILED", task.getId(), result.getRun().getRunId(), task.getSourceTableName(),
                    "ERROR", "Repair failed", null, null);
        }
        return result;
    }

    public void notifyRepairFailed(RepairRun run, String tableName) {
        if (run == null) {
            return;
        }
        triggerAlert("REPAIR_FAILED", run.getTaskId(), run.getRunId(), tableName,
                "ERROR", "Repair failed", null, null);
    }

    public List<RepairRun> listRepairRuns(Long validationRunId, int limit) {
        if (validationRunId == null) {
            return repairRepository.findRecentRuns(limit);
        }
        return repairRepository.findRecentRunsByValidationRunId(validationRunId.longValue(), limit);
    }

    public List<RepairDetail> listRepairDetails(long repairRunId) {
        return repairRepository.findDetailsByRunId(repairRunId);
    }

    private List<ValidationDifference> filterDifferences(List<ValidationDifference> differences, List<Long> selectedIds) {
        if (differences == null || differences.isEmpty()) {
            return new ArrayList<ValidationDifference>();
        }
        if (selectedIds == null || selectedIds.isEmpty()) {
            return new ArrayList<ValidationDifference>(differences);
        }
        List<ValidationDifference> result = new ArrayList<ValidationDifference>();
        for (ValidationDifference difference : differences) {
            if (difference != null && difference.getId() != null && selectedIds.contains(difference.getId())) {
                result.add(difference);
            }
        }
        return result;
    }

    private List<String> resolvePrimaryKeyColumns(DatasourceConfig datasource, String schemaName, String tableName) throws SQLException {
        List<SchemaMetadata> schemas = metadataScanner.scan(datasource);
        TableMetadata tableMetadata = findTableMetadata(schemas, schemaName, tableName);
        if (tableMetadata == null) {
            return new ArrayList<String>();
        }
        List<String> columns = new ArrayList<String>();
        if (tableMetadata.getColumns() != null) {
            for (ColumnMetadata column : tableMetadata.getColumns()) {
                if (column != null && column.isPrimaryKey() && column.getName() != null) {
                    columns.add(column.getName());
                }
            }
        }
        return columns;
    }

    private void executeTask(long taskId, TaskExecutionState state) {
        SyncTask task = null;
        boolean preserveBatchState = false;
        try {
            task = loadTask(taskId);
            DatasourceConfig source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
            DatasourceConfig target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
            task.setTaskStatus(SyncTaskStatus.RUNNING);
            task.setStartedAt(task.getStartedAt() == null ? Long.valueOf(System.currentTimeMillis()) : task.getStartedAt());
            task.setEndedAt(null);
            task.setProgressMessage("Task running");
            syncTaskRepository.save(task);

            SyncTaskProgressListener listener = state.createListener(task);
            TaskBatchRunRequest batchRunRequest = state.getBatchRunRequest();
            if (batchRunRequest != null) {
                runBatchTask(task, source, target, listener, state, batchRunRequest);
            } else if (task.getSyncMode() == SyncMode.INCREMENTAL) {
                IncrementalSyncResult result = incrementalSync(task, source, target, listener);
                appendLog(taskId, "INFO", "Incremental sync inserted " + result.getInsertedRowCount() + " rows");
            } else {
                FullSyncResult result = fullSync(task, source, target, listener);
                appendLog(taskId, "INFO", "Full sync inserted " + result.getInsertedRowCount() + " rows");
            }

            task = loadTask(taskId);
            if (batchRunRequest != null) {
                if (task.getTaskStatus() == SyncTaskStatus.PARTIAL_SUCCESS) {
                    updateScheduleResult(task, "PARTIAL_SUCCESS", "Task partially successful");
                    syncTaskRepository.save(task);
                } else if (task.getTaskStatus() == SyncTaskStatus.SUCCESS) {
                    updateScheduleResult(task, "SUCCESS", "Task completed successfully");
                    syncTaskRepository.save(task);
                } else if (task.getTaskStatus() == SyncTaskStatus.RUNNING || task.getTaskStatus() == SyncTaskStatus.PENDING) {
                    task.setTaskStatus(SyncTaskStatus.SUCCESS);
                    task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
                    task.setDurationMillis(task.getStartedAt() == null ? null : Long.valueOf(task.getEndedAt().longValue() - task.getStartedAt().longValue()));
                    task.setProgressMessage("Task completed");
                    updateScheduleResult(task, "SUCCESS", "Task completed successfully");
                    syncTaskRepository.save(task);
                }
            } else {
                task.setTaskStatus(SyncTaskStatus.SUCCESS);
                task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
                task.setDurationMillis(Long.valueOf(task.getEndedAt().longValue() - task.getStartedAt().longValue()));
                task.setProgressMessage("Task completed");
                updateScheduleResult(task, "SUCCESS", "Task completed successfully");
                syncTaskRepository.save(task);
            }
            captureTaskRunMetric(task, state.getBatchRunId(), null);
            appendLog(taskId, "INFO", "Task finished successfully: " + task.getTaskName());
        } catch (SyncTaskPausedException ex) {
            try {
                preserveBatchState = state.getBatchRunRequest() != null;
                task = loadTask(taskId);
                task.setTaskStatus(SyncTaskStatus.PAUSED);
                task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
                task.setDurationMillis(Long.valueOf(task.getEndedAt().longValue() - task.getStartedAt().longValue()));
                task.setProgressMessage("Task paused");
                updateScheduleResult(task, "PAUSED", "Task paused");
                syncTaskRepository.save(task);
                captureTaskRunMetric(task, state.getBatchRunId(), ex.getMessage());
                appendLog(taskId, "INFO", "Task paused: " + task.getTaskName());
            } catch (Exception ignored) {
                // best effort
            }
        } catch (SyncTaskStoppedException ex) {
            try {
                preserveBatchState = state.getBatchRunRequest() != null;
                task = loadTask(taskId);
                task.setTaskStatus(SyncTaskStatus.STOPPED);
                task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
                task.setDurationMillis(Long.valueOf(task.getEndedAt().longValue() - task.getStartedAt().longValue()));
                task.setProgressMessage("Task stopped");
                updateScheduleResult(task, "STOPPED", "Task stopped");
                syncTaskRepository.save(task);
                captureTaskRunMetric(task, state.getBatchRunId(), ex.getMessage());
                appendLog(taskId, "INFO", "Task stopped: " + task.getTaskName());
            } catch (Exception ignored) {
                // best effort
            }
        } catch (SQLException ex) {
            try {
                task = loadTask(taskId);
                task.setTaskStatus(SyncTaskStatus.FAILED);
                task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
                task.setDurationMillis(task.getStartedAt() == null ? null : Long.valueOf(task.getEndedAt().longValue() - task.getStartedAt().longValue()));
                task.setProgressMessage("Task failed");
                updateScheduleResult(task, "FAILED", ex.getMessage());
                syncTaskRepository.save(task);
                captureTaskRunMetric(task, state.getBatchRunId(), ex.getMessage());
                triggerAlert("TASK_EXECUTION_FAILED", task.getId(), state.getBatchRunId(), task.getSourceTableName(),
                        "ERROR", ex.getMessage(), null, null);
                appendLog(taskId, "ERROR", "Task failed: " + ex.getMessage());
            } catch (Exception ignored) {
                // best effort
            }
        } catch (RuntimeException ex) {
            try {
                task = loadTask(taskId);
                task.setTaskStatus(SyncTaskStatus.FAILED);
                task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
                task.setDurationMillis(task.getStartedAt() == null ? null : Long.valueOf(task.getEndedAt().longValue() - task.getStartedAt().longValue()));
                task.setProgressMessage("Task failed");
                updateScheduleResult(task, "FAILED", ex.getMessage());
                syncTaskRepository.save(task);
                captureTaskRunMetric(task, state.getBatchRunId(), ex.getMessage());
                triggerAlert("TASK_EXECUTION_FAILED", task.getId(), state.getBatchRunId(), task.getSourceTableName(),
                        "ERROR", ex.getMessage(), null, null);
                appendLog(taskId, "ERROR", "Task failed: " + ex.getMessage());
            } catch (Exception ignored) {
                // best effort
            }
        } finally {
            if (!preserveBatchState) {
                state.clearBatchState();
                taskExecutionStates.remove(Long.valueOf(taskId));
            }
        }
    }

    private FullSyncResult fullSync(SyncTask task, DatasourceConfig source, DatasourceConfig target,
                                    SyncTaskProgressListener listener) throws SQLException {
        return fullSync(task, source, target, task.getSourceSchemaName(), task.getSourceTableName(),
                task.getTargetSchemaName(), task.getTargetTableName(),
                "task-" + task.getId() + "-full", resolveFullCheckpointValue("task-" + task.getId() + "-full"), listener, 500);
    }

    private FullSyncResult fullSync(SyncTask task, DatasourceConfig source, DatasourceConfig target,
                                    String sourceSchemaName, String sourceTableName,
                                    String targetSchemaName, String targetTableName,
                                    String checkpointKey, String checkpointValue,
                                    SyncTaskProgressListener listener, int batchSize) throws SQLException {
        FullSyncRequest request = FullSyncRequest.builder()
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceSchemaName(sourceSchemaName)
                .targetSchemaName(targetSchemaName)
                .sourceTableName(sourceTableName)
                .targetTableName(targetTableName)
                .checkpointKey(checkpointKey)
                .checkpointValue(checkpointValue)
                .pageSize(500)
                .batchSize(batchSize <= 0 ? 500 : batchSize)
                .replaceTargetData(true)
                .build();
        return fullSyncEngine.sync(request, listener);
    }

    private IncrementalSyncResult incrementalSync(SyncTask task, DatasourceConfig source, DatasourceConfig target,
                                                   SyncTaskProgressListener listener) throws SQLException {
        return incrementalSync(task, source, target, task.getSourceSchemaName(), task.getSourceTableName(),
                task.getTargetSchemaName(), task.getTargetTableName(), "task-" + task.getId(),
                resolveCheckpointValue(task.getId()), listener, task.getIncrementalMode(),
                resolveIncrementalColumnName(task, task.getIncrementalMode() == null ? IncrementalSyncMode.TIMESTAMP : task.getIncrementalMode()),
                trimToNull(task.getIncrementalTieBreakerColumnName()),
                trimToNull(task.getIncrementalCompositeColumnName()), 500);
    }

    private IncrementalSyncResult incrementalSync(SyncTask task, DatasourceConfig source, DatasourceConfig target,
                                                   String sourceSchemaName, String sourceTableName,
                                                   String targetSchemaName, String targetTableName,
                                                   String checkpointKey, String checkpointValue,
                                                   SyncTaskProgressListener listener,
                                                   IncrementalSyncMode incrementalMode,
                                                   String incrementalColumnName,
                                                   String incrementalTieBreakerColumnName,
                                                   String incrementalCompositeColumnName,
                                                   int batchSize) throws SQLException {
        IncrementalSyncRequest request = IncrementalSyncRequest.builder()
                .taskId(task.getId())
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceSchemaName(sourceSchemaName)
                .targetSchemaName(targetSchemaName)
                .sourceTableName(sourceTableName)
                .targetTableName(targetTableName)
                .incrementalMode(incrementalMode)
                .watermarkColumnName(incrementalMode == IncrementalSyncMode.TIMESTAMP ? incrementalColumnName : null)
                .autoIncrementColumnName(incrementalMode == IncrementalSyncMode.AUTO_INCREMENT_ID ? incrementalColumnName : null)
                .compositeWatermarkColumnName(incrementalMode == IncrementalSyncMode.COMPOSITE ? incrementalCompositeColumnName : null)
                .compositeTieBreakerColumnName(incrementalTieBreakerColumnName)
                .checkpointKey(checkpointKey)
                .checkpointValue(checkpointValue)
                .pageSize(500)
                .batchSize(batchSize <= 0 ? 500 : batchSize)
                .replaceTargetData(false)
                .build();
        return incrementalSyncEngine.sync(request, listener);
    }

    private void runBatchTask(SyncTask task, DatasourceConfig source, DatasourceConfig target,
                              SyncTaskProgressListener listener, TaskExecutionState state,
                              TaskBatchRunRequest batchRunRequest) throws SQLException {
        List<TaskBatchTableRequest> tables = batchRunRequest.getTables();
        String batchRunId = state.getBatchRunId();
        SyncRun syncRun = createSyncRun(task, batchRunId, tables.size());
        long syncRunId = syncRun == null || syncRun.getId() == null ? -1L : syncRun.getId().longValue();
        int concurrency = Math.max(1, batchRunRequest.getMaxConcurrency() == null ? 3 : batchRunRequest.getMaxConcurrency().intValue());
        ExecutorService batchExecutor = Executors.newFixedThreadPool(concurrency);
        ExecutorCompletionService<BatchTableExecutionResult> completionService = new ExecutorCompletionService<BatchTableExecutionResult>(batchExecutor);
        List<Future<BatchTableExecutionResult>> futures = new ArrayList<Future<BatchTableExecutionResult>>();
        final AtomicBoolean pauseOrStopRequested = new AtomicBoolean(false);
        try {
            for (int i = 0; i < tables.size(); i++) {
                final int tableIndex = i;
                final TaskBatchTableRequest tableRequest = tables.get(i);
                futures.add(completionService.submit(new Callable<BatchTableExecutionResult>() {
                    @Override
                    public BatchTableExecutionResult call() throws Exception {
                        if (listener.isStopRequested()) {
                            throw new SyncTaskStoppedException("Batch sync stopped");
                        }
                        if (listener.isPauseRequested()) {
                            throw new SyncTaskPausedException("Batch sync paused");
                        }
                        return executeBatchTable(task, source, target, listener, syncRunId, batchRunId, tableRequest, tableIndex);
                    }
                }));
            }

            int finishedCount = 0;
            int successCount = 0;
            int failedCount = 0;
            int pausedCount = 0;
            int stoppedCount = 0;
            while (finishedCount < tables.size()) {
                if (listener.isStopRequested() || listener.isPauseRequested()) {
                    pauseOrStopRequested.set(true);
                }
                Future<BatchTableExecutionResult> future = completionService.take();
                finishedCount++;
                try {
                    BatchTableExecutionResult result = future.get();
                    if (result == null) {
                        failedCount++;
                        continue;
                    }
                    if (result.tableRun != null) {
                        state.setBatchTableIndex(result.tableIndex + 1);
                    }
                    applySyncRunProgress(syncRun,
                            result.sourceRowCount,
                            result.syncedRowCount,
                            result.successRowCount,
                            result.failedRowCount);
                    if ("SUCCESS".equals(result.tableStatus)) {
                        successCount++;
                    } else if ("PAUSED".equals(result.tableStatus)) {
                        pausedCount++;
                    } else if ("STOPPED".equals(result.tableStatus)) {
                        stoppedCount++;
                    } else {
                        failedCount++;
                    }
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof SyncTaskPausedException) {
                        pausedCount++;
                    } else if (cause instanceof SyncTaskStoppedException) {
                        stoppedCount++;
                    } else {
                        failedCount++;
                    }
                }
            }

            String finalStatus = resolveBatchRunStatus(successCount, failedCount, pausedCount, stoppedCount);
            markSyncRunStatus(syncRun, finalStatus, "Batch sync completed");
            updateBatchTaskStatus(task, finalStatus, syncRun);
            if ("STOPPED".equals(finalStatus)) {
                throw new SyncTaskStoppedException("Batch sync stopped");
            }
            if ("PAUSED".equals(finalStatus)) {
                throw new SyncTaskPausedException("Batch sync paused");
            }
            if ("FAILED".equals(finalStatus)) {
                throw new SQLException("Batch sync failed");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SQLException("Batch sync interrupted", ex);
        } finally {
            for (Future<BatchTableExecutionResult> future : futures) {
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
            }
            batchExecutor.shutdownNow();
            if (pauseOrStopRequested.get()) {
                state.clearBatchState();
            }
        }
    }

    private BatchTableExecutionResult executeBatchTable(SyncTask task, DatasourceConfig source, DatasourceConfig target,
                                                        SyncTaskProgressListener listener, long syncRunId, String batchRunId,
                                                        TaskBatchTableRequest tableRequest, int tableIndex) throws SQLException {
        String sourceSchemaName = trimToNull(tableRequest.getSourceSchemaName());
        String targetSchemaName = trimToNull(tableRequest.getTargetSchemaName());
        String sourceTableName = tableRequest.getSourceTableName();
        String targetTableName = tableRequest.getTargetTableName();
        String tableRunCheckpointKey = batchRunId + "-" + (tableIndex + 1);
        SyncTaskTable taskTable = resolveTaskTable(task.getId().longValue(), tableRequest, tableIndex);
        SyncTableRun tableRun = createSyncTableRun(syncRunId, task, taskTable, tableRequest, tableIndex, batchRunId);
        SyncMode effectiveSyncMode = resolveEffectiveSyncMode(task, taskTable, tableRequest);
        IncrementalSyncMode effectiveIncrementalMode = resolveEffectiveIncrementalMode(task, taskTable, tableRequest);
        String effectiveIncrementalColumnName = resolveEffectiveIncrementalColumnName(task, taskTable, tableRequest, effectiveIncrementalMode);
        String effectiveIncrementalTieBreakerColumnName = resolveEffectiveIncrementalTieBreakerColumnName(task, taskTable, tableRequest);
        String effectiveIncrementalCompositeColumnName = resolveEffectiveIncrementalCompositeColumnName(task, taskTable, tableRequest);
        int effectiveBatchSize = resolveEffectiveBatchSize(taskTable, tableRequest);
        appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                sourceTableName, "INFO", "Batch sync started for " + sourceTableName + " -> " + targetTableName);
        try {
            if (SyncMode.INCREMENTAL == effectiveSyncMode) {
                Long batchTaskId = Long.valueOf(task.getId().longValue() * 1000L + tableIndex + 1L);
                IncrementalSyncResult result = incrementalSync(task, source, target,
                        sourceSchemaName, sourceTableName, targetSchemaName, targetTableName,
                        tableRunCheckpointKey, resolveCheckpointValue(batchTaskId), listener,
                        effectiveIncrementalMode, effectiveIncrementalColumnName,
                        effectiveIncrementalTieBreakerColumnName, effectiveIncrementalCompositeColumnName,
                        effectiveBatchSize);
                updateSyncTableRunSuccess(tableRun, result, tableRunCheckpointKey);
                appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                        sourceTableName, "INFO", "Batch incremental sync inserted " + result.getInsertedRowCount()
                                + " rows for " + sourceTableName);
                markSyncTableRunStatus(tableRun, "SUCCESS", null, null);
                return BatchTableExecutionResult.success(tableIndex, tableRun, result.getSourceRowCount(),
                        result.getInsertedRowCount(), result.getInsertedRowCount(), 0L);
            }
            FullSyncResult result = fullSync(task, source, target,
                    sourceSchemaName, sourceTableName, targetSchemaName, targetTableName,
                    tableRunCheckpointKey, resolveFullCheckpointValue(tableRunCheckpointKey), listener, effectiveBatchSize);
            updateSyncTableRunSuccess(tableRun, result, tableRunCheckpointKey);
            appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                    sourceTableName, "INFO", "Batch full sync inserted " + result.getInsertedRowCount()
                            + " rows for " + sourceTableName);
            markSyncTableRunStatus(tableRun, "SUCCESS", null, null);
            return BatchTableExecutionResult.success(tableIndex, tableRun, result.getSourceRowCount(),
                    result.getInsertedRowCount(), result.getInsertedRowCount(), 0L);
        } catch (SyncTaskPausedException ex) {
            markSyncTableRunStatus(tableRun, "PAUSED", ex.getMessage(), null);
            appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                    sourceTableName, "INFO", "Batch table paused: " + sourceTableName);
            throw ex;
        } catch (SyncTaskStoppedException ex) {
            markSyncTableRunStatus(tableRun, "STOPPED", ex.getMessage(), null);
            appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                    sourceTableName, "INFO", "Batch table stopped: " + sourceTableName);
            throw ex;
        } catch (SQLException ex) {
            markSyncTableRunStatus(tableRun, "FAILED", ex.getMessage(), null);
            triggerAlert("TABLE_SYNC_FAILED", task.getId(), batchRunId, sourceTableName, "ERROR",
                    ex.getMessage(), taskTable == null ? null : taskTable.getId(), null);
            appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                    sourceTableName, "ERROR", "Batch table failed: " + ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markSyncTableRunStatus(tableRun, "FAILED", ex.getMessage(), null);
            triggerAlert("TABLE_SYNC_FAILED", task.getId(), batchRunId, sourceTableName, "ERROR",
                    ex.getMessage(), taskTable == null ? null : taskTable.getId(), null);
            appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                    sourceTableName, "ERROR", "Batch table failed: " + ex.getMessage());
            throw ex;
        }
    }

    private SyncTaskTable resolveTaskTable(long taskId, TaskBatchTableRequest tableRequest, int tableOrder) throws SQLException {
        if (syncTaskTableRepository == null) {
            return null;
        }
        List<SyncTaskTable> taskTables = syncTaskTableRepository.findByTaskId(taskId);
        for (SyncTaskTable taskTable : taskTables) {
            if (taskTable == null) {
                continue;
            }
            if (tableRequest.getSourceTableName() != null && taskTable.getSourceTableName() != null
                    && !taskTable.getSourceTableName().equalsIgnoreCase(tableRequest.getSourceTableName())) {
                continue;
            }
            if (tableRequest.getTargetTableName() != null && taskTable.getTargetTableName() != null
                    && !taskTable.getTargetTableName().equalsIgnoreCase(tableRequest.getTargetTableName())) {
                continue;
            }
            if (taskTable.getTableOrder() != null && taskTable.getTableOrder().intValue() == tableOrder + 1) {
                return taskTable;
            }
        }
        if (!taskTables.isEmpty()) {
            return taskTables.get(Math.min(tableOrder, taskTables.size() - 1));
        }
        return null;
    }

    private SyncRun createSyncRun(SyncTask task, String runId, int totalTableCount) throws SQLException {
        if (syncRunRepository == null) {
            return null;
        }
        SyncRun run = new SyncRun();
        run.setTaskId(task.getId());
        run.setRunId(runId);
        run.setSyncMode(task.getSyncMode() == null ? SyncMode.MANUAL.name() : task.getSyncMode().name());
        run.setRunStatus("RUNNING");
        run.setTotalTableCount(Integer.valueOf(totalTableCount));
        run.setCompletedTableCount(Integer.valueOf(0));
        run.setTotalRowCount(Long.valueOf(0L));
        run.setSyncedRowCount(Long.valueOf(0L));
        run.setSuccessRowCount(Long.valueOf(0L));
        run.setFailedRowCount(Long.valueOf(0L));
        run.setStartedAt(Long.valueOf(System.currentTimeMillis()));
        run.setProgressMessage("Batch sync running");
        syncRunRepository.save(run);
        appendRunLog(run.getId(), null, task.getId().longValue(), runId, null, "INFO", "Batch run started");
        return run;
    }

    private SyncTableRun createSyncTableRun(long syncRunId, SyncTask task, SyncTaskTable taskTable,
                                            TaskBatchTableRequest tableRequest, int tableOrder, String runId) throws SQLException {
        if (syncTableRunRepository == null) {
            return null;
        }
        SyncTableRun tableRun = SyncTableRun.builder()
                .syncRunId(Long.valueOf(syncRunId))
                .taskId(task.getId())
                .runId(runId)
                .taskTableId(taskTable == null ? null : taskTable.getId())
                .sourceSchemaName(trimToNull(tableRequest.getSourceSchemaName()))
                .sourceTableName(tableRequest.getSourceTableName())
                .targetSchemaName(trimToNull(tableRequest.getTargetSchemaName()))
                .targetTableName(tableRequest.getTargetTableName())
                .tableOrder(Integer.valueOf(tableOrder + 1))
                .tableStatus("RUNNING")
                .startedAt(Long.valueOf(System.currentTimeMillis()))
                .progressMessage("Table running")
                .build();
        syncTableRunRepository.save(tableRun);
        return tableRun;
    }

    private void updateSyncTableRunSuccess(SyncTableRun tableRun, FullSyncResult result, String checkpointValue) throws SQLException {
        if (tableRun == null || syncTableRunRepository == null) {
            return;
        }
        tableRun.setTableStatus("SUCCESS");
        tableRun.setTotalRowCount(Long.valueOf(result.getSourceRowCount()));
        tableRun.setSyncedRowCount(Long.valueOf(result.getInsertedRowCount()));
        tableRun.setSuccessRowCount(Long.valueOf(result.getInsertedRowCount()));
        tableRun.setFailedRowCount(Long.valueOf(0L));
        tableRun.setSpeedRowsPerSecond(result.getDurationMillis() > 0L
                ? Double.valueOf((result.getInsertedRowCount() * 1000.0d) / result.getDurationMillis())
                : null);
        tableRun.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        tableRun.setDurationMillis(result.getDurationMillis());
        tableRun.setProgressMessage(result.getMessage());
        tableRun.setCheckpointValue(checkpointValue);
        syncTableRunRepository.save(tableRun);
        captureTableRunMetric(tableRun, Integer.valueOf(1), Integer.valueOf(0), checkpointValue, null);
    }

    private void updateSyncTableRunSuccess(SyncTableRun tableRun, IncrementalSyncResult result, String checkpointValue) throws SQLException {
        if (tableRun == null || syncTableRunRepository == null) {
            return;
        }
        tableRun.setTableStatus("SUCCESS");
        tableRun.setTotalRowCount(Long.valueOf(result.getSourceRowCount()));
        tableRun.setSyncedRowCount(Long.valueOf(result.getInsertedRowCount()));
        tableRun.setSuccessRowCount(Long.valueOf(result.getInsertedRowCount()));
        tableRun.setFailedRowCount(Long.valueOf(0L));
        tableRun.setSpeedRowsPerSecond(result.getDurationMillis() > 0L
                ? Double.valueOf((result.getInsertedRowCount() * 1000.0d) / result.getDurationMillis())
                : null);
        tableRun.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        tableRun.setDurationMillis(result.getDurationMillis());
        tableRun.setProgressMessage(result.getMessage());
        tableRun.setCheckpointValue(checkpointValue);
        syncTableRunRepository.save(tableRun);
        captureTableRunMetric(tableRun, Integer.valueOf(1), Integer.valueOf(0), checkpointValue, null);
    }

    private void markSyncTableRunStatus(SyncTableRun tableRun, String status, String errorMessage, String progressMessage) throws SQLException {
        if (tableRun == null || syncTableRunRepository == null) {
            return;
        }
        tableRun.setTableStatus(status);
        tableRun.setErrorMessage(errorMessage);
        if (progressMessage != null) {
            tableRun.setProgressMessage(progressMessage);
        }
        if (tableRun.getEndedAt() == null) {
            tableRun.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        }
        if (tableRun.getStartedAt() != null && tableRun.getEndedAt() != null) {
            tableRun.setDurationMillis(Long.valueOf(tableRun.getEndedAt().longValue() - tableRun.getStartedAt().longValue()));
        }
        syncTableRunRepository.save(tableRun);
        captureTableRunMetric(tableRun, Integer.valueOf(0), Integer.valueOf(0), tableRun.getCheckpointValue(), errorMessage);
    }

    private void applySyncRunProgress(SyncRun syncRun, long sourceRowCount, long syncedRowCount, long successRowCount, long failedRowCount) throws SQLException {
        if (syncRun == null || syncRunRepository == null) {
            return;
        }
        syncRun.setTotalRowCount(Long.valueOf((syncRun.getTotalRowCount() == null ? 0L : syncRun.getTotalRowCount().longValue()) + sourceRowCount));
        syncRun.setSyncedRowCount(Long.valueOf((syncRun.getSyncedRowCount() == null ? 0L : syncRun.getSyncedRowCount().longValue()) + syncedRowCount));
        syncRun.setSuccessRowCount(Long.valueOf((syncRun.getSuccessRowCount() == null ? 0L : syncRun.getSuccessRowCount().longValue()) + successRowCount));
        syncRun.setFailedRowCount(Long.valueOf((syncRun.getFailedRowCount() == null ? 0L : syncRun.getFailedRowCount().longValue()) + failedRowCount));
        syncRun.setCompletedTableCount(Integer.valueOf((syncRun.getCompletedTableCount() == null ? 0 : syncRun.getCompletedTableCount().intValue()) + 1));
        syncRun.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        syncRunRepository.save(syncRun);
    }

    private void markSyncRunStatus(SyncRun syncRun, String status, String message) throws SQLException {
        if (syncRun == null || syncRunRepository == null) {
            return;
        }
        syncRun.setRunStatus(status);
        syncRun.setProgressMessage(message);
        syncRun.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        if (syncRun.getStartedAt() != null && syncRun.getEndedAt() != null) {
            syncRun.setDurationMillis(Long.valueOf(syncRun.getEndedAt().longValue() - syncRun.getStartedAt().longValue()));
        }
        syncRun.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        syncRunRepository.save(syncRun);
        appendRunLog(syncRun.getId(), null, syncRun.getTaskId().longValue(), syncRun.getRunId(), null, "INFO", "Batch run finished with status " + status);
    }

    private void appendRunLog(Long syncRunId, Long syncTableRunId, long taskId, String runId, String tableName, String logLevel, String message) throws SQLException {
        if (syncRunLogRepository == null) {
            return;
        }
        SyncRunLogEntry entry = new SyncRunLogEntry();
        entry.setTaskId(Long.valueOf(taskId));
        entry.setSyncRunId(syncRunId);
        entry.setSyncTableRunId(syncTableRunId);
        entry.setRunId(runId);
        entry.setTableName(tableName);
        entry.setLogLevel(logLevel);
        entry.setLogMessage(message);
        entry.setCreatedAt(Long.valueOf(System.currentTimeMillis()));
        syncRunLogRepository.append(entry);
    }

    private String resolveBatchRunStatus(int successCount, int failedCount, int pausedCount, int stoppedCount) {
        if (stoppedCount > 0) {
            return "STOPPED";
        }
        if (pausedCount > 0 && successCount == 0 && failedCount == 0) {
            return "PAUSED";
        }
        if (failedCount > 0 && successCount > 0) {
            return "PARTIAL_SUCCESS";
        }
        if (failedCount > 0) {
            return "FAILED";
        }
        return "SUCCESS";
    }

    private void updateBatchTaskStatus(SyncTask task, String finalStatus, SyncRun syncRun) throws SQLException {
        if (task == null) {
            return;
        }
        SyncTask currentTask = loadTask(task.getId().longValue());
        if ("PARTIAL_SUCCESS".equals(finalStatus)) {
            currentTask.setTaskStatus(SyncTaskStatus.PARTIAL_SUCCESS);
            currentTask.setProgressMessage("Task partially successful");
        } else if ("SUCCESS".equals(finalStatus)) {
            currentTask.setTaskStatus(SyncTaskStatus.SUCCESS);
            currentTask.setProgressMessage("Task completed");
        } else if ("PAUSED".equals(finalStatus)) {
            currentTask.setTaskStatus(SyncTaskStatus.PAUSED);
            currentTask.setProgressMessage("Task paused");
        } else if ("STOPPED".equals(finalStatus)) {
            currentTask.setTaskStatus(SyncTaskStatus.STOPPED);
            currentTask.setProgressMessage("Task stopped");
        } else {
            currentTask.setTaskStatus(SyncTaskStatus.FAILED);
            currentTask.setProgressMessage("Task failed");
        }
        currentTask.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        if (currentTask.getStartedAt() != null && currentTask.getEndedAt() != null) {
            currentTask.setDurationMillis(Long.valueOf(currentTask.getEndedAt().longValue() - currentTask.getStartedAt().longValue()));
        }
        if (syncRun != null) {
            currentTask.setTotalRowCount(syncRun.getTotalRowCount());
            currentTask.setSyncedRowCount(syncRun.getSyncedRowCount());
            currentTask.setSuccessRowCount(syncRun.getSuccessRowCount());
            currentTask.setFailedRowCount(syncRun.getFailedRowCount());
            currentTask.setSpeedRowsPerSecond(syncRun.getSpeedRowsPerSecond());
        }
        syncTaskRepository.save(currentTask);
    }

    private SyncMode resolveEffectiveSyncMode(SyncTask task, SyncTaskTable taskTable, TaskBatchTableRequest tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getSyncMode());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getSyncMode());
        }
        if (value == null) {
            return task == null || task.getSyncMode() == null ? SyncMode.FULL : task.getSyncMode();
        }
        return SyncMode.valueOf(value);
    }

    private IncrementalSyncMode resolveEffectiveIncrementalMode(SyncTask task, SyncTaskTable taskTable, TaskBatchTableRequest tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalMode());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalMode());
        }
        if (value == null) {
            return task == null || task.getIncrementalMode() == null ? IncrementalSyncMode.TIMESTAMP : task.getIncrementalMode();
        }
        return IncrementalSyncMode.valueOf(value);
    }

    private String resolveEffectiveIncrementalColumnName(SyncTask task, SyncTaskTable taskTable, TaskBatchTableRequest tableRequest,
                                                         IncrementalSyncMode incrementalMode) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalColumnName());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalColumnName());
        }
        if (value == null && task != null) {
            value = trimToNull(task.getIncrementalColumnName());
        }
        if (value == null && incrementalMode == IncrementalSyncMode.AUTO_INCREMENT_ID) {
            return "id";
        }
        return value == null ? "updated_at" : value;
    }

    private String resolveEffectiveIncrementalTieBreakerColumnName(SyncTask task, SyncTaskTable taskTable, TaskBatchTableRequest tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalTieBreakerColumnName());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalTieBreakerColumnName());
        }
        if (value == null && task != null) {
            value = trimToNull(task.getIncrementalTieBreakerColumnName());
        }
        return value;
    }

    private String resolveEffectiveIncrementalCompositeColumnName(SyncTask task, SyncTaskTable taskTable, TaskBatchTableRequest tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalCompositeColumnName());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalCompositeColumnName());
        }
        if (value == null && task != null) {
            value = trimToNull(task.getIncrementalCompositeColumnName());
        }
        return value;
    }

    private int resolveEffectiveBatchSize(SyncTaskTable taskTable, TaskBatchTableRequest tableRequest) {
        Integer value = tableRequest == null ? null : tableRequest.getBatchSize();
        if (value == null || value.intValue() <= 0) {
            value = taskTable == null ? null : taskTable.getBatchSize();
        }
        if (value == null || value.intValue() <= 0) {
            return 500;
        }
        return value.intValue();
    }

    private static final class BatchTableExecutionResult {
        private final int tableIndex;
        private final SyncTableRun tableRun;
        private final String tableStatus;
        private final long sourceRowCount;
        private final long syncedRowCount;
        private final long successRowCount;
        private final long failedRowCount;

        private BatchTableExecutionResult(int tableIndex, SyncTableRun tableRun, String tableStatus,
                                          long sourceRowCount, long syncedRowCount, long successRowCount, long failedRowCount) {
            this.tableIndex = tableIndex;
            this.tableRun = tableRun;
            this.tableStatus = tableStatus;
            this.sourceRowCount = sourceRowCount;
            this.syncedRowCount = syncedRowCount;
            this.successRowCount = successRowCount;
            this.failedRowCount = failedRowCount;
        }

        private static BatchTableExecutionResult success(int tableIndex, SyncTableRun tableRun,
                                                         long sourceRowCount, long syncedRowCount,
                                                         long successRowCount, long failedRowCount) {
            return new BatchTableExecutionResult(tableIndex, tableRun, "SUCCESS",
                    sourceRowCount, syncedRowCount, successRowCount, failedRowCount);
        }
    }

    private List<SyncRunLogEntry> loadSyncRunLogCandidates(Long taskId, String runId, Long syncRunId, Long syncTableRunId, int limit) throws SQLException {
        if (syncRunLogRepository == null) {
            return new ArrayList<SyncRunLogEntry>();
        }
        if (syncTableRunId != null) {
            return syncRunLogRepository.findBySyncTableRunId(syncTableRunId.longValue());
        }
        if (syncRunId != null) {
            return syncRunLogRepository.findBySyncRunId(syncRunId.longValue());
        }
        if (runId != null && runId.trim().length() > 0) {
            return syncRunLogRepository.findByRunId(runId);
        }
        if (taskId != null) {
            return syncRunLogRepository.findByTaskId(taskId.longValue());
        }
        return syncRunLogRepository.findRecent(limit);
    }

    private boolean matchesSyncRunLogIdentifiers(SyncRunLogEntry entry, Long taskId, String runId, Long syncRunId, Long syncTableRunId) {
        if (entry == null) {
            return false;
        }
        if (taskId != null && (entry.getTaskId() == null || entry.getTaskId().longValue() != taskId.longValue())) {
            return false;
        }
        if (runId != null && (entry.getRunId() == null || !runId.equals(entry.getRunId()))) {
            return false;
        }
        if (syncRunId != null && (entry.getSyncRunId() == null || entry.getSyncRunId().longValue() != syncRunId.longValue())) {
            return false;
        }
        if (syncTableRunId != null && (entry.getSyncTableRunId() == null || entry.getSyncTableRunId().longValue() != syncTableRunId.longValue())) {
            return false;
        }
        return true;
    }

    private String resolveCheckpointValue(Long taskId) {
        if (taskId == null) {
            return null;
        }
        Optional<com.dbsyncstudio.model.sync.IncrementalSyncCheckpointEntry> checkpoint = incrementalCheckpointRepository.findByTaskId(taskId.longValue());
        if (!checkpoint.isPresent()) {
            return null;
        }
        return checkpoint.get().getCheckpointValue();
    }

    private String resolveFullCheckpointValue(String checkpointKey) {
        Optional<com.dbsyncstudio.model.sync.SyncCheckpoint> checkpoint = checkpointRepository.findByKey(checkpointKey);
        if (!checkpoint.isPresent()) {
            return null;
        }
        return checkpoint.get().getCheckpointValue();
    }

    private String resolveIncrementalColumnName(SyncTask task, IncrementalSyncMode incrementalMode) {
        String configured = trimToNull(task.getIncrementalColumnName());
        if (configured != null) {
            return configured;
        }
        if (incrementalMode == IncrementalSyncMode.AUTO_INCREMENT_ID) {
            return "id";
        }
        return "updated_at";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private SyncCheckpoint loadOrCreateCheckpoint(String checkpointKey, String defaultValue) {
        Optional<SyncCheckpoint> checkpoint = checkpointRepository.findByKey(checkpointKey);
        if (checkpoint.isPresent()) {
            return checkpoint.get();
        }
        SyncCheckpoint created = new SyncCheckpoint();
        created.setCheckpointKey(checkpointKey);
        created.setCheckpointValue(defaultValue);
        created.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        checkpointRepository.save(created);
        return created;
    }

    private void setNextScheduleRunAt(SyncTask task, long baseTime) {
        long nextRunAt = TaskScheduleCalculator.computeNextRunAt(task, baseTime);
        task.setScheduleNextRunAt(nextRunAt > 0L ? Long.valueOf(nextRunAt) : null);
    }

    private TaskExecutionState ensureTaskState(long taskId) {
        Long key = Long.valueOf(taskId);
        TaskExecutionState state = taskExecutionStates.get(key);
        if (state == null) {
            state = new TaskExecutionState(taskId);
            taskExecutionStates.put(key, state);
        }
        return state;
    }

    private int readDatabaseUserVersion() throws SQLException {
        if (connectionFactory == null) {
            return 0;
        }
        Connection connection = null;
        try {
            connection = connectionFactory.openConnection();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("PRAGMA user_version")) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private int readMigrationEntryCount() throws SQLException {
        if (connectionFactory == null) {
            return 0;
        }
        Connection connection = null;
        try {
            connection = connectionFactory.openConnection();
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT COUNT(1) FROM schema_migration_entry")) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private final class TaskExecutionState {

        private final long taskId;
        private volatile boolean pauseRequested;
        private volatile boolean stopRequested;
        private volatile TaskBatchRunRequest batchRunRequest;
        private volatile String batchRunId;
        private volatile int batchTableIndex;

        private TaskExecutionState(long taskId) {
            this.taskId = taskId;
        }

        private void pause() {
            this.pauseRequested = true;
        }

        private void stop() {
            this.stopRequested = true;
        }

        private void clearControlFlags() {
            this.pauseRequested = false;
            this.stopRequested = false;
        }

        private void setBatchRunRequest(TaskBatchRunRequest batchRunRequest) {
            this.batchRunRequest = batchRunRequest;
            this.batchTableIndex = 0;
        }

        private TaskBatchRunRequest getBatchRunRequest() {
            return batchRunRequest;
        }

        private void setBatchRunId(String batchRunId) {
            this.batchRunId = batchRunId;
        }

        private String getBatchRunId() {
            return batchRunId;
        }

        private void setBatchTableIndex(int batchTableIndex) {
            this.batchTableIndex = batchTableIndex;
        }

        private int getBatchTableIndex() {
            return batchTableIndex;
        }

        private void clearBatchState() {
            this.batchRunRequest = null;
            this.batchRunId = null;
            this.batchTableIndex = 0;
        }

        private SyncTaskProgressListener createListener(final SyncTask task) {
            final long startedAt = task.getStartedAt() == null ? System.currentTimeMillis() : task.getStartedAt().longValue();
            return new SyncTaskProgressListener() {
                @Override
                public boolean isPauseRequested() {
                    return pauseRequested;
                }

                @Override
                public boolean isStopRequested() {
                    return stopRequested;
                }

                @Override
                public void updateProgress(long totalRowCount, long syncedRowCount, long successRowCount, long failedRowCount,
                                           double speedRowsPerSecond, Long startedAtValue, Long endedAtValue, Long durationMillis,
                                           String progressMessage) {
                    try {
                        SyncTask currentTask = loadTask(taskId);
                        currentTask.setTotalRowCount(Long.valueOf(totalRowCount));
                        currentTask.setSyncedRowCount(Long.valueOf(syncedRowCount));
                        currentTask.setSuccessRowCount(Long.valueOf(successRowCount));
                        currentTask.setFailedRowCount(Long.valueOf(failedRowCount));
                        currentTask.setSpeedRowsPerSecond(Double.valueOf(speedRowsPerSecond));
                        currentTask.setStartedAt(startedAtValue == null ? Long.valueOf(startedAt) : startedAtValue);
                        currentTask.setEndedAt(endedAtValue);
                        currentTask.setDurationMillis(durationMillis);
                        currentTask.setProgressMessage(progressMessage);
                        syncTaskRepository.save(currentTask);
                        captureTaskRunMetric(currentTask, getBatchRunId(), currentTask.getFailedRowCount() != null
                                && currentTask.getFailedRowCount().longValue() > 0L ? progressMessage : null);
                    } catch (SQLException ex) {
                        throw new IllegalStateException("Failed to persist task progress", ex);
                    }
                }

                @Override
                public void saveCheckpoint(String checkpointKey, String checkpointValue) {
                    if (checkpointKey == null || checkpointKey.trim().length() == 0) {
                        return;
                    }
                    com.dbsyncstudio.model.sync.SyncCheckpoint checkpoint = new com.dbsyncstudio.model.sync.SyncCheckpoint();
                    checkpoint.setCheckpointKey(checkpointKey);
                    checkpoint.setCheckpointValue(checkpointValue);
                    checkpoint.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
                    checkpointRepository.save(checkpoint);
                }
            };
        }
    }

    public SyncTask runBatchTask(long taskId, TaskBatchRunRequest request) throws SQLException {
        SyncTask task = loadTask(taskId);
        TaskBatchRunRequest batchRunRequest = validateBatchRunRequest(request);
        if ((batchRunRequest.getTables() == null || batchRunRequest.getTables().isEmpty()) && syncTaskTableRepository != null) {
            List<SyncTaskTable> taskTables = syncTaskTableRepository.findByTaskId(taskId);
            List<TaskBatchTableRequest> tables = new ArrayList<TaskBatchTableRequest>();
            for (SyncTaskTable taskTable : taskTables) {
                if (taskTable == null) {
                    continue;
                }
                if (taskTable.getEnabled() != null && !taskTable.getEnabled().booleanValue()) {
                    continue;
                }
                TaskBatchTableRequest tableRequest = new TaskBatchTableRequest();
                tableRequest.setSourceSchemaName(taskTable.getSourceSchemaName());
                tableRequest.setSourceTableName(taskTable.getSourceTableName());
                tableRequest.setTargetSchemaName(taskTable.getTargetSchemaName());
                tableRequest.setTargetTableName(taskTable.getTargetTableName());
                tableRequest.setSyncMode(taskTable.getSyncMode());
                tableRequest.setIncrementalMode(taskTable.getIncrementalMode());
                tableRequest.setIncrementalColumnName(taskTable.getIncrementalColumnName());
                tableRequest.setIncrementalTieBreakerColumnName(taskTable.getIncrementalTieBreakerColumnName());
                tableRequest.setIncrementalCompositeColumnName(taskTable.getIncrementalCompositeColumnName());
                tableRequest.setBatchSize(taskTable.getBatchSize());
                tables.add(tableRequest);
            }
            batchRunRequest.setTables(tables);
        }
        TaskExecutionState executionState = ensureTaskState(taskId);
        executionState.setBatchRunRequest(batchRunRequest);
        executionState.setBatchRunId(resolveBatchRunId(taskId, batchRunRequest.getRunId()));
        return startTask(taskId);
    }

    private TaskBatchRunRequest validateBatchRunRequest(TaskBatchRunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch run request must not be null");
        }
        if (request.getTables() != null) {
            for (TaskBatchTableRequest tableRequest : request.getTables()) {
                if (tableRequest == null) {
                    throw new IllegalArgumentException("Batch run table must not be null");
                }
                if (trimToNull(tableRequest.getSourceTableName()) == null) {
                    throw new IllegalArgumentException("Batch source table name must not be blank");
                }
                if (trimToNull(tableRequest.getTargetTableName()) == null) {
                    throw new IllegalArgumentException("Batch target table name must not be blank");
                }
            }
        }
        if (trimToNull(request.getRunId()) == null) {
            request.setRunId(null);
        }
        return request;
    }

    private String resolveBatchRunId(long taskId, String runId) {
        String normalized = trimToNull(runId);
        if (normalized != null) {
            return normalized;
        }
        return "batch-" + taskId + "-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private void normalizeTaskTableConfig(SyncTaskTable taskTable, SyncTask task) {
        if (taskTable == null) {
            return;
        }
        if (trimToNull(taskTable.getSyncMode()) == null && task != null && task.getSyncMode() != null) {
            taskTable.setSyncMode(task.getSyncMode().name());
        }
        if (trimToNull(taskTable.getIncrementalMode()) == null && task != null && task.getIncrementalMode() != null) {
            taskTable.setIncrementalMode(task.getIncrementalMode().name());
        }
        if (trimToNull(taskTable.getIncrementalColumnName()) == null && task != null) {
            taskTable.setIncrementalColumnName(task.getIncrementalColumnName());
        }
        if (trimToNull(taskTable.getIncrementalTieBreakerColumnName()) == null && task != null) {
            taskTable.setIncrementalTieBreakerColumnName(task.getIncrementalTieBreakerColumnName());
        }
        if (trimToNull(taskTable.getIncrementalCompositeColumnName()) == null && task != null) {
            taskTable.setIncrementalCompositeColumnName(task.getIncrementalCompositeColumnName());
        }
        if (taskTable.getBatchSize() == null || taskTable.getBatchSize().intValue() <= 0) {
            taskTable.setBatchSize(Integer.valueOf(500));
        }
    }

    private void validatePreviewRequest(DataPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Data preview request must not be null");
        }
        if (request.getDatasource() == null) {
            throw new IllegalArgumentException("Datasource must not be null");
        }
        if (request.getTableName() == null || request.getTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Table name must not be blank");
        }
        if (request.getPageNumber() <= 0) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }
        if (request.getPageSize() <= 0 || request.getPageSize() > 1000) {
            throw new IllegalArgumentException("Page size must be between 1 and 1000");
        }
    }

    private TableMetadata findTableMetadata(List<SchemaMetadata> schemas, String schemaName, String tableName) {
        for (SchemaMetadata schemaMetadata : schemas) {
            if (schemaName != null && schemaName.trim().length() > 0
                    && !schemaName.equalsIgnoreCase(schemaMetadata.getSchemaName())) {
                continue;
            }
            if (schemaMetadata.getTables() == null) {
                continue;
            }
            for (TableMetadata tableMetadata : schemaMetadata.getTables()) {
                if (tableName.equalsIgnoreCase(tableMetadata.getTableName())) {
                    return tableMetadata;
                }
            }
        }
        return null;
    }

    private String buildPreviewSelectSql(DatasourceConfig datasource, TableMetadata tableMetadata, DataPreviewRequest request) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        List<ColumnMetadata> columns = tableMetadata.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(quoteIdentifier(columns.get(i).getName()));
        }
        sql.append(" FROM ").append(qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName()));
        String whereClause = buildPreviewWhereClause(columns, request);
        if (whereClause.length() > 0) {
            sql.append(" WHERE ").append(whereClause);
        }
        if (request.getSortColumn() != null && request.getSortColumn().trim().length() > 0) {
            String resolvedSortColumn = resolveAllowedColumn(columns, request.getSortColumn());
            if (resolvedSortColumn == null) {
                sql.append(" ORDER BY ").append(buildPreviewOrderByClause(columns));
            } else {
                sql.append(" ORDER BY ").append(quoteIdentifier(resolvedSortColumn));
                if ("DESC".equalsIgnoreCase(request.getSortDirection())) {
                    sql.append(" DESC");
                } else {
                    sql.append(" ASC");
                }
            }
        } else {
            sql.append(" ORDER BY ").append(buildPreviewOrderByClause(columns));
        }
        sql.append(buildPreviewPaginationClause(datasource.getType()));
        return sql.toString();
    }

    private String buildPreviewWhereClause(List<ColumnMetadata> columns, DataPreviewRequest request) {
        if (request.getFilters() == null || request.getFilters().isEmpty()) {
            return "";
        }
        List<String> clauses = new ArrayList<String>();
        for (DataPreviewFilter filter : request.getFilters()) {
            if (filter == null || filter.getColumnName() == null || filter.getColumnName().trim().length() == 0) {
                continue;
            }
            String columnName = resolveAllowedColumn(columns, filter.getColumnName());
            if (columnName == null) {
                continue;
            }
            DataPreviewFilterOperator operator = filter.getOperator() == null ? DataPreviewFilterOperator.EQ : filter.getOperator();
            if (operator == DataPreviewFilterOperator.IS_NULL) {
                clauses.add(quoteIdentifier(columnName) + " IS NULL");
            } else if (operator == DataPreviewFilterOperator.IS_NOT_NULL) {
                clauses.add(quoteIdentifier(columnName) + " IS NOT NULL");
            } else if (operator == DataPreviewFilterOperator.LIKE) {
                clauses.add(quoteIdentifier(columnName) + " LIKE ?");
            } else {
                clauses.add(quoteIdentifier(columnName) + " " + renderOperator(operator) + " ?");
            }
        }
        return joinClauses(clauses, " AND ");
    }

    private String buildPreviewOrderByClause(List<ColumnMetadata> columns) {
        for (ColumnMetadata columnMetadata : columns) {
            if (columnMetadata.isPrimaryKey()) {
                return quoteIdentifier(columnMetadata.getName());
            }
        }
        if (columns.isEmpty()) {
            return "1";
        }
        return quoteIdentifier(columns.get(0).getName());
    }

    private String buildPreviewPaginationClause(com.dbsyncstudio.model.datasource.DatasourceType datasourceType) {
        if (datasourceType == com.dbsyncstudio.model.datasource.DatasourceType.DM) {
            return " LIMIT ? OFFSET ?";
        }
        return " LIMIT ? OFFSET ?";
    }

    private void bindPreviewParameters(PreparedStatement statement, DataPreviewRequest request) throws SQLException {
        int index = bindPreviewFilterParameters(statement, request);
        statement.setInt(index, request.getPageSize());
        statement.setInt(index + 1, (request.getPageNumber() - 1) * request.getPageSize());
    }

    private int bindPreviewFilterParameters(PreparedStatement statement, DataPreviewRequest request) throws SQLException {
        int index = 1;
        if (request.getFilters() != null) {
            for (DataPreviewFilter filter : request.getFilters()) {
                if (filter == null || filter.getColumnName() == null || filter.getColumnName().trim().length() == 0) {
                    continue;
                }
                DataPreviewFilterOperator operator = filter.getOperator() == null ? DataPreviewFilterOperator.EQ : filter.getOperator();
                if (operator == DataPreviewFilterOperator.IS_NULL || operator == DataPreviewFilterOperator.IS_NOT_NULL) {
                    continue;
                }
                if (operator == DataPreviewFilterOperator.LIKE) {
                    statement.setObject(index, "%" + (filter.getValue() == null ? "" : String.valueOf(filter.getValue())) + "%");
                } else {
                    statement.setObject(index, filter.getValue());
                }
                index++;
            }
        }
        return index;
    }

    private String resolveAllowedColumn(List<ColumnMetadata> columns, String columnName) {
        for (ColumnMetadata columnMetadata : columns) {
            if (columnMetadata.getName().equalsIgnoreCase(columnName)) {
                return columnMetadata.getName();
            }
        }
        return null;
    }

    private String renderOperator(DataPreviewFilterOperator operator) {
        if (operator == DataPreviewFilterOperator.NE) {
            return "<>";
        }
        if (operator == DataPreviewFilterOperator.GT) {
            return ">";
        }
        if (operator == DataPreviewFilterOperator.GTE) {
            return ">=";
        }
        if (operator == DataPreviewFilterOperator.LT) {
            return "<";
        }
        if (operator == DataPreviewFilterOperator.LTE) {
            return "<=";
        }
        return "=";
    }

    private String joinClauses(List<String> clauses, String delimiter) {
        if (clauses == null || clauses.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(clauses.get(i));
        }
        return builder.toString();
    }

    private void validateSqlRequest(SqlExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("SQL execution request must not be null");
        }
        if (request.getDatasource() == null) {
            throw new IllegalArgumentException("Datasource must not be null");
        }
        if (request.getSql() == null || request.getSql().trim().length() == 0) {
            throw new IllegalArgumentException("SQL must not be blank");
        }
    }

    private String sanitizeSql(String sql) {
        String trimmed = sql == null ? "" : sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Multiple SQL statements are not allowed");
        }
        return trimmed;
    }

    private String trimSql(String sql) {
        String trimmed = sql == null ? "" : sql.trim();
        if (trimmed.endsWith(";")) {
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<String>();
        if (sql == null || sql.trim().length() == 0) {
            return statements;
        }
        String[] parts = sql.split(";");
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.length() > 0) {
                statements.add(trimmed);
            }
        }
        return statements;
    }

    private String resolveSqlStatementType(String sql) {
        String normalized = stripLeadingComments(sql).trim().toUpperCase(Locale.ROOT);
        int splitIndex = normalized.indexOf(' ');
        if (splitIndex < 0) {
            return normalized;
        }
        return normalized.substring(0, splitIndex);
    }

    private String stripLeadingComments(String sql) {
        String result = sql == null ? "" : sql.trim();
        while (result.startsWith("--")) {
            int lineBreak = result.indexOf('\n');
            if (lineBreak < 0) {
                return "";
            }
            result = result.substring(lineBreak + 1).trim();
        }
        return result;
    }

    private boolean isDangerousStatement(String statementType) {
        return "DROP".equalsIgnoreCase(statementType)
                || "TRUNCATE".equalsIgnoreCase(statementType)
                || "ALTER".equalsIgnoreCase(statementType);
    }

    private boolean isQueryStatement(String statementType) {
        return "SELECT".equalsIgnoreCase(statementType) || "WITH".equalsIgnoreCase(statementType);
    }

    private String qualifiedTableName(String schemaName, String tableName) {
        if (schemaName == null || schemaName.trim().length() == 0 || "default".equalsIgnoreCase(schemaName)) {
            return quoteIdentifier(tableName);
        }
        return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    private String quoteIdentifier(String identifier) {
        return identifier;
    }

    private void saveSchemaComparisonHistory(SchemaComparisonRequest request, SchemaComparisonResult result) {
        SchemaComparisonHistoryEntry entry = new SchemaComparisonHistoryEntry();
        entry.setSourceDatasourceId(request.getSourceDatasource().getId());
        entry.setTargetDatasourceId(request.getTargetDatasource().getId());
        entry.setSourceSchemaName(request.getSourceSchemaName());
        entry.setSourceTableName(request.getSourceTableName());
        entry.setTargetSchemaName(request.getTargetSchemaName());
        entry.setTargetTableName(request.getTargetTableName());
        entry.setDiffSummary(buildSchemaComparisonHistorySummary(result));
        entry.setCreatedAt(Long.valueOf(System.currentTimeMillis()));
        schemaComparisonHistoryRepository.save(entry);
    }

    public static String buildSchemaComparisonHistorySummary(SchemaComparisonResult result) {
        List<SchemaDiffEntry> diffEntries = result == null || result.getDiffEntries() == null
                ? Collections.<SchemaDiffEntry>emptyList()
                : result.getDiffEntries();
        List<String> sqlList = result == null || result.getSuggestedSqlList() == null
                ? Collections.<String>emptyList()
                : result.getSuggestedSqlList();

        Map<String, Integer> diffTypeCounts = new LinkedHashMap<String, Integer>();
        for (SchemaDiffEntry entry : diffEntries) {
            if (entry == null || entry.getDiffType() == null) {
                continue;
            }
            String key = entry.getDiffType().name();
            Integer count = diffTypeCounts.get(key);
            diffTypeCounts.put(key, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        }

        StringBuilder builder = new StringBuilder();
        builder.append("差异 ").append(diffEntries.size()).append(" 项");
        builder.append(" | SQL ").append(sqlList.size()).append(" 条");
        if (!diffTypeCounts.isEmpty()) {
            builder.append(" | ");
            int index = 0;
            for (Map.Entry<String, Integer> item : diffTypeCounts.entrySet()) {
                if (index > 0) {
                    builder.append(", ");
                }
                builder.append(item.getKey()).append("=").append(item.getValue());
                index++;
            }
        }
        return builder.toString();
    }

    private void validateSchemaSqlRequest(SchemaSqlPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Schema SQL request must not be null");
        }
        validateDatasource(request.getDatasource());
        if (request.getSql() == null || request.getSql().trim().length() == 0) {
            throw new IllegalArgumentException("SQL must not be blank");
        }
    }

    public SyncTask runTask(long taskId) throws SQLException {
        return startTask(taskId);
    }

    public SyncTask startTask(long taskId) throws SQLException {
        SyncTask task = loadTask(taskId);
        if (task.getTaskStatus() == SyncTaskStatus.RUNNING) {
            return task;
        }
        TaskExecutionState executionState = taskExecutionStates.get(Long.valueOf(taskId));
        if (executionState == null) {
            executionState = new TaskExecutionState(taskId);
            taskExecutionStates.put(Long.valueOf(taskId), executionState);
        }
        executionState.clearControlFlags();
        task.setTaskStatus(SyncTaskStatus.RUNNING);
        if (task.getStartedAt() == null) {
            task.setStartedAt(Long.valueOf(System.currentTimeMillis()));
        }
        task.setEndedAt(null);
        task.setProgressMessage("Task queued");
        syncTaskRepository.save(task);
        appendLog(taskId, "INFO", "Task started: " + task.getTaskName());

        final TaskExecutionState finalExecutionState = executionState;
        taskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                executeTask(taskId, finalExecutionState);
            }
        });
        return task;
    }

    public SyncTask pauseTask(long taskId) throws SQLException {
        TaskExecutionState state = ensureTaskState(taskId);
        state.pause();
        SyncTask task = loadTask(taskId);
        task.setTaskStatus(SyncTaskStatus.PAUSED);
        task.setProgressMessage("Pause requested");
        syncTaskRepository.save(task);
        appendLog(taskId, "INFO", "Pause requested for task: " + task.getTaskName());
        return task;
    }

    public SyncTask resumeTask(long taskId) throws SQLException {
        SyncTask task = loadTask(taskId);
        if (task.getTaskStatus() != SyncTaskStatus.PAUSED && task.getTaskStatus() != SyncTaskStatus.STOPPED) {
            return task;
        }
        TaskExecutionState state = ensureTaskState(taskId);
        state.clearControlFlags();
        return startTask(taskId);
    }

    public int recoverUnfinishedTasks() throws SQLException {
        List<SyncTask> tasks = syncTaskRepository.findAll();
        int recoveredCount = 0;
        long now = System.currentTimeMillis();
        for (SyncTask task : tasks) {
            if (task == null || task.getId() == null || task.getTaskStatus() != SyncTaskStatus.RUNNING) {
                continue;
            }
            task.setTaskStatus(SyncTaskStatus.PENDING);
            task.setEndedAt(Long.valueOf(now));
            task.setProgressMessage("Recovered after unexpected shutdown");
            syncTaskRepository.save(task);
            taskExecutionStates.remove(task.getId());
            appendLog(task.getId().longValue(), "WARN", "Recovered unfinished task after startup: " + task.getTaskName());
            recoveredCount++;
        }
        lastRecoveredTaskCount = recoveredCount;
        return recoveredCount;
    }

    public SyncTask stopTask(long taskId) throws SQLException {
        TaskExecutionState state = ensureTaskState(taskId);
        state.stop();
        SyncTask task = loadTask(taskId);
        task.setTaskStatus(SyncTaskStatus.STOPPED);
        task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        task.setProgressMessage("Stop requested");
        syncTaskRepository.save(task);
        appendLog(taskId, "INFO", "Stop requested for task: " + task.getTaskName());
        return task;
    }

    public List<SyncTask> listTasksWithProgress() throws SQLException {
        return listTasks();
    }

    public DataPreviewResult previewTableData(DataPreviewRequest request) throws SQLException {
        validatePreviewRequest(request);
        DatasourceConfig datasource = request.getDatasource();
        List<SchemaMetadata> schemas = metadataScanner.scan(datasource);
        TableMetadata tableMetadata = findTableMetadata(schemas, request.getSchemaName(), request.getTableName());
        if (tableMetadata == null) {
            throw new SQLException("Table not found: " + request.getTableName());
        }

        List<ColumnMetadata> columns = tableMetadata.getColumns();
        String selectSql = buildPreviewSelectSql(datasource, tableMetadata, request);
        String countSql = "SELECT COUNT(1) FROM " + qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName());
        String whereClause = buildPreviewWhereClause(columns, request);
        if (whereClause.length() > 0) {
            countSql = countSql + " WHERE " + whereClause;
        }

        try (Connection connection = com.dbsyncstudio.core.connection.JdbcConnectionSupport.openConnection(datasource);
             PreparedStatement countStatement = connection.prepareStatement(countSql);
             PreparedStatement statement = connection.prepareStatement(selectSql)) {
            bindPreviewFilterParameters(countStatement, request);
            try (ResultSet countResultSet = countStatement.executeQuery()) {
                long totalRowCount = countResultSet.next() ? countResultSet.getLong(1) : 0L;
                bindPreviewParameters(statement, request);
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<String, Object>();
                        for (int i = 0; i < columns.size(); i++) {
                            String columnName = columns.get(i).getName();
                            row.put(columnName, resultSet.getObject(i + 1));
                        }
                        rows.add(row);
                    }
                }

                List<String> columnNames = new ArrayList<String>();
                for (ColumnMetadata columnMetadata : columns) {
                    columnNames.add(columnMetadata.getName());
                }

                DataPreviewResult result = new DataPreviewResult();
                result.setColumns(columnNames);
                result.setRows(rows);
                result.setTotalRowCount(totalRowCount);
                result.setPageNumber(request.getPageNumber());
                result.setPageSize(request.getPageSize());
                return result;
            }
        }
    }

    public SqlExecutionResult executeSql(SqlExecutionRequest request) throws SQLException {
        validateSqlRequest(request);
        long startTime = System.currentTimeMillis();
        DatasourceConfig datasource = request.getDatasource();
        String sql = sanitizeSql(request.getSql());
        String statementType = resolveSqlStatementType(sql);
        if (isDangerousStatement(statementType) && !request.isAllowDangerousSql()) {
            throw new SQLException("Dangerous SQL is disabled by default: " + statementType);
        }

        SqlExecutionLogEntry logEntry = SqlExecutionLogEntry.builder()
                .datasourceId(datasource.getId())
                .sqlText(sql)
                .statementType(statementType)
                .success(false)
                .createdAt(Long.valueOf(startTime))
                .build();

        try (Connection connection = com.dbsyncstudio.core.connection.JdbcConnectionSupport.openConnection(datasource);
             Statement statement = connection.createStatement()) {
            SqlExecutionResult result = new SqlExecutionResult();
            result.setStatementType(statementType);
            if (isQueryStatement(statementType)) {
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    List<String> columns = new ArrayList<String>();
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(resultSet.getMetaData().getColumnLabel(i));
                    }
                    List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<String, Object>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(columns.get(i - 1), resultSet.getObject(i));
                        }
                        rows.add(row);
                    }
                    result.setColumns(columns);
                    result.setRows(rows);
                    result.setAffectedRows(rows.size());
                    result.setSuccess(true);
                    result.setMessage("Query executed successfully");
                }
            } else {
                long affectedRows = statement.executeUpdate(sql);
                result.setAffectedRows(affectedRows);
                result.setSuccess(true);
                result.setMessage("Statement executed successfully");
            }
            long elapsedMillis = System.currentTimeMillis() - startTime;
            result.setElapsedMillis(elapsedMillis);
            logEntry.setSuccess(true);
            logEntry.setAffectedRows(Long.valueOf(result.getAffectedRows()));
            logEntry.setElapsedMillis(Long.valueOf(elapsedMillis));
            long logId = sqlExecutionLogRepository.append(logEntry);
            result.setLogId(logId);
            return result;
        } catch (SQLException ex) {
            logEntry.setErrorMessage(ex.getMessage());
            logEntry.setElapsedMillis(Long.valueOf(System.currentTimeMillis() - startTime));
            sqlExecutionLogRepository.append(logEntry);
            throw ex;
        }
    }

    public BackendDashboardStats dashboardStats() throws SQLException {
        List<DatasourceConfig> datasources = datasourceRepository.findAll();
        List<SyncTask> tasks = syncTaskRepository.findAll();
        return new BackendDashboardStats(datasources.size(), tasks.size(), listAllLogs(tasks).size());
    }

    public BackendDiagnosticsResponse diagnosticsStatus() throws SQLException {
        BackendDiagnosticsResponse response = new BackendDiagnosticsResponse();
        response.setGeneratedAt(System.currentTimeMillis());
        response.setApplicationDirectory(com.dbsyncstudio.store.sqlite.SqliteDatabasePaths.appDirectory().getAbsolutePath());
        response.setDatabaseFilePath(connectionFactory == null || connectionFactory.getDatabaseFile() == null
                ? null
                : connectionFactory.getDatabaseFile().getAbsolutePath());
        response.setSchemaVersion(com.dbsyncstudio.store.sqlite.SqliteSchemaInitializer.currentSchemaVersion());
        response.setDatabaseUserVersion(readDatabaseUserVersion());
        response.setMigrationEntryCount(readMigrationEntryCount());
        List<SyncTask> tasks = syncTaskRepository.findAll();
        response.setTotalTaskCount(tasks.size());
        int unfinishedTaskCount = 0;
        int runningTaskCount = 0;
        for (SyncTask task : tasks) {
            if (task != null && task.getTaskStatus() == SyncTaskStatus.RUNNING) {
                unfinishedTaskCount++;
                runningTaskCount++;
            }
        }
        response.setUnfinishedTaskCount(unfinishedTaskCount);
        response.setRunningTaskCount(runningTaskCount);
        response.setRecoveredTaskCount(lastRecoveredTaskCount);
        return response;
    }

    public MonitoringOverviewResponse monitoringOverview() throws SQLException {
        long now = System.currentTimeMillis();
        long dayStart = startOfDay(now);
        long dayEnd = dayStart + 24L * 60L * 60L * 1000L;
        TaskRunMetricSummary summary = monitoringRepository == null
                ? TaskRunMetricSummary.builder()
                .totalTaskCount(Integer.valueOf(0))
                .successTaskCount(Integer.valueOf(0))
                .failedTaskCount(Integer.valueOf(0))
                .latestRunningTaskCount(Integer.valueOf(taskExecutionStates.size()))
                .build()
                : monitoringRepository.summarizeTaskMetricsForToday(dayStart, dayEnd);
        TaskRunMetric latestTaskMetric = null;
        if (monitoringRepository != null) {
            List<TaskRunMetric> latestMetrics = monitoringRepository.findTaskRunMetrics(null, null, null, null, 1);
            if (!latestMetrics.isEmpty()) {
                latestTaskMetric = latestMetrics.get(0);
            }
        }
        MonitoringOverviewResponse response = new MonitoringOverviewResponse();
        response.setSummary(summary);
        response.setLatestTaskMetric(latestTaskMetric);
        return response;
    }

    public List<TaskRunMetric> listTaskRunMetrics(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException {
        if (monitoringRepository == null) {
            return new ArrayList<TaskRunMetric>();
        }
        return monitoringRepository.findTaskRunMetrics(trimToNull(runId), taskId, startTime, endTime, limit);
    }

    public List<TableRunMetric> listTableRunMetrics(String runId, Long taskId, Long tableTaskId, Long startTime,
                                                    Long endTime, int limit) throws SQLException {
        if (monitoringRepository == null) {
            return new ArrayList<TableRunMetric>();
        }
        return monitoringRepository.findTableRunMetrics(trimToNull(runId), taskId, tableTaskId, startTime, endTime, limit);
    }

    public List<DatasourceConnectionMetric> listDatasourceConnectionMetrics(Long datasourceId, Long startTime, Long endTime,
                                                                            int limit) throws SQLException {
        if (monitoringRepository == null) {
            return new ArrayList<DatasourceConnectionMetric>();
        }
        return monitoringRepository.findDatasourceConnectionMetrics(datasourceId, startTime, endTime, limit);
    }

    public MonitoringTrendResponse taskRunTrend(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException {
        List<TaskRunMetric> metrics = listTaskRunMetrics(runId, taskId, startTime, endTime, limit);
        List<MonitoringTrendPoint> points = new ArrayList<MonitoringTrendPoint>();
        for (TaskRunMetric metric : metrics) {
            if (metric == null) {
                continue;
            }
            MonitoringTrendPoint point = new MonitoringTrendPoint();
            point.setMetricTime(metric.getMetricTime());
            point.setSuccessRowCount(metric.getSuccessRowCount());
            point.setFailedRowCount(metric.getFailedRowCount());
            point.setSpeedRowsPerSecond(metric.getSpeedRowsPerSecond());
            point.setLatencyMillis(metric.getLatencyMillis());
            point.setDurationMillis(metric.getDurationMillis());
            points.add(point);
        }
        Collections.reverse(points);
        MonitoringTrendResponse response = new MonitoringTrendResponse();
        response.setTaskRunTrend(points);
        return response;
    }

    public MonitoringCleanupSummary cleanupMonitoringMetrics(Integer retentionDays) throws SQLException {
        if (monitoringRepository == null) {
            return MonitoringCleanupSummary.builder()
                    .retentionDays(Integer.valueOf(retentionDays == null || retentionDays.intValue() <= 0
                            ? DEFAULT_MONITORING_RETENTION_DAYS : retentionDays.intValue()))
                    .cutoffTime(Long.valueOf(System.currentTimeMillis()))
                    .taskRunMetricDeletedCount(Integer.valueOf(0))
                    .tableRunMetricDeletedCount(Integer.valueOf(0))
                    .datasourceConnectionMetricDeletedCount(Integer.valueOf(0))
                    .build();
        }
        int safeRetentionDays = retentionDays == null || retentionDays.intValue() <= 0
                ? DEFAULT_MONITORING_RETENTION_DAYS : retentionDays.intValue();
        return monitoringRepository.cleanupExpiredMetrics(safeRetentionDays, System.currentTimeMillis());
    }

    public List<FieldMappingRule> listFieldMappings(long taskId) throws SQLException {
        return fieldMappingRepository.findByTaskId(taskId);
    }

    public List<FieldMappingSuggestion> suggestFieldMappings(long taskId) throws SQLException {
        SyncTask task = loadTask(taskId);
        DatasourceConfig source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
        DatasourceConfig target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
        List<SchemaMetadata> sourceSchemas = metadataScanner.scan(source);
        List<SchemaMetadata> targetSchemas = metadataScanner.scan(target);
        TableMetadata sourceTable = findTableMetadata(sourceSchemas, task.getSourceSchemaName(), task.getSourceTableName());
        TableMetadata targetTable = findTableMetadata(targetSchemas, task.getTargetSchemaName(), task.getTargetTableName());
        if (sourceTable == null) {
            throw new SQLException("Source table not found: " + task.getSourceTableName());
        }
        if (targetTable == null) {
            throw new SQLException("Target table not found: " + task.getTargetTableName());
        }
        return fieldMappingSuggestionMatcher.match(sourceTable.getColumns(), targetTable.getColumns());
    }

    public SchemaComparisonResult compareSchema(SchemaComparisonRequest request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Schema comparison request must not be null");
        }
        validateDatasource(request.getSourceDatasource());
        validateDatasource(request.getTargetDatasource());
        if (request.getSourceTableName() == null || request.getSourceTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Source table name must not be blank");
        }
        if (request.getTargetTableName() == null || request.getTargetTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Target table name must not be blank");
        }
        List<SchemaMetadata> sourceSchemas = metadataScanner.scan(request.getSourceDatasource());
        List<SchemaMetadata> targetSchemas = metadataScanner.scan(request.getTargetDatasource());
        TableMetadata sourceTable = findTableMetadata(sourceSchemas, request.getSourceSchemaName(), request.getSourceTableName());
        TableMetadata targetTable = findTableMetadata(targetSchemas, request.getTargetSchemaName(), request.getTargetTableName());
        if (sourceTable == null) {
            throw new SQLException("Source table not found: " + request.getSourceTableName());
        }
        if (targetTable == null) {
            throw new SQLException("Target table not found: " + request.getTargetTableName());
        }
        SchemaComparisonResult result = schemaComparisonEngine.compare(request, sourceTable, targetTable,
                DatabaseDialect.from(request.getTargetDatasource().getType()));
        saveSchemaComparisonHistory(request, result);
        return result;
    }

    public SchemaSqlPreviewResult previewSchemaSql(SchemaSqlPreviewRequest request) throws SQLException {
        validateSchemaSqlRequest(request);
        String sql = trimSql(request.getSql());
        List<String> statements = splitSqlStatements(sql);
        String statementType = statements.isEmpty() ? resolveSqlStatementType(sql) : resolveSqlStatementType(statements.get(0));
        boolean executable = true;
        for (String statementSql : statements) {
            if (isDangerousStatement(resolveSqlStatementType(statementSql))) {
                executable = executable && request.isAllowDangerousSql();
            }
        }
        SchemaSqlPreviewResult result = new SchemaSqlPreviewResult();
        result.setExecutable(statements.isEmpty() || executable);
        result.setStatementType(statementType);
        result.setSql(sql);
        result.setMessage(result.isExecutable() ? "SQL is ready for confirmation" : "Dangerous SQL requires explicit confirmation");
        return result;
    }

    public SqlExecutionResult executeSchemaSql(SchemaSqlPreviewRequest request) throws SQLException {
        validateSchemaSqlRequest(request);
        String sql = trimSql(request.getSql());
        List<String> statements = splitSqlStatements(sql);
        if (statements.isEmpty()) {
            throw new SQLException("SQL must not be blank");
        }

        long startTime = System.currentTimeMillis();
        DatasourceConfig datasource = request.getDatasource();
        SqlExecutionLogEntry logEntry = SqlExecutionLogEntry.builder()
                .datasourceId(datasource.getId())
                .sqlText(sql)
                .statementType(resolveSqlStatementType(sql))
                .success(false)
                .createdAt(Long.valueOf(startTime))
                .build();

        try (Connection connection = com.dbsyncstudio.core.connection.JdbcConnectionSupport.openConnection(datasource);
             Statement statement = connection.createStatement()) {
            long affectedRows = 0L;
            for (String statementSql : statements) {
                String statementType = resolveSqlStatementType(statementSql);
                if (isDangerousStatement(statementType) && !request.isAllowDangerousSql()) {
                    throw new SQLException("Dangerous SQL is disabled by default: " + statementType);
                }
                affectedRows += statement.executeUpdate(statementSql);
            }

            SqlExecutionResult result = new SqlExecutionResult();
            result.setSuccess(true);
            result.setStatementType(resolveSqlStatementType(sql));
            result.setMessage("DDL executed successfully");
            result.setElapsedMillis(System.currentTimeMillis() - startTime);
            result.setAffectedRows(affectedRows);
            logEntry.setSuccess(true);
            logEntry.setAffectedRows(Long.valueOf(affectedRows));
            logEntry.setElapsedMillis(Long.valueOf(result.getElapsedMillis()));
            result.setLogId(sqlExecutionLogRepository.append(logEntry));
            return result;
        } catch (SQLException ex) {
            logEntry.setErrorMessage(ex.getMessage());
            logEntry.setElapsedMillis(Long.valueOf(System.currentTimeMillis() - startTime));
            sqlExecutionLogRepository.append(logEntry);
            throw ex;
        }
    }

    public SyncTask updateScheduleState(long taskId, boolean enabled, String scheduleType, String cronExpression, Integer intervalSeconds) throws SQLException {
        SyncTask task = loadTask(taskId);
        String normalizedScheduleType = scheduleType == null ? null : scheduleType.trim().toUpperCase(Locale.ROOT);
        boolean manualSchedule = "MANUAL".equals(normalizedScheduleType);
        task.setScheduleEnabled(Boolean.valueOf(enabled && !manualSchedule));
        task.setScheduleType(normalizedScheduleType);
        task.setScheduleCronExpression(trimToNull(cronExpression));
        task.setScheduleIntervalSeconds(intervalSeconds);
        if (enabled && !manualSchedule) {
            setNextScheduleRunAt(task, System.currentTimeMillis());
        } else {
            task.setScheduleNextRunAt(null);
        }
        syncTaskRepository.save(task);
        return task;
    }

    public FieldMappingRule saveFieldMapping(FieldMappingRule mappingRule) throws SQLException {
        validateMapping(mappingRule);
        fieldMappingRepository.save(mappingRule);
        return mappingRule;
    }

    public boolean deleteFieldMapping(long id) throws SQLException {
        return fieldMappingRepository.deleteById(id);
    }

    public List<SchemaMetadata> scanMetadata(long datasourceId) throws SQLException {
        DatasourceConfig config = loadDatasource(datasourceId, "Datasource not found: ");
        return metadataScanner.scan(config);
    }

    private FullSyncResult fullSync(SyncTask task, DatasourceConfig source, DatasourceConfig target) {
        FullSyncRequest request = FullSyncRequest.builder()
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceSchemaName(task.getSourceSchemaName())
                .targetSchemaName(task.getTargetSchemaName())
                .sourceTableName(task.getSourceTableName())
                .targetTableName(task.getTargetTableName())
                .pageSize(500)
                .batchSize(500)
                .replaceTargetData(true)
                .build();
        FullSyncResult result = fullSyncEngine.sync(request);
        appendLog(task.getId().longValue(), "INFO", "Full sync inserted " + result.getInsertedRowCount() + " rows");
        return result;
    }

    private IncrementalSyncResult incrementalSync(SyncTask task, DatasourceConfig source, DatasourceConfig target) {
        IncrementalSyncRequest request = IncrementalSyncRequest.builder()
                .sourceDatasource(source)
                .targetDatasource(target)
                .sourceSchemaName(task.getSourceSchemaName())
                .targetSchemaName(task.getTargetSchemaName())
                .sourceTableName(task.getSourceTableName())
                .targetTableName(task.getTargetTableName())
                .watermarkColumnName("updated_at")
                .checkpointKey("task-" + task.getId())
                .pageSize(500)
                .batchSize(500)
                .replaceTargetData(false)
                .build();
        IncrementalSyncResult result = incrementalSyncEngine.sync(request);
        appendLog(task.getId().longValue(), "INFO", "Incremental sync inserted " + result.getInsertedRowCount() + " rows");
        return result;
    }

    private void validateDatasource(DatasourceConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Datasource must not be null");
        }
        if (config.getType() == null) {
            throw new IllegalArgumentException("Datasource type must not be null");
        }
        if (config.getName() == null || config.getName().trim().length() == 0) {
            throw new IllegalArgumentException("Datasource name must not be blank");
        }
    }

    private void validateTask(SyncTask task) {
        if (task == null) {
            throw new IllegalArgumentException("Sync task must not be null");
        }
        if (task.getTaskName() == null || task.getTaskName().trim().length() == 0) {
            throw new IllegalArgumentException("Task name must not be blank");
        }
        if (task.getSourceDatasourceId() == null) {
            throw new IllegalArgumentException("Source datasource id must not be null");
        }
        if (task.getTargetDatasourceId() == null) {
            throw new IllegalArgumentException("Target datasource id must not be null");
        }
        if (task.getSourceTableName() == null || task.getSourceTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Source table name must not be blank");
        }
        if (task.getTargetTableName() == null || task.getTargetTableName().trim().length() == 0) {
            throw new IllegalArgumentException("Target table name must not be blank");
        }
    }

    private void normalizeSchedule(SyncTask task) {
        Boolean enabled = task.getScheduleEnabled();
        if (enabled == null || !enabled.booleanValue()) {
            task.setScheduleNextRunAt(null);
            return;
        }
        if (task.getScheduleType() == null || task.getScheduleType().trim().length() == 0) {
            task.setScheduleType("MANUAL");
        }
        setNextScheduleRunAt(task, System.currentTimeMillis());
    }

    private void updateScheduleResult(SyncTask task, String lastResult, String lastMessage) {
        if (task == null || task.getScheduleEnabled() == null || !task.getScheduleEnabled().booleanValue()) {
            return;
        }
        task.setScheduleLastResult(lastResult);
        task.setScheduleLastMessage(lastMessage);
        long baseTime = task.getEndedAt() == null ? System.currentTimeMillis() : task.getEndedAt().longValue();
        setNextScheduleRunAt(task, baseTime);
    }

    private void validateMapping(FieldMappingRule mappingRule) {
        if (mappingRule == null) {
            throw new IllegalArgumentException("Field mapping must not be null");
        }
        if (mappingRule.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        if (mappingRule.getSourceColumnName() == null || mappingRule.getSourceColumnName().trim().length() == 0) {
            throw new IllegalArgumentException("Source column name must not be blank");
        }
        if (mappingRule.getTargetColumnName() == null || mappingRule.getTargetColumnName().trim().length() == 0) {
            throw new IllegalArgumentException("Target column name must not be blank");
        }
    }

    private void appendLog(long taskId, String logLevel, String message) {
        ExecutionLogEntry entry = new ExecutionLogEntry();
        entry.setTaskId(Long.valueOf(taskId));
        entry.setLogLevel(logLevel);
        entry.setLogMessage(message);
        executionLogRepository.append(entry);
    }

    private void triggerAlert(String alertType, Long taskId, String runId, String tableName, String alertLevel,
                              String alertContent, Long tableTaskId, Long datasourceId) {
        if (alertRuleRepository == null || alertChannelRepository == null || alertHistoryRepository == null || alertDedupStateRepository == null) {
            return;
        }
        try {
            List<AlertRule> rules = alertRuleRepository.findEnabled();
            long now = System.currentTimeMillis();
            for (AlertRule rule : rules) {
                if (rule == null || rule.getAlertType() == null || !rule.getAlertType().equals(alertType)) {
                    continue;
                }
                if (rule.getTaskId() != null && taskId != null && !rule.getTaskId().equals(taskId)) {
                    continue;
                }
                if (rule.getTableName() != null && tableName != null && !rule.getTableName().equalsIgnoreCase(tableName)) {
                    continue;
                }
                List<Long> channelIds = parseChannelIds(rule.getChannelIdsJson());
                for (Long channelId : channelIds) {
                    if (channelId == null) {
                        continue;
                    }
                    AlertChannel channel = alertChannelRepository.findById(channelId.longValue()).orElse(null);
                    if (channel == null || Boolean.FALSE.equals(channel.getEnabled())) {
                        continue;
                    }
                    String dedupKey = buildDedupKey(alertType, taskId, tableName, channel.getChannelType(), channelId);
                    AlertDedupState existing = alertDedupStateRepository.findByDedupKey(dedupKey).orElse(null);
                    if (existing != null && existing.getCooldownUntil() != null && existing.getCooldownUntil().longValue() > now) {
                        continue;
                    }
                    String content = renderAlertContent(rule, alertContent, taskId, runId, tableName);
                    AlertSendResult sendResult = alertSenderService.send(channel,
                            rule.getRuleName(),
                            content);
                    AlertHistoryEntry history = AlertHistoryEntry.builder()
                            .alertId("alert-" + now + "-" + Math.abs((dedupKey + content).hashCode()))
                            .ruleId(rule.getId())
                            .alertType(alertType)
                            .taskId(taskId)
                            .runId(runId)
                            .tableName(tableName)
                            .alertLevel(rule.getAlertLevel() == null ? alertLevel : rule.getAlertLevel())
                            .alertContent(content)
                            .channelType(channel.getChannelType())
                            .channelId(channel.getId())
                            .sendStatus(sendResult.isSuccess() ? "SUCCESS" : "FAILED")
                            .errorMessage(sendResult.getMessage())
                            .createdTime(Long.valueOf(now))
                            .sentTime(sendResult.isSuccess() ? Long.valueOf(now + sendResult.getElapsedMillis()) : null)
                            .build();
                    alertHistoryRepository.save(history);

                    AlertDedupState dedupState = AlertDedupState.builder()
                            .dedupKey(dedupKey)
                            .ruleId(rule.getId())
                            .alertType(alertType)
                            .taskId(taskId)
                            .tableName(tableName)
                            .channelType(channel.getChannelType())
                            .channelId(channel.getId())
                            .lastAlertId(history.getAlertId())
                            .lastContentHash(Integer.toHexString(content == null ? 0 : content.hashCode()))
                            .lastSentTime(Long.valueOf(now))
                            .cooldownUntil(Long.valueOf(now + Math.max(60, safeCooldownSeconds(rule)) * 1000L))
                            .createdAt(Long.valueOf(now))
                            .updatedAt(Long.valueOf(now))
                            .build();
                    alertDedupStateRepository.save(dedupState);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to send alert", ex);
        }
    }

    private String renderAlertContent(AlertRule rule, String alertContent, Long taskId, String runId, String tableName) {
        String template = rule == null ? null : rule.getAlertContentTemplate();
        if (template == null || template.trim().length() == 0) {
            return alertContent;
        }
        String rendered = template;
        rendered = rendered.replace("${taskId}", taskId == null ? "-" : String.valueOf(taskId));
        rendered = rendered.replace("${runId}", runId == null ? "-" : runId);
        rendered = rendered.replace("${tableName}", tableName == null ? "-" : tableName);
        rendered = rendered.replace("${content}", alertContent == null ? "-" : alertContent);
        return rendered;
    }

    private int safeCooldownSeconds(AlertRule rule) {
        if (rule == null || rule.getCooldownSeconds() == null || rule.getCooldownSeconds().intValue() <= 0) {
            return DEFAULT_ALERT_COOLDOWN_SECONDS;
        }
        return rule.getCooldownSeconds().intValue();
    }

    private String buildDedupKey(String alertType, Long taskId, String tableName, AlertChannelType channelType, Long channelId) {
        return String.valueOf(alertType) + ":" + String.valueOf(taskId) + ":" + String.valueOf(tableName) + ":" + String.valueOf(channelType) + ":" + String.valueOf(channelId);
    }

    private List<Long> parseChannelIds(String channelIdsJson) {
        List<Long> result = new ArrayList<Long>();
        if (channelIdsJson == null || channelIdsJson.trim().length() == 0) {
            return result;
        }
        String cleaned = channelIdsJson.replace("[", "").replace("]", "");
        String[] parts = cleaned.split(",");
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String value = part.trim();
            if (value.length() == 0) {
                continue;
            }
            try {
                result.add(Long.valueOf(Long.parseLong(value)));
            } catch (NumberFormatException ignored) {
                // skip invalid id
            }
        }
        return result;
    }

    private AlertChannel sanitizeAlertChannel(AlertChannel channel) {
        if (channel == null) {
            return null;
        }
        AlertChannel sanitized = new AlertChannel();
        sanitized.setId(channel.getId());
        sanitized.setChannelName(channel.getChannelName());
        sanitized.setChannelType(channel.getChannelType());
        sanitized.setEnabled(channel.getEnabled());
        sanitized.setSmtpHost(channel.getSmtpHost());
        sanitized.setSmtpPort(channel.getSmtpPort());
        sanitized.setSmtpUsername(channel.getSmtpUsername());
        sanitized.setSmtpPassword(maskSecret(channel.getSmtpPassword()));
        sanitized.setSmtpToAddress(channel.getSmtpToAddress());
        sanitized.setSmtpFromAddress(channel.getSmtpFromAddress());
        sanitized.setWebhookUrl(channel.getWebhookUrl());
        sanitized.setWebhookToken(maskSecret(channel.getWebhookToken()));
        sanitized.setCreatedAt(channel.getCreatedAt());
        sanitized.setUpdatedAt(channel.getUpdatedAt());
        return sanitized;
    }

    private void mergeAlertChannelSecrets(AlertChannel target, AlertChannel existing) {
        if (target == null || existing == null) {
            return;
        }
        if (isBlank(target.getSmtpPassword()) || isMaskedSecret(target.getSmtpPassword())) {
            target.setSmtpPassword(existing.getSmtpPassword());
        }
        if (isBlank(target.getWebhookToken()) || isMaskedSecret(target.getWebhookToken())) {
            target.setWebhookToken(existing.getWebhookToken());
        }
    }

    private String maskSecret(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    private boolean isMaskedSecret(String value) {
        return value != null && value.contains("****");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private void captureDatasourceConnectionMetric(final DatasourceConfig config, final ConnectionTestResult result) {
        if (monitoringRepository == null || config == null || config.getId() == null || result == null) {
            return;
        }
        submitMonitoringWrite(new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    Optional<DatasourceConnectionMetric> latest = monitoringRepository
                            .findLatestDatasourceConnectionMetricByDatasourceId(config.getId().longValue());
                    int sampleCount = latest.isPresent() ? 2 : 1;
                    double totalCost = result.getCostMillis();
                    Long lastSuccessTime = result.isSuccess() ? Long.valueOf(now)
                            : (latest.isPresent() ? latest.get().getLastSuccessTime() : null);
                    Long lastFailureTime = result.isSuccess()
                            ? (latest.isPresent() ? latest.get().getLastFailureTime() : null)
                            : Long.valueOf(now);
                    if (latest.isPresent() && latest.get().getAverageTestConnectionMillis() != null) {
                        totalCost += latest.get().getAverageTestConnectionMillis().doubleValue();
                    }
                    DatasourceConnectionMetric metric = DatasourceConnectionMetric.builder()
                            .datasourceId(config.getId())
                            .connectionStatus(result.isSuccess() ? "SUCCESS" : "FAILED")
                            .lastSuccessTime(lastSuccessTime)
                            .lastFailureTime(lastFailureTime)
                            .failureReason(result.isSuccess() ? null : result.getMessage())
                            .averageTestConnectionMillis(Double.valueOf(totalCost / sampleCount))
                            .lastTestConnectionMillis(Long.valueOf(result.getCostMillis()))
                            .metricTime(Long.valueOf(now))
                            .build();
                    monitoringRepository.saveDatasourceConnectionMetric(metric);
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "Failed to persist datasource connection metric", ex);
                }
            }
        });
    }

    private void captureTaskRunMetric(final SyncTask task, final String runId, final String errorMessage) {
        if (monitoringRepository == null || task == null || task.getId() == null) {
            return;
        }
        final SyncTask metricTask = copyTask(task);
        submitMonitoringWrite(new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    TaskRunMetricSummary summary = monitoringRepository.summarizeTaskMetricsForToday(startOfDay(now),
                            startOfDay(now) + 24L * 60L * 60L * 1000L);
                    TaskRunMetric metric = TaskRunMetric.builder()
                            .runId(resolveMetricRunId(runId, metricTask))
                            .taskId(metricTask.getId())
                            .metricTime(Long.valueOf(now))
                            .successRowCount(metricTask.getSuccessRowCount())
                            .failedRowCount(metricTask.getFailedRowCount())
                            .speedRowsPerSecond(metricTask.getSpeedRowsPerSecond())
                            .latencyMillis(resolveLatencyMillis(metricTask))
                            .durationMillis(metricTask.getDurationMillis())
                            .errorMessage(trimToNull(errorMessage))
                            .runningTaskCount(Integer.valueOf(countRunningTasks()))
                            .todayTaskCount(summary == null ? Integer.valueOf(0) : summary.getTotalTaskCount())
                            .todaySuccessTaskCount(summary == null ? Integer.valueOf(0) : summary.getSuccessTaskCount())
                            .todayFailedTaskCount(summary == null ? Integer.valueOf(0) : summary.getFailedTaskCount())
                            .build();
                    monitoringRepository.saveTaskRunMetric(metric);
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "Failed to persist task run metric", ex);
                }
            }
        });
    }

    private void captureTableRunMetric(final SyncTableRun tableRun, final Integer batchCount, final Integer retryCount,
                                       final String lastCheckpoint, final String lastError) {
        if (monitoringRepository == null || tableRun == null || tableRun.getTaskId() == null
                || tableRun.getRunId() == null || tableRun.getTaskTableId() == null) {
            return;
        }
        submitMonitoringWrite(new Runnable() {
            @Override
            public void run() {
                try {
                    TableRunMetric metric = TableRunMetric.builder()
                            .tableTaskId(tableRun.getTaskTableId())
                            .taskId(tableRun.getTaskId())
                            .runId(tableRun.getRunId())
                            .tableName(tableRun.getSourceTableName())
                            .syncedRowCount(tableRun.getSyncedRowCount())
                            .successRowCount(tableRun.getSuccessRowCount())
                            .failedRowCount(tableRun.getFailedRowCount())
                            .speedRowsPerSecond(tableRun.getSpeedRowsPerSecond())
                            .batchCount(batchCount)
                            .retryCount(retryCount)
                            .lastCheckpoint(lastCheckpoint)
                            .lastError(lastError)
                            .metricTime(Long.valueOf(System.currentTimeMillis()))
                            .build();
                    monitoringRepository.saveTableRunMetric(metric);
                } catch (SQLException ex) {
                    LOGGER.log(Level.FINE, "Failed to persist table run metric", ex);
                }
            }
        });
    }

    private void submitMonitoringWrite(Runnable task) {
        if (monitoringRepository == null || task == null) {
            return;
        }
        taskExecutor.submit(task);
    }

    private int countRunningTasks() {
        try {
            int count = 0;
            for (SyncTask task : syncTaskRepository.findAll()) {
                if (task != null && task.getTaskStatus() == SyncTaskStatus.RUNNING) {
                    count++;
                }
            }
            return count;
        } catch (SQLException ex) {
            LOGGER.log(Level.FINE, "Failed to count running tasks for monitoring", ex);
            return taskExecutionStates.size();
        }
    }

    private long startOfDay(long currentTimeMillis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private Long resolveLatencyMillis(SyncTask task) {
        if (task == null || task.getDurationMillis() == null || task.getSyncedRowCount() == null
                || task.getSyncedRowCount().longValue() <= 0L) {
            return null;
        }
        return Long.valueOf(task.getDurationMillis().longValue() / Math.max(1L, task.getSyncedRowCount().longValue()));
    }

    private String resolveMetricRunId(String runId, SyncTask task) {
        String normalizedRunId = trimToNull(runId);
        if (normalizedRunId != null) {
            return normalizedRunId;
        }
        if (task == null || task.getId() == null) {
            return "task-run";
        }
        return "task-" + task.getId();
    }

    private SyncTask copyTask(SyncTask source) {
        SyncTask copy = new SyncTask();
        copy.setId(source.getId());
        copy.setTaskName(source.getTaskName());
        copy.setSourceDatasourceId(source.getSourceDatasourceId());
        copy.setTargetDatasourceId(source.getTargetDatasourceId());
        copy.setSyncMode(source.getSyncMode());
        copy.setTaskStatus(source.getTaskStatus());
        copy.setSourceSchemaName(source.getSourceSchemaName());
        copy.setSourceTableName(source.getSourceTableName());
        copy.setTargetSchemaName(source.getTargetSchemaName());
        copy.setTargetTableName(source.getTargetTableName());
        copy.setTotalRowCount(source.getTotalRowCount());
        copy.setSyncedRowCount(source.getSyncedRowCount());
        copy.setSuccessRowCount(source.getSuccessRowCount());
        copy.setFailedRowCount(source.getFailedRowCount());
        copy.setSpeedRowsPerSecond(source.getSpeedRowsPerSecond());
        copy.setStartedAt(source.getStartedAt());
        copy.setEndedAt(source.getEndedAt());
        copy.setDurationMillis(source.getDurationMillis());
        copy.setProgressMessage(source.getProgressMessage());
        return copy;
    }

    private SyncTask loadTask(long taskId) throws SQLException {
        Optional<SyncTask> task = syncTaskRepository.findById(taskId);
        if (!task.isPresent()) {
            throw new SQLException("Sync task not found: " + taskId);
        }
        return applyIncrementalCheckpoint(task.get());
    }

    private DatasourceConfig loadDatasource(long datasourceId, String messagePrefix) throws SQLException {
        Optional<DatasourceConfig> datasource = datasourceRepository.findById(datasourceId);
        if (!datasource.isPresent()) {
            throw new SQLException(messagePrefix + datasourceId);
        }
        return datasource.get();
    }

    private SyncTask applyIncrementalCheckpoint(SyncTask task) throws SQLException {
        if (task == null) {
            return null;
        }
        Optional<IncrementalSyncCheckpointEntry> checkpoint = incrementalCheckpointRepository.findByTaskId(task.getId().longValue());
        if (!checkpoint.isPresent()) {
            task.setIncrementalCheckpointMode(null);
            task.setIncrementalCheckpointValue(null);
            task.setIncrementalCheckpointUpdatedAt(null);
            return task;
        }
        IncrementalSyncCheckpointEntry entry = checkpoint.get();
        task.setIncrementalCheckpointMode(entry.getCheckpointMode());
        task.setIncrementalCheckpointValue(entry.getCheckpointValue());
        task.setIncrementalCheckpointUpdatedAt(entry.getUpdatedAt());
        return task;
    }
}
