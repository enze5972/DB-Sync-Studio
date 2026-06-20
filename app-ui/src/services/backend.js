import { invoke } from '@tauri-apps/api/tauri'

let backendBaseUrlPromise = null

function isTauriRuntime() {
  return typeof window !== 'undefined'
}

async function getBackendBaseUrl() {
  if (!backendBaseUrlPromise) {
    backendBaseUrlPromise = (async function () {
      if (typeof window !== 'undefined' && window.location && window.location.protocol === 'http:' && window.location.port === '5173') {
        return ''
      }
      if (isTauriRuntime()) {
        try {
          return await invoke('get_backend_base_url')
        } catch (error) {
          // fall through to the development default
        }
      }
      return 'http://127.0.0.1:18444'
    })()
  }
  return backendBaseUrlPromise
}

async function apiRequest(path, options) {
  const baseUrl = await getBackendBaseUrl()
  let response
  try {
    response = await fetch(baseUrl + path, Object.assign({
      headers: {
        'Content-Type': 'application/json'
      }
    }, options || {}))
  } catch (error) {
    throw new Error(error.message || '无法连接到后端服务')
  }

  let body = null
  try {
    body = await response.json()
  } catch (error) {
    body = null
  }

  if (!response.ok || (body && body.success === false)) {
    const message = normalizeErrorMessage(body, response)
    throw new Error(message)
  }
  return body ? body.data : null
}

function normalizeErrorMessage(body, response) {
  const userMessage = body && typeof body.message === 'string' && body.message.trim().length > 0
    ? body.message.trim()
    : ''
  if (userMessage) {
    return userMessage
  }
  const detail = body && typeof body.detail === 'string' && body.detail.trim().length > 0
    ? body.detail.trim()
    : ''
  if (detail) {
    return detail
  }
  if (response && response.status) {
    return '请求失败（' + response.status + '）'
  }
  return '请求失败，请稍后重试'
}

function buildQueryString(params) {
  if (!params) {
    return ''
  }
  const query = []
  Object.keys(params).forEach(function (key) {
    const value = params[key]
    if (value === undefined || value === null || value === '') {
      return
    }
    query.push(encodeURIComponent(key) + '=' + encodeURIComponent(value))
  })
  return query.length ? ('?' + query.join('&')) : ''
}

export async function bootstrapBackend() {
  return getBackendBaseUrl()
}

export async function fetchDashboardStats() {
  return apiRequest('/api/dashboard')
}

export async function listDatasources() {
  return apiRequest('/api/datasources')
}

