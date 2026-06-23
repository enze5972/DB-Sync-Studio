package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.core.connection.JdbcDatasourceConnectionTester;
import com.dbsyncstudio.core.connection.DatasourceConnectionTester;
import com.dbsyncstudio.core.connection.DefaultDatasourceConnectionOpener;
import com.dbsyncstudio.model.alert.vo.AlertSendResult;
import com.dbsyncstudio.core.alert.AlertSenderService;
import com.dbsyncstudio.core.metadata.DatabaseMetadataScanner;
import com.dbsyncstudio.core.metadata.JdbcDatabaseMetadataScanner;
import com.dbsyncstudio.core.mapping.FieldMappingSuggestionMatcher;
import com.dbsyncstudio.core.transform.TransformContext;
import com.dbsyncstudio.core.transform.TransformEngine;
import com.dbsyncstudio.core.transform.TransformPlan;
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
import com.dbsyncstudio.model.datasource.vo.ConnectionTestResultVO;
import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;
import com.dbsyncstudio.store.repository.DatasourceRepository;
import com.dbsyncstudio.model.datasource.DatasourceType;
import com.dbsyncstudio.model.alert.entity.AlertChannelDO;
import com.dbsyncstudio.store.repository.AlertChannelRepository;
import com.dbsyncstudio.model.alert.AlertChannelType;
import com.dbsyncstudio.model.alert.entity.AlertDedupStateDO;
import com.dbsyncstudio.store.repository.AlertDedupStateRepository;
import com.dbsyncstudio.model.alert.entity.AlertHistoryEntryDO;
import com.dbsyncstudio.store.repository.AlertHistoryRepository;
import com.dbsyncstudio.model.alert.entity.AlertRuleDO;
import com.dbsyncstudio.store.repository.AlertRuleRepository;
import com.dbsyncstudio.model.monitoring.entity.DatasourceConnectionMetricDO;
import com.dbsyncstudio.model.monitoring.vo.BackendDashboardStatsVO;
import com.dbsyncstudio.model.monitoring.vo.BackendDiagnosticsVO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringCleanupSummaryVO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringOverviewVO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringTrendPointVO;
import com.dbsyncstudio.model.monitoring.vo.MonitoringTrendVO;
import com.dbsyncstudio.model.monitoring.entity.TaskRunMetricDO;
import com.dbsyncstudio.model.monitoring.vo.TaskRunMetricSummaryVO;
import com.dbsyncstudio.model.monitoring.entity.TableRunMetricDO;
import com.dbsyncstudio.model.preview.dto.DataPreviewFilterDTO;
import com.dbsyncstudio.model.preview.DataPreviewFilterOperator;
import com.dbsyncstudio.model.preview.dto.DataPreviewRequestDTO;
import com.dbsyncstudio.model.preview.vo.DataPreviewResultVO;
import com.dbsyncstudio.store.repository.FieldMappingRepository;
import com.dbsyncstudio.model.sync.entity.FieldMappingRuleDO;
import com.dbsyncstudio.model.sync.vo.FieldMappingSuggestionVO;
import com.dbsyncstudio.model.sync.entity.ExecutionLogEntryDO;
import com.dbsyncstudio.store.repository.ExecutionLogRepository;
import com.dbsyncstudio.model.sync.TransformErrorStrategy;
import com.dbsyncstudio.model.sync.entity.TransformRuleDO;
import com.dbsyncstudio.model.sync.dto.TransformTestRequestDTO;
import com.dbsyncstudio.model.sync.vo.TransformTestResultVO;
import com.dbsyncstudio.model.sync.entity.SyncRunDO;
import com.dbsyncstudio.model.sync.entity.SyncRunLogEntryDO;
import com.dbsyncstudio.store.repository.SyncRunLogRepository;
import com.dbsyncstudio.store.repository.SyncRunRepository;
import com.dbsyncstudio.model.sync.dto.FullSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.FullSyncResultVO;
import com.dbsyncstudio.model.sync.entity.IncrementalSyncCheckpointEntryDO;
import com.dbsyncstudio.model.sync.dto.IncrementalSyncRequestDTO;
import com.dbsyncstudio.model.sync.vo.IncrementalSyncResultVO;
import com.dbsyncstudio.model.sync.IncrementalSyncMode;
import com.dbsyncstudio.model.sync.SyncMode;
import com.dbsyncstudio.model.sync.entity.SyncTableRunDO;
import com.dbsyncstudio.store.repository.SyncTableRunRepository;
import com.dbsyncstudio.model.sync.entity.SyncTaskTableDO;
import com.dbsyncstudio.store.repository.SyncTaskTableRepository;
import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;
import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.schema.entity.SchemaComparisonHistoryEntryDO;
import com.dbsyncstudio.store.repository.SchemaComparisonHistoryRepository;
import com.dbsyncstudio.model.schema.dto.SchemaComparisonRequestDTO;
import com.dbsyncstudio.model.schema.vo.SchemaComparisonResultVO;
import com.dbsyncstudio.model.schema.entity.SchemaDiffEntryDO;
import com.dbsyncstudio.model.schema.dto.SchemaSqlPreviewRequestDTO;
import com.dbsyncstudio.model.schema.vo.SchemaSqlPreviewResultVO;
import com.dbsyncstudio.model.validation.entity.RepairDetailDO;
import com.dbsyncstudio.model.validation.dto.RepairRequestDTO;
import com.dbsyncstudio.model.validation.vo.RepairResultVO;
import com.dbsyncstudio.model.validation.entity.RepairRunDO;
import com.dbsyncstudio.model.validation.RepairType;
import com.dbsyncstudio.model.validation.entity.ValidationDifferenceDO;
import com.dbsyncstudio.model.validation.ValidationMode;
import com.dbsyncstudio.model.validation.dto.ValidationRequestDTO;
import com.dbsyncstudio.model.validation.vo.ValidationResultVO;
import com.dbsyncstudio.model.validation.entity.ValidationRunDO;
import com.dbsyncstudio.model.sync.entity.SyncTaskDO;
import com.dbsyncstudio.store.repository.SyncTaskRepository;
import com.dbsyncstudio.model.sync.SyncTaskStatus;
import com.dbsyncstudio.store.repository.IncrementalSyncCheckpointRepository;
import com.dbsyncstudio.model.sync.vo.LogCleanupSummaryVO;
import com.dbsyncstudio.model.sync.dto.LogCleanupDTO;
import com.dbsyncstudio.model.sync.dto.TaskBatchRunDTO;
import com.dbsyncstudio.model.sync.dto.TaskBatchTableDTO;
import com.dbsyncstudio.model.sync.vo.SyncRunDetailVO;
import com.dbsyncstudio.model.sync.entity.SyncCheckpointDO;
import com.dbsyncstudio.model.sql.entity.SqlExecutionLogEntryDO;
import com.dbsyncstudio.store.repository.SqlExecutionLogRepository;
import com.dbsyncstudio.model.sql.dto.SqlExecutionRequestDTO;
import com.dbsyncstudio.model.sql.vo.SqlExecutionResultVO;
import com.dbsyncstudio.model.settings.vo.AppSettingsVO;
import com.dbsyncstudio.store.sqlite.DatabaseConnectionFactory;
import com.dbsyncstudio.store.sqlite.LocalSecretCryptoService;
import com.dbsyncstudio.store.sqlite.LocalSecretKeyProvider;
import com.dbsyncstudio.store.repository.sqlite.AppSettingsRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.DatasourceRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.FieldMappingRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.FieldTransformRuleRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.AlertChannelRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.AlertDedupStateRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.AlertHistoryRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.AlertRuleRepositoryImpl;
import com.dbsyncstudio.store.sqlite.DatabasePaths;
import com.dbsyncstudio.store.repository.sqlite.RepairRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncRunLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncRunRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTableRunRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskTableRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.MonitoringRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SqlExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.ValidationRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SchemaComparisonHistoryRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncTaskRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.ExecutionLogRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.IncrementalSyncCheckpointRepositoryImpl;
import com.dbsyncstudio.store.repository.sqlite.SyncCheckpointRepositoryImpl;
import com.dbsyncstudio.store.sqlite.DatabasePaths;
import com.dbsyncstudio.model.settings.entity.AppSettingsDO;
import com.dbsyncstudio.store.repository.AppSettingsRepository;
import com.dbsyncstudio.model.settings.vo.AppBuildInfoVO;
import com.dbsyncstudio.model.settings.vo.AppLicenseInfoVO;
import com.dbsyncstudio.model.settings.vo.AppUpdateCheckResultVO;

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

    private final DatasourceRepositoryImpl datasourceRepository;
    private final SyncTaskRepositoryImpl syncTaskRepository;
    private final ExecutionLogRepositoryImpl executionLogRepository;
    private final SyncCheckpointRepositoryImpl checkpointRepository;
    private final FieldMappingRepositoryImpl fieldMappingRepository;
    private final SyncTaskTableRepositoryImpl syncTaskTableRepository;
    private final SyncRunRepositoryImpl syncRunRepository;
    private final SyncTableRunRepositoryImpl syncTableRunRepository;
    private final SyncRunLogRepositoryImpl syncRunLogRepository;
    private final SqlExecutionLogRepositoryImpl sqlExecutionLogRepository;
    private final SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository;
    private final IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository;
    private final ValidationRepositoryImpl validationRepository;
    private final RepairRepositoryImpl repairRepository;
    private final MonitoringRepositoryImpl monitoringRepository;
    private FieldTransformRuleRepositoryImpl transformRuleRepository;
    private final AlertRuleRepositoryImpl alertRuleRepository;
    private final AlertChannelRepositoryImpl alertChannelRepository;
    private final AlertHistoryRepositoryImpl alertHistoryRepository;
    private final AlertDedupStateRepositoryImpl alertDedupStateRepository;
    private final AppSettingsRepositoryImpl appSettingsRepository;
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
    private final TransformEngine transformEngine = new TransformEngine();
    private final ExecutorService taskExecutor;
    private final Map<Long, TaskExecutionState> taskExecutionStates;
    private DatabaseConnectionFactory connectionFactory;
    private volatile int lastRecoveredTaskCount;
    private volatile AppSettingsDO cachedSettings;

    private LicenseService licenseService() {
        if (appSettingsRepository == null) {
            return null;
        }
        return new LicenseService(appSettingsRepository, alertCryptoService);
    }

    public DesktopBackendService(DatasourceRepositoryImpl datasourceRepository,
                                 SyncTaskRepositoryImpl syncTaskRepository,
                                 ExecutionLogRepositoryImpl executionLogRepository,
                                 SyncCheckpointRepositoryImpl checkpointRepository,
                                 FieldMappingRepositoryImpl fieldMappingRepository,
                                 SyncTaskTableRepositoryImpl syncTaskTableRepository,
                                 SyncRunRepositoryImpl syncRunRepository,
                                 SyncTableRunRepositoryImpl syncTableRunRepository,
                                 SyncRunLogRepositoryImpl syncRunLogRepository,
                                 SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                                 SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                                 IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
                                 ValidationRepositoryImpl validationRepository,
                                 RepairRepositoryImpl repairRepository,
                                 MonitoringRepositoryImpl monitoringRepository,
                                 AlertRuleRepositoryImpl alertRuleRepository,
                                 AlertChannelRepositoryImpl alertChannelRepository,
                                 AlertHistoryRepositoryImpl alertHistoryRepository,
                                 AlertDedupStateRepositoryImpl alertDedupStateRepository,
                                 AppSettingsRepositoryImpl appSettingsRepository,
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
        this.appSettingsRepository = appSettingsRepository;
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

    public DesktopBackendService(DatasourceRepositoryImpl datasourceRepository,
                                 SyncTaskRepositoryImpl syncTaskRepository,
                                 ExecutionLogRepositoryImpl executionLogRepository,
                                 SyncCheckpointRepositoryImpl checkpointRepository,
                                 FieldMappingRepositoryImpl fieldMappingRepository,
                                 SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                                 SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                                 IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
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

    public DesktopBackendService(DatasourceRepositoryImpl datasourceRepository,
                                 SyncTaskRepositoryImpl syncTaskRepository,
                                 ExecutionLogRepositoryImpl executionLogRepository,
                                 SyncCheckpointRepositoryImpl checkpointRepository,
                                 FieldMappingRepositoryImpl fieldMappingRepository,
                                 SyncTaskTableRepositoryImpl syncTaskTableRepository,
                                 SyncRunRepositoryImpl syncRunRepository,
                                 SyncTableRunRepositoryImpl syncTableRunRepository,
                                 SyncRunLogRepositoryImpl syncRunLogRepository,
                                 SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                                 SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                                 IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
                                 ValidationRepositoryImpl validationRepository,
                                 RepairRepositoryImpl repairRepository,
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

    public DesktopBackendService(DatasourceRepositoryImpl datasourceRepository,
                                 SyncTaskRepositoryImpl syncTaskRepository,
                                 ExecutionLogRepositoryImpl executionLogRepository,
                                 SyncCheckpointRepositoryImpl checkpointRepository,
                                 FieldMappingRepositoryImpl fieldMappingRepository,
                                 SyncTaskTableRepositoryImpl syncTaskTableRepository,
                                 SyncRunRepositoryImpl syncRunRepository,
                                 SyncTableRunRepositoryImpl syncTableRunRepository,
                                 SyncRunLogRepositoryImpl syncRunLogRepository,
                                 SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                                 SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                                 IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
                                 ValidationRepositoryImpl validationRepository,
                                 RepairRepositoryImpl repairRepository,
                                 MonitoringRepositoryImpl monitoringRepository,
                                 AlertRuleRepositoryImpl alertRuleRepository,
                                 AlertChannelRepositoryImpl alertChannelRepository,
                                 AlertHistoryRepositoryImpl alertHistoryRepository,
                                 AlertDedupStateRepositoryImpl alertDedupStateRepository,
                                 AppSettingsRepositoryImpl appSettingsRepository,
                                 LocalSecretCryptoService alertCryptoService,
                                 DatabaseMetadataScanner metadataScanner,
                                 DatasourceConnectionTester connectionTester,
                                 FieldMappingSuggestionMatcher fieldMappingSuggestionMatcher,
                                 SchemaComparisonEngine schemaComparisonEngine,
                                 DataValidationEngine dataValidationEngine,
                                 DataRepairEngine dataRepairEngine,
                                 JdbcFullSyncEngine fullSyncEngine,
                                 JdbcIncrementalSyncEngine incrementalSyncEngine,
                                 DatabaseConnectionFactory connectionFactory) {
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
                appSettingsRepository,
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

    public DesktopBackendService(DatasourceRepositoryImpl datasourceRepository,
                                 SyncTaskRepositoryImpl syncTaskRepository,
                                 ExecutionLogRepositoryImpl executionLogRepository,
                                 SyncCheckpointRepositoryImpl checkpointRepository,
                                 FieldMappingRepositoryImpl fieldMappingRepository,
                                 SyncTaskTableRepositoryImpl syncTaskTableRepository,
                                 SyncRunRepositoryImpl syncRunRepository,
                                 SyncTableRunRepositoryImpl syncTableRunRepository,
                                 SyncRunLogRepositoryImpl syncRunLogRepository,
                                 SqlExecutionLogRepositoryImpl sqlExecutionLogRepository,
                                 SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository,
                                 IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository,
                                 ValidationRepositoryImpl validationRepository,
                                 RepairRepositoryImpl repairRepository,
                                 MonitoringRepositoryImpl monitoringRepository,
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
        this.appSettingsRepository = null;
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
        DatabaseConnectionFactory connectionFactory = new DatabaseConnectionFactory(databaseFile);
        JdbcDatabaseMetadataScanner metadataScanner = new JdbcDatabaseMetadataScanner();
        DefaultDatasourceConnectionOpener connectionOpener = new DefaultDatasourceConnectionOpener();
        DatasourceRepositoryImpl datasourceRepository = new DatasourceRepositoryImpl(connectionFactory);
        SyncTaskRepositoryImpl syncTaskRepository = new SyncTaskRepositoryImpl(connectionFactory);
        ExecutionLogRepositoryImpl executionLogRepository = new ExecutionLogRepositoryImpl(connectionFactory);
        SyncCheckpointRepositoryImpl checkpointRepository = new SyncCheckpointRepositoryImpl(connectionFactory);
        FieldMappingRepositoryImpl fieldMappingRepository = new FieldMappingRepositoryImpl(connectionFactory);
        SyncTaskTableRepositoryImpl syncTaskTableRepository = new SyncTaskTableRepositoryImpl(connectionFactory);
        SyncRunRepositoryImpl syncRunRepository = new SyncRunRepositoryImpl(connectionFactory);
        SyncTableRunRepositoryImpl syncTableRunRepository = new SyncTableRunRepositoryImpl(connectionFactory);
        SyncRunLogRepositoryImpl syncRunLogRepository = new SyncRunLogRepositoryImpl(connectionFactory);
        SqlExecutionLogRepositoryImpl sqlExecutionLogRepository = new SqlExecutionLogRepositoryImpl(connectionFactory);
        SchemaComparisonHistoryRepositoryImpl schemaComparisonHistoryRepository = new SchemaComparisonHistoryRepositoryImpl(connectionFactory);
        IncrementalSyncCheckpointRepositoryImpl incrementalCheckpointRepository = new IncrementalSyncCheckpointRepositoryImpl(connectionFactory);
        ValidationRepositoryImpl validationRepository = new ValidationRepositoryImpl(connectionFactory);
        RepairRepositoryImpl repairRepository = new RepairRepositoryImpl(connectionFactory);
        MonitoringRepositoryImpl monitoringRepository = new MonitoringRepositoryImpl(connectionFactory);
        FieldTransformRuleRepositoryImpl transformRuleRepository = new FieldTransformRuleRepositoryImpl(connectionFactory);
        AppSettingsRepositoryImpl appSettingsRepository = new AppSettingsRepositoryImpl(connectionFactory);
        LocalSecretCryptoService alertCryptoService =
                new LocalSecretCryptoService(new LocalSecretKeyProvider(DatabasePaths.defaultAlertSecretKeyFile()));
        AlertRuleRepositoryImpl alertRuleRepository = new AlertRuleRepositoryImpl(connectionFactory);
        AlertChannelRepositoryImpl alertChannelRepository = new AlertChannelRepositoryImpl(connectionFactory, alertCryptoService);
        AlertHistoryRepositoryImpl alertHistoryRepository = new AlertHistoryRepositoryImpl(connectionFactory);
        AlertDedupStateRepositoryImpl alertDedupStateRepository = new AlertDedupStateRepositoryImpl(connectionFactory);

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
        transformRuleRepository.initialize();
        appSettingsRepository.initialize();
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
                appSettingsRepository,
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
        service.transformRuleRepository = transformRuleRepository;
        service.lastRecoveredTaskCount = service.recoverUnfinishedTasks();
        return service;
    }

    public List<DatasourceConfigDO> listDatasources() throws SQLException {
        return datasourceRepository.findAll();
    }

    public DatasourceConfigDO saveDatasource(DatasourceConfigDO config) throws SQLException {
        validateDatasource(config);
        datasourceRepository.save(config);
        return config;
    }

    public boolean deleteDatasource(long id) throws SQLException {
        return datasourceRepository.deleteById(id);
    }

    public Optional<DatasourceConfigDO> findDatasourceById(long id) throws SQLException {
        return datasourceRepository.findById(id);
    }

    public ConnectionTestResultVO testConnection(DatasourceConfigDO config) {
        validateDatasource(config);
        ConnectionTestResultVO result = connectionTester.test(config);
        captureDatasourceConnectionMetric(config, result);
        if (!result.isSuccess()) {
            triggerAlert("DATASOURCE_CONNECTION_FAILED", null, null, null, "ERROR",
                    result.getMessage(), null, config.getId());
        }
        return result;
    }

    public List<SyncTaskDO> listTasks() throws SQLException {
        List<SyncTaskDO> tasks = syncTaskRepository.findAll();
        List<SyncTaskDO> result = new ArrayList<SyncTaskDO>();
        for (SyncTaskDO task : tasks) {
            SyncTaskDO loadedTask = applyIncrementalCheckpoint(task);
            if (syncTaskTableRepository != null) {
                loadedTask.setTaskTables(syncTaskTableRepository.findByTaskId(task.getId().longValue()));
            }
            result.add(loadedTask);
        }
        return result;
    }

    public Optional<SyncTaskDO> findTaskById(long taskId) throws SQLException {
        Optional<SyncTaskDO> task = syncTaskRepository.findById(taskId);
        if (!task.isPresent()) {
            return task;
        }
        SyncTaskDO loadedTask = applyIncrementalCheckpoint(task.get());
        if (syncTaskTableRepository != null) {
            loadedTask.setTaskTables(syncTaskTableRepository.findByTaskId(taskId));
        }
        return Optional.of(loadedTask);
    }

    public SyncTaskDO saveTask(SyncTaskDO task) throws SQLException {
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

    public List<ExecutionLogEntryDO> listLogs(Long taskId) {
        if (taskId == null) {
            return new ArrayList<ExecutionLogEntryDO>();
        }
        return executionLogRepository.findByTaskId(taskId.longValue());
    }

    public void appendTaskLog(long taskId, String logLevel, String message) {
        appendLog(taskId, logLevel, message);
    }

    public List<ExecutionLogEntryDO> listAllLogs(List<SyncTaskDO> tasks) {
        List<ExecutionLogEntryDO> result = new ArrayList<ExecutionLogEntryDO>();
        for (SyncTaskDO task : tasks) {
            result.addAll(executionLogRepository.findByTaskId(task.getId().longValue()));
        }
        return result;
    }

    public List<ExecutionLogEntryDO> listTaskScheduleHistory(long taskId) {
        List<ExecutionLogEntryDO> entries = executionLogRepository.findByTaskId(taskId);
        List<ExecutionLogEntryDO> result = new ArrayList<ExecutionLogEntryDO>();
        for (ExecutionLogEntryDO entry : entries) {
            String message = entry.getLogMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("scheduled execution")) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<SyncTaskTableDO> listTaskTables(long taskId) throws SQLException {
        if (syncTaskTableRepository == null) {
            return new ArrayList<SyncTaskTableDO>();
        }
        return syncTaskTableRepository.findByTaskId(taskId);
    }

    public SyncTaskTableDO saveTaskTable(long taskId, SyncTaskTableDO taskTable) throws SQLException {
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

    private void saveTaskTables(SyncTaskDO task) throws SQLException {
        if (syncTaskTableRepository == null || task == null || task.getId() == null) {
            return;
        }
        List<SyncTaskTableDO> taskTables = task.getTaskTables();
        if (taskTables == null) {
            return;
        }
        List<SyncTaskTableDO> existingTaskTables = syncTaskTableRepository.findByTaskId(task.getId().longValue());
        for (SyncTaskTableDO existingTaskTable : existingTaskTables) {
            if (existingTaskTable != null && existingTaskTable.getId() != null) {
                syncTaskTableRepository.deleteById(existingTaskTable.getId().longValue());
            }
        }
        for (int i = 0; i < taskTables.size(); i++) {
            SyncTaskTableDO taskTable = taskTables.get(i);
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

    public List<SyncRunDO> listSyncRuns(Long taskId, int limit) throws SQLException {
        if (syncRunRepository == null) {
            return new ArrayList<SyncRunDO>();
        }
        int safeLimit = limit <= 0 ? 20 : limit;
        if (taskId == null) {
            return syncRunRepository.findRecent(safeLimit);
        }
        List<SyncRunDO> runs = syncRunRepository.findByTaskId(taskId.longValue());
        if (runs.size() <= safeLimit) {
            return runs;
        }
        return new ArrayList<SyncRunDO>(runs.subList(0, safeLimit));
    }

    public Optional<SyncRunDO> findSyncRunById(long syncRunId) throws SQLException {
        if (syncRunRepository == null) {
            return Optional.empty();
        }
        return syncRunRepository.findById(syncRunId);
    }

    public Optional<SyncRunDO> findSyncRunByRunId(String runId) throws SQLException {
        if (syncRunRepository == null) {
            return Optional.empty();
        }
        return syncRunRepository.findByRunId(runId);
    }

    public List<SyncTableRunDO> listSyncTableRuns(long syncRunId) throws SQLException {
        if (syncTableRunRepository == null) {
            return new ArrayList<SyncTableRunDO>();
        }
        return syncTableRunRepository.findBySyncRunId(syncRunId);
    }

    public List<SyncRunLogEntryDO> listSyncRunLogs(Long taskId, String runId, Long syncRunId, Long syncTableRunId,
                                                 String tableName, String logLevel, String keyword,
                                                 Long startTime, Long endTime, int limit) throws SQLException {
        if (syncRunLogRepository == null) {
            return new ArrayList<SyncRunLogEntryDO>();
        }
        int safeLimit = limit <= 0 ? 100 : limit;
        List<SyncRunLogEntryDO> candidates = loadSyncRunLogCandidates(taskId, runId, syncRunId, syncTableRunId, safeLimit);
        List<SyncRunLogEntryDO> result = new ArrayList<SyncRunLogEntryDO>();
        String normalizedTableName = trimToNull(tableName);
        String normalizedLogLevel = trimToNull(logLevel);
        String normalizedKeyword = trimToNull(keyword);
        for (SyncRunLogEntryDO entry : candidates) {
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

    public SyncRunDetailVO loadSyncRunDetail(long taskId, String runId, int limit) throws SQLException {
        SyncRunDO run = findSyncRunByRunId(runId).orElse(null);
        if (run == null) {
            throw new SQLException("Sync run not found: " + runId);
        }
        if (run.getTaskId() == null || run.getTaskId().longValue() != taskId) {
            throw new SQLException("Sync run does not belong to task: " + taskId);
        }
        List<SyncTableRunDO> tableRuns = listSyncTableRuns(run.getId().longValue());
        List<SyncRunLogEntryDO> logs = listSyncRunLogs(Long.valueOf(taskId), runId, run.getId(), null, null, null, null, null, null, limit);
        SyncRunDetailVO detail = new SyncRunDetailVO();
        detail.setRun(run);
        detail.setTableRuns(tableRuns);
        detail.setLogs(logs);
        return detail;
    }

    public List<SqlExecutionLogEntryDO> listSqlExecutionLogs(int limit) {
        return sqlExecutionLogRepository.findRecent(limit);
    }

    public int getLogRetentionDays() {
        AppSettingsDO settings = loadAppSettings();
        Integer value = settings.getLogRetentionDays();
        return value == null ? DEFAULT_LOG_RETENTION_DAYS : Math.max(1, value.intValue());
    }

    public int updateLogRetentionDays(int retentionDays) {
        int safeDays = Math.max(1, retentionDays);
        AppSettingsDO settings = loadAppSettings();
        settings.setLogRetentionDays(Integer.valueOf(safeDays));
        saveAppSettings(settings);
        return safeDays;
    }

    public AppSettingsDO loadAppSettings() {
        if (appSettingsRepository == null) {
            return defaultAppSettings();
        }
        if (cachedSettings == null) {
            cachedSettings = mergeWithDefaults(appSettingsRepository.load());
        }
        return copySettings(cachedSettings);
    }

    public AppSettingsVO loadAppSettingsResponse() {
        AppSettingsDO settings = loadAppSettings();
        AppBuildInfoVO buildInfo = RuntimeUtils.buildInfo(com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer.currentSchemaVersion(), readGitCommit());
        AppSettingsVO response = new AppSettingsVO();
        response.setBuildInfo(buildInfo);
        response.setSettings(settings);
        response.setApplicationDirectory(EnvironmentUtils.appDirectory().getAbsolutePath());
        response.setLogsDirectory(EnvironmentUtils.logsDirectory().getAbsolutePath());
        response.setDatabaseFilePath(connectionFactory == null || connectionFactory.getDatabaseFile() == null
                ? null
                : connectionFactory.getDatabaseFile().getAbsolutePath());
        try {
            response.setDatabaseUserVersion(readDatabaseUserVersion());
            response.setMigrationEntryCount(readMigrationEntryCount());
        } catch (SQLException ex) {
            response.setDatabaseUserVersion(Integer.valueOf(-1));
            response.setMigrationEntryCount(Integer.valueOf(-1));
        }
        response.setSchemaVersion(com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer.currentSchemaVersion());
        return response;
    }

    public AppSettingsDO saveAppSettings(AppSettingsDO settings) {
        AppSettingsDO merged = mergeWithDefaults(settings);
        if (appSettingsRepository != null) {
            appSettingsRepository.save(merged);
        }
        cachedSettings = copySettings(merged);
        return copySettings(merged);
    }

    public AppLicenseInfoVO loadLicenseInfo() {
        LicenseService service = licenseService();
        if (service == null) {
            return AppLicenseInfoVO.builder().message("未初始化").build();
        }
        return service.loadLicenseInfo();
    }

    public AppLicenseInfoVO activateLicense(String licenseKey) {
        LicenseService service = licenseService();
        if (service == null) {
            return AppLicenseInfoVO.builder().message("未初始化").build();
        }
        return service.activateLicense(licenseKey);
    }

    public AppLicenseInfoVO clearLicense() {
        LicenseService service = licenseService();
        if (service == null) {
            return AppLicenseInfoVO.builder().message("未初始化").build();
        }
        return service.clearLicense();
    }

    public AppUpdateCheckResultVO checkForUpdate() {
        LicenseService service = licenseService();
        if (service == null) {
            return AppUpdateCheckResultVO.builder().message("未初始化").available(false).latest(false).build();
        }
        return service.checkForUpdate();
    }

    public LogCleanupSummaryVO cleanupLogs(Integer retentionDays) throws SQLException {
        int safeDays = retentionDays == null ? getLogRetentionDays() : Math.max(1, retentionDays.intValue());
        long cutoffTime = System.currentTimeMillis() - (long) safeDays * 24L * 60L * 60L * 1000L;
        List<SyncTaskDO> tasks = syncTaskRepository.findAll();
        List<Long> runningTaskIds = new ArrayList<Long>();
        for (SyncTaskDO task : tasks) {
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
        return LogCleanupSummaryVO.builder()
                .retentionDays(Integer.valueOf(safeDays))
                .cutoffTime(Long.valueOf(cutoffTime))
                .executionLogDeletedCount(Integer.valueOf(executionDeleted))
                .syncRunLogDeletedCount(Integer.valueOf(syncRunDeleted))
                .sqlExecutionLogDeletedCount(Integer.valueOf(sqlDeleted))
                .build();
    }

    public List<SchemaComparisonHistoryEntryDO> listSchemaComparisonHistory(int limit) {
        return schemaComparisonHistoryRepository.findRecent(limit);
    }

    public List<AlertRuleDO> listAlertRules() throws SQLException {
        if (alertRuleRepository == null) {
            return new ArrayList<AlertRuleDO>();
        }
        return alertRuleRepository.findAll();
    }

    public List<AlertChannelDO> listAlertChannels() throws SQLException {
        if (alertChannelRepository == null) {
            return new ArrayList<AlertChannelDO>();
        }
        List<AlertChannelDO> channels = alertChannelRepository.findAll();
        List<AlertChannelDO> result = new ArrayList<AlertChannelDO>();
        for (AlertChannelDO channel : channels) {
            result.add(sanitizeAlertChannel(channel));
        }
        return result;
    }

    public List<AlertHistoryEntryDO> listAlertHistory(int limit) throws SQLException {
        return listAlertHistory(null, null, null, null, null, null, limit);
    }

    public List<AlertHistoryEntryDO> listAlertHistory(Long taskId, String alertType, String sendStatus,
                                                    Long startTime, Long endTime, String keyword, int limit) throws SQLException {
        if (alertHistoryRepository == null) {
            return new ArrayList<AlertHistoryEntryDO>();
        }
        List<AlertHistoryEntryDO> history = alertHistoryRepository.findAll();
        List<AlertHistoryEntryDO> result = new ArrayList<AlertHistoryEntryDO>();
        int safeLimit = limit <= 0 ? 50 : limit;
        String normalizedAlertType = trimToNull(alertType);
        String normalizedSendStatus = trimToNull(sendStatus);
        String normalizedKeyword = trimToNull(keyword);
        for (AlertHistoryEntryDO entry : history) {
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

    public AlertRuleDO saveAlertRule(AlertRuleDO rule) throws SQLException {
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

    public AlertChannelDO saveAlertChannel(AlertChannelDO channel) throws SQLException {
        if (channel == null) {
            throw new IllegalArgumentException("Alert channel must not be null");
        }
        if (channel.getEnabled() == null) {
            channel.setEnabled(Boolean.TRUE);
        }
        if (channel.getId() != null && alertChannelRepository != null) {
            AlertChannelDO existing = alertChannelRepository.findById(channel.getId().longValue()).orElse(null);
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
        AlertChannelDO channel = alertChannelRepository.findById(channelId)
                .orElseThrow(() -> new SQLException("Alert channel not found: " + channelId));
        AlertSendResult result = alertSenderService.send(channel,
                "DB Sync Studio Alert Test",
                content == null ? "This is a test alert from DB Sync Studio." : content);
        AlertHistoryEntryDO history = AlertHistoryEntryDO.builder()
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

    public ValidationResultVO runValidation(ValidationRequestDTO request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Validation request must not be null");
        }
        if (request.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        SyncTaskDO task = loadTask(request.getTaskId().longValue());
        DatasourceConfigDO source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
        DatasourceConfigDO target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
        ValidationRequestDTO effective = ValidationRequestDTO.builder()
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
        Long tableTaskId = resolveTransformTableTaskId(task.getId(), task.getSourceSchemaName(), task.getSourceTableName(),
                task.getTargetSchemaName(), task.getTargetTableName());
        TransformPlan transformPlan = loadTransformPlan(task.getId(), tableTaskId);
        ValidationResultVO result = dataValidationEngine.validate(effective, transformPlan);
        validationRepository.saveRun(result.getRun());
        for (ValidationDifferenceDO difference : result.getDifferences()) {
            validationRepository.saveDifference(difference);
        }
        if (result.getRun() != null && result.getRun().getInconsistentCount() != null
                && result.getRun().getInconsistentCount().longValue() > 0L) {
            triggerAlert("VALIDATION_INCONSISTENT", task.getId(), result.getRun().getRunId(), task.getSourceTableName(),
                    "WARNING", "Validation found inconsistent rows", null, null);
        }
        return result;
    }

    public void notifyValidationInconsistent(ValidationRunDO run, String tableName) {
        if (run == null) {
            return;
        }
        triggerAlert("VALIDATION_INCONSISTENT", run.getTaskId(), run.getRunId(), tableName,
                "WARNING", "Validation found inconsistent rows", null, null);
    }

    public void handleScheduledSkip(long taskId, String message) {
        try {
            SyncTaskDO task = loadTask(taskId);
            triggerAlert("SCHEDULE_SKIPPED", task.getId(), null, task.getSourceTableName(),
                    "WARNING", message == null ? "Scheduled execution skipped" : message, null, null);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Failed to handle scheduled skip alert", ex);
        }
    }

    public List<ValidationRunDO> listValidationRuns(Long taskId, int limit) throws SQLException {
        if (taskId == null) {
            return validationRepository.findRecentRuns(limit);
        }
        return validationRepository.findRecentRunsByTaskId(taskId.longValue(), limit);
    }

    public List<ValidationDifferenceDO> listValidationDifferences(long validationRunId) {
        return validationRepository.findDifferencesByRunId(validationRunId);
    }

    public RepairResultVO runRepair(RepairRequestDTO request) throws SQLException {
        if (request == null) {
            throw new IllegalArgumentException("Repair request must not be null");
        }
        if (request.getValidationRunId() == null) {
            throw new IllegalArgumentException("Validation run id must not be null");
        }
        ValidationRunDO validationRun = validationRepository.findRunById(request.getValidationRunId().longValue()).orElse(null);
        if (validationRun == null) {
            throw new SQLException("Validation run not found: " + request.getValidationRunId());
        }
        SyncTaskDO task = loadTask(validationRun.getTaskId().longValue());
        DatasourceConfigDO source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
        DatasourceConfigDO target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
        List<ValidationDifferenceDO> allDifferences = validationRepository.findDifferencesByRunId(validationRun.getId().longValue());
        List<ValidationDifferenceDO> selectedDifferences = filterDifferences(allDifferences, request.getValidationDifferenceIds());
        RepairRequestDTO effective = RepairRequestDTO.builder()
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
        Long tableTaskId = resolveTransformTableTaskId(task.getId(), task.getSourceSchemaName(), task.getSourceTableName(),
                task.getTargetSchemaName(), task.getTargetTableName());
        TransformPlan transformPlan = loadTransformPlan(task.getId(), tableTaskId);
        RepairResultVO result = dataRepairEngine.repair(effective, selectedDifferences, transformPlan);
        long repairRunId = repairRepository.saveRun(result.getRun());
        List<RepairDetailDO> persistedDetails = new ArrayList<RepairDetailDO>();
        for (RepairDetailDO detail : result.getDetails()) {
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

    public void notifyRepairFailed(RepairRunDO run, String tableName) {
        if (run == null) {
            return;
        }
        triggerAlert("REPAIR_FAILED", run.getTaskId(), run.getRunId(), tableName,
                "ERROR", "Repair failed", null, null);
    }

    public List<RepairRunDO> listRepairRuns(Long validationRunId, int limit) {
        if (validationRunId == null) {
            return repairRepository.findRecentRuns(limit);
        }
        return repairRepository.findRecentRunsByValidationRunId(validationRunId.longValue(), limit);
    }

    public List<RepairDetailDO> listRepairDetails(long repairRunId) {
        return repairRepository.findDetailsByRunId(repairRunId);
    }

    private List<ValidationDifferenceDO> filterDifferences(List<ValidationDifferenceDO> differences, List<Long> selectedIds) {
        if (differences == null || differences.isEmpty()) {
            return new ArrayList<ValidationDifferenceDO>();
        }
        if (selectedIds == null || selectedIds.isEmpty()) {
            return new ArrayList<ValidationDifferenceDO>(differences);
        }
        List<ValidationDifferenceDO> result = new ArrayList<ValidationDifferenceDO>();
        for (ValidationDifferenceDO difference : differences) {
            if (difference != null && difference.getId() != null && selectedIds.contains(difference.getId())) {
                result.add(difference);
            }
        }
        return result;
    }

    private Long resolveTransformTableTaskId(Long taskId, String sourceSchemaName, String sourceTableName,
                                             String targetSchemaName, String targetTableName) throws SQLException {
        if (syncTaskTableRepository == null || taskId == null) {
            return null;
        }
        List<SyncTaskTableDO> taskTables = syncTaskTableRepository.findByTaskId(taskId.longValue());
        for (SyncTaskTableDO taskTable : taskTables) {
            if (taskTable == null) {
                continue;
            }
            if (matchesTableName(taskTable.getSourceSchemaName(), sourceSchemaName)
                    && matchesTableName(taskTable.getSourceTableName(), sourceTableName)
                    && matchesTableName(taskTable.getTargetSchemaName(), targetSchemaName)
                    && matchesTableName(taskTable.getTargetTableName(), targetTableName)) {
                return taskTable.getId();
            }
        }
        return null;
    }

    private boolean matchesTableName(String left, String right) {
        String normalizedLeft = trimToNull(left);
        String normalizedRight = trimToNull(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return normalizedLeft == null && normalizedRight == null;
        }
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    private List<String> resolvePrimaryKeyColumns(DatasourceConfigDO datasource, String schemaName, String tableName) throws SQLException {
        List<SchemaMetadataDO> schemas = metadataScanner.scan(datasource);
        TableMetadataDO tableMetadata = findTableMetadata(schemas, schemaName, tableName);
        if (tableMetadata == null) {
            return new ArrayList<String>();
        }
        List<String> columns = new ArrayList<String>();
        if (tableMetadata.getColumns() != null) {
            for (ColumnMetadataDO column : tableMetadata.getColumns()) {
                if (column != null && column.isPrimaryKey() && column.getName() != null) {
                    columns.add(column.getName());
                }
            }
        }
        return columns;
    }

    private void executeTask(long taskId, TaskExecutionState state) {
        SyncTaskDO task = null;
        boolean preserveBatchState = false;
        try {
            task = loadTask(taskId);
            DatasourceConfigDO source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
            DatasourceConfigDO target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
            task.setTaskStatus(SyncTaskStatus.RUNNING);
            task.setStartedAt(task.getStartedAt() == null ? Long.valueOf(System.currentTimeMillis()) : task.getStartedAt());
            task.setEndedAt(null);
            task.setProgressMessage("Task running");
            syncTaskRepository.save(task);

            SyncTaskProgressListener listener = state.createListener(task);
            TaskBatchRunDTO batchRunRequest = state.getBatchRunRequest();
            if (batchRunRequest != null) {
                runBatchTask(task, source, target, listener, state, batchRunRequest);
            } else if (task.getSyncMode() == SyncMode.INCREMENTAL) {
                IncrementalSyncResultVO result = incrementalSync(task, source, target, listener);
                appendLog(taskId, "INFO", "Incremental sync inserted " + result.getInsertedRowCount() + " rows");
            } else {
                FullSyncResultVO result = fullSync(task, source, target, listener);
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

    private FullSyncResultVO fullSync(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target,
                                    SyncTaskProgressListener listener) throws SQLException {
        return fullSync(task, source, target, task.getSourceSchemaName(), task.getSourceTableName(),
                task.getTargetSchemaName(), task.getTargetTableName(),
                "task-" + task.getId() + "-full", resolveFullCheckpointValue("task-" + task.getId() + "-full"), listener, 500, null);
    }

    private FullSyncResultVO fullSync(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target,
                                    String sourceSchemaName, String sourceTableName,
                                    String targetSchemaName, String targetTableName,
                                    String checkpointKey, String checkpointValue,
                                    SyncTaskProgressListener listener, int batchSize, Long tableTaskId) throws SQLException {
        FullSyncRequestDTO request = FullSyncRequestDTO.builder()
                .taskId(task.getId())
                .tableTaskId(tableTaskId)
                .runId(checkpointKey)
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
        TransformPlan transformPlan = loadTransformPlan(task.getId(), tableTaskId);
        if (transformPlan == null || transformPlan.isEmpty()) {
            return fullSyncEngine.sync(request, listener);
        }
        return fullSyncEngine.sync(request, listener, transformPlan);
    }

    private IncrementalSyncResultVO incrementalSync(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target,
                                                   SyncTaskProgressListener listener) throws SQLException {
        return incrementalSync(task, source, target, task.getSourceSchemaName(), task.getSourceTableName(),
                task.getTargetSchemaName(), task.getTargetTableName(), "task-" + task.getId(),
                resolveCheckpointValue(task.getId()), listener, task.getIncrementalMode(),
                resolveIncrementalColumnName(task, task.getIncrementalMode() == null ? IncrementalSyncMode.TIMESTAMP : task.getIncrementalMode()),
                trimToNull(task.getIncrementalTieBreakerColumnName()),
                trimToNull(task.getIncrementalCompositeColumnName()), 500, null);
    }

    private IncrementalSyncResultVO incrementalSync(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target,
                                                   String sourceSchemaName, String sourceTableName,
                                                   String targetSchemaName, String targetTableName,
                                                   String checkpointKey, String checkpointValue,
                                                   SyncTaskProgressListener listener,
                                                   IncrementalSyncMode incrementalMode,
                                                   String incrementalColumnName,
                                                   String incrementalTieBreakerColumnName,
                                                   String incrementalCompositeColumnName,
                                                   int batchSize, Long tableTaskId) throws SQLException {
        IncrementalSyncRequestDTO request = IncrementalSyncRequestDTO.builder()
                .taskId(task.getId())
                .tableTaskId(tableTaskId)
                .runId(checkpointKey)
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
        TransformPlan transformPlan = loadTransformPlan(task.getId(), tableTaskId);
        if (transformPlan == null || transformPlan.isEmpty()) {
            return incrementalSyncEngine.sync(request, listener);
        }
        return incrementalSyncEngine.sync(request, listener, transformPlan);
    }

    private void runBatchTask(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target,
                              SyncTaskProgressListener listener, TaskExecutionState state,
                              TaskBatchRunDTO batchRunRequest) throws SQLException {
        List<TaskBatchTableDTO> tables = batchRunRequest.getTables();
        String batchRunId = state.getBatchRunId();
        SyncRunDO syncRun = createSyncRun(task, batchRunId, tables.size());
        long syncRunId = syncRun == null || syncRun.getId() == null ? -1L : syncRun.getId().longValue();
        int concurrency = Math.max(1, batchRunRequest.getMaxConcurrency() == null ? 3 : batchRunRequest.getMaxConcurrency().intValue());
        ExecutorService batchExecutor = Executors.newFixedThreadPool(concurrency);
        ExecutorCompletionService<BatchTableExecutionResult> completionService = new ExecutorCompletionService<BatchTableExecutionResult>(batchExecutor);
        List<Future<BatchTableExecutionResult>> futures = new ArrayList<Future<BatchTableExecutionResult>>();
        final AtomicBoolean pauseOrStopRequested = new AtomicBoolean(false);
        try {
            for (int i = 0; i < tables.size(); i++) {
                final int tableIndex = i;
                final TaskBatchTableDTO tableRequest = tables.get(i);
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

    private BatchTableExecutionResult executeBatchTable(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target,
                                                        SyncTaskProgressListener listener, long syncRunId, String batchRunId,
                                                        TaskBatchTableDTO tableRequest, int tableIndex) throws SQLException {
        String sourceSchemaName = trimToNull(tableRequest.getSourceSchemaName());
        String targetSchemaName = trimToNull(tableRequest.getTargetSchemaName());
        String sourceTableName = tableRequest.getSourceTableName();
        String targetTableName = tableRequest.getTargetTableName();
        String tableRunCheckpointKey = batchRunId + "-" + (tableIndex + 1);
        SyncTaskTableDO taskTable = resolveTaskTable(task.getId().longValue(), tableRequest, tableIndex);
        SyncTableRunDO tableRun = createSyncTableRun(syncRunId, task, taskTable, tableRequest, tableIndex, batchRunId);
        SyncMode effectiveSyncMode = resolveEffectiveSyncMode(task, taskTable, tableRequest);
        IncrementalSyncMode effectiveIncrementalMode = resolveEffectiveIncrementalMode(task, taskTable, tableRequest);
        String effectiveIncrementalColumnName = resolveEffectiveIncrementalColumnName(task, taskTable, tableRequest, effectiveIncrementalMode);
        String effectiveIncrementalTieBreakerColumnName = resolveEffectiveIncrementalTieBreakerColumnName(task, taskTable, tableRequest);
        String effectiveIncrementalCompositeColumnName = resolveEffectiveIncrementalCompositeColumnName(task, taskTable, tableRequest);
        int effectiveBatchSize = resolveEffectiveBatchSize(taskTable, tableRequest);
        Long tableTaskId = taskTable == null ? null : taskTable.getId();
        appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                sourceTableName, "INFO", "Batch sync started for " + sourceTableName + " -> " + targetTableName);
        try {
            if (SyncMode.INCREMENTAL == effectiveSyncMode) {
                Long batchTaskId = Long.valueOf(task.getId().longValue() * 1000L + tableIndex + 1L);
                IncrementalSyncResultVO result = incrementalSync(task, source, target,
                        sourceSchemaName, sourceTableName, targetSchemaName, targetTableName,
                        tableRunCheckpointKey, resolveCheckpointValue(batchTaskId), listener,
                        effectiveIncrementalMode, effectiveIncrementalColumnName,
                        effectiveIncrementalTieBreakerColumnName, effectiveIncrementalCompositeColumnName,
                        effectiveBatchSize, tableTaskId);
                updateSyncTableRunSuccess(tableRun, result, tableRunCheckpointKey);
                appendRunLog(Long.valueOf(syncRunId), tableRun == null ? null : tableRun.getId(), task.getId().longValue(), batchRunId,
                        sourceTableName, "INFO", "Batch incremental sync inserted " + result.getInsertedRowCount()
                                + " rows for " + sourceTableName);
                markSyncTableRunStatus(tableRun, "SUCCESS", null, null);
                return BatchTableExecutionResult.success(tableIndex, tableRun, result.getSourceRowCount(),
                        result.getInsertedRowCount(), result.getInsertedRowCount(), 0L);
            }
            FullSyncResultVO result = fullSync(task, source, target,
                    sourceSchemaName, sourceTableName, targetSchemaName, targetTableName,
                    tableRunCheckpointKey, resolveFullCheckpointValue(tableRunCheckpointKey), listener, effectiveBatchSize, tableTaskId);
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

    private SyncTaskTableDO resolveTaskTable(long taskId, TaskBatchTableDTO tableRequest, int tableOrder) throws SQLException {
        if (syncTaskTableRepository == null) {
            return null;
        }
        List<SyncTaskTableDO> taskTables = syncTaskTableRepository.findByTaskId(taskId);
        for (SyncTaskTableDO taskTable : taskTables) {
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

    private SyncRunDO createSyncRun(SyncTaskDO task, String runId, int totalTableCount) throws SQLException {
        if (syncRunRepository == null) {
            return null;
        }
        SyncRunDO run = new SyncRunDO();
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

    private SyncTableRunDO createSyncTableRun(long syncRunId, SyncTaskDO task, SyncTaskTableDO taskTable,
                                            TaskBatchTableDTO tableRequest, int tableOrder, String runId) throws SQLException {
        if (syncTableRunRepository == null) {
            return null;
        }
        SyncTableRunDO tableRun = SyncTableRunDO.builder()
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

    private void updateSyncTableRunSuccess(SyncTableRunDO tableRun, FullSyncResultVO result, String checkpointValue) throws SQLException {
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

    private void updateSyncTableRunSuccess(SyncTableRunDO tableRun, IncrementalSyncResultVO result, String checkpointValue) throws SQLException {
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

    private void markSyncTableRunStatus(SyncTableRunDO tableRun, String status, String errorMessage, String progressMessage) throws SQLException {
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

    private void applySyncRunProgress(SyncRunDO syncRun, long sourceRowCount, long syncedRowCount, long successRowCount, long failedRowCount) throws SQLException {
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

    private void markSyncRunStatus(SyncRunDO syncRun, String status, String message) throws SQLException {
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
        SyncRunLogEntryDO entry = new SyncRunLogEntryDO();
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

    private void updateBatchTaskStatus(SyncTaskDO task, String finalStatus, SyncRunDO syncRun) throws SQLException {
        if (task == null) {
            return;
        }
        SyncTaskDO currentTask = loadTask(task.getId().longValue());
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

    private SyncMode resolveEffectiveSyncMode(SyncTaskDO task, SyncTaskTableDO taskTable, TaskBatchTableDTO tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getSyncMode());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getSyncMode());
        }
        if (value == null) {
            return task == null || task.getSyncMode() == null ? SyncMode.FULL : task.getSyncMode();
        }
        return SyncMode.valueOf(value);
    }

    private IncrementalSyncMode resolveEffectiveIncrementalMode(SyncTaskDO task, SyncTaskTableDO taskTable, TaskBatchTableDTO tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalMode());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalMode());
        }
        if (value == null) {
            return task == null || task.getIncrementalMode() == null ? IncrementalSyncMode.TIMESTAMP : task.getIncrementalMode();
        }
        return IncrementalSyncMode.valueOf(value);
    }

    private String resolveEffectiveIncrementalColumnName(SyncTaskDO task, SyncTaskTableDO taskTable, TaskBatchTableDTO tableRequest,
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

    private String resolveEffectiveIncrementalTieBreakerColumnName(SyncTaskDO task, SyncTaskTableDO taskTable, TaskBatchTableDTO tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalTieBreakerColumnName());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalTieBreakerColumnName());
        }
        if (value == null && task != null) {
            value = trimToNull(task.getIncrementalTieBreakerColumnName());
        }
        return value;
    }

    private String resolveEffectiveIncrementalCompositeColumnName(SyncTaskDO task, SyncTaskTableDO taskTable, TaskBatchTableDTO tableRequest) {
        String value = trimToNull(tableRequest == null ? null : tableRequest.getIncrementalCompositeColumnName());
        if (value == null) {
            value = trimToNull(taskTable == null ? null : taskTable.getIncrementalCompositeColumnName());
        }
        if (value == null && task != null) {
            value = trimToNull(task.getIncrementalCompositeColumnName());
        }
        return value;
    }

    private int resolveEffectiveBatchSize(SyncTaskTableDO taskTable, TaskBatchTableDTO tableRequest) {
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
        private final SyncTableRunDO tableRun;
        private final String tableStatus;
        private final long sourceRowCount;
        private final long syncedRowCount;
        private final long successRowCount;
        private final long failedRowCount;

        private BatchTableExecutionResult(int tableIndex, SyncTableRunDO tableRun, String tableStatus,
                                          long sourceRowCount, long syncedRowCount, long successRowCount, long failedRowCount) {
            this.tableIndex = tableIndex;
            this.tableRun = tableRun;
            this.tableStatus = tableStatus;
            this.sourceRowCount = sourceRowCount;
            this.syncedRowCount = syncedRowCount;
            this.successRowCount = successRowCount;
            this.failedRowCount = failedRowCount;
        }

        private static BatchTableExecutionResult success(int tableIndex, SyncTableRunDO tableRun,
                                                         long sourceRowCount, long syncedRowCount,
                                                         long successRowCount, long failedRowCount) {
            return new BatchTableExecutionResult(tableIndex, tableRun, "SUCCESS",
                    sourceRowCount, syncedRowCount, successRowCount, failedRowCount);
        }
    }

    private List<SyncRunLogEntryDO> loadSyncRunLogCandidates(Long taskId, String runId, Long syncRunId, Long syncTableRunId, int limit) throws SQLException {
        if (syncRunLogRepository == null) {
            return new ArrayList<SyncRunLogEntryDO>();
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

    private boolean matchesSyncRunLogIdentifiers(SyncRunLogEntryDO entry, Long taskId, String runId, Long syncRunId, Long syncTableRunId) {
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
        Optional<com.dbsyncstudio.model.sync.entity.IncrementalSyncCheckpointEntryDO> checkpoint = incrementalCheckpointRepository.findByTaskId(taskId.longValue());
        if (!checkpoint.isPresent()) {
            return null;
        }
        return checkpoint.get().getCheckpointValue();
    }

    private String resolveFullCheckpointValue(String checkpointKey) {
        Optional<com.dbsyncstudio.model.sync.entity.SyncCheckpointDO> checkpoint = checkpointRepository.findByKey(checkpointKey);
        if (!checkpoint.isPresent()) {
            return null;
        }
        return checkpoint.get().getCheckpointValue();
    }

    private String resolveIncrementalColumnName(SyncTaskDO task, IncrementalSyncMode incrementalMode) {
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

    private SyncCheckpointDO loadOrCreateCheckpoint(String checkpointKey, String defaultValue) {
        Optional<SyncCheckpointDO> checkpoint = checkpointRepository.findByKey(checkpointKey);
        if (checkpoint.isPresent()) {
            return checkpoint.get();
        }
        SyncCheckpointDO created = new SyncCheckpointDO();
        created.setCheckpointKey(checkpointKey);
        created.setCheckpointValue(defaultValue);
        created.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
        checkpointRepository.save(created);
        return created;
    }

    private void setNextScheduleRunAt(SyncTaskDO task, long baseTime) {
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
        private volatile TaskBatchRunDTO batchRunRequest;
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

        private void setBatchRunRequest(TaskBatchRunDTO batchRunRequest) {
            this.batchRunRequest = batchRunRequest;
            this.batchTableIndex = 0;
        }

        private TaskBatchRunDTO getBatchRunRequest() {
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

        private SyncTaskProgressListener createListener(final SyncTaskDO task) {
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
                        SyncTaskDO currentTask = loadTask(taskId);
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
                    com.dbsyncstudio.model.sync.entity.SyncCheckpointDO checkpoint = new com.dbsyncstudio.model.sync.entity.SyncCheckpointDO();
                    checkpoint.setCheckpointKey(checkpointKey);
                    checkpoint.setCheckpointValue(checkpointValue);
                    checkpoint.setUpdatedAt(Long.valueOf(System.currentTimeMillis()));
                    checkpointRepository.save(checkpoint);
                }
            };
        }
    }

    public SyncTaskDO runBatchTask(long taskId, TaskBatchRunDTO request) throws SQLException {
        SyncTaskDO task = loadTask(taskId);
        TaskBatchRunDTO batchRunRequest = validateBatchRunRequest(request);
        if ((batchRunRequest.getTables() == null || batchRunRequest.getTables().isEmpty()) && syncTaskTableRepository != null) {
            List<SyncTaskTableDO> taskTables = syncTaskTableRepository.findByTaskId(taskId);
            List<TaskBatchTableDTO> tables = new ArrayList<TaskBatchTableDTO>();
            for (SyncTaskTableDO taskTable : taskTables) {
                if (taskTable == null) {
                    continue;
                }
                if (taskTable.getEnabled() != null && !taskTable.getEnabled().booleanValue()) {
                    continue;
                }
                TaskBatchTableDTO tableRequest = new TaskBatchTableDTO();
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

    private TaskBatchRunDTO validateBatchRunRequest(TaskBatchRunDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Batch run request must not be null");
        }
        if (request.getTables() != null) {
            for (TaskBatchTableDTO tableRequest : request.getTables()) {
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

    private void normalizeTaskTableConfig(SyncTaskTableDO taskTable, SyncTaskDO task) {
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

    private void validatePreviewRequest(DataPreviewRequestDTO request) {
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

    private TableMetadataDO findTableMetadata(List<SchemaMetadataDO> schemas, String schemaName, String tableName) {
        if (tableName == null || tableName.trim().length() == 0) {
            return null;
        }
        String normalizedTableName = tableName.trim();
        String copyFallbackTableName = normalizeCopyTableName(normalizedTableName);
        TableMetadataDO fallback = null;
        for (SchemaMetadataDO schemaMetadata : schemas) {
            if (schemaMetadata.getTables() == null) {
                continue;
            }
            boolean schemaMatches = schemaName == null || schemaName.trim().length() == 0
                    || schemaName.equalsIgnoreCase(schemaMetadata.getSchemaName());
            for (TableMetadataDO tableMetadata : schemaMetadata.getTables()) {
                if (tableMetadata.getTableName() == null) {
                    continue;
                }
                boolean tableMatches = normalizedTableName.equalsIgnoreCase(tableMetadata.getTableName())
                        || (copyFallbackTableName != null && copyFallbackTableName.equalsIgnoreCase(tableMetadata.getTableName()));
                if (tableMatches) {
                    if (schemaMatches) {
                        return tableMetadata;
                    }
                    if (fallback == null) {
                        fallback = tableMetadata;
                    }
                }
            }
        }
        return fallback;
    }

    private String normalizeCopyTableName(String tableName) {
        String lowerCaseName = tableName.toLowerCase();
        if (!lowerCaseName.endsWith("_copy") || tableName.length() <= 5) {
            return null;
        }
        return tableName.substring(0, tableName.length() - 5);
    }

    private String buildPreviewSelectSql(DatasourceConfigDO datasource, TableMetadataDO tableMetadata, DataPreviewRequestDTO request) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        List<ColumnMetadataDO> columns = tableMetadata.getColumns();
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

    private String buildPreviewWhereClause(List<ColumnMetadataDO> columns, DataPreviewRequestDTO request) {
        if (request.getFilters() == null || request.getFilters().isEmpty()) {
            return "";
        }
        List<String> clauses = new ArrayList<String>();
        for (DataPreviewFilterDTO filter : request.getFilters()) {
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

    private String buildPreviewOrderByClause(List<ColumnMetadataDO> columns) {
        for (ColumnMetadataDO columnMetadata : columns) {
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

    private void bindPreviewParameters(PreparedStatement statement, DataPreviewRequestDTO request) throws SQLException {
        int index = bindPreviewFilterParameters(statement, request);
        statement.setInt(index, request.getPageSize());
        statement.setInt(index + 1, (request.getPageNumber() - 1) * request.getPageSize());
    }

    private int bindPreviewFilterParameters(PreparedStatement statement, DataPreviewRequestDTO request) throws SQLException {
        int index = 1;
        if (request.getFilters() != null) {
            for (DataPreviewFilterDTO filter : request.getFilters()) {
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

    private String resolveAllowedColumn(List<ColumnMetadataDO> columns, String columnName) {
        for (ColumnMetadataDO columnMetadata : columns) {
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

    private void validateSqlRequest(SqlExecutionRequestDTO request) {
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

    private void saveSchemaComparisonHistory(SchemaComparisonRequestDTO request, SchemaComparisonResultVO result) {
        SchemaComparisonHistoryEntryDO entry = new SchemaComparisonHistoryEntryDO();
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

    public static String buildSchemaComparisonHistorySummary(SchemaComparisonResultVO result) {
        List<SchemaDiffEntryDO> diffEntries = result == null || result.getDiffEntries() == null
                ? Collections.<SchemaDiffEntryDO>emptyList()
                : result.getDiffEntries();
        List<String> sqlList = result == null || result.getSuggestedSqlList() == null
                ? Collections.<String>emptyList()
                : result.getSuggestedSqlList();

        Map<String, Integer> diffTypeCounts = new LinkedHashMap<String, Integer>();
        for (SchemaDiffEntryDO entry : diffEntries) {
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

    private void validateSchemaSqlRequest(SchemaSqlPreviewRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Schema SQL request must not be null");
        }
        validateDatasource(request.getDatasource());
        if (request.getSql() == null || request.getSql().trim().length() == 0) {
            throw new IllegalArgumentException("SQL must not be blank");
        }
    }

    public SyncTaskDO runTask(long taskId) throws SQLException {
        return startTask(taskId);
    }

    public SyncTaskDO startTask(long taskId) throws SQLException {
        SyncTaskDO task = loadTask(taskId);
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

    public SyncTaskDO pauseTask(long taskId) throws SQLException {
        TaskExecutionState state = ensureTaskState(taskId);
        state.pause();
        SyncTaskDO task = loadTask(taskId);
        task.setTaskStatus(SyncTaskStatus.PAUSED);
        task.setProgressMessage("Pause requested");
        syncTaskRepository.save(task);
        appendLog(taskId, "INFO", "Pause requested for task: " + task.getTaskName());
        return task;
    }

    public SyncTaskDO resumeTask(long taskId) throws SQLException {
        SyncTaskDO task = loadTask(taskId);
        if (task.getTaskStatus() != SyncTaskStatus.PAUSED && task.getTaskStatus() != SyncTaskStatus.STOPPED) {
            return task;
        }
        TaskExecutionState state = ensureTaskState(taskId);
        state.clearControlFlags();
        return startTask(taskId);
    }

    public int recoverUnfinishedTasks() throws SQLException {
        List<SyncTaskDO> tasks = syncTaskRepository.findAll();
        int recoveredCount = 0;
        long now = System.currentTimeMillis();
        for (SyncTaskDO task : tasks) {
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

    public SyncTaskDO stopTask(long taskId) throws SQLException {
        TaskExecutionState state = ensureTaskState(taskId);
        state.stop();
        SyncTaskDO task = loadTask(taskId);
        task.setTaskStatus(SyncTaskStatus.STOPPED);
        task.setEndedAt(Long.valueOf(System.currentTimeMillis()));
        task.setProgressMessage("Stop requested");
        syncTaskRepository.save(task);
        appendLog(taskId, "INFO", "Stop requested for task: " + task.getTaskName());
        return task;
    }

    public List<SyncTaskDO> listTasksWithProgress() throws SQLException {
        return listTasks();
    }

    public DataPreviewResultVO previewTableData(DataPreviewRequestDTO request) throws SQLException {
        validatePreviewRequest(request);
        DatasourceConfigDO datasource = request.getDatasource();
        List<SchemaMetadataDO> schemas = metadataScanner.scan(datasource);
        TableMetadataDO tableMetadata = findTableMetadata(schemas, request.getSchemaName(), request.getTableName());
        if (tableMetadata == null) {
            throw new SQLException("Table not found: " + request.getTableName());
        }

        List<ColumnMetadataDO> columns = tableMetadata.getColumns();
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
                for (ColumnMetadataDO columnMetadata : columns) {
                    columnNames.add(columnMetadata.getName());
                }

                DataPreviewResultVO result = new DataPreviewResultVO();
                result.setColumns(columnNames);
                result.setRows(rows);
                result.setTotalRowCount(totalRowCount);
                result.setPageNumber(request.getPageNumber());
                result.setPageSize(request.getPageSize());
                return result;
            }
        }
    }

    public SqlExecutionResultVO executeSql(SqlExecutionRequestDTO request) throws SQLException {
        validateSqlRequest(request);
        long startTime = System.currentTimeMillis();
        DatasourceConfigDO datasource = request.getDatasource();
        String sql = sanitizeSql(request.getSql());
        String statementType = resolveSqlStatementType(sql);
        if (isDangerousStatement(statementType) && !request.isAllowDangerousSql()) {
            throw new SQLException("Dangerous SQL is disabled by default: " + statementType);
        }

        SqlExecutionLogEntryDO logEntry = SqlExecutionLogEntryDO.builder()
                .datasourceId(datasource.getId())
                .sqlText(sql)
                .statementType(statementType)
                .success(false)
                .createdAt(Long.valueOf(startTime))
                .build();

        try (Connection connection = com.dbsyncstudio.core.connection.JdbcConnectionSupport.openConnection(datasource);
             Statement statement = connection.createStatement()) {
            SqlExecutionResultVO result = new SqlExecutionResultVO();
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

    public BackendDashboardStatsVO dashboardStats() throws SQLException {
        List<DatasourceConfigDO> datasources = datasourceRepository.findAll();
        List<SyncTaskDO> tasks = syncTaskRepository.findAll();
        return new BackendDashboardStatsVO(datasources.size(), tasks.size(), listAllLogs(tasks).size());
    }

    public BackendDiagnosticsVO diagnosticsStatus() throws SQLException {
        BackendDiagnosticsVO response = new BackendDiagnosticsVO();
        response.setGeneratedAt(System.currentTimeMillis());
        response.setApplicationDirectory(EnvironmentUtils.appDirectory().getAbsolutePath());
        response.setLogsDirectory(EnvironmentUtils.logsDirectory().getAbsolutePath());
        response.setDatabaseFilePath(connectionFactory == null || connectionFactory.getDatabaseFile() == null
                ? null
                : connectionFactory.getDatabaseFile().getAbsolutePath());
        response.setSchemaVersion(com.dbsyncstudio.store.sqlite.DatabaseSchemaInitializer.currentSchemaVersion());
        response.setDatabaseUserVersion(readDatabaseUserVersion());
        response.setMigrationEntryCount(readMigrationEntryCount());
        List<SyncTaskDO> tasks = syncTaskRepository.findAll();
        response.setTotalTaskCount(tasks.size());
        int unfinishedTaskCount = 0;
        int runningTaskCount = 0;
        for (SyncTaskDO task : tasks) {
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

    public MonitoringOverviewVO monitoringOverview() throws SQLException {
        long now = System.currentTimeMillis();
        long dayStart = startOfDay(now);
        long dayEnd = dayStart + 24L * 60L * 60L * 1000L;
        TaskRunMetricSummaryVO summary = monitoringRepository == null
                ? TaskRunMetricSummaryVO.builder()
                .totalTaskCount(Integer.valueOf(0))
                .successTaskCount(Integer.valueOf(0))
                .failedTaskCount(Integer.valueOf(0))
                .latestRunningTaskCount(Integer.valueOf(taskExecutionStates.size()))
                .build()
                : monitoringRepository.summarizeTaskMetricsForToday(dayStart, dayEnd);
        TaskRunMetricDO latestTaskMetric = null;
        if (monitoringRepository != null) {
            List<TaskRunMetricDO> latestMetrics = monitoringRepository.findTaskRunMetrics(null, null, null, null, 1);
            if (!latestMetrics.isEmpty()) {
                latestTaskMetric = latestMetrics.get(0);
            }
        }
        MonitoringOverviewVO response = new MonitoringOverviewVO();
        response.setSummary(summary);
        response.setLatestTaskMetric(latestTaskMetric);
        return response;
    }

    public List<TaskRunMetricDO> listTaskRunMetrics(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException {
        if (monitoringRepository == null) {
            return new ArrayList<TaskRunMetricDO>();
        }
        return monitoringRepository.findTaskRunMetrics(trimToNull(runId), taskId, startTime, endTime, limit);
    }

    public List<TableRunMetricDO> listTableRunMetrics(String runId, Long taskId, Long tableTaskId, Long startTime,
                                                    Long endTime, int limit) throws SQLException {
        if (monitoringRepository == null) {
            return new ArrayList<TableRunMetricDO>();
        }
        return monitoringRepository.findTableRunMetrics(trimToNull(runId), taskId, tableTaskId, startTime, endTime, limit);
    }

    public List<DatasourceConnectionMetricDO> listDatasourceConnectionMetrics(Long datasourceId, Long startTime, Long endTime,
                                                                            int limit) throws SQLException {
        if (monitoringRepository == null) {
            return new ArrayList<DatasourceConnectionMetricDO>();
        }
        return monitoringRepository.findDatasourceConnectionMetrics(datasourceId, startTime, endTime, limit);
    }

    public MonitoringTrendVO taskRunTrend(String runId, Long taskId, Long startTime, Long endTime, int limit) throws SQLException {
        List<TaskRunMetricDO> metrics = listTaskRunMetrics(runId, taskId, startTime, endTime, limit);
        List<MonitoringTrendPointVO> points = new ArrayList<MonitoringTrendPointVO>();
        for (TaskRunMetricDO metric : metrics) {
            if (metric == null) {
                continue;
            }
            MonitoringTrendPointVO point = new MonitoringTrendPointVO();
            point.setMetricTime(metric.getMetricTime());
            point.setSuccessRowCount(metric.getSuccessRowCount());
            point.setFailedRowCount(metric.getFailedRowCount());
            point.setSpeedRowsPerSecond(metric.getSpeedRowsPerSecond());
            point.setLatencyMillis(metric.getLatencyMillis());
            point.setDurationMillis(metric.getDurationMillis());
            points.add(point);
        }
        Collections.reverse(points);
        MonitoringTrendVO response = new MonitoringTrendVO();
        response.setTaskRunTrend(points);
        return response;
    }

    public MonitoringCleanupSummaryVO cleanupMonitoringMetrics(Integer retentionDays) throws SQLException {
        if (monitoringRepository == null) {
            return MonitoringCleanupSummaryVO.builder()
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

    private AppSettingsDO defaultAppSettings() {
        return AppSettingsDO.builder()
                .logRetentionDays(Integer.valueOf(DEFAULT_LOG_RETENTION_DAYS))
                .monitoringRetentionDays(Integer.valueOf(DEFAULT_MONITORING_RETENTION_DAYS))
                .defaultPageSize(Integer.valueOf(100))
                .defaultSyncBatchSize(Integer.valueOf(500))
                .defaultMaxConcurrency(Integer.valueOf(4))
                .updateSourceUrl("")
                .allowDangerousSql(Boolean.FALSE)
                .restartScheduledTasksOnStartup(Boolean.TRUE)
                .autoCheckUpdatesOnStartup(Boolean.FALSE)
                .onboardingGuideEnabled(Boolean.TRUE)
                .build();
    }

    private AppSettingsDO mergeWithDefaults(AppSettingsDO settings) {
        AppSettingsDO defaults = defaultAppSettings();
        if (settings == null) {
            return defaults;
        }
        AppSettingsDO merged = copySettings(settings);
        if (merged.getLogRetentionDays() == null) {
            merged.setLogRetentionDays(defaults.getLogRetentionDays());
        }
        if (merged.getMonitoringRetentionDays() == null) {
            merged.setMonitoringRetentionDays(defaults.getMonitoringRetentionDays());
        }
        if (merged.getDefaultPageSize() == null) {
            merged.setDefaultPageSize(defaults.getDefaultPageSize());
        }
        if (merged.getDefaultSyncBatchSize() == null) {
            merged.setDefaultSyncBatchSize(defaults.getDefaultSyncBatchSize());
        }
        if (merged.getDefaultMaxConcurrency() == null) {
            merged.setDefaultMaxConcurrency(defaults.getDefaultMaxConcurrency());
        }
        if (merged.getUpdateSourceUrl() == null) {
            merged.setUpdateSourceUrl(defaults.getUpdateSourceUrl());
        }
        if (merged.getAllowDangerousSql() == null) {
            merged.setAllowDangerousSql(defaults.getAllowDangerousSql());
        }
        if (merged.getRestartScheduledTasksOnStartup() == null) {
            merged.setRestartScheduledTasksOnStartup(defaults.getRestartScheduledTasksOnStartup());
        }
        if (merged.getAutoCheckUpdatesOnStartup() == null) {
            merged.setAutoCheckUpdatesOnStartup(defaults.getAutoCheckUpdatesOnStartup());
        }
        if (merged.getOnboardingGuideEnabled() == null) {
            merged.setOnboardingGuideEnabled(defaults.getOnboardingGuideEnabled());
        }
        return merged;
    }

    private AppSettingsDO copySettings(AppSettingsDO settings) {
        if (settings == null) {
            return null;
        }
        return AppSettingsDO.builder()
                .logRetentionDays(settings.getLogRetentionDays())
                .monitoringRetentionDays(settings.getMonitoringRetentionDays())
                .defaultPageSize(settings.getDefaultPageSize())
                .defaultSyncBatchSize(settings.getDefaultSyncBatchSize())
                .defaultMaxConcurrency(settings.getDefaultMaxConcurrency())
                .updateSourceUrl(settings.getUpdateSourceUrl())
                .allowDangerousSql(settings.getAllowDangerousSql())
                .restartScheduledTasksOnStartup(settings.getRestartScheduledTasksOnStartup())
                .autoCheckUpdatesOnStartup(settings.getAutoCheckUpdatesOnStartup())
                .onboardingGuideEnabled(settings.getOnboardingGuideEnabled())
                .build();
    }

    private String readGitCommit() {
        String value = System.getenv("GIT_COMMIT");
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    public List<FieldMappingRuleDO> listFieldMappings(long taskId) throws SQLException {
        return fieldMappingRepository.findByTaskId(taskId);
    }

    public List<FieldMappingSuggestionVO> suggestFieldMappings(long taskId) throws SQLException {
        SyncTaskDO task = loadTask(taskId);
        DatasourceConfigDO source = loadDatasource(task.getSourceDatasourceId().longValue(), "Source datasource not found: ");
        DatasourceConfigDO target = loadDatasource(task.getTargetDatasourceId().longValue(), "Target datasource not found: ");
        List<SchemaMetadataDO> sourceSchemas = metadataScanner.scan(source);
        List<SchemaMetadataDO> targetSchemas = metadataScanner.scan(target);
        TableMetadataDO sourceTable = findTableMetadata(sourceSchemas, task.getSourceSchemaName(), task.getSourceTableName());
        TableMetadataDO targetTable = findTableMetadata(targetSchemas, task.getTargetSchemaName(), task.getTargetTableName());
        if (sourceTable == null) {
            throw new SQLException("Source table not found: " + task.getSourceTableName());
        }
        if (targetTable == null) {
            throw new SQLException("Target table not found: " + task.getTargetTableName());
        }
        return fieldMappingSuggestionMatcher.match(sourceTable.getColumns(), targetTable.getColumns());
    }

    public SchemaComparisonResultVO compareSchema(SchemaComparisonRequestDTO request) throws SQLException {
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
        List<SchemaMetadataDO> sourceSchemas = metadataScanner.scan(request.getSourceDatasource());
        List<SchemaMetadataDO> targetSchemas = metadataScanner.scan(request.getTargetDatasource());
        TableMetadataDO sourceTable = findTableMetadata(sourceSchemas, request.getSourceSchemaName(), request.getSourceTableName());
        TableMetadataDO targetTable = findTableMetadata(targetSchemas, request.getTargetSchemaName(), request.getTargetTableName());
        if (sourceTable == null) {
            throw new SQLException("Source table not found: " + request.getSourceTableName());
        }
        if (targetTable == null) {
            throw new SQLException("Target table not found: " + request.getTargetTableName());
        }
        SchemaComparisonResultVO result = schemaComparisonEngine.compare(request, sourceTable, targetTable,
                DatabaseDialect.from(request.getTargetDatasource().getType()));
        saveSchemaComparisonHistory(request, result);
        return result;
    }

    public SchemaSqlPreviewResultVO previewSchemaSql(SchemaSqlPreviewRequestDTO request) throws SQLException {
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
        SchemaSqlPreviewResultVO result = new SchemaSqlPreviewResultVO();
        result.setExecutable(statements.isEmpty() || executable);
        result.setStatementType(statementType);
        result.setSql(sql);
        result.setMessage(result.isExecutable() ? "SQL is ready for confirmation" : "Dangerous SQL requires explicit confirmation");
        return result;
    }

    public SqlExecutionResultVO executeSchemaSql(SchemaSqlPreviewRequestDTO request) throws SQLException {
        validateSchemaSqlRequest(request);
        String sql = trimSql(request.getSql());
        List<String> statements = splitSqlStatements(sql);
        if (statements.isEmpty()) {
            throw new SQLException("SQL must not be blank");
        }

        long startTime = System.currentTimeMillis();
        DatasourceConfigDO datasource = request.getDatasource();
        SqlExecutionLogEntryDO logEntry = SqlExecutionLogEntryDO.builder()
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

            SqlExecutionResultVO result = new SqlExecutionResultVO();
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

    public SyncTaskDO updateScheduleState(long taskId, boolean enabled, String scheduleType, String cronExpression, Integer intervalSeconds) throws SQLException {
        SyncTaskDO task = loadTask(taskId);
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

    public FieldMappingRuleDO saveFieldMapping(FieldMappingRuleDO mappingRule) throws SQLException {
        validateMapping(mappingRule);
        fieldMappingRepository.save(mappingRule);
        return mappingRule;
    }

    public boolean deleteFieldMapping(long id) throws SQLException {
        return fieldMappingRepository.deleteById(id);
    }

    public List<TransformRuleDO> listTransformRules(Long taskId, Long tableTaskId, Long fieldMappingId) throws SQLException {
        ensureTransformRuleRepository();
        if (taskId == null) {
            return new ArrayList<TransformRuleDO>();
        }
        List<TransformRuleDO> rules = transformRuleRepository.findByTaskId(taskId.longValue());
        List<TransformRuleDO> result = new ArrayList<TransformRuleDO>();
        for (TransformRuleDO rule : rules) {
            if (rule == null) {
                continue;
            }
            if (tableTaskId != null) {
                if (rule.getTableTaskId() != null && !tableTaskId.equals(rule.getTableTaskId())) {
                    continue;
                }
            } else if (rule.getTableTaskId() != null) {
                continue;
            }
            if (fieldMappingId != null) {
                if (rule.getFieldMappingId() == null || !fieldMappingId.equals(rule.getFieldMappingId())) {
                    continue;
                }
            }
            result.add(rule);
        }
        return result;
    }

    public TransformRuleDO saveTransformRule(TransformRuleDO rule) throws SQLException {
        ensureTransformRuleRepository();
        validateTransformRule(rule);
        transformRuleRepository.save(rule);
        return rule;
    }

    public boolean deleteTransformRule(long id) throws SQLException {
        ensureTransformRuleRepository();
        return transformRuleRepository.deleteById(id);
    }

    public TransformRuleDO setTransformRuleEnabled(long id, boolean enabled) throws SQLException {
        ensureTransformRuleRepository();
        TransformRuleDO rule = transformRuleRepository.findById(id).orElse(null);
        if (rule == null) {
            throw new SQLException("Transform rule not found: " + id);
        }
        rule.setEnabled(Boolean.valueOf(enabled));
        transformRuleRepository.save(rule);
        return rule;
    }

    public TransformTestResultVO testTransformRule(TransformRuleDO rule, Object value) {
        TransformTestRequestDTO request = new TransformTestRequestDTO();
        request.setValue(value);
        request.setRules(java.util.Collections.singletonList(rule));
        return testTransformRules(request);
    }

    public TransformTestResultVO testTransformRules(TransformTestRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Transform test request must not be null");
        }
        TransformContext context = TransformContext.builder()
                .taskId(request.getTaskId())
                .tableTaskId(request.getTableTaskId())
                .fieldMappingId(request.getFieldMappingId())
                .sourceField(request.getSourceField())
                .targetField(request.getTargetField())
                .runId(request.getRunId())
                .currentValue(request.getValue())
                .build();
        return transformEngine.test(request.getValue(), request.getRules(), context);
    }

    public TransformPlan loadTransformPlan(Long taskId, Long tableTaskId) throws SQLException {
        if (transformRuleRepository == null || taskId == null) {
            return transformEngine.compile(java.util.Collections.<String, java.util.List<TransformRuleDO>>emptyMap());
        }
        List<TransformRuleDO> rules = listTransformRules(taskId, tableTaskId, null);
        Map<String, List<TransformRuleDO>> rulesByField = new LinkedHashMap<String, List<TransformRuleDO>>();
        for (TransformRuleDO rule : rules) {
            String fieldKey = resolveTransformFieldKey(rule);
            if (fieldKey == null) {
                continue;
            }
            List<TransformRuleDO> fieldRules = rulesByField.get(fieldKey);
            if (fieldRules == null) {
                fieldRules = new ArrayList<TransformRuleDO>();
                rulesByField.put(fieldKey, fieldRules);
            }
            fieldRules.add(rule);
        }
        return transformEngine.compile(rulesByField);
    }

    public List<SchemaMetadataDO> scanMetadata(long datasourceId) throws SQLException {
        DatasourceConfigDO config = loadDatasource(datasourceId, "Datasource not found: ");
        return metadataScanner.scan(config);
    }

    private FullSyncResultVO fullSync(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target) {
        FullSyncRequestDTO request = FullSyncRequestDTO.builder()
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
        FullSyncResultVO result = fullSyncEngine.sync(request);
        appendLog(task.getId().longValue(), "INFO", "Full sync inserted " + result.getInsertedRowCount() + " rows");
        return result;
    }

    private IncrementalSyncResultVO incrementalSync(SyncTaskDO task, DatasourceConfigDO source, DatasourceConfigDO target) {
        IncrementalSyncRequestDTO request = IncrementalSyncRequestDTO.builder()
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
        IncrementalSyncResultVO result = incrementalSyncEngine.sync(request);
        appendLog(task.getId().longValue(), "INFO", "Incremental sync inserted " + result.getInsertedRowCount() + " rows");
        return result;
    }

    private void validateDatasource(DatasourceConfigDO config) {
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

    private void validateTask(SyncTaskDO task) {
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

    private void normalizeSchedule(SyncTaskDO task) {
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

    private void updateScheduleResult(SyncTaskDO task, String lastResult, String lastMessage) {
        if (task == null || task.getScheduleEnabled() == null || !task.getScheduleEnabled().booleanValue()) {
            return;
        }
        task.setScheduleLastResult(lastResult);
        task.setScheduleLastMessage(lastMessage);
        long baseTime = task.getEndedAt() == null ? System.currentTimeMillis() : task.getEndedAt().longValue();
        setNextScheduleRunAt(task, baseTime);
    }

    private void validateMapping(FieldMappingRuleDO mappingRule) {
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

    private void validateTransformRule(TransformRuleDO rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Transform rule must not be null");
        }
        if (rule.getTaskId() == null) {
            throw new IllegalArgumentException("Task id must not be null");
        }
        if (rule.getSourceField() == null || rule.getSourceField().trim().length() == 0) {
            throw new IllegalArgumentException("Source field must not be blank");
        }
        if (rule.getTargetField() == null || rule.getTargetField().trim().length() == 0) {
            throw new IllegalArgumentException("Target field must not be blank");
        }
        if (rule.getTransformType() == null || rule.getTransformType().trim().length() == 0) {
            throw new IllegalArgumentException("Transform type must not be blank");
        }
        if (rule.getOnError() == null) {
            rule.setOnError(TransformErrorStrategy.FAIL);
        }
        if (rule.getTransformOrder() == null) {
            rule.setTransformOrder(Integer.valueOf(0));
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(Boolean.TRUE);
        }
    }

    private void ensureTransformRuleRepository() {
        if (transformRuleRepository == null) {
            throw new IllegalStateException("Transform rule repository is not initialized");
        }
    }

    private String resolveTransformFieldKey(TransformRuleDO rule) {
        if (rule == null) {
            return null;
        }
        String fieldName = rule.getTargetField();
        if (fieldName == null || fieldName.trim().length() == 0) {
            fieldName = rule.getSourceField();
        }
        if (fieldName == null || fieldName.trim().length() == 0) {
            return null;
        }
        return fieldName.trim().toLowerCase(Locale.ROOT);
    }

    private void appendLog(long taskId, String logLevel, String message) {
        ExecutionLogEntryDO entry = new ExecutionLogEntryDO();
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
            List<AlertRuleDO> rules = alertRuleRepository.findEnabled();
            long now = System.currentTimeMillis();
            for (AlertRuleDO rule : rules) {
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
                    AlertChannelDO channel = alertChannelRepository.findById(channelId.longValue()).orElse(null);
                    if (channel == null || Boolean.FALSE.equals(channel.getEnabled())) {
                        continue;
                    }
                    String dedupKey = buildDedupKey(alertType, taskId, tableName, channel.getChannelType(), channelId);
                    AlertDedupStateDO existing = alertDedupStateRepository.findByDedupKey(dedupKey).orElse(null);
                    if (existing != null && existing.getCooldownUntil() != null && existing.getCooldownUntil().longValue() > now) {
                        continue;
                    }
                    String content = renderAlertContent(rule, alertContent, taskId, runId, tableName);
                    AlertSendResult sendResult = alertSenderService.send(channel,
                            rule.getRuleName(),
                            content);
                    AlertHistoryEntryDO history = AlertHistoryEntryDO.builder()
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

                    AlertDedupStateDO dedupState = AlertDedupStateDO.builder()
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

    private String renderAlertContent(AlertRuleDO rule, String alertContent, Long taskId, String runId, String tableName) {
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

    private int safeCooldownSeconds(AlertRuleDO rule) {
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

    private AlertChannelDO sanitizeAlertChannel(AlertChannelDO channel) {
        if (channel == null) {
            return null;
        }
        AlertChannelDO sanitized = new AlertChannelDO();
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

    private void mergeAlertChannelSecrets(AlertChannelDO target, AlertChannelDO existing) {
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

    private void captureDatasourceConnectionMetric(final DatasourceConfigDO config, final ConnectionTestResultVO result) {
        if (monitoringRepository == null || config == null || config.getId() == null || result == null) {
            return;
        }
        submitMonitoringWrite(new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    Optional<DatasourceConnectionMetricDO> latest = monitoringRepository
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
                    DatasourceConnectionMetricDO metric = DatasourceConnectionMetricDO.builder()
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

    private void captureTaskRunMetric(final SyncTaskDO task, final String runId, final String errorMessage) {
        if (monitoringRepository == null || task == null || task.getId() == null) {
            return;
        }
        final SyncTaskDO metricTask = copyTask(task);
        submitMonitoringWrite(new Runnable() {
            @Override
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    TaskRunMetricSummaryVO summary = monitoringRepository.summarizeTaskMetricsForToday(startOfDay(now),
                            startOfDay(now) + 24L * 60L * 60L * 1000L);
                    TaskRunMetricDO metric = TaskRunMetricDO.builder()
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

    private void captureTableRunMetric(final SyncTableRunDO tableRun, final Integer batchCount, final Integer retryCount,
                                       final String lastCheckpoint, final String lastError) {
        if (monitoringRepository == null || tableRun == null || tableRun.getTaskId() == null
                || tableRun.getRunId() == null || tableRun.getTaskTableId() == null) {
            return;
        }
        submitMonitoringWrite(new Runnable() {
            @Override
            public void run() {
                try {
                    TableRunMetricDO metric = TableRunMetricDO.builder()
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
            for (SyncTaskDO task : syncTaskRepository.findAll()) {
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

    private Long resolveLatencyMillis(SyncTaskDO task) {
        if (task == null || task.getDurationMillis() == null || task.getSyncedRowCount() == null
                || task.getSyncedRowCount().longValue() <= 0L) {
            return null;
        }
        return Long.valueOf(task.getDurationMillis().longValue() / Math.max(1L, task.getSyncedRowCount().longValue()));
    }

    private String resolveMetricRunId(String runId, SyncTaskDO task) {
        String normalizedRunId = trimToNull(runId);
        if (normalizedRunId != null) {
            return normalizedRunId;
        }
        if (task == null || task.getId() == null) {
            return "task-run";
        }
        return "task-" + task.getId();
    }

    private SyncTaskDO copyTask(SyncTaskDO source) {
        SyncTaskDO copy = new SyncTaskDO();
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

    private SyncTaskDO loadTask(long taskId) throws SQLException {
        Optional<SyncTaskDO> task = syncTaskRepository.findById(taskId);
        if (!task.isPresent()) {
            throw new SQLException("Sync task not found: " + taskId);
        }
        return applyIncrementalCheckpoint(task.get());
    }

    private DatasourceConfigDO loadDatasource(long datasourceId, String messagePrefix) throws SQLException {
        Optional<DatasourceConfigDO> datasource = datasourceRepository.findById(datasourceId);
        if (!datasource.isPresent()) {
            throw new SQLException(messagePrefix + datasourceId);
        }
        return datasource.get();
    }

    private SyncTaskDO applyIncrementalCheckpoint(SyncTaskDO task) throws SQLException {
        if (task == null) {
            return null;
        }
        Optional<IncrementalSyncCheckpointEntryDO> checkpoint = incrementalCheckpointRepository.findByTaskId(task.getId().longValue());
        if (!checkpoint.isPresent()) {
            task.setIncrementalCheckpointMode(null);
            task.setIncrementalCheckpointValue(null);
            task.setIncrementalCheckpointUpdatedAt(null);
            return task;
        }
        IncrementalSyncCheckpointEntryDO entry = checkpoint.get();
        task.setIncrementalCheckpointMode(entry.getCheckpointMode());
        task.setIncrementalCheckpointValue(entry.getCheckpointValue());
        task.setIncrementalCheckpointUpdatedAt(entry.getUpdatedAt());
        return task;
    }
}
