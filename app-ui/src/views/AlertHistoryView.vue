<template>
  <div class="page-section list-page alert-history-workbench">
    <div class="page-header alert-history-workbench__header">
      <div class="alert-history-workbench__titleblock">
        <h1>告警历史</h1>
        <p>查看每次告警发送结果、关联任务与运行批次，支持按任务、状态、类型和时间筛选。</p>
      </div>
      <div class="alert-history-workbench__toolbar">
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
      </div>
    </div>

    <div class="stats-grid alert-history-workbench__stats">
      <div v-for="item in summaryCards" :key="item.label" class="stat-card alert-history-workbench__stat-card">
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
        <div class="stat-card__hint">{{ item.hint }}</div>
      </div>
    </div>

    <div class="panel-card alert-history-workbench__filters">
      <div class="section-title">
        <div class="section-title__left">
          <h2>筛选条件</h2>
          <el-tag :type="filterTagType" effect="light">{{ filterSummaryChip }}</el-tag>
        </div>
        <el-space>
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" :loading="loading" @click="loadPageData">查询</el-button>
        </el-space>
      </div>
      <div class="alert-history-workbench__filter-hint">
        {{ filterHint }}
      </div>
      <div class="alert-history-workbench__filter-grid">
        <el-select v-model="filters.taskId" clearable filterable placeholder="任务" style="width: 100%;">
          <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
        </el-select>
        <el-select v-model="filters.alertType" clearable placeholder="类型" style="width: 100%;">
          <el-option v-for="item in alertTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.sendStatus" clearable placeholder="状态" style="width: 100%;">
          <el-option label="发送成功" value="SUCCESS" />
          <el-option label="发送失败" value="FAILED" />
          <el-option label="等待发送" value="PENDING" />
        </el-select>
        <el-input v-model="filters.keyword" clearable placeholder="内容关键字" />
        <el-date-picker
          v-model="filters.timeRange"
          type="datetimerange"
          value-format="x"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 100%;"
        />
        <el-input-number v-model="filters.limit" :min="1" :max="200" style="width: 100%;" />
      </div>
      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
        style="margin-top: 14px;"
      />
    </div>

    <div class="panel-card alert-history-workbench__table-panel">
      <div class="section-title">
        <div class="section-title__left">
          <h2>告警记录</h2>
          <el-tag :type="historyTagType" effect="light">{{ historySummaryChip }}</el-tag>
        </div>
        <el-tag effect="dark" :type="failedCount > 0 ? 'danger' : 'success'">{{ failedCount > 0 ? '需排查' : '当前正常' }}</el-tag>
      </div>
      <div class="alert-history-workbench__table-hint">
        {{ tableHint }}
      </div>
      <div v-if="hasHistory" class="table-shell alert-history-workbench__table-shell" v-loading="loading">
        <el-table :data="history" border stripe>
          <el-table-column prop="createdTime" label="时间" width="190">
            <template #default="{ row }">
              <span :title="formatTime(row.createdTime)">{{ formatTime(row.createdTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="alert-history-workbench__cell-stack">
                <div class="alert-history-workbench__primary">{{ alertTypeLabel(row.alertType) }}</div>
                <div class="alert-history-workbench__secondary">{{ row.alertType || '-' }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="任务" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              {{ resolveTaskName(row.taskId) }}
            </template>
          </el-table-column>
          <el-table-column label="运行批次" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="alert-history-workbench__cell-stack">
                <div class="alert-history-workbench__primary">{{ row.runId || '-' }}</div>
                <div class="alert-history-workbench__secondary">Run ID</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="tableName" label="表名" min-width="140" show-overflow-tooltip />
          <el-table-column label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="alertLevelTagType(row.alertLevel)" effect="light">{{ alertLevelLabel(row.alertLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="渠道" width="140">
            <template #default="{ row }">
              <el-tag :type="channelTagType(row.channelType)" effect="light">{{ channelTypeLabel(row.channelType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="sendStatusTagType(row.sendStatus)" effect="light">{{ sendStatusLabel(row.sendStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="内容摘要" min-width="280" show-overflow-tooltip>
            <template #default="{ row }">
              {{ previewText(row.alertContent) }}
            </template>
          </el-table-column>
          <el-table-column label="结果摘要" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ previewResult(row) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button link type="primary" @click="goToAlertSettings">配置</el-button>
                <el-button link @click="goToHistoryRoute(row)">查看关联</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <StateEmpty
        v-else-if="!loading && !isFiltered"
        title="还没有告警历史"
        description="告警规则触发并完成发送后，这里会记录发送结果、关联任务和运行批次。"
        hint="请先在“告警设置”中配置告警规则和渠道。"
        button-text="去告警设置"
        @action="goToAlertSettings"
      />
      <StateEmpty
        v-else-if="!loading && isFiltered && !history.length"
        title="当前筛选条件下没有告警历史"
        description="可以调整任务、状态、类型或时间范围后重新查询。"
        hint="也可以先重置筛选，再查看全部记录。"
        button-text="重置筛选"
        @action="resetFilters"
      />
    </div>

    <div v-if="hasHistory" class="alert-history-workbench__footer-note">
      告警历史会保留发送时间、任务、运行批次、渠道和错误摘要，便于快速定位渠道配置问题。
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listAlertHistory, listTasks } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const tasks = ref([])
const history = ref([])
const filters = reactive({
  taskId: null,
  alertType: '',
  sendStatus: '',
  timeRange: [],
  keyword: '',
  limit: 50
})

const alertTypeOptions = [
  { label: '任务执行失败', value: 'TASK_EXECUTION_FAILED' },
  { label: '任务连续失败 N 次', value: 'TASK_CONSECUTIVE_FAILED' },
  { label: '数据校验不一致', value: 'VALIDATION_INCONSISTENT' },
  { label: '数据修复失败', value: 'REPAIR_FAILED' },
  { label: '数据源连接失败', value: 'DATASOURCE_CONNECTION_FAILED' },
  { label: '同步耗时超过阈值', value: 'TASK_DURATION_EXCEEDED' },
  { label: '同步延迟超过阈值', value: 'SYNC_DELAY_EXCEEDED' },
  { label: '表同步失败', value: 'TABLE_SYNC_FAILED' },
  { label: '调度任务跳过执行', value: 'SCHEDULE_SKIPPED' }
]

const successCount = computed(function () {
  return history.value.filter(function (item) {
    return item.sendStatus === 'SUCCESS'
  }).length
})

const failedCount = computed(function () {
  return history.value.filter(function (item) {
    return item.sendStatus === 'FAILED' || item.sendStatus === 'CONFIG_ERROR'
  }).length
})

const latestAlert = computed(function () {
  return history.value.length ? history.value[0] : null
})

const isFiltered = computed(function () {
  return !!filters.taskId || !!filters.alertType || !!filters.sendStatus || !!filters.keyword || hasTimeRange(filters.timeRange) || Number(filters.limit || 50) !== 50
})

const hasHistory = computed(function () {
  return history.value.length > 0
})

const filterSummaryChip = computed(function () {
  if (!isFiltered.value) {
    return '全部记录'
  }
  if (!history.value.length) {
    return '0 条结果'
  }
  return '已筛选'
})

const filterTagType = computed(function () {
  if (!isFiltered.value) {
    return 'info'
  }
  if (!history.value.length) {
    return 'danger'
  }
  return 'warning'
})

const historySummaryChip = computed(function () {
  if (!history.value.length) {
    return '0 条'
  }
  return isFiltered.value ? '已筛选' : 'N 条'
})

const historyTagType = computed(function () {
  if (!history.value.length) {
    return 'info'
  }
  return isFiltered.value ? 'warning' : 'success'
})

const summaryCards = computed(function () {
  return [
    { label: '告警总数', value: history.value.length, hint: '按当前筛选条件统计' },
    { label: '成功发送', value: successCount.value, hint: '发送状态 = 成功' },
    { label: '发送失败', value: failedCount.value, hint: '便于定位渠道配置问题' },
    { label: '最近告警', value: latestAlert.value ? sendStatusLabel(latestAlert.value.sendStatus) : '—', hint: latestAlert.value ? formatLatestAlertHint(latestAlert.value) : '暂无告警发送记录' }
  ]
})

const filterHint = computed(function () {
  if (!isFiltered.value) {
    return '筛选条件会同步影响统计卡片和表格结果。'
  }
  return history.value.length ? '当前筛选条件已生效，结果会同步影响统计卡片和表格内容。' : '当前筛选条件下没有告警历史。'
})

const tableHint = computed(function () {
  if (!history.value.length) {
    return isFiltered.value ? '调整任务、状态、类型或时间范围后重新查询。' : '告警规则触发并完成发送后，这里会记录发送结果、关联任务和运行批次。'
  }
  if (failedCount.value > 0) {
    return '失败记录会优先显示错误摘要，便于排查渠道配置、超时或模板问题。'
  }
  return '记录会保留任务、运行批次、渠道和发送结果，方便回溯。'
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  const query = buildQuery()
  loading.value = true
  loadError.value = ''
  try {
    const result = await Promise.all([
      listTasks(),
      listAlertHistory(query)
    ])
    tasks.value = result[0] || []
    history.value = result[1] || []
  } catch (error) {
    loadError.value = getErrorMessage(error, '加载告警历史失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.taskId = null
  filters.alertType = ''
  filters.sendStatus = ''
  filters.timeRange = []
  filters.keyword = ''
  filters.limit = 50
  loadPageData()
}

function buildQuery() {
  return {
    taskId: filters.taskId,
    alertType: filters.alertType,
    sendStatus: filters.sendStatus,
    keyword: filters.keyword,
    startTime: filters.timeRange && filters.timeRange.length ? Number(filters.timeRange[0]) : null,
    endTime: filters.timeRange && filters.timeRange.length ? Number(filters.timeRange[1]) : null,
    limit: filters.limit
  }
}

function resolveTaskName(taskId) {
  const task = tasks.value.find(function (item) {
    return item.id === taskId
  })
  return task ? task.taskName : (taskId || '-')
}

function goToAlertSettings() {
  router.push('/alert-settings')
}

function goToHistoryRoute(row) {
  if (!row || !row.taskId || !row.runId) {
    return
  }
  router.push({
    path: '/execution-history/detail',
    query: {
      taskId: String(row.taskId),
      runId: row.runId
    }
  })
}

function alertTypeLabel(type) {
  const item = alertTypeOptions.find(function (option) {
    return option.value === type
  })
  return item ? item.label : (type || '-')
}

function alertLevelTagType(level) {
  if (level === 'ERROR') {
    return 'danger'
  }
  if (level === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function alertLevelLabel(level) {
  if (level === 'ERROR') {
    return '高'
  }
  if (level === 'WARNING') {
    return '中'
  }
  if (level === 'INFO') {
    return '低'
  }
  return '-'
}

function channelTypeLabel(channelType) {
  if (channelType === 'SMTP') {
    return '邮件 SMTP'
  }
  if (channelType === 'WEBHOOK') {
    return 'Webhook'
  }
  return '-'
}

function channelTagType(channelType) {
  if (channelType === 'WEBHOOK') {
    return 'info'
  }
  if (channelType === 'SMTP') {
    return 'primary'
  }
  return 'info'
}

function sendStatusTagType(status) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAILED' || status === 'CONFIG_ERROR') {
    return 'danger'
  }
  if (status === 'PENDING' || status === 'RETRYING') {
    return 'warning'
  }
  return 'info'
}

function sendStatusLabel(status) {
  if (status === 'SUCCESS') {
    return '发送成功'
  }
  if (status === 'FAILED') {
    return '发送失败'
  }
  if (status === 'PENDING') {
    return '等待发送'
  }
  if (status === 'RETRYING') {
    return '已重试'
  }
  if (status === 'SKIPPED') {
    return '已跳过'
  }
  if (status === 'CONFIG_ERROR') {
    return '配置异常'
  }
  return status || '-'
}

function previewText(value) {
  return sanitizeText(value || '-', 140)
}

function previewResult(row) {
  const source = row && row.errorMessage ? row.errorMessage : (row && row.sendStatus === 'SUCCESS' ? '发送成功' : '暂无结果')
  return sanitizeText(source, 160)
}

function sanitizeText(value, maxLength) {
  const text = String(value || '-')
    .replace(/(smtp:\/\/)[^@]+@/ig, '$1***@')
    .replace(/(webhook[^\\s]*)/ig, 'webhook***')
    .replace(/([?&](token|secret|password)=)[^&\\s]+/ig, '$1***')
  if (text.length <= maxLength) {
    return text
  }
  return text.slice(0, maxLength - 1) + '…'
}

function formatLatestAlertHint(row) {
  return sendStatusLabel(row.sendStatus) + ' · ' + channelTypeLabel(row.channelType)
}

function hasTimeRange(timeRange) {
  return Array.isArray(timeRange) && timeRange.length === 2 && !!timeRange[0] && !!timeRange[1]
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function getErrorMessage(error, fallback) {
  if (error && error.message) {
    return error.message
  }
  return fallback
}
</script>

<style scoped>
.alert-history-workbench {
  display: grid;
  gap: 16px;
}

.alert-history-workbench__header {
  align-items: flex-start;
}

.alert-history-workbench__titleblock {
  display: grid;
  gap: 6px;
}

.alert-history-workbench__toolbar {
  display: flex;
  align-items: flex-start;
}

.alert-history-workbench__stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.alert-history-workbench__stat-card {
  min-height: 120px;
}

.alert-history-workbench__filters {
  display: grid;
  gap: 14px;
  border-radius: 16px;
  border: 1px solid #e5eaf3;
  background: rgba(255, 255, 255, 0.96);
}

.alert-history-workbench__filter-hint,
.alert-history-workbench__table-hint,
.alert-history-workbench__footer-note {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.alert-history-workbench__filter-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 1.1fr 1.4fr 0.7fr;
  gap: 12px;
  align-items: center;
}

.alert-history-workbench__table-panel {
  display: grid;
  gap: 14px;
}

.alert-history-workbench__table-shell {
  min-height: 180px;
}

.alert-history-workbench__cell-stack {
  display: grid;
  gap: 4px;
}

.alert-history-workbench__primary {
  font-weight: 650;
  color: #0f172a;
}

.alert-history-workbench__secondary {
  color: #64748b;
  font-size: 12px;
}

.alert-history-workbench__footer-note {
  padding: 0 4px;
}

@media (max-width: 1280px) {
  .alert-history-workbench__filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .alert-history-workbench__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .alert-history-workbench__filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .alert-history-workbench__stats,
  .alert-history-workbench__filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
