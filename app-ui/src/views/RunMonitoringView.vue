<template>
  <div class="page-section run-monitoring">
    <div class="page-header">
      <div>
        <h1>运行监控</h1>
        <p>集中查看任务指标、表级指标、连接状态和近 24 小时趋势。</p>
      </div>
      <el-space>
        <el-button round :loading="loading" @click="cleanupMonitoring">清理 30 天前数据</el-button>
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
        <el-button round @click="goToExecutionHistory">执行历史</el-button>
      </el-space>
    </div>

    <div class="stats-grid">
      <div v-for="item in summaryCards" :key="item.label" class="stat-card glass-panel">
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
        <div class="stat-card__hint">{{ item.hint }}</div>
      </div>
    </div>

    <div class="dashboard-panels">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>近 24 小时趋势</h2>
          <el-tag type="info" effect="dark">SQLite 指标</el-tag>
        </div>
        <div class="monitor-chart">
          <svg
            v-if="trendChart.points.length"
            class="monitor-chart__svg"
            viewBox="0 0 560 220"
            preserveAspectRatio="none"
            role="img"
            aria-label="近 24 小时任务趋势图"
          >
            <line
              v-for="gridLine in trendChart.gridLines"
              :key="gridLine.y"
              x1="44"
              :y1="gridLine.y"
              x2="530"
              :y2="gridLine.y"
              class="monitor-chart__grid"
            />
            <polyline :points="trendChart.totalPolyline" class="monitor-chart__line monitor-chart__line--total" />
            <polyline :points="trendChart.failedPolyline" class="monitor-chart__line monitor-chart__line--failed" />
            <g v-for="point in trendChart.points" :key="point.key">
              <circle :cx="point.x" :cy="point.totalY" r="4" class="monitor-chart__dot monitor-chart__dot--total" />
              <circle :cx="point.x" :cy="point.failedY" r="4" class="monitor-chart__dot monitor-chart__dot--failed" />
              <text :x="point.x" y="204" text-anchor="middle" class="monitor-chart__label">{{ point.label }}</text>
              <text :x="point.x" :y="point.totalY - 10" text-anchor="middle" class="monitor-chart__value">{{ point.totalCount }}</text>
            </g>
          </svg>
          <el-empty v-else description="暂无趋势数据" />
          <StateEmpty
            v-if="!trendChart.points.length"
            title="还没有监控趋势"
            description="先执行一次同步任务，或等待定时任务产生指标。"
            hint="运行监控会展示任务趋势、失败任务、连接状态和最近指标。"
            button-text="去执行历史"
            @action="goToExecutionHistory"
          />
        </div>
        <div class="monitor-legend">
          <span class="monitor-legend__item">
            <span class="monitor-legend__swatch monitor-legend__swatch--total"></span>
            总任务数
          </span>
          <span class="monitor-legend__item">
            <span class="monitor-legend__swatch monitor-legend__swatch--failed"></span>
            失败任务数
          </span>
          <span class="monitor-legend__item">
            <span class="monitor-legend__swatch monitor-legend__swatch--success"></span>
            成功任务数
          </span>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>最近失败任务</h2>
          <el-tag type="danger" effect="dark">{{ monitoring.recentFailedTasks.length }} 个</el-tag>
        </div>
        <div class="status-stack">
          <div v-for="item in monitoring.recentFailedTasks" :key="item.runId" class="capability-item">
            <div class="capability-item__title">{{ item.taskName }}</div>
            <div class="capability-item__desc">
              <div>{{ item.message }}</div>
              <div class="task-log-meta">
                <span>run_id: {{ item.runId }}</span>
                <span>{{ formatTime(item.startedAt) }}</span>
                <span>{{ formatDuration(item.durationMillis) }}</span>
              </div>
            </div>
            <div class="run-monitoring__actions">
              <el-button link type="primary" @click="openRunDetail(item)">进入详情</el-button>
              <el-button link @click="openTaskHistory(item)">执行历史</el-button>
            </div>
          </div>
          <div v-if="!monitoring.recentFailedTasks.length" class="status-item">
            <span class="status-item__label">提示</span>
            <span class="status-item__value">最近没有失败任务</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>数据源连接状态</h2>
          <el-tag type="success" effect="dark">{{ connectedCount }} / {{ monitoring.datasourceStates.length }}</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="monitoring.datasourceStates" border stripe v-loading="testingConnections">
            <el-table-column prop="name" label="数据源" min-width="180" />
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="connectionTagType(row.status)">{{ connectionLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="latencyText" label="耗时" width="120" />
            <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>最近指标</h2>
          <el-tag type="warning" effect="dark">{{ monitoring.latestRuns.length }} 条</el-tag>
        </div>
        <div class="status-stack">
          <div v-for="item in monitoring.latestRuns.slice(0, 6)" :key="item.runId || item.taskId" class="status-item">
            <span class="status-item__label">
              {{ item.taskName }}
              <el-tag size="small" :type="statusTagType(item.runStatus)">{{ item.runStatus || '未运行' }}</el-tag>
            </span>
            <span class="status-item__value">{{ item.runId || '暂无 run' }}</span>
          </div>
          <div v-if="!monitoring.latestRuns.length" class="status-item">
            <span class="status-item__label">提示</span>
            <span class="status-item__value">当前没有可展示的指标记录</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  cleanupMonitoringMetrics,
  listDatasourceConnectionMetrics,
  listMonitoringOverview,
  listTableRunMetrics,
  listTaskRunMetrics,
  listDatasources,
  testDatasourceConnection
} from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const loading = ref(false)
const testingConnections = ref(false)
const monitoring = reactive({
  todaySummary: {
    totalTaskCount: 0,
    successTaskCount: 0,
    failedTaskCount: 0,
    runningTaskCount: 0,
    averageSpeedText: '-'
  },
  recentFailedTasks: [],
  datasourceStates: [],
  trend: [],
  latestRuns: []
})

const summaryCards = computed(function () {
  return [
    { label: '今日任务总数', value: monitoring.todaySummary.totalTaskCount, hint: '基于 task_run_metric' },
    { label: '成功任务数', value: monitoring.todaySummary.successTaskCount, hint: '成功与部分成功' },
    { label: '失败任务数', value: monitoring.todaySummary.failedTaskCount, hint: '包含失败与异常' },
    { label: '运行中任务数', value: monitoring.todaySummary.runningTaskCount, hint: '当前仍在执行' },
    { label: '平均同步速度', value: monitoring.todaySummary.averageSpeedText, hint: 'rows/s' }
  ]
})

const connectedCount = computed(function () {
  return monitoring.datasourceStates.filter(function (item) {
    return item.status === 'SUCCESS'
  }).length
})

const trendChart = computed(function () {
  const trend = monitoring.trend || []
  if (!trend.length) {
    return {
      points: [],
      gridLines: [],
      totalPolyline: '',
      failedPolyline: ''
    }
  }
  const chartLeft = 44
  const chartTop = 18
  const chartWidth = 486
  const chartHeight = 154
  const maxValue = Math.max(1, ...trend.map(function (item) {
    return Math.max(item.totalCount, item.failedCount, item.successCount)
  }))
  const gridLines = [0, 0.5, 1].map(function (ratio) {
    return { y: chartTop + (chartHeight * ratio) }
  })
  const points = trend.map(function (item, index) {
    const x = chartLeft + ((chartWidth / Math.max(1, trend.length - 1)) * index)
    return {
      key: item.key,
      label: item.label,
      totalCount: item.totalCount,
      failedCount: item.failedCount,
      successCount: item.successCount,
      x: x,
      totalY: chartTop + chartHeight - ((item.totalCount / maxValue) * chartHeight),
      failedY: chartTop + chartHeight - ((item.failedCount / maxValue) * chartHeight)
    }
  })
  return {
    points: points,
    gridLines: gridLines,
    totalPolyline: points.map(function (item) {
      return item.x + ',' + item.totalY
    }).join(' '),
    failedPolyline: points.map(function (item) {
      return item.x + ',' + item.failedY
    }).join(' ')
  }
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  loading.value = true
  try {
    const now = Date.now()
    const dayStart = startOfDay(now)
    const dayEnd = dayStart + (24 * 60 * 60 * 1000)
    const [overview, taskMetrics, tableMetrics, datasourceMetrics, datasources] = await Promise.all([
      listMonitoringOverview(),
      listTaskRunMetrics({ startTime: dayStart, endTime: dayEnd, limit: 200 }),
      listTableRunMetrics({ startTime: dayStart, endTime: dayEnd, limit: 200 }),
      listDatasourceConnectionMetrics({ startTime: dayStart, endTime: dayEnd, limit: 200 }),
      listDatasources()
    ])
    const datasourceStatuses = await loadDatasourceStatuses(datasources)
    applySnapshot({
      overview: overview,
      taskMetrics: taskMetrics || [],
      tableMetrics: tableMetrics || [],
      datasourceMetrics: mergeDatasourceMetrics(datasources, datasourceMetrics, datasourceStatuses),
      now: now
    })
  } catch (error) {
    ElMessage.error(error.message || '加载运行监控失败')
  } finally {
    loading.value = false
  }
}

async function loadDatasourceStatuses(datasources) {
  if (!datasources || !datasources.length) {
    return {}
  }
  testingConnections.value = true
  try {
    const entries = await Promise.all(datasources.map(async function (datasource) {
      try {
        const result = await testDatasourceConnection(datasource)
        return [datasource.id, result]
      } catch (error) {
        return [datasource.id, {
          success: false,
          message: error.message || '连接测试失败',
          costMillis: 0
        }]
      }
    }))
    return Object.fromEntries(entries)
  } finally {
    testingConnections.value = false
  }
}

function applySnapshot(snapshot) {
  const taskMetrics = Array.isArray(snapshot.taskMetrics) ? snapshot.taskMetrics : []
  const tableMetrics = Array.isArray(snapshot.tableMetrics) ? snapshot.tableMetrics : []
  const datasourceMetrics = Array.isArray(snapshot.datasourceMetrics) ? snapshot.datasourceMetrics : []
  monitoring.todaySummary = buildTodaySummary(snapshot.overview)
  monitoring.recentFailedTasks = buildRecentFailures(taskMetrics)
  monitoring.datasourceStates = datasourceMetrics.map(function (item) {
    return {
      datasourceId: item.datasourceId,
      name: item.name,
      type: item.type,
      status: item.connectionStatus,
      message: item.failureReason || (item.connectionStatus === 'SUCCESS' ? '连接正常' : '连接异常'),
      latencyText: item.lastTestConnectionMillis !== null && item.lastTestConnectionMillis !== undefined
        ? item.lastTestConnectionMillis + ' ms'
        : '-'
    }
  })
  monitoring.trend = buildTrendSeries(taskMetrics)
  monitoring.latestRuns = buildLatestRuns(taskMetrics, tableMetrics)
}

function buildTodaySummary(overview) {
  const summary = overview && overview.summary ? overview.summary : {}
  const latestTaskMetric = overview && overview.latestTaskMetric ? overview.latestTaskMetric : null
  const averageSpeed = latestTaskMetric && latestTaskMetric.speedRowsPerSecond
    ? Number(latestTaskMetric.speedRowsPerSecond)
    : 0
  return {
    totalTaskCount: Number(summary.totalTaskCount || 0),
    successTaskCount: Number(summary.successTaskCount || 0),
    failedTaskCount: Number(summary.failedTaskCount || 0),
    runningTaskCount: Number(summary.latestRunningTaskCount || 0),
    averageSpeedText: averageSpeed > 0 ? averageSpeed.toFixed(1) + ' rows/s' : '-'
  }
}

function buildRecentFailures(taskMetrics) {
  return taskMetrics.filter(function (metric) {
    return metric && (Number(metric.failedRowCount || 0) > 0 || metric.errorMessage)
  }).slice(0, 6).map(function (metric) {
    return {
      taskId: metric.taskId,
      taskName: metric.taskName || ('任务 #' + metric.taskId),
      runId: metric.runId,
      message: metric.errorMessage || '执行失败',
      startedAt: metric.metricTime,
      durationMillis: metric.durationMillis
    }
  })
}

function buildTrendSeries(taskMetrics) {
  const buckets = {}
  const now = Date.now()
  const hourMillis = 60 * 60 * 1000
  taskMetrics.forEach(function (metric) {
    const bucketTime = floorToHour(metric.metricTime)
    if (bucketTime < now - (24 * hourMillis)) {
      return
    }
    if (!buckets[bucketTime]) {
      buckets[bucketTime] = []
    }
    buckets[bucketTime].push(metric)
  })
  const points = []
  for (let index = 23; index >= 0; index -= 1) {
    const bucketTime = floorToHour(now - (index * hourMillis))
    const items = buckets[bucketTime] || []
    points.push({
      key: String(bucketTime),
      label: formatHourLabel(bucketTime),
      totalCount: items.length,
      failedCount: items.filter(function (item) {
        return Number(item.failedRowCount || 0) > 0 || item.errorMessage
      }).length,
      successCount: items.filter(function (item) {
        return Number(item.failedRowCount || 0) <= 0 && !item.errorMessage
      }).length
    })
  }
  return points
}

function buildLatestRuns(taskMetrics, tableMetrics) {
  const latestTask = taskMetrics.length ? taskMetrics[0] : null
  const latestTable = tableMetrics.length ? tableMetrics[0] : null
  const result = []
  if (latestTask) {
    result.push({
      taskId: latestTask.taskId,
      taskName: latestTask.taskName || ('任务 #' + latestTask.taskId),
      runId: latestTask.runId,
      runStatus: Number(latestTask.failedRowCount || 0) > 0 || latestTask.errorMessage ? 'FAILED' : 'SUCCESS'
    })
  }
  if (latestTable) {
    result.push({
      taskId: latestTable.taskId,
      taskName: latestTable.taskName || latestTable.tableName || ('表任务 #' + latestTable.tableTaskId),
      runId: latestTable.runId,
      runStatus: Number(latestTable.failedRowCount || 0) > 0 || latestTable.lastError ? 'FAILED' : 'SUCCESS'
    })
  }
  return result
}

function mergeDatasourceMetrics(datasources, datasourceMetrics, datasourceStatuses) {
  const metricsById = {}
  ;(datasourceMetrics || []).forEach(function (metric) {
    metricsById[metric.datasourceId] = metric
  })
  return (datasources || []).map(function (datasource) {
    const metric = metricsById[datasource.id] || {}
    const status = datasourceStatuses[datasource.id] || null
    return {
      datasourceId: datasource.id,
      name: datasource.name || ('数据源 #' + datasource.id),
      type: datasource.type || '-',
      connectionStatus: metric.connectionStatus || (status && status.success ? 'SUCCESS' : 'UNKNOWN'),
      failureReason: metric.failureReason || (status && !status.success ? status.message : null),
      lastTestConnectionMillis: metric.lastTestConnectionMillis !== undefined && metric.lastTestConnectionMillis !== null
        ? metric.lastTestConnectionMillis
        : (status ? status.costMillis : null)
    }
  })
}

function cleanupMonitoring() {
  return cleanupMonitoringMetrics({ retentionDays: 30 })
    .then(function () {
      ElMessage.success('已清理 30 天前的监控数据')
      return loadPageData()
    })
    .catch(function (error) {
      ElMessage.error(error.message || '清理监控数据失败')
    })
}

function openRunDetail(item) {
  if (!item || !item.taskId || !item.runId) {
    return
  }
  router.push({
    path: '/execution-history/detail',
    query: {
      taskId: String(item.taskId),
      runId: item.runId
    }
  })
}

function openTaskHistory(item) {
  if (!item || !item.taskId) {
    return
  }
  router.push({
    path: '/execution-history',
    query: {
      taskId: String(item.taskId)
    }
  })
}

function goToExecutionHistory() {
  router.push('/execution-history')
}

function statusTagType(status) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

function connectionTagType(status) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
}

function connectionLabel(status) {
  if (status === 'SUCCESS') {
    return '已连接'
  }
  if (status === 'FAILED') {
    return '异常'
  }
  if (status === 'UNKNOWN') {
    return '未测试'
  }
  return '未测试'
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function formatDuration(value) {
  if (!value && value !== 0) {
    return '-'
  }
  const seconds = Math.max(0, Math.floor(Number(value) / 1000))
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  if (minutes > 0) {
    return minutes + 'm ' + remainingSeconds + 's'
  }
  return remainingSeconds + 's'
}

function floorToHour(value) {
  const hourMillis = 60 * 60 * 1000
  const numeric = Number(value || 0)
  return Math.floor(numeric / hourMillis) * hourMillis
}

function formatHourLabel(time) {
  const date = new Date(time)
  const hour = date.getHours()
  return (hour < 10 ? '0' + hour : String(hour)) + ':00'
}
</script>

<style scoped>
.run-monitoring {
  display: grid;
  gap: 18px;
}

.run-monitoring .stats-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-top: 0;
}

.monitor-chart {
  min-height: 240px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.88), rgba(255, 255, 255, 0.98));
  border: 1px solid rgba(148, 163, 184, 0.16);
  padding: 10px 12px 2px;
}

