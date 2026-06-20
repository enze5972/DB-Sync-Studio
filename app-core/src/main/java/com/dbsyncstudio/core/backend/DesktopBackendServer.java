package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.datasource.ConnectionTestResult;
import com.dbsyncstudio.model.datasource.DatasourceConfig;
import com.dbsyncstudio.model.alert.AlertChannel;
import com.dbsyncstudio.model.alert.AlertRule;
import com.dbsyncstudio.model.preview.DataPreviewRequest;
import com.dbsyncstudio.model.preview.DataPreviewResult;
import com.dbsyncstudio.model.metadata.SchemaMetadata;
import com.dbsyncstudio.model.schema.SchemaComparisonHistoryEntry;
import com.dbsyncstudio.model.schema.SchemaComparisonRequest;
import com.dbsyncstudio.model.schema.SchemaComparisonResult;
import com.dbsyncstudio.model.schema.SchemaSqlPreviewRequest;
import com.dbsyncstudio.model.schema.SchemaSqlPreviewResult;
import com.dbsyncstudio.model.sync.ExecutionLogEntry;
import com.dbsyncstudio.model.sync.FieldMappingRule;
import com.dbsyncstudio.model.sync.SyncRun;
import com.dbsyncstudio.model.sync.SyncRunLogEntry;
import com.dbsyncstudio.model.sync.SyncTableRun;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskTable;
import com.dbsyncstudio.model.sync.LogCleanupSummary;
import com.dbsyncstudio.model.sql.SqlExecutionLogEntry;
import com.dbsyncstudio.model.sql.SqlExecutionRequest;
import com.dbsyncstudio.model.sql.SqlExecutionResult;
import com.dbsyncstudio.model.validation.RepairDetail;
import com.dbsyncstudio.model.validation.RepairRequest;
import com.dbsyncstudio.model.validation.RepairResult;
import com.dbsyncstudio.model.validation.RepairRun;
import com.dbsyncstudio.model.validation.ValidationDifference;
import com.dbsyncstudio.model.validation.ValidationMode;
import com.dbsyncstudio.model.validation.ValidationRequest;
import com.dbsyncstudio.model.validation.ValidationResult;
import com.dbsyncstudio.model.validation.ValidationRun;
import com.dbsyncstudio.model.settings.AppSettings;
import com.dbsyncstudio.model.settings.AppLicenseRequest;
import com.dbsyncstudio.core.backend.AppUpdateCheckRequest;
import com.dbsyncstudio.core.scheduler.TaskSchedulerService;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DesktopBackendServer {

    private static final Logger LOGGER = Logger.getLogger(DesktopBackendServer.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern TASK_ID_PATTERN = Pattern.compile("^/api/tasks/(\\d+)(?:/.*)?$");
    private static final Pattern VALIDATION_PARENT_ID_PATTERN = Pattern.compile("^/api/validation/(\\d+)(?:/.*)?$");
    private static final Pattern REPAIR_RUN_ID_PATTERN = Pattern.compile("^/api/validation/\\d+/repairs/(\\d+)/details$");

    private final DesktopBackendService backendService;
    private TaskSchedulerService taskSchedulerService;
    private HttpServer httpServer;
    private int port;

    public DesktopBackendServer(DesktopBackendService backendService) {
        this.backendService = backendService;
    }

    public int start(int requestedPort) throws IOException {
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", requestedPort);
        httpServer = HttpServer.create(address, 0);
        port = httpServer.getAddress().getPort();
        httpServer.setExecutor(Executors.newCachedThreadPool());
        registerContexts();
        httpServer.start();
        if (taskSchedulerService == null) {
            taskSchedulerService = new TaskSchedulerService(backendService);
        }
        taskSchedulerService.start();
        taskSchedulerService.resumeEnabledTasks();
        LOGGER.log(Level.INFO, "DB Sync Studio backend server started on port {0}", Integer.valueOf(port));
        return port;
    }

    public void await() throws InterruptedException {
        new CountDownLatch(1).await();
    }

    public void stop() {
        if (taskSchedulerService != null) {
            taskSchedulerService.stop();
        }
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private void registerContexts() {
        httpServer.createContext("/api/health", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                return backendService.dashboardStats();
            }
        });

        httpServer.createContext("/api/app-settings", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    return backendService.loadAppSettingsResponse();
                }
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    AppSettings settings = readBody(exchange, AppSettings.class);
                    return backendService.saveAppSettings(settings);
                }
                throw methodNotAllowed();
            }
        });

        httpServer.createContext("/api/license", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    return backendService.loadLicenseInfo();
                }
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    AppLicenseRequest request = readBody(exchange, AppLicenseRequest.class);
                    return backendService.activateLicense(request == null ? null : request.getLicenseKey());
                }
                if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    return backendService.clearLicense();
                }
                throw methodNotAllowed();
            }
        });

        httpServer.createContext("/api/updates/check", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                readBody(exchange, AppUpdateCheckRequest.class);
                return backendService.checkForUpdate();
            }
        });

        httpServer.createContext("/api/datasources", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    return backendService.listDatasources();
                }
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    DatasourceConfig config = readBody(exchange, DatasourceConfig.class);
                    return backendService.saveDatasource(config);
                }
                if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    long id = extractTailId(exchange.getRequestURI());
                    return Boolean.valueOf(backendService.deleteDatasource(id));
                }
                throw methodNotAllowed();
            }
        });

        httpServer.createContext("/api/datasources/test", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                DatasourceConfig config = readBody(exchange, DatasourceConfig.class);
                ConnectionTestResult result = backendService.testConnection(config);
                return result;
            }
        });

        httpServer.createContext("/api/tasks", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                if ("/api/tasks".equals(path)) {
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        return backendService.listTasks();
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        SyncTask task = readBody(exchange, SyncTask.class);
                        return backendService.saveTask(task);
                    }
                }

                if (path.matches("^/api/tasks/\\d+/run$")) {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.runTask(taskId);
                }

                if (path.matches("^/api/tasks/\\d+/run-batch$")) {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    TaskBatchRunRequest request = readBody(exchange, TaskBatchRunRequest.class);
                    return backendService.runBatchTask(taskId, request);
                }

                if (path.matches("^/api/tasks/\\d+/tables$")) {
                    long taskId = extractTaskId(path);
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        return backendService.listTaskTables(taskId);
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        SyncTaskTable taskTable = readBody(exchange, SyncTaskTable.class);
                        return backendService.saveTaskTable(taskId, taskTable);
                    }
                    throw methodNotAllowed();
                }

                if (path.matches("^/api/tasks/\\d+/tables/\\d+$")) {
                    if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskTableId = extractTailId(requestUri);
                    return Boolean.valueOf(backendService.deleteTaskTable(taskTableId));
                }

                if (path.matches("^/api/tasks/\\d+/runs$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    Integer limit = extractQueryInt(requestUri, "limit");
                    return backendService.listSyncRuns(Long.valueOf(taskId), limit == null ? 20 : limit.intValue());
                }

                if (path.matches("^/api/tasks/\\d+/runs/[^/]+/tables$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    String runId = extractNestedRunId(path);
                    SyncRun run = backendService.findSyncRunByRunId(runId).orElse(null);
                    if (run == null || run.getTaskId() == null || run.getTaskId().longValue() != taskId) {
                        throw new HttpError(404, "Sync run not found");
                    }
                    return backendService.listSyncTableRuns(run.getId().longValue());
                }

                if (path.matches("^/api/tasks/\\d+/runs/[^/]+/logs$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    String runId = extractNestedRunId(path);
                    Long syncRunId = extractQueryLong(requestUri, "syncRunId");
                    Long syncTableRunId = extractQueryLong(requestUri, "syncTableRunId");
                    String tableName = extractQueryString(requestUri, "tableName");
                    String logLevel = extractQueryString(requestUri, "logLevel");
                    String keyword = extractQueryString(requestUri, "keyword");
                    Long startTime = extractQueryLong(requestUri, "startTime");
                    Long endTime = extractQueryLong(requestUri, "endTime");
                    Integer limit = extractQueryInt(requestUri, "limit");
                    return backendService.listSyncRunLogs(Long.valueOf(taskId), runId, syncRunId, syncTableRunId,
                            tableName, logLevel, keyword, startTime, endTime, limit == null ? 100 : limit.intValue());
                }

                if (path.matches("^/api/tasks/\\d+/runs/[^/]+$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    String runId = extractRunId(path);
                    Integer limit = extractQueryInt(requestUri, "limit");
                    return backendService.loadSyncRunDetail(taskId, runId, limit == null ? 100 : limit.intValue());
                }

                if (path.matches("^/api/tasks/\\d+/start$")) {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.startTask(taskId);
                }

                if (path.matches("^/api/tasks/\\d+/pause$")) {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.pauseTask(taskId);
                }

                if (path.matches("^/api/tasks/\\d+/resume$")) {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.resumeTask(taskId);
                }

                if (path.matches("^/api/tasks/\\d+/stop$")) {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.stopTask(taskId);
                }

                if (path.matches("^/api/tasks/\\d+/schedule$")) {
                    long taskId = extractTaskId(path);
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        return backendService.findTaskById(taskId).orElse(null);
                    }
                    if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                        ScheduleRequest request = readBody(exchange, ScheduleRequest.class);
                        return backendService.updateScheduleState(taskId,
                                request.enabled,
                                request.scheduleType,
                                request.cronExpression,
                                request.intervalSeconds);
                    }
                    throw methodNotAllowed();
                }

                if (path.matches("^/api/tasks/\\d+/schedule/history$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.listTaskScheduleHistory(taskId);
                }

                if (path.matches("^/api/tasks/\\d+/logs$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long taskId = extractTaskId(path);
                    return backendService.listLogs(Long.valueOf(taskId));
                }

                if (path.matches("^/api/tasks/\\d+$")) {
                    if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                        long taskId = extractTaskId(path);
                        return Boolean.valueOf(backendService.deleteTask(taskId));
                    }
                }

                throw notFound();
            }
        });

        httpServer.createContext("/api/dashboard", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                return backendService.dashboardStats();
            }
        });

        httpServer.createContext("/api/diagnostics", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                return backendService.diagnosticsStatus();
            }
        });

        httpServer.createContext("/api/monitoring/overview", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                return backendService.monitoringOverview();
            }
        });

        httpServer.createContext("/api/monitoring/task-metrics", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                URI uri = exchange.getRequestURI();
                return backendService.listTaskRunMetrics(
                        extractQueryString(uri, "runId"),
                        extractQueryLong(uri, "taskId"),
                        extractQueryLong(uri, "startTime"),
                        extractQueryLong(uri, "endTime"),
                        defaultLimit(extractQueryInt(uri, "limit")));
            }
        });

        httpServer.createContext("/api/monitoring/table-metrics", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                URI uri = exchange.getRequestURI();
                return backendService.listTableRunMetrics(
                        extractQueryString(uri, "runId"),
                        extractQueryLong(uri, "taskId"),
                        extractQueryLong(uri, "tableTaskId"),
                        extractQueryLong(uri, "startTime"),
                        extractQueryLong(uri, "endTime"),
                        defaultLimit(extractQueryInt(uri, "limit")));
            }
        });

        httpServer.createContext("/api/monitoring/datasource-metrics", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                URI uri = exchange.getRequestURI();
                return backendService.listDatasourceConnectionMetrics(
                        extractQueryLong(uri, "datasourceId"),
                        extractQueryLong(uri, "startTime"),
                        extractQueryLong(uri, "endTime"),
                        defaultLimit(extractQueryInt(uri, "limit")));
            }
        });

        httpServer.createContext("/api/monitoring/trend", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                URI uri = exchange.getRequestURI();
                return backendService.taskRunTrend(
                        extractQueryString(uri, "runId"),
                        extractQueryLong(uri, "taskId"),
                        extractQueryLong(uri, "startTime"),
                        extractQueryLong(uri, "endTime"),
                        defaultLimit(extractQueryInt(uri, "limit")));
            }
        });

        httpServer.createContext("/api/monitoring/cleanup", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                LogCleanupRequest request = readBody(exchange, LogCleanupRequest.class);
                return backendService.cleanupMonitoringMetrics(request == null ? null : request.getRetentionDays());
            }
        });

        httpServer.createContext("/api/alerts/rules", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                URI uri = exchange.getRequestURI();
                String path = uri.getPath();
                if ("/api/alerts/rules".equals(path)) {
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        return backendService.listAlertRules();
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        AlertRule rule = readBody(exchange, AlertRule.class);
                        return backendService.saveAlertRule(rule);
                    }
                }
                if (path.matches("^/api/alerts/rules/\\d+$")) {
                    if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    return Boolean.valueOf(backendService.deleteAlertRule(extractTailId(uri)));
                }
                throw notFound();
            }
        });

        httpServer.createContext("/api/alerts/channels", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                URI uri = exchange.getRequestURI();
                String path = uri.getPath();
                if ("/api/alerts/channels".equals(path)) {
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        return backendService.listAlertChannels();
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        AlertChannel channel = readBody(exchange, AlertChannel.class);
                        return backendService.saveAlertChannel(channel);
                    }
                }
                if (path.matches("^/api/alerts/channels/\\d+$")) {
                    if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    return Boolean.valueOf(backendService.deleteAlertChannel(extractTailId(uri)));
                }
                throw notFound();
            }
        });

        httpServer.createContext("/api/alerts/channels/test", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                AlertChannelTestRequest request = readBody(exchange, AlertChannelTestRequest.class);
                if (request == null || request.channelId == null) {
                    throw new HttpError(400, "channelId is required");
                }
                return backendService.testAlertChannel(request.channelId.longValue(), request.content);
            }
        });

        httpServer.createContext("/api/alerts/history", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                URI uri = exchange.getRequestURI();
                Integer limit = extractQueryInt(uri, "limit");
                Long taskId = extractQueryLong(uri, "taskId");
                Long startTime = extractQueryLong(uri, "startTime");
                Long endTime = extractQueryLong(uri, "endTime");
                String keyword = extractQueryString(uri, "keyword");
                return backendService.listAlertHistory(taskId, extractQueryString(uri, "alertType"),
                        extractQueryString(uri, "sendStatus"), startTime, endTime, keyword, defaultLimit(limit));
            }
        });

        httpServer.createContext("/api/logs", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    return loadLogs(exchange);
                }
                if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    LogCleanupRequest request = readBody(exchange, LogCleanupRequest.class);
                    return backendService.cleanupLogs(request == null ? null : request.getRetentionDays());
                }
                if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                    LogCleanupRequest request = readBody(exchange, LogCleanupRequest.class);
                    return Integer.valueOf(backendService.updateLogRetentionDays(request == null || request.getRetentionDays() == null ? 30 : request.getRetentionDays().intValue()));
                }
                throw methodNotAllowed();
            }
        });

        httpServer.createContext("/api/logs/retention", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                return Integer.valueOf(backendService.getLogRetentionDays());
            }
        });

        httpServer.createContext("/api/mappings", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                if ("/api/mappings".equals(path)) {
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Long taskId = extractQueryLong(requestUri, "taskId");
                        if (taskId == null) {
                            throw new HttpError(400, "taskId is required");
                        }
                        return backendService.listFieldMappings(taskId.longValue());
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        FieldMappingRule mappingRule = readBody(exchange, FieldMappingRule.class);
                        return backendService.saveFieldMapping(mappingRule);
                    }
                }
                if (path.matches("^/api/mappings/\\d+$")) {
                    if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                        long mappingId = extractTailId(requestUri);
                        return Boolean.valueOf(backendService.deleteFieldMapping(mappingId));
                    }
                }
                throw notFound();
            }
        });

        httpServer.createContext("/api/mappings/suggest", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                Long taskId = extractQueryLong(exchange.getRequestURI(), "taskId");
                if (taskId == null) {
                    throw new HttpError(400, "taskId is required");
                }
                return backendService.suggestFieldMappings(taskId.longValue());
            }
        });

        httpServer.createContext("/api/metadata", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                Long datasourceId = extractQueryLong(exchange.getRequestURI(), "datasourceId");
                if (datasourceId == null) {
                    throw new HttpError(400, "datasourceId is required");
                }
                List<SchemaMetadata> schemas = backendService.scanMetadata(datasourceId.longValue());
                return schemas;
            }
        });

        httpServer.createContext("/api/schema/compare", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                SchemaComparisonRequest request = readBody(exchange, SchemaComparisonRequest.class);
                SchemaComparisonResult result = backendService.compareSchema(request);
                return result;
            }
        });

        httpServer.createContext("/api/schema/history", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                Integer limit = extractQueryInt(exchange.getRequestURI(), "limit");
                int safeLimit = limit == null ? 20 : limit.intValue();
                if (safeLimit <= 0) {
                    safeLimit = 20;
                }
                List<SchemaComparisonHistoryEntry> historyEntries = backendService.listSchemaComparisonHistory(safeLimit);
                return historyEntries;
            }
        });

        httpServer.createContext("/api/schema/sql/preview", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                SchemaSqlPreviewRequest request = readBody(exchange, SchemaSqlPreviewRequest.class);
                SchemaSqlPreviewResult result = backendService.previewSchemaSql(request);
                return result;
            }
        });

        httpServer.createContext("/api/schema/sql/execute", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                SchemaSqlPreviewRequest request = readBody(exchange, SchemaSqlPreviewRequest.class);
                return backendService.executeSchemaSql(request);
            }
        });

        httpServer.createContext("/api/preview", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                DataPreviewRequest request = readBody(exchange, DataPreviewRequest.class);
                return backendService.previewTableData(request);
            }
        });

        httpServer.createContext("/api/sql/execute", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                SqlExecutionRequest request = readBody(exchange, SqlExecutionRequest.class);
                return backendService.executeSql(request);
            }
        });

        httpServer.createContext("/api/sql/logs", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    throw methodNotAllowed();
                }
                Integer limit = extractQueryInt(exchange.getRequestURI(), "limit");
                int safeLimit = limit == null ? 20 : limit.intValue();
                if (safeLimit <= 0) {
                    safeLimit = 20;
                }
                return backendService.listSqlExecutionLogs(safeLimit);
            }
        });

        httpServer.createContext("/api/validation", new JsonHandler() {
            @Override
            protected Object process(HttpExchange exchange) throws Exception {
                URI requestUri = exchange.getRequestURI();
                String path = requestUri.getPath();
                if ("/api/validation".equals(path)) {
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        ValidationRequest request = readBody(exchange, ValidationRequest.class);
                        return backendService.runValidation(request);
                    }
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        Long taskId = extractQueryLong(requestUri, "taskId");
                        Integer limit = extractQueryInt(requestUri, "limit");
                        return backendService.listValidationRuns(taskId, limit == null ? 20 : limit.intValue());
                    }
                }
                if (path.matches("^/api/validation/\\d+/differences$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long validationRunId = extractTailParentId(path);
                    return backendService.listValidationDifferences(validationRunId);
                }
                if (path.matches("^/api/validation/\\d+/repairs$")) {
                    if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        long validationRunId = extractTailParentId(path);
                        Integer limit = extractQueryInt(requestUri, "limit");
                        return backendService.listRepairRuns(Long.valueOf(validationRunId), limit == null ? 20 : limit.intValue());
                    }
                    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        RepairRequest request = readBody(exchange, RepairRequest.class);
                        return backendService.runRepair(request);
                    }
                }
                if (path.matches("^/api/validation/\\d+/repairs/\\d+/details$")) {
                    if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        throw methodNotAllowed();
                    }
                    long repairRunId = extractRepairRunId(path);
                    return backendService.listRepairDetails(repairRunId);
                }
                throw notFound();
            }
        });
    }

    private Object loadLogs(HttpExchange exchange) throws SQLException {
        String query = exchange.getRequestURI().getQuery();
        Long taskId = null;
        if (query != null && query.contains("taskId=")) {
            String[] parts = query.split("&");
            for (String part : parts) {
                if (part.startsWith("taskId=")) {
                    taskId = Long.valueOf(part.substring("taskId=".length()));
                    break;
                }
            }
        }
        if (taskId == null) {
            List<SyncTask> tasks = backendService.listTasks();
            if (tasks.isEmpty()) {
                return backendService.listLogs(null);
            }
            return backendService.listAllLogs(tasks);
        }
        return backendService.listLogs(taskId);
    }

    private abstract class JsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                Object result = process(exchange);
                writeJson(exchange, 200, wrapSuccess(result));
            } catch (HttpError ex) {
                writeJson(exchange, ex.statusCode, wrapError(ex.getMessage()));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "API request failed", ex);
                writeJson(exchange, 500, wrapError(resolveUserMessage(ex)));
            } finally {
                exchange.close();
            }
        }

        protected abstract Object process(HttpExchange exchange) throws Exception;
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(body);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
    }

    private <T> T readBody(HttpExchange exchange, Class<T> type) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return OBJECT_MAPPER.readValue(inputStream, type);
        }
    }

    private long extractTailId(URI uri) {
        String[] segments = uri.getPath().split("/");
        return Long.parseLong(segments[segments.length - 1]);
    }

    private Long extractQueryLong(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null || query.trim().length() == 0) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                String value = part.substring((key + "=").length());
                if (value.trim().length() == 0) {
                    return null;
                }
                return Long.valueOf(value);
            }
        }
        return null;
    }

    private String extractQueryString(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null || query.trim().length() == 0) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                String value = part.substring((key + "=").length());
                if (value.trim().length() == 0) {
                    return null;
                }
                return value;
            }
        }
        return null;
    }

    private Integer extractQueryInt(URI uri, String key) {
        Long value = extractQueryLong(uri, key);
        if (value == null) {
            return null;
        }
        return Integer.valueOf(value.intValue());
    }

    private int defaultLimit(Integer limit) {
        if (limit == null || limit.intValue() <= 0) {
            return 20;
        }
        return limit.intValue();
    }

    private long extractTaskId(String path) {
        return extractPatternGroup(path, TASK_ID_PATTERN, 1);
    }

    private long extractTailParentId(String path) {
        return extractPatternGroup(path, VALIDATION_PARENT_ID_PATTERN, 1);
    }

    private long extractRepairRunId(String path) {
        return extractPatternGroup(path, REPAIR_RUN_ID_PATTERN, 1);
    }

    private String extractRunId(String path) {
        String[] segments = path.split("/");
        return segments[segments.length - 1];
    }

    private String extractNestedRunId(String path) {
        String[] segments = path.split("/");
        return segments[segments.length - 2];
    }

    private static class ScheduleRequest {
        public Boolean enabled;
        public String scheduleType;
        public String cronExpression;
        public Integer intervalSeconds;
    }

    private static class AlertChannelTestRequest {
        public Long channelId;
        public String content;
    }

    private Map<String, Object> wrapSuccess(Object data) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", Boolean.TRUE);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> wrapError(String message) {
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("success", Boolean.FALSE);
        response.put("message", message);
        return response;
    }

    private HttpError methodNotAllowed() {
        return new HttpError(405, "Method not allowed");
    }

    private HttpError notFound() {
        return new HttpError(404, "Not found");
    }

    private long extractPatternGroup(String input, Pattern pattern, int groupIndex) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            throw new HttpError(400, "请求参数不正确");
        }
        return Long.parseLong(matcher.group(groupIndex));
    }

    private String resolveUserMessage(Exception ex) {
        if (ex == null) {
            return "请求失败，请稍后重试";
        }
        if (ex instanceof HttpError) {
            return ex.getMessage();
        }
        String message = ex.getMessage();
        if (message == null || message.trim().length() == 0) {
            return "请求失败，请稍后重试";
        }
        if (message.indexOf("null") >= 0 && message.trim().length() <= 10) {
            return "请求失败，请稍后重试";
        }
        return message;
    }

    private static class HttpError extends RuntimeException {
        private final int statusCode;

        private HttpError(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }
}
