<template>
  <div class="page-section list-page execution-log-workbench">
    <div class="page-header execution-log-workbench__header">
      <div class="execution-log-workbench__titleblock">
        <h1>执行日志</h1>
        <p>查看同步任务执行过程、失败原因和重试记录，支持按任务、级别、run_id 和时间范围筛选。</p>
      </div>
      <div class="execution-log-workbench__toolbar">
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
        <el-tooltip :disabled="hasLogs" content="当前没有可导出的日志" placement="bottom">
          <span>
            <el-button round type="primary" plain :disabled="!hasLogs" @click="exportCurrentLogs">导出</el-button>
          </span>
        </el-tooltip>
        <el-tooltip :disabled="hasLogs" content="当前没有可导出的日志" placement="bottom">
          <span>
            <el-button round :disabled="!hasLogs" :loading="diagnosticsLoading" @click="exportDiagnostics">导出诊断</el-button>
          </span>
        </el-tooltip>
        <el-tooltip :disabled="hasLogs" content="当前没有可清理的日志" placement="bottom">
          <span>
            <el-button round type="warning" plain :loading="cleaning" :disabled="!hasLogs" @click="cleanupHistory">
              清理历史
            </el-button>
          </span>
        </el-tooltip>
      </div>
    </div>

    <div class="page-overview page-overview--four execution-log-workbench__overview">
      <div class="page-overview__item">
        <div class="page-overview__label">当前任务</div>
        <div class="page-overview__value">{{ selectedTaskId ? selectedTaskName : '未选择' }}</div>
        <div class="page-overview__hint">{{ currentTaskHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">日志条数</div>
        <div class="page-overview__value">{{ logs.length }}</div>
        <div class="page-overview__hint">失败原因、重试信息和执行状态都可追踪</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">错误 / 警告</div>
        <div class="page-overview__value">{{ errorWarningSummary }}</div>
        <div class="page-overview__hint">{{ errorWarningHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">保留天数</div>
        <div class="page-overview__value">{{ retentionDays }}</div>
        <div class="page-overview__hint">默认保留 30 天，可安全调整</div>
      </div>
    </div>

    <div class="panel-card glass-panel execution-log-workbench__prompt-strip">
      <div class="execution-log-workbench__prompt-main">
        <span class="execution-log-workbench__prompt-label">提示</span>
        <span class="execution-log-workbench__prompt-text">{{ promptText }}</span>
      </div>
      <div class="execution-log-workbench__prompt-meta">
        <el-tag :type="selectedTaskId ? 'success' : 'info'" effect="light">
          {{ selectedTaskId ? '已选择任务' : '未选择任务' }}
        </el-tag>
        <el-tag :type="hasFilteredState ? 'warning' : 'info'" effect="light">
          {{ hasFilteredState ? '筛选已生效' : '未过滤' }}
        </el-tag>
        <el-tag type="info" effect="light">时间范围 {{ timeRangeLabel }}</el-tag>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact execution-log-workbench__workspace">
      <div class="panel-card glass-panel execution-log-workbench__left-panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>筛选条件</h2>
            <el-tag :type="selectedTaskId ? 'success' : 'info'" effect="dark">
              {{ selectedTaskId ? selectedTaskName : '未选择' }}
            </el-tag>
          </div>
          <el-space>
            <el-button size="small" @click="resetFilters" :disabled="!hasFilteredState">重置</el-button>
            <el-tooltip :disabled="!!selectedTaskId" content="请先选择同步任务" placement="top">
              <span>
                <el-button size="small" type="primary" plain :disabled="!selectedTaskId" @click="loadTaskLogs">查询</el-button>
              </span>
            </el-tooltip>
          </el-space>
        </div>

        <div class="execution-log-workbench__filter-grid">
          <el-select
            v-model="selectedTaskId"
            placeholder="选择任务"
            filterable
            style="width: 100%;"
            @change="handleTaskChange"
          >
            <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
          </el-select>
          <el-select v-model="filters.logLevel" placeholder="日志级别" clearable style="width: 100%;" @change="loadTaskLogs">
            <el-option label="信息" value="INFO" />
            <el-option label="警告" value="WARN" />
            <el-option label="错误" value="ERROR" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="关键词" clearable @input="loadTaskLogs" />
          <el-input v-model="filters.runId" placeholder="run_id" clearable @input="loadTaskLogs" />
          <el-input v-model="filters.tableName" placeholder="表名" clearable @input="loadTaskLogs" />
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%;"
            @change="onTimeRangeChange"
          />
        </div>

        <div class="execution-log-workbench__quick-stats">
          <div class="status-item">
            <span class="status-item__label">run_id</span>
            <span class="status-item__value">{{ filters.runId || '-' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">表名</span>
            <span class="status-item__value">{{ filters.tableName || '-' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">级别</span>
            <span class="status-item__value">{{ filters.logLevel || '全部' }}</span>
          </div>
        </div>

        <div class="execution-log-workbench__retention-card">
          <div class="section-title section-title--compact">
            <div class="section-title__left">
              <h3>日志保留设置</h3>
              <el-tag type="info" effect="light">{{ retentionDays }} 天</el-tag>
            </div>
          </div>
          <div class="execution-log-workbench__retention-row">
            <el-input-number v-model="retentionDays" :min="1" :max="3650" :step="1" />
            <el-button type="primary" plain @click="saveRetentionDays">保存</el-button>
          </div>
          <div class="execution-log-workbench__retention-hint">
            调整后仅影响后续清理历史的保留周期，不会立刻删除日志。
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel execution-log-workbench__table-panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>日志记录</h2>
            <el-tag :type="logDensityTagType" effect="dark">{{ logDensityText }}</el-tag>
          </div>
          <el-space>
            <el-button size="small" @click="goToExecutionHistory">执行历史</el-button>
            <el-button size="small" type="primary" plain :disabled="!logs.length" @click="exportCurrentLogs">导出</el-button>
          </el-space>
        </div>
        <div class="execution-log-workbench__table-hint">
          {{ tableHint }}
        </div>
        <div class="table-shell execution-log-workbench__table-shell">
          <el-table :data="logs" border stripe v-loading="loading" :empty-text="tableEmptyText">
            <el-table-column label="时间" width="220">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="runId" label="run_id" width="220" show-overflow-tooltip />
            <el-table-column prop="tableName" label="表名" width="180" show-overflow-tooltip />
            <el-table-column label="级别" width="120">
              <template #default="{ row }">
                <el-tag :type="logLevelTagType(row.logLevel)" effect="light">{{ logLevelLabel(row.logLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="logMessage" label="日志内容" min-width="520" show-overflow-tooltip />
          </el-table>
          <StateEmpty
            v-if="emptyStateType === 'no-task'"
            title="先选择一个任务"
            description="选择同步任务后，这里会展示对应的执行轨迹、失败原因和重试记录。"
            hint="你也可以从执行历史进入某个 run 再回到这里。"
            button-text="去执行历史"
            @action="goToExecutionHistory"
          />
          <StateEmpty
            v-else-if="emptyStateType === 'no-logs'"
            title="还没有执行日志"
            description="先执行一次同步任务，或调整筛选条件后重新查询。"
            hint="日志会记录 run_id、表名、级别和失败原因。"
            button-text="去执行历史"
            @action="goToExecutionHistory"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cleanupLogs, getDiagnosticsStatus, getLogRetentionDays, listLogs, listTasks, updateLogRetentionDays } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])
const logs = ref([])
const selectedTaskId = ref(null)
const selectedTaskName = ref('')
const cleaning = ref(false)
const diagnosticsLoading = ref(false)
const retentionDays = ref(30)
const filters = reactive({
  keyword: '',
  logLevel: '',
  runId: '',
  tableName: '',
  startTime: '',
  endTime: ''
})
const timeRange = ref([])

const hasLogs = computed(function () {
  return logs.value.length > 0
})

const hasFilteredState = computed(function () {
  return !!filters.keyword || !!filters.logLevel || !!filters.runId || !!filters.tableName || !!filters.startTime || !!filters.endTime
})

const selectedTask = computed(function () {
  if (!selectedTaskId.value) {
    return null
  }
  return tasks.value.find(function (task) {
    return task.id === selectedTaskId.value
  }) || null
})

const currentTaskHint = computed(function () {
  if (!selectedTask.value) {
    return '选择任务后即可查看对应执行轨迹'
  }
  const source = selectedTask.value.sourceSchemaName ? selectedTask.value.sourceSchemaName + '.' : ''
  const target = selectedTask.value.targetSchemaName ? selectedTask.value.targetSchemaName + '.' : ''
  return source + (selectedTask.value.sourceTableName || '-') + ' → ' + target + (selectedTask.value.targetTableName || '-')
})

const errorCount = computed(function () {
  return logs.value.filter(function (row) {
    return String(row.logLevel || '').toUpperCase() === 'ERROR'
  }).length
})

const warningCount = computed(function () {
  return logs.value.filter(function (row) {
    return String(row.logLevel || '').toUpperCase() === 'WARN'
  }).length
})

const errorWarningSummary = computed(function () {
  return errorCount.value + ' / ' + warningCount.value
})

const errorWarningHint = computed(function () {
  if (errorCount.value > 0) {
    return '优先排查 ERROR 日志'
  }
  if (warningCount.value > 0) {
    return '当前存在 WARN 日志'
  }
  return '当前没有 ERROR / WARN 日志'
})

const timeRangeLabel = computed(function () {
  if (filters.startTime && filters.endTime) {
    return '已选择'
  }
  return '全部'
})

const promptText = computed(function () {
  if (!selectedTask.value) {
    return '先选择任务，再查看执行轨迹、失败原因和重试记录。'
  }
  if (!logs.value.length) {
    return '当前筛选条件下没有日志，可以调整关键字、run_id、表名或时间范围后再查询。'
  }
  if (errorCount.value > 0) {
    return '已有 ERROR 日志，建议先定位 run_id、表名和时间范围。'
  }
  if (warningCount.value > 0) {
    return '当前存在 WARN 日志，可继续结合 run_id 排查。'
  }
  return '可以直接按 run_id、表名和级别定位执行轨迹。'
})

const logDensityText = computed(function () {
  if (!logs.value.length) {
    return '0 条'
  }
  return logs.value.length + ' 条'
})

const logDensityTagType = computed(function () {
  if (errorCount.value > 0) {
    return 'danger'
  }
  if (warningCount.value > 0) {
    return 'warning'
  }
  return logs.value.length ? 'success' : 'info'
})

const tableHint = computed(function () {
  if (!selectedTask.value) {
    return '请选择任务后查看执行日志'
  }
  if (!logs.value.length) {
    return '当前任务暂无日志，或筛选条件下无结果'
  }
  return '日志内容支持按 run_id、表名、级别和时间范围进行排查'
})

const tableEmptyText = computed(function () {
  if (!selectedTask.value) {
    return '请选择任务'
  }
  if (!logs.value.length) {
    return '暂无日志'
  }
  return '暂无更多日志'
})

const emptyStateType = computed(function () {
  if (!selectedTask.value) {
    return 'no-task'
  }
  if (!logs.value.length) {
    return 'no-logs'
  }
  return ''
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    tasks.value = await listTasks()
    retentionDays.value = Number(await getLogRetentionDays()) || 30
    if (tasks.value.length > 0) {
      selectedTaskId.value = selectedTaskId.value || tasks.value[0].id
      const current = tasks.value.find(function (task) {
        return task.id === selectedTaskId.value
      }) || tasks.value[0]
      if (current) {
        selectedTaskId.value = current.id
        selectedTaskName.value = current.taskName
      }
    }
    await loadTaskLogs()
  } catch (error) {
    ElMessage.error(error.message || '加载日志任务列表失败')
  }
}

async function loadTaskLogs() {
  if (!selectedTaskId.value) {
    logs.value = []
    return
  }
  loading.value = true
  try {
    const currentTask = tasks.value.find(function (task) {
      return task.id === selectedTaskId.value
    })
    selectedTaskName.value = currentTask ? currentTask.taskName : ''
    logs.value = await listLogs(selectedTaskId.value, {
      runId: filters.runId,
      tableName: filters.tableName,
      logLevel: filters.logLevel,
      keyword: filters.keyword,
      startTime: filters.startTime,
      endTime: filters.endTime
    })
  } catch (error) {
    ElMessage.error(error.message || '加载日志失败')
  } finally {
    loading.value = false
  }
}

function handleTaskChange() {
  loadTaskLogs()
}

function onTimeRangeChange(value) {
  if (!value || value.length !== 2) {
    filters.startTime = ''
    filters.endTime = ''
  } else {
    filters.startTime = value[0]
    filters.endTime = value[1]
  }
  loadTaskLogs()
}

function resetFilters() {
  filters.keyword = ''
  filters.logLevel = ''
  filters.runId = ''
  filters.tableName = ''
  filters.startTime = ''
  filters.endTime = ''
  timeRange.value = []
  loadTaskLogs()
}

async function saveRetentionDays() {
  try {
    await updateLogRetentionDays({
      retentionDays: retentionDays.value
    })
    ElMessage.success('日志保留天数已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存保留天数失败')
  }
}

async function cleanupHistory() {
  if (!logs.value.length) {
    ElMessage.warning('当前没有可清理的日志')
    return
  }
  try {
    await ElMessageBox.confirm('清理历史会删除早于保留天数的执行日志，是否继续？', '清理历史', {
      type: 'warning',
      confirmButtonText: '继续清理',
      cancelButtonText: '取消'
    })
  } catch (error) {
    return
  }
  cleaning.value = true
  try {
    const result = await cleanupLogs({
      retentionDays: retentionDays.value
    })
    ElMessage.success('已清理 ' + ((result && result.executionLogDeletedCount) || 0) + ' 条日志')
    await loadTaskLogs()
  } catch (error) {
    ElMessage.error(error.message || '清理日志失败')
  } finally {
    cleaning.value = false
  }
}

function goToExecutionHistory() {
  router.push('/execution-history')
}

function exportCurrentLogs() {
  const rows = logs.value || []
  if (!rows.length) {
    ElMessage.warning('当前没有可导出的日志')
    return
  }
  const header = ['时间', 'run_id', '表名', '级别', '日志内容']
  const lines = [header.join(',')]
  rows.forEach(function (row) {
    lines.push([
      csvEscape(formatTime(row.createdAt)),
      csvEscape(row.runId || ''),
      csvEscape(row.tableName || ''),
      csvEscape(row.logLevel || ''),
      csvEscape(row.logMessage || '')
    ].join(','))
  })
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = buildExportFileName('logs')
  link.click()
  window.setTimeout(function () {
    URL.revokeObjectURL(url)
  }, 0)
  ElMessage.success('日志已导出')
}

async function exportDiagnostics() {
  diagnosticsLoading.value = true
  try {
    const diagnostics = await getDiagnosticsStatus()
    const payload = JSON.stringify(diagnostics || {}, null, 2)
    const blob = new Blob([payload], { type: 'application/json;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = buildExportFileName('diagnostics', 'json')
    link.click()
    window.setTimeout(function () {
      URL.revokeObjectURL(url)
    }, 0)
    ElMessage.success('诊断信息已导出')
  } catch (error) {
    ElMessage.error(error.message || '导出诊断信息失败')
  } finally {
    diagnosticsLoading.value = false
  }
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function logLevelLabel(level) {
  const value = String(level || '').toUpperCase()
  if (value === 'INFO') {
    return '信息'
  }
  if (value === 'WARN') {
    return '警告'
  }
  if (value === 'ERROR') {
    return '错误'
  }
  if (value === 'DEBUG') {
    return '调试'
  }
  if (value === 'RETRY') {
    return '重试'
  }
  if (value === 'SYSTEM') {
    return '系统'
  }
  return '-'
}

function logLevelTagType(level) {
  const value = String(level || '').toUpperCase()
  if (value === 'ERROR') {
    return 'danger'
  }
  if (value === 'WARN') {
    return 'warning'
  }
  if (value === 'DEBUG') {
    return 'info'
  }
  if (value === 'RETRY') {
    return 'primary'
  }
  if (value === 'SYSTEM') {
    return 'info'
  }
  return 'info'
}

function csvEscape(value) {
  const text = String(value).replace(/\"/g, '""')
  return '"' + text + '"'
}

function buildExportFileName(prefix, extension) {
  const safeTaskName = (selectedTaskName.value || 'task').replace(/[^a-zA-Z0-9-_]+/g, '_')
  const stamp = formatFileStamp(new Date())
  return 'db-sync-studio-' + prefix + '-' + safeTaskName + '-' + stamp + '.' + (extension || 'csv')
}

function formatFileStamp(date) {
  const pad = function (value) {
    return String(value).padStart(2, '0')
  }
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    '_',
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join('')
}
</script>

<style scoped>
.execution-log-workbench {
  gap: 16px;
}

.execution-log-workbench__header {
  align-items: flex-start;
}

.execution-log-workbench__titleblock {
  min-width: 0;
}

.execution-log-workbench__titleblock h1 {
  margin-bottom: 4px;
}

.execution-log-workbench__toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.execution-log-workbench__overview .page-overview__value {
  font-size: 26px;
}

.execution-log-workbench__prompt-strip {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 14px 18px;
}

.execution-log-workbench__prompt-main {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.execution-log-workbench__prompt-label {
  color: var(--text-sub);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.execution-log-workbench__prompt-text {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.6;
}

.execution-log-workbench__prompt-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.execution-log-workbench__workspace {
  grid-template-columns: minmax(320px, 0.86fr) minmax(0, 1.14fr);
  align-items: start;
}

.execution-log-workbench__left-panel,
.execution-log-workbench__table-panel {
  min-height: 100%;
}

.execution-log-workbench__filter-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.execution-log-workbench__quick-stats {
  display: grid;
  gap: 10px;
}

.execution-log-workbench__retention-card {
  display: grid;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.execution-log-workbench__retention-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.execution-log-workbench__retention-hint {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
}

.execution-log-workbench__table-hint {
  margin-bottom: 12px;
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
}

.execution-log-workbench__table-shell {
  position: relative;
}

@media (max-width: 1120px) {
  .execution-log-workbench__workspace {
    grid-template-columns: minmax(0, 1fr);
  }

  .execution-log-workbench__filter-grid {
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  }
}

@media (max-width: 760px) {
  .execution-log-workbench__prompt-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .execution-log-workbench__prompt-meta {
    justify-content: flex-start;
  }

  .execution-log-workbench__retention-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
