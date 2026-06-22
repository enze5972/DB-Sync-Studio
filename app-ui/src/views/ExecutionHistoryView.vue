<template>
  <div class="page-section list-page execution-history-workbench">
    <div class="page-header execution-history-workbench__header">
      <div class="execution-history-workbench__titleblock">
        <h1>执行历史</h1>
        <p>按任务查看同步执行批次、表级进度和日志，方便回溯每次运行结果。</p>
      </div>
      <div class="execution-history-workbench__toolbar">
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
        <el-button round type="primary" plain :disabled="!canOpenLatestRun" @click="openLatestRun">
          查看最新
        </el-button>
        <el-button round :disabled="!selectedTaskId || !selectedRun" @click="goToLogs">
          查看日志
        </el-button>
      </div>
    </div>

    <div class="page-overview page-overview--four execution-history-workbench__overview">
      <div class="page-overview__item">
        <div class="page-overview__label">当前任务</div>
        <div class="page-overview__value">{{ selectedTaskName || '未选择' }}</div>
        <div class="page-overview__hint">{{ currentTaskHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">执行批次</div>
        <div class="page-overview__value">{{ runs.length }}</div>
        <div class="page-overview__hint">每次同步运行都会生成一个 run</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">最近运行</div>
        <div class="page-overview__value">{{ latestRunSummaryText }}</div>
        <div class="page-overview__hint">{{ latestRunHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">筛选状态</div>
        <div class="page-overview__value">{{ filterStateText }}</div>
        <div class="page-overview__hint">{{ filterStateHint }}</div>
      </div>
    </div>

    <div class="panel-card glass-panel execution-history-workbench__prompt-strip">
      <div class="execution-history-workbench__prompt-main">
        <span class="execution-history-workbench__prompt-label">提示</span>
        <span class="execution-history-workbench__prompt-text">{{ promptText }}</span>
      </div>
      <div class="execution-history-workbench__prompt-meta">
        <el-tag :type="selectedTaskId ? 'success' : 'info'" effect="light">
          {{ selectedTaskId ? '已选择任务' : '未选择任务' }}
        </el-tag>
        <el-tag :type="isFiltered ? 'warning' : 'info'" effect="light">
          {{ isFiltered ? '筛选已生效' : '未过滤' }}
        </el-tag>
        <el-tag v-if="selectedTaskId" type="info" effect="light">保留 {{ retentionDays }} 天</el-tag>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact execution-history-workbench__workspace">
      <div class="panel-card glass-panel execution-history-workbench__list-panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>Run 列表</h2>
            <el-tag :type="listBadgeType" effect="dark">{{ listBadgeText }}</el-tag>
          </div>
          <el-space>
            <el-button size="small" @click="resetFilters" :disabled="!isFiltered">重置</el-button>
            <el-button size="small" type="primary" plain @click="applyFilters">筛选</el-button>
          </el-space>
        </div>
        <div class="execution-history-workbench__list-body">
          <div class="execution-history-workbench__task-box">
            <div class="section-title section-title--compact">
              <div class="section-title__left">
                <h3>任务选择</h3>
                <el-tag type="info" effect="light">{{ tasks.length }} 个</el-tag>
              </div>
            </div>
            <el-select
              v-model="selectedTaskId"
              class="execution-history-workbench__task-select"
              filterable
              placeholder="选择同步任务"
              @change="handleTaskChange"
            >
              <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
            </el-select>
            <div class="status-stack execution-history-workbench__task-summary">
              <div class="status-item">
                <span class="status-item__label">任务摘要</span>
                <span class="status-item__value">{{ selectedTaskTableSummary }}</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">同步模式</span>
                <span class="status-item__value">{{ selectedTaskModeText }}</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">调度</span>
                <span class="status-item__value">{{ scheduleSummaryText }}</span>
              </div>
            </div>
          </div>

          <div class="execution-history-workbench__filter-box">
            <div class="section-title section-title--compact">
              <div class="section-title__left">
                <h3>筛选条件</h3>
                <el-tag :type="isFiltered ? 'warning' : 'info'" effect="light">
                  {{ isFiltered ? '已筛选' : '未过滤' }}
                </el-tag>
              </div>
            </div>
            <div class="execution-history-workbench__filter-grid">
              <el-input v-model="filters.keyword" clearable placeholder="关键字" />
              <el-input v-model="filters.runId" clearable placeholder="run_id" />
              <el-select v-model="filters.status" clearable placeholder="状态">
                <el-option label="运行中" value="RUNNING" />
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILED" />
                <el-option label="部分成功" value="PARTIAL_SUCCESS" />
                <el-option label="暂停" value="PAUSED" />
                <el-option label="停止" value="STOPPED" />
              </el-select>
              <el-select v-model="filters.scheduleType" clearable placeholder="调度">
                <el-option label="全部调度" value="ALL" />
                <el-option label="手动" value="MANUAL" />
                <el-option label="定时" value="SCHEDULED" />
              </el-select>
              <el-input v-model="filters.tableText" clearable placeholder="任务表" />
              <el-select v-model="filters.limitMode" clearable placeholder="保留天数">
                <el-option :label="'保留 ' + retentionDays + ' 天'" :value="String(retentionDays)" />
              </el-select>
            </div>
          </div>

          <div class="execution-history-workbench__table-shell table-shell">
            <el-table
              :data="filteredRuns"
              border
              stripe
              v-loading="loading"
              highlight-current-row
              :empty-text="tableEmptyText"
              @row-click="selectRun"
            >
              <el-table-column prop="runId" label="运行批次" min-width="220" show-overflow-tooltip />
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.runStatus)" effect="light">{{ statusLabel(row.runStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="表数" width="90">
                <template #default="{ row }">
                  {{ row.completedTableCount || 0 }} / {{ row.totalTableCount || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="行数" width="140">
                <template #default="{ row }">
                  {{ row.syncedRowCount || 0 }} / {{ row.totalRowCount || 0 }}
                </template>
              </el-table-column>
              <el-table-column label="耗时" width="120">
                <template #default="{ row }">
                  {{ formatDuration(row.durationMillis) }}
                </template>
              </el-table-column>
              <el-table-column label="开始时间" width="200">
                <template #default="{ row }">
                  {{ formatTime(row.startedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="结束时间" width="200">
                <template #default="{ row }">
                  {{ formatTime(row.endedAt) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="210">
                <template #default="{ row }">
                  <el-space>
                    <el-button link type="primary" @click.stop="openRunDetail(row)">详情</el-button>
                    <el-button link type="primary" @click.stop="openRunTables(row)">表级</el-button>
                    <el-button link @click.stop="openRunLogs(row)">日志</el-button>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
            <StateEmpty
              v-if="emptyStateType === 'no-task'"
              title="先选择一个任务"
              description="选择同步任务后，这里会展示该任务的执行批次、表级进度和日志入口。"
              hint="右侧会同步显示最近一次 run 的摘要。"
              button-text="去任务页"
              @action="goToTasks"
            />
            <StateEmpty
              v-else-if="emptyStateType === 'no-runs'"
              title="当前任务还没有执行历史"
              description="执行一次同步任务后，这里会生成 run 记录，并保留运行批次、状态、耗时和错误摘要。"
              hint="你也可以先查看任务配置再返回这里。"
              button-text="查看任务"
              @action="goToTasks"
            />
            <StateEmpty
              v-else-if="emptyStateType === 'filtered-empty'"
              title="当前筛选条件下没有执行历史"
              description="可以调整关键字、run_id、状态或调度条件后重新查询。"
              hint="也可以先重置筛选，再查看全部 run。"
              button-text="重置筛选"
              @action="resetFilters"
            />
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel execution-history-workbench__summary-panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>Run 摘要</h2>
            <el-tag :type="selectedRun ? statusTagType(selectedRun.runStatus) : 'info'" effect="dark">
              {{ selectedRun ? statusLabel(selectedRun.runStatus) : '未选择' }}
            </el-tag>
          </div>
          <el-space>
            <el-button size="small" @click="selectMostRecentRun" :disabled="!selectedRun">选中最新</el-button>
            <el-button size="small" type="primary" plain :disabled="!selectedRun" @click="openSelectedRunDetail">
              查看详情
            </el-button>
          </el-space>
        </div>
        <div v-if="selectedRun" class="execution-history-workbench__summary-stack">
          <div class="status-item">
            <span class="status-item__label">run_id</span>
            <span class="status-item__value">{{ selectedRun.runId }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">状态</span>
            <span class="status-item__value">{{ statusLabel(selectedRun.runStatus) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">表 / 行</span>
            <span class="status-item__value">
              {{ selectedRun.completedTableCount || 0 }} / {{ selectedRun.totalTableCount || 0 }}
              · {{ selectedRun.syncedRowCount || 0 }} / {{ selectedRun.totalRowCount || 0 }}
            </span>
          </div>
          <div class="status-item">
            <span class="status-item__label">耗时</span>
            <span class="status-item__value">{{ formatDuration(selectedRun.durationMillis) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">开始时间</span>
            <span class="status-item__value">{{ formatTime(selectedRun.startedAt) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">结束时间</span>
            <span class="status-item__value">{{ formatTime(selectedRun.endedAt) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">消息</span>
            <span class="status-item__value">{{ selectedRun.progressMessage || '-' }}</span>
          </div>
          <div v-if="selectedRun.errorMessage" class="status-item">
            <span class="status-item__label">错误摘要</span>
            <span class="status-item__value execution-history-workbench__error-text">{{ selectedRun.errorMessage }}</span>
          </div>
          <div class="execution-history-workbench__summary-actions">
            <el-button type="primary" @click="openSelectedRunDetail">进入 Run 详情</el-button>
            <el-button @click="openSelectedRunTables">查看表级进度</el-button>
            <el-button @click="openSelectedRunLogs">查看执行日志</el-button>
          </div>
        </div>
        <StateEmpty
          v-else-if="!selectedTaskId"
          title="未选择任务"
          description="右侧会显示你选中的 run 摘要，先从左侧选择一个同步任务。"
          hint="选择任务后，摘要区会展示最近一次运行状态、耗时和错误摘要。"
        />
        <StateEmpty
          v-else
          title="还没有选中 Run"
          description="点击左侧任意 run 后，右侧会显示状态、耗时、表数、行数和日志入口。"
          hint="也可以直接点“选中最新”查看最近一次运行。"
          button-text="选中最新"
          @action="selectMostRecentRun"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLogRetentionDays, listTaskRuns, listTasks } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const tasks = ref([])
const runs = ref([])
const selectedTaskId = ref(null)
const selectedTaskName = ref('')
const selectedRun = ref(null)
const retentionDays = ref(30)
const filters = reactive({
  keyword: '',
  runId: '',
  status: '',
  scheduleType: '',
  tableText: '',
  limitMode: ''
})

const initialTaskId = computed(function () {
  const value = Number(route.query.taskId)
  return Number.isFinite(value) && value > 0 ? value : null
})

const isFiltered = computed(function () {
  return !!filters.keyword || !!filters.runId || !!filters.status || !!filters.scheduleType || !!filters.tableText || !!filters.limitMode
})

const currentTask = computed(function () {
  if (!selectedTaskId.value) {
    return null
  }
  return tasks.value.find(function (task) {
    return task.id === selectedTaskId.value
  }) || null
})

const filteredRuns = computed(function () {
  return runs.value.filter(function (item) {
    if (!item) {
      return false
    }
    if (filters.status && item.runStatus !== filters.status) {
      return false
    }
    if (filters.runId && String(item.runId || '').indexOf(filters.runId) < 0) {
      return false
    }
    if (filters.keyword) {
      const keyword = String(filters.keyword).toLowerCase()
      const text = [item.runId, item.runStatus, item.progressMessage, item.errorMessage].join(' ').toLowerCase()
      if (text.indexOf(keyword) < 0) {
        return false
      }
    }
    if (filters.tableText) {
      const tableText = String(filters.tableText).toLowerCase()
      const text = [item.sourceSchemaName, item.sourceTableName, item.targetSchemaName, item.targetTableName].join(' ').toLowerCase()
      if (text.indexOf(tableText) < 0) {
        return false
      }
    }
    return true
  })
})

const canOpenLatestRun = computed(function () {
  return !!selectedTaskId.value && filteredRuns.value.length > 0
})

const latestRun = computed(function () {
  return runs.value.length ? runs.value[0] : null
})

const latestRunSummaryText = computed(function () {
  if (!selectedTaskId.value || !latestRun.value) {
    return '—'
  }
  return statusLabel(latestRun.value.runStatus)
})

const latestRunHint = computed(function () {
  if (!selectedTaskId.value || !latestRun.value) {
    return '暂无执行记录'
  }
  const started = formatTime(latestRun.value.startedAt)
  const duration = formatDuration(latestRun.value.durationMillis)
  return started + ' · 耗时 ' + duration
})

const currentTaskHint = computed(function () {
  if (!currentTask.value) {
    return '先选择任务，再看 run 历史'
  }
  return selectedTaskRouteText.value
})

const selectedTaskRouteText = computed(function () {
  const task = currentTask.value
  if (!task) {
    return '先选择任务，再看 run 历史'
  }
  const source = task.sourceSchemaName ? task.sourceSchemaName + '.' : ''
  const target = task.targetSchemaName ? task.targetSchemaName + '.' : ''
  return source + (task.sourceTableName || '-') + ' → ' + target + (task.targetTableName || '-')
})

const selectedTaskModeText = computed(function () {
  const task = currentTask.value
  if (!task) {
    return '-'
  }
  if (task.syncMode) {
    return task.syncMode
  }
  if (task.incrementalMode) {
    return String(task.incrementalMode)
  }
  return task.incrementalEnabled ? '增量' : '全量'
})

const scheduleSummaryText = computed(function () {
  const task = currentTask.value
  if (!task) {
    return '-'
  }
  if (!task.scheduleEnabled) {
    return '未启用调度'
  }
  if (task.scheduleType === 'CRON') {
    return 'Cron ' + (task.scheduleCronExpression || '-')
  }
  if (task.scheduleType === 'INTERVAL') {
    return '间隔 ' + (task.scheduleIntervalSeconds || 0) + ' 秒'
  }
  return '手动'
})

const selectedTaskTableSummary = computed(function () {
  const task = currentTask.value
  if (!task) {
    return '未选择'
  }
  const source = task.sourceSchemaName ? task.sourceSchemaName + '.' : ''
  const target = task.targetSchemaName ? task.targetSchemaName + '.' : ''
  return source + (task.sourceTableName || '-') + ' → ' + target + (task.targetTableName || '-')
})

const filterStateText = computed(function () {
  return isFiltered.value ? '已筛选' : '未过滤'
})

const filterStateHint = computed(function () {
  return isFiltered.value ? ('当前条件下 ' + filteredRuns.value.length + ' 条结果') : '支持关键字、run_id、状态和调度'
})

const promptText = computed(function () {
  if (!selectedTaskId.value) {
    return '请选择一个同步任务，再查看执行历史；也可以直接从左侧菜单进入 Run 详情。'
  }
  if (!runs.value.length) {
    return '当前任务还没有执行历史，执行一次同步任务后会生成 run 记录。'
  }
  if (isFiltered.value && !filteredRuns.value.length) {
    return '当前筛选条件下没有执行历史，可以重置筛选后再查询。'
  }
  return '点击左侧 Run 批次查看摘要，或进入 Run 详情查看表级进度。'
})

const listBadgeText = computed(function () {
  if (!selectedTaskId.value) {
    return '未选择任务'
  }
  if (isFiltered.value) {
    return '已筛选'
  }
  return String(runs.value.length) + ' 个'
})

const listBadgeType = computed(function () {
  if (!selectedTaskId.value) {
    return 'info'
  }
  if (isFiltered.value) {
    return 'warning'
  }
  return runs.value.length ? 'success' : 'info'
})

const tableEmptyText = computed(function () {
  if (!selectedTaskId.value) {
    return '请选择任务'
  }
  if (!runs.value.length) {
    return '当前任务还没有执行历史'
  }
  if (isFiltered.value && !filteredRuns.value.length) {
    return '当前筛选条件下没有结果'
  }
  return '暂无数据'
})

const emptyStateType = computed(function () {
  if (!selectedTaskId.value) {
    return 'no-task'
  }
  if (!runs.value.length) {
    return 'no-runs'
  }
  if (isFiltered.value && !filteredRuns.value.length) {
    return 'filtered-empty'
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
    if (!selectedTaskId.value && tasks.value.length > 0) {
      const preferredTaskId = initialTaskId.value
      const initialTask = preferredTaskId ? tasks.value.find(function (task) {
        return task.id === preferredTaskId
      }) : null
      const firstTask = initialTask || tasks.value[0]
      selectedTaskId.value = firstTask.id
      selectedTaskName.value = firstTask.taskName
    }
    await loadRuns()
  } catch (error) {
    ElMessage.error(error.message || '加载执行历史失败')
  }
}

async function handleTaskChange(taskId) {
  const selectedTask = tasks.value.find(function (task) {
    return task.id === taskId
  })
  selectedTaskName.value = selectedTask ? selectedTask.taskName : ''
  selectedRun.value = null
  await loadRuns()
}

function applyFilters() {
  if (!selectedTaskId.value) {
    return
  }
  loadRuns()
}

function resetFilters() {
  filters.keyword = ''
  filters.runId = ''
  filters.status = ''
  filters.scheduleType = ''
  filters.tableText = ''
  filters.limitMode = ''
  loadRuns()
}

async function loadRuns() {
  if (!selectedTaskId.value) {
    runs.value = []
    selectedRun.value = null
    return
  }
  loading.value = true
  try {
    const rows = await listTaskRuns(selectedTaskId.value, 50)
    runs.value = Array.isArray(rows) ? rows : []
    if (filters.runId) {
      runs.value = runs.value.filter(function (item) {
        return String(item.runId || '').indexOf(filters.runId) >= 0
      })
    }
    selectedRun.value = runs.value.length > 0 ? runs.value[0] : null
    if (!selectedTaskName.value) {
      const selectedTask = tasks.value.find(function (task) {
        return task.id === selectedTaskId.value
      })
      selectedTaskName.value = selectedTask ? selectedTask.taskName : ''
    }
  } catch (error) {
    ElMessage.error(error.message || '加载 run 列表失败')
  } finally {
    loading.value = false
  }
}

function selectRun(row) {
  if (!row) {
    return
  }
  selectedRun.value = row
}

function selectMostRecentRun() {
  if (!filteredRuns.value.length) {
    return
  }
  selectedRun.value = filteredRuns.value[0]
}

function openSelectedRunDetail() {
  if (!selectedRun.value) {
    return
  }
  openRunDetail(selectedRun.value)
}

function openSelectedRunTables() {
  if (!selectedRun.value) {
    return
  }
  openRunTables(selectedRun.value)
}

function openSelectedRunLogs() {
  if (!selectedRun.value) {
    return
  }
  openRunLogs(selectedRun.value)
}

function openRunDetail(row) {
  if (!row || !selectedTaskId.value) {
    return
  }
  router.push({
    path: '/execution-history/detail',
    query: {
      taskId: selectedTaskId.value,
      runId: row.runId
    }
  })
}

function openRunTables(row) {
  if (!row || !selectedTaskId.value) {
    return
  }
  router.push({
    path: '/execution-history/detail',
    query: {
      taskId: selectedTaskId.value,
      runId: row.runId,
      tab: 'tables'
    }
  })
}

function openRunLogs(row) {
  if (!row || !selectedTaskId.value) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: String(selectedTaskId.value),
      runId: row.runId
    }
  })
}

function openLatestRun() {
  if (filteredRuns.value.length === 0) {
    return
  }
  selectedRun.value = filteredRuns.value[0]
  openRunDetail(filteredRuns.value[0])
}

function goToLogs() {
  if (!selectedTaskId.value || !selectedRun.value) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: String(selectedTaskId.value),
      runId: selectedRun.value.runId
    }
  })
}

function goToTasks() {
  router.push('/tasks')
}

function statusTagType(status) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'PARTIAL_SUCCESS') {
    return 'warning'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  if (status === 'PAUSED') {
    return 'warning'
  }
  if (status === 'STOPPED') {
    return 'info'
  }
  return 'primary'
}

function statusLabel(status) {
  if (status === 'RUNNING') {
    return '运行中'
  }
  if (status === 'SUCCESS') {
    return '成功'
  }
  if (status === 'FAILED') {
    return '失败'
  }
  if (status === 'PAUSED') {
    return '暂停'
  }
  if (status === 'STOPPED') {
    return '停止'
  }
  if (status === 'PARTIAL_SUCCESS') {
    return '部分成功'
  }
  return '-'
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
</script>

<style scoped>
.execution-history-workbench {
  gap: 16px;
}

.execution-history-workbench__header {
  align-items: flex-start;
}

.execution-history-workbench__titleblock {
  min-width: 0;
}

.execution-history-workbench__titleblock h1 {
  margin-bottom: 4px;
}

.execution-history-workbench__toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
}

.execution-history-workbench__overview .page-overview__value {
  font-size: 26px;
}

.execution-history-workbench__prompt-strip {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 14px 18px;
}

.execution-history-workbench__prompt-main {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.execution-history-workbench__prompt-label {
  color: var(--text-sub);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.execution-history-workbench__prompt-text {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.6;
}

.execution-history-workbench__prompt-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.execution-history-workbench__workspace {
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.85fr);
  align-items: start;
}

.execution-history-workbench__list-panel,
.execution-history-workbench__summary-panel {
  min-height: 100%;
}

.execution-history-workbench__list-body {
  display: grid;
  gap: 14px;
}

.execution-history-workbench__task-box,
.execution-history-workbench__filter-box {
  display: grid;
  gap: 12px;
}

.execution-history-workbench__task-select {
  width: 100%;
}

.execution-history-workbench__task-summary {
  padding-top: 2px;
}

.execution-history-workbench__filter-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.execution-history-workbench__table-shell {
  position: relative;
}

.execution-history-workbench__summary-stack {
  display: grid;
  gap: 10px;
}

.execution-history-workbench__summary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}

.execution-history-workbench__error-text {
  color: #b42318;
}

.execution-history-workbench__summary-panel .state-empty {
  margin-top: 8px;
}

@media (max-width: 1120px) {
  .execution-history-workbench__workspace {
    grid-template-columns: minmax(0, 1fr);
  }

  .execution-history-workbench__filter-grid {
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  }
}

@media (max-width: 760px) {
  .execution-history-workbench__prompt-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .execution-history-workbench__prompt-meta {
    justify-content: flex-start;
  }

  .execution-history-workbench__filter-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