export async function saveDatasource(payload) {
  return apiRequest('/api/datasources', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteDatasource(id) {
  return apiRequest('/api/datasources/' + id, {
    method: 'DELETE'
  })
}

export async function testDatasourceConnection(payload) {
  return apiRequest('/api/datasources/test', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function listTasks() {
  return apiRequest('/api/tasks')
}

export async function saveTask(payload) {
  return apiRequest('/api/tasks', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function runBatchTask(id, payload) {
  return apiRequest('/api/tasks/' + id + '/run-batch', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteTask(id) {
  return apiRequest('/api/tasks/' + id, {
    method: 'DELETE'
  })
}

export async function listTaskTables(taskId) {
  return apiRequest('/api/tasks/' + taskId + '/tables')
}

export async function saveTaskTable(taskId, payload) {
  return apiRequest('/api/tasks/' + taskId + '/tables', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteTaskTable(taskId, tableId) {
  return apiRequest('/api/tasks/' + taskId + '/tables/' + tableId, {
    method: 'DELETE'
  })
}

export async function runTask(id) {
  return apiRequest('/api/tasks/' + id + '/run', {
    method: 'POST',
    body: JSON.stringify({})
  })
}

export async function startTask(id) {
  return apiRequest('/api/tasks/' + id + '/start', {
    method: 'POST',
    body: JSON.stringify({})
  })
}

export async function pauseTask(id) {
  return apiRequest('/api/tasks/' + id + '/pause', {
    method: 'POST',
    body: JSON.stringify({})
  })
}

export async function resumeTask(id) {
  return apiRequest('/api/tasks/' + id + '/resume', {
    method: 'POST',
    body: JSON.stringify({})
  })
}

export async function stopTask(id) {
  return apiRequest('/api/tasks/' + id + '/stop', {
    method: 'POST',
    body: JSON.stringify({})
  })
}

export async function listTaskRuns(taskId, limit) {
  return apiRequest('/api/tasks/' + taskId + '/runs' + buildQueryString({
    limit: typeof limit === 'number' ? limit : ''
  }))
}

export async function listMonitoringOverview() {
  return apiRequest('/api/monitoring/overview')
}

export async function listTaskRunMetrics(params) {
  return apiRequest('/api/monitoring/task-metrics' + buildQueryString({
    runId: params && params.runId,
    taskId: params && params.taskId,
    startTime: params && params.startTime,
    endTime: params && params.endTime,
    limit: params && typeof params.limit === 'number' ? params.limit : ''
  }))
}

export async function listTableRunMetrics(params) {
  return apiRequest('/api/monitoring/table-metrics' + buildQueryString({
    runId: params && params.runId,
    taskId: params && params.taskId,
    tableTaskId: params && params.tableTaskId,
    startTime: params && params.startTime,
    endTime: params && params.endTime,
    limit: params && typeof params.limit === 'number' ? params.limit : ''
  }))
}

export async function listDatasourceConnectionMetrics(params) {
  return apiRequest('/api/monitoring/datasource-metrics' + buildQueryString({
    datasourceId: params && params.datasourceId,
    startTime: params && params.startTime,
    endTime: params && params.endTime,
    limit: params && typeof params.limit === 'number' ? params.limit : ''
  }))
}

export async function cleanupMonitoringMetrics(payload) {
  return apiRequest('/api/monitoring/cleanup', {
    method: 'DELETE',
    body: JSON.stringify(payload)
  })
}

export async function getDiagnosticsStatus() {
  return apiRequest('/api/diagnostics')
}

export async function getAppSettings() {
  return apiRequest('/api/app-settings')
}

export async function saveAppSettings(payload) {
  return apiRequest('/api/app-settings', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function getLicenseInfo() {
  return apiRequest('/api/license')
}

export async function activateLicense(payload) {
  return apiRequest('/api/license', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function clearLicense() {
  return apiRequest('/api/license', {
    method: 'DELETE'
  })
}

export async function checkForUpdate(payload) {
  return apiRequest('/api/updates/check', {
    method: 'POST',
    body: JSON.stringify(payload || {})
  })
}

export async function listTaskRunsForMonitoring(limitPerTask) {
  const tasks = await listTasks()
  const runsByTaskId = {}
  const normalizedLimit = typeof limitPerTask === 'number' ? limitPerTask : 10

  await Promise.all(tasks.map(async function (task) {
    try {
      runsByTaskId[task.id] = await listTaskRuns(task.id, normalizedLimit)
    } catch (error) {
      runsByTaskId[task.id] = []
    }
  }))

  return {
    tasks: tasks,
    runsByTaskId: runsByTaskId
  }
}

export async function getTaskRun(taskId, runId, limit) {
  return apiRequest('/api/tasks/' + taskId + '/runs/' + encodeURIComponent(runId) + buildQueryString({
    limit: typeof limit === 'number' ? limit : ''
  }))
}

export async function listTaskRunTables(taskId, runId) {
  return apiRequest('/api/tasks/' + taskId + '/runs/' + encodeURIComponent(runId) + '/tables')
}

export async function listTaskRunLogs(taskId, runId, params) {
  return apiRequest('/api/tasks/' + taskId + '/runs/' + encodeURIComponent(runId) + '/logs' + buildQueryString({
    syncRunId: params && params.syncRunId,
    syncTableRunId: params && params.syncTableRunId,
    tableName: params && params.tableName,
    logLevel: params && params.logLevel,
    keyword: params && params.keyword,
    startTime: params && params.startTime,
    endTime: params && params.endTime,
    limit: params && typeof params.limit === 'number' ? params.limit : ''
  }))
}

export async function getTaskSchedule(id) {
  return apiRequest('/api/tasks/' + id + '/schedule')
}

export async function loadTaskSchedule(id) {
  return getTaskSchedule(id)
}

export async function updateTaskScheduleState(id, payload) {
  return apiRequest('/api/tasks/' + id + '/schedule', {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export async function updateTaskSchedule(id, payload) {
  return updateTaskScheduleState(id, payload)
}

export async function listTaskScheduleHistory(id) {
  return apiRequest('/api/tasks/' + id + '/schedule/history')
}

export async function listTaskLogs(taskId) {
  return apiRequest('/api/tasks/' + taskId + '/logs')
}

export async function listLogs(taskId, params) {
  return apiRequest('/api/logs' + buildQueryString({
    taskId: taskId,
    runId: params && params.runId,
    tableName: params && params.tableName,
    logLevel: params && params.logLevel,
    keyword: params && params.keyword,
    startTime: params && params.startTime,
    endTime: params && params.endTime
  }))
}

export async function getLogRetentionDays() {
  return apiRequest('/api/logs/retention')
}

export async function updateLogRetentionDays(payload) {
  return apiRequest('/api/logs', {
    method: 'PUT',
    body: JSON.stringify(payload)
  })
}

export async function cleanupLogs(payload) {
  return apiRequest('/api/logs', {
    method: 'DELETE',
    body: JSON.stringify(payload)
  })
}

export async function listFieldMappings(taskId) {
  return apiRequest('/api/mappings?taskId=' + taskId)
}

export async function suggestFieldMappings(taskId) {
  return apiRequest('/api/mappings/suggest?taskId=' + taskId)
}

export async function saveFieldMapping(payload) {
  return apiRequest('/api/mappings', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteFieldMapping(id) {
  return apiRequest('/api/mappings/' + id, {
    method: 'DELETE'
  })
}

export async function scanMetadata(datasourceId) {
  return apiRequest('/api/metadata?datasourceId=' + datasourceId)
}

export async function compareSchema(payload) {
  return apiRequest('/api/schema/compare', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function listSchemaComparisonHistory(limit) {
  return apiRequest('/api/schema/history' + buildQueryString({
    limit: typeof limit === 'number' ? limit : ''
  }))
}

export async function previewSchemaSql(payload) {
  return apiRequest('/api/schema/sql/preview', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function executeSchemaSql(payload) {
  return apiRequest('/api/schema/sql/execute', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function previewTableData(payload) {
  return apiRequest('/api/preview', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function executeSql(payload) {
  return apiRequest('/api/sql/execute', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function runValidation(payload) {
  return apiRequest('/api/validation', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function listValidationRuns(taskId, limit) {
  return apiRequest('/api/validation' + buildQueryString({
    taskId: typeof taskId === 'number' ? taskId : '',
    limit: typeof limit === 'number' ? limit : ''
  }))
}

export async function listValidationDifferences(validationRunId) {
  return apiRequest('/api/validation/' + validationRunId + '/differences')
}

export async function runRepair(payload) {
  return apiRequest('/api/validation/' + payload.validationRunId + '/repairs', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function listRepairRuns(validationRunId, limit) {
  return apiRequest('/api/validation/' + validationRunId + '/repairs' + buildQueryString({
    limit: typeof limit === 'number' ? limit : ''
  }))
}

export async function listRepairDetails(validationRunId, repairRunId) {
  return apiRequest('/api/validation/' + validationRunId + '/repairs/' + repairRunId + '/details')
}

export async function listSqlExecutionLogs(limit) {
  return apiRequest('/api/sql/logs' + buildQueryString({
    limit: typeof limit === 'number' ? limit : ''
  }))
}

export async function listAlertRules() {
  return apiRequest('/api/alerts/rules')
}

export async function saveAlertRule(payload) {
  return apiRequest('/api/alerts/rules', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteAlertRule(id) {
  return apiRequest('/api/alerts/rules/' + id, {
    method: 'DELETE'
  })
}

export async function listAlertChannels() {
  return apiRequest('/api/alerts/channels')
}

export async function saveAlertChannel(payload) {
  return apiRequest('/api/alerts/channels', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function deleteAlertChannel(id) {
  return apiRequest('/api/alerts/channels/' + id, {
    method: 'DELETE'
  })
}

export async function testAlertChannel(payload) {
  return apiRequest('/api/alerts/channels/test', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export async function listAlertHistory(params) {
  return apiRequest('/api/alerts/history' + buildQueryString({
    taskId: params && params.taskId,
    alertType: params && params.alertType,
    sendStatus: params && params.sendStatus,
    keyword: params && params.keyword,
    startTime: params && params.startTime,
    endTime: params && params.endTime,
    limit: params && typeof params.limit === 'number' ? params.limit : ''
  }))
}
