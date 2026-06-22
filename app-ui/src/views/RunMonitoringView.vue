<template>
  <div class="page-section run-monitoring">
    <div class="page-header run-monitoring__header">
      <div class="run-monitoring__titleblock">
        <h1>运行监控</h1>
        <p>集中查看任务指标、表级指标、数据源连接状态和近 24 小时运行趋势。</p>
      </div>
      <div class="run-monitoring__toolbar">
        <div class="run-monitoring__toolbar-actions">
          <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
          <el-button round @click="goToExecutionHistory">执行历史</el-button>
          <el-button round :loading="loading" type="danger" plain @click="handleCleanup">清理 30 天前数据</el-button>
        </div>
        <div class="run-monitoring__toolbar-note">
          {{ toolbarHint }}
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel run-monitoring__health-strip">
      <div class="run-monitoring__health-main">
        <div class="run-monitoring__health-head">
          <span class="run-monitoring__health-dot" :class="'run-monitoring__health-dot--' + healthState.type"></span>
          <div>
            <div class="run-monitoring__health-label">系统健康</div>
            <div class="run-monitoring__health-value">{{ healthState.label }}</div>
          </div>
        </div>
        <div class="run-monitoring__health-hint">{{ healthState.hint }}</div>
      </div>
      <div class="run-monitoring__health-meta">
        <div class="run-monitoring__health-summary">{{ healthSummaryText }}</div>
        <div class="run-monitoring__health-chips">
          <el-tag :type="healthState.type" effect="light">{{ healthState.badge }}</el-tag>
          <el-tag type="info" effect="light">{{ trendState.label }}</el-tag>
          <el-tag type="success" effect="light">{{ connectedCount }} / {{ monitoring.datasourceStates.length }} 在线</el-tag>
        </div>
      </div>
    </div>

    <div class="stats-grid run-monitoring__stats">
      <div v-for="item in summaryCards" :key="item.label" class="stat-card glass-panel">
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
        <div class="stat-card__hint">{{ item.hint }}</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact run-monitoring__top-panels">
      <div class="panel-card glass-panel run-monitoring__trend-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>近 24 小时趋势</h2>
            <el-tag :type="trendState.type" effect="light">{{ trendState.label }}</el-tag>
          </div>
          <el-tag type="info" effect="dark">SQLite 指标</el-tag>
        </div>
        <div class="run-monitoring__panel-hint">
          <span>{{ trendState.hint }}</span>
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
              <text
                v-if="point.showLabel"
                :x="point.x"
                y="204"
                text-anchor="middle"
                class="monitor-chart__label"
              >
                {{ point.label }}
              </text>
              <text :x="point.x" :y="point.totalY - 10" text-anchor="middle" class="monitor-chart__value">{{ point.totalCount }}</text>
            </g>
          </svg>
          <StateEmpty
            v-else
            title="还没有监控趋势"
            description="先执行一次同步任务，或等待定时任务产生指标。"
            hint="运行监控会展示任务趋势、失败任务、连接状态和最近指标。"
            button-text="执行历史"
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

      <div class="panel-card glass-panel run-monitoring__failure-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>最近失败任务</h2>
            <el-tag type="danger" effect="dark">{{ monitoring.recentFailedTasks.length }} 个</el-tag>
          </div>
          <el-tag :type="monitoring.recentFailedTasks.length ? 'warning' : 'success'" effect="light">
            {{ recentFailureLabel }}
          </el-tag>
        </div>
        <div class="run-monitoring__panel-hint">
          <span>{{ recentFailureHint }}</span>
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
          <div v-if="!monitoring.recentFailedTasks.length" class="status-item run-monitoring__empty-line">
            <span class="status-item__label">提示</span>
            <span class="status-item__value">最近没有失败任务</span>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact run-monitoring__bottom-panels">
      <div class="panel-card glass-panel run-monitoring__datasource-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>数据源连接状态</h2>
            <el-tag :type="datasourceHealthTagType" effect="light">{{ datasourceSummaryLabel }}</el-tag>
          </div>
          <el-tag type="success" effect="dark">{{ connectedCount }} / {{ monitoring.datasourceStates.length }}</el-tag>
        </div>
        <div class="run-monitoring__panel-hint">
          <span>{{ datasourceHint }}</span>
        </div>
        <div v-if="monitoring.datasourceStates.length" class="table-shell">
          <el-table :data="monitoring.datasourceStates" border stripe v-loading="testingConnections">
            <el-table-column prop="name" label="数据源" min-width="180" />
            <el-table-column prop="type" label="类型" width="140" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="connectionTagType(row.status)" effect="light">{{ connectionLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="latencyText" label="耗时" width="120" />
            <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
        <div v-else class="run-monitoring__empty-line run-monitoring__empty-line--panel">
          <span class="status-item__label">提示</span>
          <span class="status-item__value">当前没有可展示的数据源状态</span>
        </div>
      </div>

      <div class="panel-card glass-panel run-monitoring__latest-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>最近指标</h2>
            <el-tag type="warning" effect="dark">{{ monitoring.latestRuns.length }} 条</el-tag>
          </div>
          <el-tag :type="latestMetricsTagType" effect="light">{{ latestMetricsLabel }}</el-tag>
        </div>
        <div class="run-monitoring__panel-hint">
          <span>{{ latestMetricsHint }}</span>
        </div>
        <div v-if="monitoring.latestRuns.length" class="status-stack">
          <div v-for="item in monitoring.latestRuns.slice(0, 6)" :key="item.metricType + '-' + (item.runId || item.taskId)" class="run-monitoring__metric-card">
            <div class="run-monitoring__metric-card-head">
              <span class="run-monitoring__metric-card-scope">{{ item.scopeLabel }}</span>
              <el-tag size="small" :type="statusTagType(item.runStatus)" effect="light">{{ runStatusLabel(item.runStatus) }}</el-tag>
            </div>
            <div class="run-monitoring__metric-card-title">{{ item.taskName }}</div>
            <div class="run-monitoring__metric-card-meta">
              <span>{{ item.runId || '暂无 run' }}</span>
              <span>{{ item.note }}</span>
            </div>
          </div>
        </div>
        <div v-else class="run-monitoring__empty-line run-monitoring__empty-line--panel">
          <span class="status-item__label">提示</span>
          <span class="status-item__value">当前没有可展示的指标记录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cleanupMonitoringMetrics,
  listDatasourceConnectionMetrics,
  listMonitoringOverview,
  listTableRunMetrics,
  listTaskRunMetrics,
  listDatasources,
  testDatasourceConnection
} from '../services/backend'
import { startOfDay } from '../utils/runMonitoring'
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

const datasourceOfflineCount = computed(function () {
  return monitoring.datasourceStates.filter(function (item) {
    return item.status === 'FAILED'
  }).length
})

const datasourceUnknownCount = computed(function () {
  return monitoring.datasourceStates.filter(function (item) {
    return item.status === 'UNKNOWN'
  }).length
})

const healthState = computed(function () {
  const failedCount = Number(monitoring.todaySummary.failedTaskCount || 0)
  const runningCount = Number(monitoring.todaySummary.runningTaskCount || 0)
  const offlineCount = datasourceOfflineCount.value
  if (failedCount >= 3 || offlineCount >= 2 || (failedCount > 0 && offlineCount > 0)) {
    return {
      label: '异常',
      type: 'danger',
      badge: '需要处理',
      hint: '请优先查看失败任务和数据源连接状态。'
    }
  }
  if (failedCount > 0 || offlineCount > 0 || runningCount > 0) {
    return {
      label: '待观察',
      type: 'warning',
      badge: '需关注',
      hint: '发现失败任务、运行中任务或数据源异常，请查看详情。'
    }
  }
  return {
    label: '正常',
    type: 'success',
    badge: '健康',
    hint: '当前无失败任务，数据源连接正常。'
  }
})

const healthSummaryText = computed(function () {
  const parts = []
  const runningCount = Number(monitoring.todaySummary.runningTaskCount || 0)
  const failedCount = Number(monitoring.todaySummary.failedTaskCount || 0)
  if (runningCount > 0) {
    parts.push('当前有 ' + runningCount + ' 个任务运行中')
  } else {
    parts.push('当前无运行中任务')
  }
  if (failedCount > 0) {
    parts.push('最近有 ' + failedCount + ' 个失败任务')
  } else {
    parts.push('最近无失败任务')
  }
  if (monitoring.datasourceStates.length > 0) {
    parts.push('数据源 ' + connectedCount.value + '/' + monitoring.datasourceStates.length + ' 在线')
  }
  return parts.join(' · ')
})

const datasourceHealthTagType = computed(function () {
  if (datasourceOfflineCount.value > 0) {
    return 'warning'
  }
  if (monitoring.datasourceStates.length && connectedCount.value === monitoring.datasourceStates.length) {
    return 'success'
  }
  return 'info'
})

const datasourceSummaryLabel = computed(function () {
  if (!monitoring.datasourceStates.length) {
    return '未连接'
  }
  if (datasourceOfflineCount.value > 0) {
    return '有异常'
  }
  if (datasourceUnknownCount.value > 0) {
    return '待测试'
  }
  return '全部在线'
})

const datasourceHint = computed(function () {
  if (!monitoring.datasourceStates.length) {
    return '先配置数据源，再看连接健康与延迟。'
  }
  if (datasourceOfflineCount.value > 0) {
    return '离线或异常的数据源需要优先处理。'
  }
  if (datasourceUnknownCount.value > 0) {
    return '未测试的数据源会显示为待测试。'
  }
  return '连接状态来自最近测试结果，便于快速排查网络或配置问题。'
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
  const labelStep = trend.length > 12 ? Math.ceil(trend.length / 12) : 1
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
      showLabel: trend.length <= 12 || index % labelStep === 0 || index === trend.length - 1,
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

const activeTrendHours = computed(function () {
  return trendChart.value.points.filter(function (item) {
    return item.totalCount > 0
  }).length
})

const trendState = computed(function () {
  if (!trendChart.value.points.length || activeTrendHours.value === 0) {
    return {
      label: '暂无活动',
      type: 'info',
      hint: '近 24 小时暂无同步活动，等待任务执行产生趋势。'
    }
  }
  if (trendChart.value.points.some(function (item) {
    return item.failedCount > 0
  })) {
    return {
      label: '存在失败',
      type: 'warning',
      hint: '趋势中出现失败点，请结合失败任务与执行历史排查。'
    }
  }
  return {
    label: '运行平稳',
    type: 'success',
    hint: '近 24 小时运行趋势稳定，未看到失败波动。'
  }
})

const trendSummaryText = computed(function () {
  if (!trendChart.value.points.length) {
    return '近 24 小时暂无趋势数据'
  }
  return '近 24 小时 ' + activeTrendHours.value + ' 个小时有运行记录'
})

const recentFailureLabel = computed(function () {
  if (!monitoring.recentFailedTasks.length) {
    return '暂无失败'
  }
  return '需处理'
})

const recentFailureHint = computed(function () {
  if (!monitoring.recentFailedTasks.length) {
    return '最近没有失败任务，系统运行状态较平稳。'
  }
  return '失败任务会优先链接到执行历史，方便进一步排查。'
})

const latestMetricsLabel = computed(function () {
  if (!monitoring.latestRuns.length) {
    return '未生成'
  }
  return '最近更新'
})

const latestMetricsTagType = computed(function () {
  if (!monitoring.latestRuns.length) {
    return 'info'
  }
  return 'success'
})

const latestMetricsHint = computed(function () {
  if (!monitoring.latestRuns.length) {
    return '等待任务运行后，这里会显示最新任务级和表级指标。'
  }
  return '最近指标会展示任务级和表级最新记录，便于快速定位异常。'
})

const toolbarHint = computed(function () {
  if (loading.value) {
    return '正在刷新运行状态。'
  }
  if (healthState.value.type === 'danger') {
    return '先处理失败任务和数据源异常，再继续观察趋势。'
  }
  if (healthState.value.type === 'warning') {
    return '当前有任务运行中或存在异常，请关注趋势和失败任务。'
  }
  return '当前健康状态正常，可直接跳转执行历史继续排查。'
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
      runStatus: Number(latestTask.failedRowCount || 0) > 0 || latestTask.errorMessage ? 'FAILED' : 'SUCCESS',
      metricType: 'TASK',
      scopeLabel: '任务指标',
      note: '源/目标校验与同步结果'
    })
  }
  if (latestTable) {
    result.push({
      taskId: latestTable.taskId,
      taskName: latestTable.taskName || latestTable.tableName || ('表任务 #' + latestTable.tableTaskId),
      runId: latestTable.runId,
      runStatus: Number(latestTable.failedRowCount || 0) > 0 || latestTable.lastError ? 'FAILED' : 'SUCCESS',
      metricType: 'TABLE',
      scopeLabel: '表级指标',
      note: '表级运行和行数统计'
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

async function handleCleanup() {
  try {
    await ElMessageBox.confirm('确定清理 30 天前的监控数据吗？', '提示', {
      type: 'warning',
      confirmButtonText: '清理',
      cancelButtonText: '取消'
    })
  } catch (error) {
    return
  }
  await cleanupMonitoring()
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
  if (status === 'RUNNING') {
    return 'warning'
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

function runStatusLabel(status) {
  if (status === 'SUCCESS') {
    return '成功'
  }
  if (status === 'FAILED') {
    return '失败'
  }
  if (status === 'RUNNING') {
    return '运行中'
  }
  if (status === 'PARTIAL_SUCCESS') {
    return '部分成功'
  }
  return status || '-'
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
  gap: 16px;
}

.run-monitoring__header {
  align-items: flex-start;
}

.run-monitoring__titleblock {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.run-monitoring__toolbar {
  display: grid;
  justify-items: end;
  gap: 8px;
  min-width: 0;
}

.run-monitoring__toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.run-monitoring__toolbar-note {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.5;
  text-align: right;
  max-width: 420px;
}

.run-monitoring__health-strip {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.run-monitoring__health-main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.run-monitoring__health-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.run-monitoring__health-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.run-monitoring__health-dot--success {
  background: #16a34a;
}

.run-monitoring__health-dot--warning {
  background: #d97706;
}

.run-monitoring__health-dot--danger {
  background: #dc2626;
}

.run-monitoring__health-dot--info {
  background: #64748b;
}

.run-monitoring__health-label {
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 2px;
}

.run-monitoring__health-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.run-monitoring__health-hint {
  color: var(--text-sub);
  line-height: 1.6;
  font-size: 13px;
}

.run-monitoring__health-meta {
  display: grid;
  gap: 10px;
  justify-items: end;
  text-align: right;
}

.run-monitoring__health-summary {
  color: #0f172a;
  font-weight: 600;
  line-height: 1.6;
}

.run-monitoring__health-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.run-monitoring__stats {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-top: 0;
}

.run-monitoring__top-panels,
.run-monitoring__bottom-panels {
  align-items: stretch;
}

.run-monitoring__trend-card,
.run-monitoring__failure-card,
.run-monitoring__datasource-card,
.run-monitoring__latest-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.run-monitoring__panel-hint {
  color: var(--text-sub);
  font-size: 13px;
  line-height: 1.6;
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

.run-monitoring__empty-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #fbfdff;
  border: 1px dashed rgba(148, 163, 184, 0.24);
}

.run-monitoring__empty-line--panel {
  min-height: 104px;
  align-content: center;
}

.run-monitoring__metric-card {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #fbfdff;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.run-monitoring__metric-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.run-monitoring__metric-card-scope {
  font-size: 12px;
  color: var(--text-sub);
}

.run-monitoring__metric-card-title {
  font-weight: 700;
  color: #0f172a;
  line-height: 1.5;
}

.run-monitoring__metric-card-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1280px) {
  .run-monitoring__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .run-monitoring__top-panels,
  .run-monitoring__bottom-panels {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .run-monitoring__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .run-monitoring__health-strip {
    flex-direction: column;
  }

  .run-monitoring__health-meta,
  .run-monitoring__toolbar {
    justify-items: start;
    text-align: left;
  }

  .run-monitoring__health-chips,
  .run-monitoring__toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .run-monitoring__stats {
    grid-template-columns: 1fr;
  }

  .run-monitoring__toolbar-actions {
    width: 100%;
  }

  .run-monitoring__toolbar-actions .el-button {
    width: 100%;
  }
}
</style>
