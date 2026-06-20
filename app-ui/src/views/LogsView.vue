<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>执行日志</h1>
        <p>查看同步任务执行过程、失败原因和重试记录。</p>
      </div>
      <el-space>
        <el-button @click="loadPageData">刷新</el-button>
        <el-button :disabled="!logs.length" @click="exportCurrentLogs">导出</el-button>
        <el-button :loading="diagnosticsLoading" @click="exportDiagnostics">导出诊断</el-button>
        <el-button type="warning" :loading="cleaning" @click="cleanupHistory">清理历史</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--two">
      <div class="page-overview__item">
        <div class="page-overview__label">当前任务</div>
        <div class="page-overview__value">{{ selectedTaskId ? selectedTaskName : '未选择' }}</div>
        <div class="page-overview__hint">选择任务后即可查看对应执行轨迹</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">日志条数</div>
        <div class="page-overview__value">{{ logs.length }}</div>
        <div class="page-overview__hint">失败原因、重试信息和执行状态都可追踪</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">保留天数</div>
        <div class="page-overview__value">{{ retentionDays }}</div>
        <div class="page-overview__hint">默认保留 30 天，可安全调整</div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>筛选条件</h2>
        <el-tag type="info" effect="dark">{{ selectedTaskId ? selectedTaskName : '未选择' }}</el-tag>
      </div>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-select v-model="selectedTaskId" placeholder="选择任务" style="width: 100%;" @change="loadTaskLogs">
            <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-select v-model="filters.logLevel" placeholder="日志级别" clearable style="width: 100%;" @change="loadTaskLogs">
            <el-option label="INFO" value="INFO" />
            <el-option label="WARN" value="WARN" />
            <el-option label="ERROR" value="ERROR" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-input v-model="filters.keyword" placeholder="关键词" clearable @input="loadTaskLogs" />
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top: 12px;">
        <el-col :span="8">
          <el-input v-model="filters.runId" placeholder="run_id" clearable @input="loadTaskLogs" />
        </el-col>
        <el-col :span="8">
          <el-input v-model="filters.tableName" placeholder="表名" clearable @input="loadTaskLogs" />
        </el-col>
        <el-col :span="8">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%;"
            @change="onTimeRangeChange"
          />
        </el-col>
      </el-row>
      <div class="status-stack logs-summary" style="margin-top: 16px;">
        <div class="status-item">
          <span class="status-item__label">run_id</span>
          <span class="status-item__value">{{ filters.runId || '-' }}</span>
        </div>
        <div class="status-item">
          <span class="status-item__label">时间范围</span>
          <span class="status-item__value">{{ filters.startTime || '-' }} ~ {{ filters.endTime || '-' }}</span>
        </div>
      </div>
      <div class="status-stack" style="margin: 16px 0 20px;">
        <div class="status-item">
          <span class="status-item__label">日志保留</span>
          <span class="status-item__value">
            <el-input-number v-model="retentionDays" :min="1" :max="3650" :step="1" style="width: 160px;" />
            <el-button style="margin-left: 12px;" type="primary" plain @click="saveRetentionDays">保存</el-button>
          </span>
        </div>
      </div>
      <div class="table-shell">
        <el-table :data="logs" border stripe v-loading="loading">
          <el-table-column label="时间" width="220">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="runId" label="run_id" width="220" show-overflow-tooltip />
          <el-table-column prop="tableName" label="表名" width="180" show-overflow-tooltip />
          <el-table-column prop="logLevel" label="级别" width="120" />
          <el-table-column prop="logMessage" label="日志内容" min-width="520" />
        </el-table>
        <StateEmpty
          v-if="!loading && !logs.length"
          title="还没有执行日志"
          description="先选一个任务，或者执行一次同步任务。"
          hint="日志会记录 run_id、表名、级别和失败原因。"
          button-text="去执行历史"
          @action="goToExecutionHistory"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    tasks.value = await listTasks()
    retentionDays.value = Number(await getLogRetentionDays()) || 30
    if (tasks.value.length > 0) {
      selectedTaskId.value = tasks.value[0].id
      selectedTaskName.value = tasks.value[0].taskName
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
    const selectedTask = tasks.value.find(function (task) {
      return task.id === selectedTaskId.value
    })
    selectedTaskName.value = selectedTask ? selectedTask.taskName : ''
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
