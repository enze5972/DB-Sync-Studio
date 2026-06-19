<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>执行历史</h1>
        <p>按任务查看同步执行批次、表级进度和日志，方便回溯每次运行的结果。</p>
      </div>
      <el-space>
        <el-button round @click="loadPageData">刷新</el-button>
        <el-button type="primary" round :disabled="!selectedTaskId" @click="openLatestRun">
          查看最新
        </el-button>
        <el-button round @click="goToLogs">查看日志</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--two">
      <div class="page-overview__item">
        <div class="page-overview__label">当前任务</div>
        <div class="page-overview__value">{{ selectedTaskName || '未选择' }}</div>
        <div class="page-overview__hint">先选任务，再看 run 历史</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">执行批次</div>
        <div class="page-overview__value">{{ runs.length }}</div>
        <div class="page-overview__hint">run_id 会贯穿任务、表和日志</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">过滤条件</div>
        <div class="page-overview__value">{{ filterSummary }}</div>
        <div class="page-overview__hint">支持关键字、状态和最近 run</div>
      </div>
    </div>

    <div v-if="!selectedTaskId" class="panel-card glass-panel">
      <div class="status-item">
        <span class="status-item__label">提示</span>
        <span class="status-item__value">先选择一个任务，再查看执行历史；也可以直接从左侧菜单进入 Run 详情</span>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>任务选择</h2>
          <el-tag type="info" effect="dark">{{ tasks.length }} 个</el-tag>
        </div>
        <el-select v-model="selectedTaskId" style="width: 100%;" filterable @change="handleTaskChange">
          <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
        </el-select>

        <div class="section-title section-title--compact" style="margin-top: 18px;">
          <h2>Run 列表</h2>
          <el-space>
            <el-input v-model="filters.keyword" placeholder="关键词过滤" clearable style="width: 180px;" />
            <el-input v-model="filters.runId" placeholder="run_id" clearable style="width: 180px;" />
            <el-select v-model="filters.status" placeholder="状态" clearable style="width: 140px;">
              <el-option label="RUNNING" value="RUNNING" />
              <el-option label="SUCCESS" value="SUCCESS" />
              <el-option label="FAILED" value="FAILED" />
              <el-option label="PAUSED" value="PAUSED" />
              <el-option label="STOPPED" value="STOPPED" />
            </el-select>
            <el-button @click="applyFilters">筛选</el-button>
          </el-space>
        </div>
        <div class="status-stack" style="margin-bottom: 16px;">
          <div class="status-item">
            <span class="status-item__label">调度</span>
            <span class="status-item__value">{{ scheduleSummaryText }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">任务表</span>
            <span class="status-item__value">{{ selectedTaskTableSummary }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">保留天数</span>
            <span class="status-item__value">{{ retentionDays }} 天</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">run_id</span>
            <span class="status-item__value">{{ selectedRun ? selectedRun.runId : '-' }}</span>
          </div>
        </div>

        <div class="table-shell">
          <el-table :data="filteredRuns" border stripe v-loading="loading" highlight-current-row @row-click="openRunDetail">
            <el-table-column prop="runId" label="run_id" min-width="220" show-overflow-tooltip />
            <el-table-column prop="runStatus" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.runStatus)">{{ row.runStatus || '-' }}</el-tag>
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
            <el-table-column label="run" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openRunDetail(row)">详情</el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" @click.stop="openRunDetail(row)">详情</el-button>
                  <el-button size="small" type="primary" plain @click.stop="openRunTables(row)">表级</el-button>
                  <el-button size="small" @click.stop="openRunLogs(row)">日志</el-button>
                </el-space>
              </template>
            </el-table-column>
            </el-table>
          <el-empty v-if="!loading && filteredRuns.length === 0" description="暂无符合条件的 Run 记录" />
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>任务摘要</h2>
          <el-tag type="success" effect="dark">{{ selectedTaskName || '未选择' }}</el-tag>
        </div>
        <div v-if="selectedRun" class="status-stack">
          <div class="status-item">
            <span class="status-item__label">最近 run</span>
            <span class="status-item__value">{{ selectedRun.runId }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">状态</span>
            <span class="status-item__value">{{ selectedRun.runStatus || '-' }}</span>
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
            <span class="status-item__label">消息</span>
            <span class="status-item__value">{{ selectedRun.progressMessage || '-' }}</span>
          </div>
        </div>
        <div v-else class="status-item">
          <span class="status-item__label">提示</span>
          <span class="status-item__value">点击左侧 run 查看详情，或使用“表级 / 日志”快速进入关联页面</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLogRetentionDays, listTaskRuns, listTasks } from '../services/backend'

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
  status: ''
})

const filterSummary = computed(function () {
  const pieces = []
  if (filters.keyword) {
    pieces.push('关键词')
  }
  if (filters.status) {
    pieces.push(filters.status)
  }
  return pieces.length ? pieces.join(' · ') : '未过滤'
})

const scheduleSummaryText = computed(function () {
  const task = tasks.value.find(function (item) {
    return item.id === selectedTaskId.value
  })
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
  const task = tasks.value.find(function (item) {
    return item.id === selectedTaskId.value
  })
  if (!task) {
    return '-'
  }
  const source = task.sourceSchemaName ? task.sourceSchemaName + '.' : ''
  const target = task.targetSchemaName ? task.targetSchemaName + '.' : ''
  return source + task.sourceTableName + ' → ' + target + task.targetTableName
})

const filteredRuns = computed(function () {
  return runs.value.filter(function (item) {
    if (!item) {
      return false
    }
    if (filters.status && item.runStatus !== filters.status) {
      return false
    }
    if (filters.keyword) {
      const keyword = String(filters.keyword).toLowerCase()
      const text = [
        item.runId,
        item.runStatus,
        item.progressMessage,
        item.errorMessage
      ].join(' ').toLowerCase()
      if (text.indexOf(keyword) < 0) {
        return false
      }
    }
    return true
  })
})

onMounted(function () {
  loadPageData()
})

const initialTaskId = computed(function () {
  const value = Number(route.query.taskId)
  return Number.isFinite(value) && value > 0 ? value : null
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
  openRunDetail(filteredRuns.value[0])
}

function goToLogs() {
  router.push('/logs')
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
