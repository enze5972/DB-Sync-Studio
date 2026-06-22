<template>
  <div class="page-section list-page task-run-workbench" v-loading="pageLoading">
    <div class="task-run-workbench__shell">
      <div class="page-header task-run-workbench__header">
        <div class="task-run-workbench__titleblock">
          <el-breadcrumb v-if="hasRunContext" separator="/">
            <el-breadcrumb-item>运行运维</el-breadcrumb-item>
            <el-breadcrumb-item>Run 详情</el-breadcrumb-item>
          </el-breadcrumb>
          <h1>Run 详情</h1>
          <p v-if="hasRunContext">查看单次执行的表级进度、日志和断点信息。</p>
          <p v-else>选择一条执行记录，查看表级进度、日志和断点信息。</p>
        </div>

        <div class="task-run-workbench__toolbar">
          <template v-if="hasRunContext">
            <el-button round :loading="pageLoading" @click="goToRunList">返回 Run 列表</el-button>
            <el-button round @click="goToExecutionHistory">查看执行历史</el-button>
            <el-button round @click="goToLogs">查看日志</el-button>
            <el-button round type="primary" :loading="pageLoading" @click="refreshPage">刷新</el-button>
          </template>
          <template v-else>
            <el-button round :loading="pageLoading" @click="refreshPage">刷新列表</el-button>
            <el-button round type="primary" plain @click="goToExecutionHistory">查看执行历史</el-button>
          </template>
        </div>
      </div>

      <StateEmpty
        v-if="blockingErrorMessage"
        :title="hasRunContext ? 'Run 详情暂不可用' : 'Run 记录加载失败'"
        :description="blockingErrorMessage"
        :hint="hasRunContext ? '请检查链接参数，或返回 Run 列表重新选择一条记录。' : '请稍后重试，或先确认后端服务是否正常。'"
        :button-text="hasRunContext ? '返回 Run 列表' : '刷新列表'"
        wide
        @action="hasRunContext ? goToRunList() : refreshPage()"
      />

      <template v-else>
        <div v-if="!hasRunContext" class="task-run-workbench__selection-view">
          <div class="page-overview task-run-workbench__overview task-run-workbench__overview--selection">
            <div v-for="card in selectionCards" :key="card.label" class="page-overview__item task-run-workbench__metric">
              <div class="task-run-workbench__metric-icon" :class="'task-run-workbench__metric-icon--' + card.tone">
                {{ card.icon }}
              </div>
              <div class="page-overview__label">{{ card.label }}</div>
              <div class="page-overview__value task-run-workbench__metric-value">{{ card.value }}</div>
              <div class="page-overview__hint">{{ card.hint }}</div>
            </div>
          </div>

          <div class="panel-card glass-panel task-run-workbench__prompt-strip">
            <div class="task-run-workbench__prompt-main">
              <span class="task-run-workbench__prompt-label">提示</span>
              <span class="task-run-workbench__prompt-text">{{ selectionPromptText }}</span>
            </div>
            <div class="task-run-workbench__prompt-meta">
              <el-tag effect="light" type="info">{{ recentRunRecords.length }} 条最近记录</el-tag>
              <el-tag effect="light" type="success">{{ filteredRecentRunRecords.length }} 条可见</el-tag>
              <el-tag effect="light" :type="selectionFilterActive ? 'warning' : 'info'">
                {{ selectionFilterActive ? '筛选已生效' : '未过滤' }}
              </el-tag>
            </div>
          </div>

          <div class="panel-card glass-panel task-run-workbench__filter-card">
            <div class="section-title section-title--compact task-run-workbench__section-head">
              <div class="section-title__left">
                <h2>筛选条件</h2>
                <el-tag effect="light" type="info">最近 Run</el-tag>
              </div>
              <el-space>
                <el-button size="small" @click="resetSelectionFilters" :disabled="!selectionFilterActive">重置</el-button>
                <el-button size="small" type="primary" plain @click="applySelectionFilters">查询</el-button>
              </el-space>
            </div>

            <div class="task-run-workbench__filter-grid">
              <el-input v-model="selectionFilterForm.keyword" clearable placeholder="任务名称关键词" />
              <el-select v-model="selectionFilterForm.status" clearable placeholder="状态">
                <el-option label="全部" value="ALL" />
                <el-option label="运行中" value="RUNNING" />
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILED" />
                <el-option label="已取消" value="STOPPED" />
              </el-select>
              <el-date-picker
                v-model="selectionFilterForm.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="x"
              />
            </div>
          </div>

          <div class="panel-card glass-panel task-run-workbench__table-card">
            <div class="section-title">
              <div class="section-title__left">
                <h2>最近 Run 记录</h2>
                <el-tag effect="dark" :type="filteredRecentRunRecords.length ? 'success' : 'info'">
                  {{ filteredRecentRunRecords.length ? '可选择' : '暂无匹配' }}
                </el-tag>
              </div>
              <el-space>
                <el-button size="small" @click="goToRunMonitoring">查看运行监控</el-button>
                <el-button size="small" type="primary" plain @click="goToTaskWizard">创建同步任务</el-button>
              </el-space>
            </div>

            <div v-if="filteredRecentRunRecords.length" class="table-shell task-run-workbench__table-shell">
              <el-table :data="filteredRecentRunRecords" border stripe :empty-text="'暂无 Run 记录'">
                <el-table-column prop="runId" label="Run ID" min-width="220" show-overflow-tooltip />
                <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag :type="statusTagType(row.runStatus)" effect="light">
                      {{ runStatusLabel(row.runStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="开始时间" width="200">
                  <template #default="{ row }">
                    {{ formatTime(row.startedAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="结束时间 / 耗时" width="210">
                  <template #default="{ row }">
                    <div class="task-run-workbench__table-meta">
                      <span>{{ formatTime(row.endedAt) }}</span>
                      <span>{{ formatDuration(row.durationMillis) }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="总表数" width="100">
                  <template #default="{ row }">
                    {{ row.totalTableCount || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="成功表数" width="100">
                  <template #default="{ row }">
                    {{ row.completedTableCount || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="失败表数" width="100">
                  <template #default="{ row }">
                    {{ countFailedTables(row) }}
                  </template>
                </el-table-column>
                <el-table-column label="已同步行数" width="120">
                  <template #default="{ row }">
                    {{ row.syncedRowCount || 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="190">
                  <template #default="{ row }">
                    <el-space>
                      <el-button link type="primary" @click="openRecentRunDetail(row)">查看详情</el-button>
                      <el-button link @click="openRecentRunLogs(row)">查看日志</el-button>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <StateEmpty
              v-else-if="recentRunRecords.length"
              title="当前筛选条件下没有 Run 记录"
              description="可以调整关键字、状态或时间范围后重新查询。"
              hint="也可以直接重置筛选，回到全部最近 Run。"
              button-text="重置筛选"
              wide
              @action="resetSelectionFilters"
            />

            <div v-else class="task-run-workbench__empty-block">
              <StateEmpty
                title="暂无 Run 记录"
                description="执行同步任务后，这里会展示每一次 Run 的详细记录。"
                hint="你可以先创建同步任务，或者去运行监控查看当前任务状态。"
                button-text="创建同步任务"
                wide
                @action="goToTaskWizard"
              />
              <div class="task-run-workbench__empty-actions">
                <el-button @click="goToRunMonitoring">查看运行监控</el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="task-run-workbench__detail-view">
          <div class="page-overview task-run-workbench__overview task-run-workbench__overview--detail">
            <div v-for="card in detailCards" :key="card.label" class="page-overview__item task-run-workbench__metric">
              <div class="task-run-workbench__metric-icon" :class="'task-run-workbench__metric-icon--' + card.tone">
                {{ card.icon }}
              </div>
              <div class="page-overview__label">{{ card.label }}</div>
              <div class="page-overview__value task-run-workbench__metric-value">{{ card.value }}</div>
              <div class="page-overview__hint">{{ card.hint }}</div>
            </div>
          </div>

          <div class="panel-card glass-panel task-run-workbench__prompt-strip">
            <div class="task-run-workbench__prompt-main">
              <span class="task-run-workbench__prompt-label">当前阶段</span>
              <span class="task-run-workbench__prompt-text">{{ currentStageText }}</span>
            </div>
            <div class="task-run-workbench__prompt-meta">
              <el-tag effect="light" :type="statusTagType(detailRun.runStatus)">{{ runStatusLabel(detailRun.runStatus) }}</el-tag>
              <el-tag effect="light" type="info">{{ detailLogs.length }} 条日志</el-tag>
              <el-tag effect="light" type="info">{{ breakpointRecords.length }} 个断点</el-tag>
            </div>
          </div>

          <div class="dashboard-panels dashboard-panels--compact task-run-workbench__detail-grid">
            <div class="panel-card glass-panel task-run-workbench__progress-card">
              <div class="section-title section-title--compact">
                <div class="section-title__left">
                  <h2>总体进度</h2>
                  <el-tag effect="dark" :type="statusTagType(detailRun.runStatus)">{{ runStatusLabel(detailRun.runStatus) }}</el-tag>
                </div>
                <el-space>
                  <el-button size="small" @click="goToExecutionHistory">查看执行历史</el-button>
                  <el-button size="small" type="primary" plain @click="goToLogs">查看完整日志</el-button>
                </el-space>
              </div>

              <div class="task-run-workbench__progress-block">
                <div class="task-run-workbench__progress-head">
                  <div>
                    <div class="task-run-workbench__progress-label">总体进度</div>
                    <div class="task-run-workbench__progress-title">{{ overallProgressText }}</div>
                  </div>
                  <div class="task-run-workbench__progress-subtitle">{{ currentTableText }}</div>
                </div>
                <el-progress :percentage="overallProgressPercent" :stroke-width="14" :status="progressStatus" />
              </div>

              <div class="task-run-workbench__progress-stack">
                <div class="task-run-workbench__progress-row">
                  <span class="task-run-workbench__progress-row-label">当前阶段</span>
                  <span class="task-run-workbench__progress-row-value">{{ currentStageText }}</span>
                </div>
                <div class="task-run-workbench__progress-row">
                  <span class="task-run-workbench__progress-row-label">成功数量</span>
                  <span class="task-run-workbench__progress-row-value">{{ successTableCount }}</span>
                </div>
                <div class="task-run-workbench__progress-row">
                  <span class="task-run-workbench__progress-row-label">失败数量</span>
                  <span class="task-run-workbench__progress-row-value">{{ failedTableCount }}</span>
                </div>
                <div class="task-run-workbench__progress-row">
                  <span class="task-run-workbench__progress-row-label">等待数量</span>
                  <span class="task-run-workbench__progress-row-value">{{ waitingTableCount }}</span>
                </div>
              </div>
            </div>

            <div class="panel-card glass-panel task-run-workbench__summary-card">
              <div class="section-title section-title--compact">
                <div class="section-title__left">
                  <h2>Run 基本信息</h2>
                  <el-tag effect="light" type="info">{{ detailTableRuns.length }} 张表</el-tag>
                </div>
                <el-tag effect="light" type="info">{{ resolvedTaskId ? ('任务 ' + resolvedTaskId) : '任务未解析' }}</el-tag>
              </div>
              <div class="status-stack">
                <div class="status-item">
                  <span class="status-item__label">任务名称</span>
                  <span class="status-item__value">{{ detailRun.taskName || '未命名任务' }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">Run ID</span>
                  <span class="status-item__value">{{ detailRun.runId || '-' }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">开始时间</span>
                  <span class="status-item__value">{{ formatTime(detailRun.startedAt) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">结束时间</span>
                  <span class="status-item__value">{{ formatTime(detailRun.endedAt) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">已运行时长</span>
                  <span class="status-item__value">{{ formatDuration(detailRun.durationMillis) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">同步行数</span>
                  <span class="status-item__value">{{ detailRun.syncedRowCount || 0 }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">最近消息</span>
                  <span class="status-item__value">{{ detailRun.progressMessage || '-' }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="dashboard-panels dashboard-panels--compact task-run-workbench__detail-grid task-run-workbench__detail-grid--lower">
            <div class="panel-card glass-panel task-run-workbench__table-progress-card">
              <div class="section-title section-title--compact">
                <div class="section-title__left">
                  <h2>表级进度</h2>
                  <el-tag effect="dark" type="info">{{ detailTableRuns.length }} 张表</el-tag>
                </div>
                <el-tag effect="light" :type="statusTagType(detailRun.runStatus)">
                  {{ currentRunTableSummary }}
                </el-tag>
              </div>

              <div v-if="detailTableRuns.length" class="table-shell task-run-workbench__table-shell">
                <el-table :data="detailTableRuns" border stripe :empty-text="'暂无表级进度'">
                  <el-table-column label="表名" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                      {{ tableDisplayName(row) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="120">
                    <template #default="{ row }">
                      <el-tag :type="statusTagType(row.tableStatus)" effect="light">{{ runStatusLabel(row.tableStatus) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="总行数" width="110">
                    <template #default="{ row }">
                      {{ row.totalRowCount || 0 }}
                    </template>
                  </el-table-column>
                  <el-table-column label="已同步行数" width="120">
                    <template #default="{ row }">
                      {{ row.syncedRowCount || 0 }}
                    </template>
                  </el-table-column>
                  <el-table-column label="进度" min-width="180">
                    <template #default="{ row }">
                      <div class="task-run-workbench__table-progress-cell">
                        <el-progress :percentage="tableProgressPercent(row)" :stroke-width="10" />
                      </div>
                    </template>
                  </el-table-column>
                  <el-table-column label="耗时" width="120">
                    <template #default="{ row }">
                      {{ formatDuration(row.durationMillis) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="最后更新时间" width="200">
                    <template #default="{ row }">
                      {{ formatTime(row.updatedAt || row.endedAt || row.startedAt) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="170">
                    <template #default="{ row }">
                      <el-space>
                        <el-button link type="primary" @click="openTableLogs(row)">查看日志</el-button>
                        <el-button link @click="viewBreakpoint(row)">查看断点</el-button>
                      </el-space>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <StateEmpty
                v-else
                title="当前 Run 暂无表级进度"
                description="这个 Run 还没有写入表级执行结果，或者执行还没开始。"
                hint="表级进度会展示每张表的状态、耗时、断点和错误。"
                button-text="返回 Run 列表"
                wide
                @action="goToRunList"
              />
            </div>

            <div class="panel-card glass-panel task-run-workbench__log-card">
              <div class="section-title section-title--compact">
                <div class="section-title__left">
                  <h2>日志摘要</h2>
                  <el-tag effect="light" type="info">{{ detailLogs.length }} 条</el-tag>
                </div>
                <el-button size="small" type="primary" plain @click="goToLogs">查看完整日志</el-button>
              </div>

              <div v-if="detailLogs.length" class="task-run-workbench__log-list">
                <div v-for="item in logSummaryRecords" :key="item.id || item.createdAt + item.logMessage" class="task-run-workbench__log-item">
                  <div class="task-run-workbench__log-time">{{ formatTime(item.createdAt) }}</div>
                  <el-tag :type="logLevelTagType(item.logLevel)" effect="light" size="small">
                    {{ logLevelLabel(item.logLevel) }}
                  </el-tag>
                  <div class="task-run-workbench__log-message">{{ item.logMessage || '-' }}</div>
                </div>
              </div>

              <StateEmpty
                v-else
                title="当前 Run 暂无日志摘要"
                description="执行过程中产生的日志会显示在这里。"
                hint="你也可以点击“查看完整日志”进入日志页面。"
                button-text="查看完整日志"
                wide
                @action="goToLogs"
              />
            </div>
          </div>

          <div ref="breakpointSectionRef" class="panel-card glass-panel task-run-workbench__breakpoint-card">
            <div class="section-title section-title--compact">
              <div class="section-title__left">
                <h2>断点信息</h2>
                <el-tag effect="light" type="info">{{ breakpointRecords.length }} 个断点</el-tag>
              </div>
              <el-tag effect="light" :type="breakpointRecords.length ? 'success' : 'info'">
                {{ breakpointRecords.length ? '可继续恢复' : '暂无断点' }}
              </el-tag>
            </div>

            <div v-if="breakpointRecords.length" class="task-run-workbench__breakpoint-body">
              <div v-if="selectedBreakpointRow" class="task-run-workbench__breakpoint-active">
                <div class="task-run-workbench__breakpoint-active-label">当前选中表</div>
                <div class="task-run-workbench__breakpoint-active-value">{{ breakpointDisplayName(selectedBreakpointRow) }}</div>
                <div class="task-run-workbench__breakpoint-active-hint">{{ selectedBreakpointRow.checkpointValue || '-' }}</div>
              </div>

              <div class="table-shell task-run-workbench__table-shell">
                <el-table :data="breakpointRecords" border stripe :empty-text="'暂无断点信息'">
                  <el-table-column label="表名" min-width="180" show-overflow-tooltip>
                    <template #default="{ row }">
                      {{ breakpointDisplayName(row) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="主键位置 / offset" min-width="240" show-overflow-tooltip>
                    <template #default="{ row }">
                      {{ row.checkpointValue || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column label="最后成功时间" width="200">
                    <template #default="{ row }">
                      {{ formatTime(row.endedAt || row.updatedAt || row.startedAt) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="下一次恢复策略" min-width="220" show-overflow-tooltip>
                    <template #default="{ row }">
                      {{ breakpointResumeText(row) }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>

            <StateEmpty
              v-else
              title="当前 Run 暂无断点信息"
              description="如果同步任务启用了增量或断点续跑，这里会显示对应的断点数据。"
              hint="现在没有可恢复的断点记录。"
              button-text="返回 Run 列表"
              wide
              @action="goToRunList"
            />
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getRecentRuns,
  getRunDetail
} from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'
import {
  buildRunDetailStats,
  filterRecentRunRecords,
  resolveRunTaskId
} from '../utils/runDetail'

const route = useRoute()
const router = useRouter()

const recentRunRecords = ref([])
const loadingRecentRuns = ref(false)
const loadingDetail = ref(false)
const recentLoadError = ref('')
const detailLoadError = ref('')
const resolvedTaskId = ref(null)
const breakpointSectionRef = ref(null)
const selectedBreakpointRow = ref(null)

const selectionFilterForm = reactive({
  keyword: '',
  status: 'ALL',
  timeRange: []
})

const appliedSelectionFilters = reactive({
  keyword: '',
  status: 'ALL',
  startTime: '',
  endTime: ''
})

const detailSnapshot = reactive({
  run: {},
  tableRuns: [],
  logs: []
})

const pageLoading = computed(function () {
  return loadingRecentRuns.value || loadingDetail.value
})

const hasRunContext = computed(function () {
  return !!resolveRouteRunId()
})

const currentRunId = computed(function () {
  return resolveRouteRunId()
})

const selectionFilterActive = computed(function () {
  return !!selectionFilterForm.keyword || selectionFilterForm.status !== 'ALL' || selectionFilterForm.timeRange.length > 0
})

const selectionCards = computed(function () {
  const stats = buildRunDetailStats(recentRunRecords.value, Date.now())
  return [
    {
      icon: '☀',
      label: '今日执行',
      value: stats.todayCount,
      hint: '按最近 Run 记录统计',
      tone: 'blue'
    },
    {
      icon: '▶',
      label: '运行中',
      value: stats.runningCount,
      hint: '当前仍在执行的 Run',
      tone: 'blue'
    },
    {
      icon: '✓',
      label: '成功',
      value: stats.successCount,
      hint: '今日成功或部分成功',
      tone: 'green'
    },
    {
      icon: '✕',
      label: '失败',
      value: stats.failedCount,
      hint: '今日失败的 Run',
      tone: 'red'
    }
  ]
})

const filteredRecentRunRecords = computed(function () {
  return filterRecentRunRecords(recentRunRecords.value, appliedSelectionFilters)
})

const detailRun = computed(function () {
  return detailSnapshot.run || {}
})

const detailTableRuns = computed(function () {
  return detailSnapshot.tableRuns.slice().sort(function (left, right) {
    const leftOrder = Number(left && left.tableOrder) || 0
    const rightOrder = Number(right && right.tableOrder) || 0
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    return tableDisplayName(left).localeCompare(tableDisplayName(right))
  })
})

const detailLogs = computed(function () {
  return detailSnapshot.logs.slice().sort(function (left, right) {
    return resolveLogTime(right) - resolveLogTime(left)
  })
})

const logSummaryRecords = computed(function () {
  return detailLogs.value.slice(0, 8)
})

const breakpointRecords = computed(function () {
  return detailTableRuns.value.filter(function (row) {
    return !!normalizeText(row && row.checkpointValue)
  })
})

const detailCards = computed(function () {
  const run = detailRun.value
  return [
    {
      icon: '◉',
      label: '运行状态',
      value: runStatusLabel(run.runStatus),
      hint: run.progressMessage || '暂无状态消息',
      tone: statusTone(run.runStatus)
    },
    {
      icon: '▣',
      label: '任务名称',
      value: run.taskName || '未命名任务',
      hint: '任务 ID ' + (run.taskId || '-'),
      tone: 'blue'
    },
    {
      icon: '⌁',
      label: 'Run ID',
      value: run.runId || '-',
      hint: '当前执行批次标识',
      tone: 'blue'
    },
    {
      icon: '⏱',
      label: '开始时间',
      value: formatTime(run.startedAt),
      hint: '执行开始时间',
      tone: 'blue'
    },
    {
      icon: '⌛',
      label: '已运行时长',
      value: formatDuration(run.durationMillis),
      hint: run.endedAt ? '包含完整执行耗时' : '运行中累计时长',
      tone: 'blue'
    },
    {
      icon: '⇄',
      label: '表 / 行',
      value: (run.completedTableCount || 0) + ' / ' + (run.totalTableCount || 0) + ' · ' + (run.syncedRowCount || 0) + ' / ' + (run.totalRowCount || 0),
      hint: '已完成表与同步行统计',
      tone: 'blue'
    }
  ]
})

const currentStageText = computed(function () {
  const run = detailRun.value
  if (run.progressMessage) {
    return run.progressMessage
  }
  return runStatusLabel(run.runStatus) || '暂无阶段信息'
})

const currentTableText = computed(function () {
  const current = detailTableRuns.value.find(function (row) {
    return row && row.tableStatus === 'RUNNING'
  }) || detailTableRuns.value[0] || null
  if (!current) {
    return '当前没有正在处理的表'
  }
  return '当前表：' + tableDisplayName(current)
})

const successTableCount = computed(function () {
  return detailTableRuns.value.filter(function (row) {
    return isSuccessStatus(row && row.tableStatus)
  }).length
})

const failedTableCount = computed(function () {
  return detailTableRuns.value.filter(function (row) {
    return isFailedStatus(row && row.tableStatus)
  }).length
})

const runningTableCount = computed(function () {
  return detailTableRuns.value.filter(function (row) {
    return normalizeText(row && row.tableStatus) === 'RUNNING'
  }).length
})

const waitingTableCount = computed(function () {
  const total = Number(detailRun.value.totalTableCount || detailTableRuns.value.length || 0)
  const waiting = total - successTableCount.value - failedTableCount.value - runningTableCount.value
  return waiting > 0 ? waiting : 0
})

const overallProgressPercent = computed(function () {
  const run = detailRun.value
  const totalRows = Number(run.totalRowCount || 0)
  const syncedRows = Number(run.syncedRowCount || 0)
  if (totalRows > 0) {
    return clampPercent((syncedRows / totalRows) * 100)
  }
  const totalTables = Number(run.totalTableCount || 0)
  const completedTables = Number(run.completedTableCount || 0)
  if (totalTables > 0) {
    return clampPercent((completedTables / totalTables) * 100)
  }
  return 0
})

const overallProgressText = computed(function () {
  const run = detailRun.value
  const tablesText = (run.completedTableCount || 0) + ' / ' + (run.totalTableCount || 0) + ' 表'
  const rowsText = (run.syncedRowCount || 0) + ' / ' + (run.totalRowCount || 0) + ' 行'
  return tablesText + ' · ' + rowsText
})

const progressStatus = computed(function () {
  const status = normalizeText(detailRun.value.runStatus)
  if (status === 'FAILED') {
    return 'exception'
  }
  if (status === 'SUCCESS' || status === 'PARTIAL_SUCCESS') {
    return 'success'
  }
  return undefined
})

const currentRunTableSummary = computed(function () {
  if (!detailTableRuns.value.length) {
    return '暂无表级数据'
  }
  return successTableCount.value + ' / ' + detailTableRuns.value.length + ' 已完成'
})

const selectionPromptText = computed(function () {
  if (!recentRunRecords.value.length) {
    return '执行同步任务后，这里会展示每一次 Run 的详细记录。'
  }
  return '从最近 Run 中选择一条记录，右侧会直接切换到详情模式。'
})

const blockingErrorMessage = computed(function () {
  if (hasRunContext.value) {
    return detailLoadError.value
  }
  return recentLoadError.value
})

onMounted(function () {
  loadPageData()
})

watch(function () {
  return route.fullPath
}, function () {
  loadPageData()
})

async function loadPageData() {
  detailLoadError.value = ''
  recentLoadError.value = ''
  selectedBreakpointRow.value = null
  await loadRecentRuns()
  if (hasRunContext.value) {
    await loadDetailForRoute()
  } else {
    clearDetailState()
  }
}

async function loadRecentRuns() {
  loadingRecentRuns.value = true
  try {
    recentRunRecords.value = await getRecentRuns(8)
    syncSelectionFilters()
    if (hasRunContext.value && !resolvedTaskId.value) {
      resolvedTaskId.value = resolveRouteTaskId() || resolveRunTaskId(currentRunId.value, recentRunRecords.value)
    }
  } catch (error) {
    recentRunRecords.value = []
    recentLoadError.value = error && error.message ? error.message : '加载最近 Run 记录失败'
  } finally {
    loadingRecentRuns.value = false
  }
}

async function loadDetailForRoute() {
  const runId = currentRunId.value
  if (!runId) {
    clearDetailState()
    return
  }

  const taskId = resolveRouteTaskId() || resolveRunTaskId(runId, recentRunRecords.value)
  if (!taskId) {
    resolvedTaskId.value = null
    detailLoadError.value = '没有找到这条 Run 记录，请先从最近 Run 列表进入后再查看。'
    clearDetailState()
    return
  }

  resolvedTaskId.value = taskId
  loadingDetail.value = true
  detailLoadError.value = ''
  try {
    const response = await getRunDetail(taskId, runId, 100)
    detailSnapshot.run = Object.assign({}, response.run || {}, {
      taskId: response.run && response.run.taskId ? response.run.taskId : taskId
    })
    detailSnapshot.tableRuns = Array.isArray(response.tableRuns) ? response.tableRuns.slice() : []
    detailSnapshot.logs = Array.isArray(response.logs) ? response.logs.slice() : []
    selectedBreakpointRow.value = breakpointRecords.value.length ? breakpointRecords.value[0] : null
  } catch (error) {
    detailSnapshot.run = {}
    detailSnapshot.tableRuns = []
    detailSnapshot.logs = []
    detailLoadError.value = normalizeDetailError(error)
  } finally {
    loadingDetail.value = false
  }
}

function refreshPage() {
  loadPageData()
}

function clearDetailState() {
  detailSnapshot.run = {}
  detailSnapshot.tableRuns = []
  detailSnapshot.logs = []
}

function syncSelectionFilters() {
  appliedSelectionFilters.keyword = normalizeInput(selectionFilterForm.keyword)
  appliedSelectionFilters.status = selectionFilterForm.status || 'ALL'
  appliedSelectionFilters.startTime = selectionFilterForm.timeRange && selectionFilterForm.timeRange[0] ? normalizeTimeValue(selectionFilterForm.timeRange[0]) : ''
  appliedSelectionFilters.endTime = selectionFilterForm.timeRange && selectionFilterForm.timeRange[1] ? normalizeTimeValue(selectionFilterForm.timeRange[1]) : ''
}

function applySelectionFilters() {
  syncSelectionFilters()
}

function resetSelectionFilters() {
  selectionFilterForm.keyword = ''
  selectionFilterForm.status = 'ALL'
  selectionFilterForm.timeRange = []
  syncSelectionFilters()
}

function goToRunList() {
  router.push('/task-run')
}

function goToExecutionHistory() {
  const query = {}
  if (resolvedTaskId.value) {
    query.taskId = String(resolvedTaskId.value)
  }
  if (currentRunId.value) {
    query.runId = currentRunId.value
  }
  router.push({
    path: '/execution-history',
    query: query
  })
}

function goToLogs() {
  const taskId = resolvedTaskId.value || detailRun.value.taskId || resolveRunTaskId(currentRunId.value, recentRunRecords.value)
  if (!taskId || !currentRunId.value) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: String(taskId),
      runId: currentRunId.value
    }
  })
}

function goToRunMonitoring() {
  router.push('/run-monitoring')
}

function goToTaskWizard() {
  router.push('/task-wizard')
}

function openRecentRunDetail(row) {
  if (!row || !row.runId) {
    return
  }
  router.push({
    path: '/task-run',
    query: {
      runId: row.runId
    }
  })
}

function openRecentRunLogs(row) {
  const taskId = row && row.taskId ? row.taskId : null
  if (!row || !row.runId || !taskId) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: String(taskId),
      runId: row.runId
    }
  })
}

function openTableLogs(row) {
  const taskId = resolvedTaskId.value || detailRun.value.taskId
  if (!row || !currentRunId.value || !taskId) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: String(taskId),
      runId: currentRunId.value,
      syncTableRunId: row.id,
      tableName: tableDisplayName(row)
    }
  })
}

function viewBreakpoint(row) {
  selectedBreakpointRow.value = row || null
  if (breakpointSectionRef.value && typeof breakpointSectionRef.value.scrollIntoView === 'function') {
    breakpointSectionRef.value.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  }
}

function tableDisplayName(row) {
  if (!row) {
    return '-'
  }
  return row.sourceTableName || row.targetTableName || '-'
}

function breakpointDisplayName(row) {
  return tableDisplayName(row)
}

function breakpointResumeText(row) {
  if (!row || !row.checkpointValue) {
    return '基于最新断点继续'
  }
  return '基于 checkpoint 继续同步'
}

function tableProgressPercent(row) {
  if (!row) {
    return 0
  }
  const total = Number(row.totalRowCount || 0)
  const synced = Number(row.syncedRowCount || 0)
  if (total <= 0) {
    return row.tableStatus === 'SUCCESS' ? 100 : 0
  }
  return clampPercent((synced / total) * 100)
}

function countFailedTables(row) {
  if (!row) {
    return 0
  }
  if (row.failedTableCount !== null && row.failedTableCount !== undefined) {
    return Number(row.failedTableCount) || 0
  }
  const status = normalizeText(row.runStatus)
  if (status !== 'FAILED' && status !== 'PARTIAL_SUCCESS') {
    return 0
  }
  const total = Number(row.totalTableCount || 0)
  const completed = Number(row.completedTableCount || 0)
  if (total <= 0) {
    return 0
  }
  return Math.max(total - completed, 0)
}

function statusTone(status) {
  const normalized = normalizeText(status)
  if (normalized === 'SUCCESS' || normalized === 'PARTIAL_SUCCESS') {
    return 'green'
  }
  if (normalized === 'FAILED') {
    return 'red'
  }
  if (normalized === 'RUNNING') {
    return 'blue'
  }
  return 'gray'
}

function statusTagType(status) {
  const normalized = normalizeText(status)
  if (normalized === 'SUCCESS') {
    return 'success'
  }
  if (normalized === 'PARTIAL_SUCCESS') {
    return 'warning'
  }
  if (normalized === 'FAILED') {
    return 'danger'
  }
  if (normalized === 'PAUSED') {
    return 'warning'
  }
  if (normalized === 'STOPPED') {
    return 'info'
  }
  if (normalized === 'RUNNING') {
    return 'primary'
  }
  return 'info'
}

function runStatusLabel(status) {
  const normalized = normalizeText(status)
  if (normalized === 'RUNNING') {
    return '运行中'
  }
  if (normalized === 'SUCCESS') {
    return '成功'
  }
  if (normalized === 'FAILED') {
    return '失败'
  }
  if (normalized === 'PAUSED') {
    return '暂停'
  }
  if (normalized === 'STOPPED') {
    return '已取消'
  }
  if (normalized === 'PARTIAL_SUCCESS') {
    return '部分成功'
  }
  if (normalized === 'PENDING') {
    return '等待中'
  }
  return '-'
}

function logLevelLabel(level) {
  const normalized = normalizeText(level)
  if (normalized === 'ERROR') {
    return '错误'
  }
  if (normalized === 'WARN') {
    return '警告'
  }
  if (normalized === 'INFO') {
    return '信息'
  }
  return normalized || '-'
}

function logLevelTagType(level) {
  const normalized = normalizeText(level)
  if (normalized === 'ERROR') {
    return 'danger'
  }
  if (normalized === 'WARN') {
    return 'warning'
  }
  if (normalized === 'INFO') {
    return 'info'
  }
  return 'info'
}

function formatTime(value) {
  if (!value && value !== 0) {
    return '-'
  }
  const numeric = Number(value)
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return '-'
  }
  return new Date(numeric).toLocaleString()
}

function formatDuration(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const numeric = Math.max(0, Math.floor(Number(value) / 1000))
  if (!Number.isFinite(numeric)) {
    return '-'
  }
  const minutes = Math.floor(numeric / 60)
  const seconds = numeric % 60
  if (minutes > 0) {
    return minutes + 'm ' + seconds + 's'
  }
  return seconds + 's'
}

function normalizeDetailError(error) {
  const message = error && error.message ? String(error.message) : '加载 run 详情失败'
  if (message.indexOf('Sync run not found') >= 0) {
    return '没有找到这条 Run 记录，可能已被清理或还没有真正执行过。'
  }
  return message
}

function normalizeInput(value) {
  return value === null || value === undefined ? '' : String(value).trim()
}

function normalizeTimeValue(value) {
  if (value === null || value === undefined || value === '') {
    return ''
  }
  const numeric = Number(value)
  return Number.isFinite(numeric) ? String(numeric) : ''
}

function normalizeText(value) {
  return value === null || value === undefined ? '' : String(value).trim().toUpperCase()
}

function isSuccessStatus(status) {
  const normalized = normalizeText(status)
  return normalized === 'SUCCESS' || normalized === 'PARTIAL_SUCCESS'
}

function isFailedStatus(status) {
  const normalized = normalizeText(status)
  return normalized === 'FAILED'
}

function resolveRouteTaskId() {
  const value = Number(resolveRouteParamValue('taskId'))
  return Number.isFinite(value) && value > 0 ? value : null
}

function resolveRouteRunId() {
  return normalizeInput(resolveRouteParamValue('runId'))
}

function resolveRouteParamValue(key) {
  if (route.query && route.query[key] !== undefined && route.query[key] !== null) {
    const queryValue = route.query[key]
    if (Array.isArray(queryValue)) {
      return queryValue.length > 0 ? queryValue[0] : ''
    }
    return queryValue
  }
  if (route.params && route.params[key] !== undefined && route.params[key] !== null) {
    const paramValue = route.params[key]
    if (Array.isArray(paramValue)) {
      return paramValue.length > 0 ? paramValue[0] : ''
    }
    return paramValue
  }
  if (typeof window !== 'undefined' && window.location && typeof window.location.hash === 'string') {
    const hash = window.location.hash
    const questionMarkIndex = hash.indexOf('?')
    if (questionMarkIndex >= 0) {
      const queryString = hash.slice(questionMarkIndex + 1)
      const parsed = new URLSearchParams(queryString)
      if (parsed.has(key)) {
        return parsed.get(key) || ''
      }
    }
  }
  return ''
}

function resolveLogTime(log) {
  return Number(log && (log.createdAt || log.updatedAt || log.startedAt || 0)) || 0
}

function clampPercent(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) {
    return 0
  }
  if (numeric < 0) {
    return 0
  }
  if (numeric > 100) {
    return 100
  }
  return Math.round(numeric)
}
</script>

<style scoped>
.task-run-workbench {
  gap: 16px;
}

.task-run-workbench__shell {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  display: grid;
  gap: 16px;
}

.task-run-workbench__header {
  align-items: flex-start;
}

.task-run-workbench__titleblock {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.task-run-workbench__titleblock h1 {
  margin: 0;
}

.task-run-workbench__titleblock p {
  margin: 0;
}

.task-run-workbench__toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.task-run-workbench__overview {
  margin-top: 0;
}

.task-run-workbench__overview--selection {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.task-run-workbench__overview--detail {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.task-run-workbench__metric {
  min-height: 128px;
  display: grid;
  align-content: start;
  gap: 6px;
}

.task-run-workbench__metric-icon {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 800;
}

.task-run-workbench__metric-icon--blue {
  background: rgba(37, 99, 235, 0.12);
  color: var(--accent);
}

.task-run-workbench__metric-icon--green {
  background: rgba(22, 163, 74, 0.12);
  color: var(--success);
}

.task-run-workbench__metric-icon--red {
  background: rgba(220, 38, 38, 0.12);
  color: #dc2626;
}

.task-run-workbench__metric-icon--gray {
  background: rgba(148, 163, 184, 0.12);
  color: #64748b;
}

.task-run-workbench__metric-value {
  font-size: 20px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.task-run-workbench__prompt-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.task-run-workbench__prompt-main {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
}

.task-run-workbench__prompt-label {
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.task-run-workbench__prompt-text {
  color: var(--text-main);
  line-height: 1.7;
}

.task-run-workbench__prompt-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.task-run-workbench__empty-block {
  display: grid;
  gap: 12px;
}

.task-run-workbench__empty-actions {
  display: flex;
  justify-content: center;
}

.task-run-workbench__filter-card,
.task-run-workbench__table-card,
.task-run-workbench__progress-card,
.task-run-workbench__summary-card,
.task-run-workbench__log-card,
.task-run-workbench__breakpoint-card {
  border-radius: 18px;
}

.task-run-workbench__filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(220px, 1fr) minmax(0, 2fr);
  gap: 12px;
}

.task-run-workbench__table-card,
.task-run-workbench__progress-card,
.task-run-workbench__summary-card,
.task-run-workbench__log-card,
.task-run-workbench__breakpoint-card {
  padding: 18px;
}

.task-run-workbench__table-shell {
  min-height: 240px;
}

.task-run-workbench__table-meta {
  display: grid;
  gap: 4px;
}

.task-run-workbench__detail-view {
  display: grid;
  gap: 16px;
}

.task-run-workbench__detail-grid {
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
  align-items: stretch;
}

.task-run-workbench__detail-grid--lower {
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
}

.task-run-workbench__progress-block {
  display: grid;
  gap: 12px;
}

.task-run-workbench__progress-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.task-run-workbench__progress-label {
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 6px;
}

.task-run-workbench__progress-title {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.35;
}

.task-run-workbench__progress-subtitle {
  color: var(--text-sub);
  font-size: 13px;
}

.task-run-workbench__progress-stack {
  display: grid;
  gap: 10px;
}

.task-run-workbench__progress-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.task-run-workbench__progress-row-label {
  color: var(--text-sub);
  font-size: 12px;
}

.task-run-workbench__progress-row-value {
  font-weight: 700;
  text-align: right;
  overflow-wrap: anywhere;
}

.task-run-workbench__summary-card .status-stack {
  gap: 10px;
}

.task-run-workbench__summary-card .status-item {
  padding: 12px 14px;
}

.task-run-workbench__table-progress-cell {
  min-width: 140px;
}

.task-run-workbench__log-list {
  display: grid;
  gap: 10px;
  max-height: 520px;
  overflow: auto;
  padding-right: 4px;
}

.task-run-workbench__log-item {
  display: grid;
  grid-template-columns: 124px auto 1fr;
  gap: 10px;
  align-items: start;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.task-run-workbench__log-time {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.5;
}

.task-run-workbench__log-message {
  min-width: 0;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.task-run-workbench__breakpoint-body {
  display: grid;
  gap: 14px;
}

.task-run-workbench__breakpoint-active {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.task-run-workbench__breakpoint-active-label {
  color: var(--text-sub);
  font-size: 12px;
}

.task-run-workbench__breakpoint-active-value {
  font-weight: 700;
}

.task-run-workbench__breakpoint-active-hint {
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

@media (max-width: 1280px) {
  .task-run-workbench__overview--selection {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-run-workbench__overview--detail {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .task-run-workbench__filter-grid,
  .task-run-workbench__detail-grid,
  .task-run-workbench__detail-grid--lower {
    grid-template-columns: 1fr;
  }

  .task-run-workbench__log-item {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .task-run-workbench__overview--selection,
  .task-run-workbench__overview--detail {
    grid-template-columns: 1fr;
  }

  .task-run-workbench__toolbar,
  .task-run-workbench__prompt-strip {
    align-items: stretch;
  }
}
</style>