.monitor-chart__svg {
  width: 100%;
  height: 220px;
  display: block;
}

.monitor-chart__grid {
  stroke: rgba(148, 163, 184, 0.28);
  stroke-width: 1;
}

.monitor-chart__line {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.monitor-chart__line--total {
  stroke: #2563eb;
}

.monitor-chart__line--failed {
  stroke: #ef4444;
}

.monitor-chart__dot {
  stroke: #ffffff;
  stroke-width: 2;
}

.monitor-chart__dot--total {
  fill: #2563eb;
}

.monitor-chart__dot--failed {
  fill: #ef4444;
}

.monitor-chart__label,
.monitor-chart__value {
  fill: #64748b;
  font-size: 11px;
}

.monitor-chart__value {
  fill: #0f172a;
  font-weight: 600;
}

.monitor-legend {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  margin-top: 12px;
  color: var(--text-sub);
  font-size: 12px;
}

.monitor-legend__item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.monitor-legend__swatch {
  width: 28px;
  height: 3px;
  border-radius: 999px;
}

.monitor-legend__swatch--total {
  background: #2563eb;
}

.monitor-legend__swatch--failed {
  background: #ef4444;
}

.monitor-legend__swatch--success {
  background: #16a34a;
}

.run-monitoring__actions {
  display: flex;
  gap: 12px;
  margin-top: 10px;
}

@media (max-width: 1280px) {
  .run-monitoring .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .run-monitoring .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .run-monitoring .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
