<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>Run 详情</h1>
        <p>查看单次执行的表级进度、日志和断点信息。</p>
      </div>
      <el-space>
        <el-button @click="goBack">返回历史</el-button>
        <el-button @click="goToLogs">查看日志</el-button>
        <el-button type="primary" :loading="loading" @click="loadDetail">刷新</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--two">
      <div class="page-overview__item">
        <div class="page-overview__label">run_id</div>
        <div class="page-overview__value">{{ detail.run.runId || '-' }}</div>
        <div class="page-overview__hint">当前执行批次标识</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">状态</div>
        <div class="page-overview__value">{{ detail.run.runStatus || '-' }}</div>
        <div class="page-overview__hint">{{ detail.run.progressMessage || '暂无消息' }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">表 / 行</div>
        <div class="page-overview__value">
          {{ detail.run.completedTableCount || 0 }} / {{ detail.run.totalTableCount || 0 }}
          · {{ detail.run.syncedRowCount || 0 }} / {{ detail.run.totalRowCount || 0 }}
        </div>
        <div class="page-overview__hint">按 run 聚合的同步结果</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">耗时</div>
        <div class="page-overview__value">{{ formatDuration(detail.run.durationMillis) }}</div>
        <div class="page-overview__hint">包含所有表的执行时间</div>
      </div>
    </div>

    <div class="panel-card glass-panel" v-if="detail.run.runId">
      <div class="section-title">
        <h2>Run 基本信息</h2>
        <el-space>
          <el-tag type="info" effect="dark">{{ detail.run.taskName || '任务未命名' }}</el-tag>
          <el-tag :type="statusTagType(detail.run.runStatus)">{{ detail.run.runStatus || '-' }}</el-tag>
        </el-space>
      </div>
      <div class="status-stack">
        <div class="status-item">
          <span class="status-item__label">task_id</span>
          <span class="status-item__value">{{ detail.run.taskId || '-' }}</span>
        </div>
        <div class="status-item">
          <span class="status-item__label">run_id</span>
          <span class="status-item__value">{{ detail.run.runId || '-' }}</span>
        </div>
        <div class="status-item">
          <span class="status-item__label">开始 / 结束</span>
          <span class="status-item__value">{{ formatTime(detail.run.startedAt) }} / {{ formatTime(detail.run.endedAt) }}</span>
        </div>
        <div class="status-item">
          <span class="status-item__label">最近消息</span>
          <span class="status-item__value">{{ detail.run.progressMessage || '-' }}</span>
        </div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>表级进度</h2>
          <el-space>
            <el-tag type="success" effect="dark">{{ detail.tableRuns.length }} 张表</el-tag>
            <el-tag type="info" effect="dark">{{ currentRunTableSummary }}</el-tag>
          </el-space>
        </div>
        <div class="table-shell">
          <el-table :data="detail.tableRuns" border stripe v-loading="loading">
            <el-table-column label="顺序" width="80">
              <template #default="{ row }">
                {{ row.tableOrder || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="sourceTableName" label="源表" min-width="180" />
            <el-table-column prop="targetTableName" label="目标表" min-width="180" />
            <el-table-column label="table_task_id" width="160" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.tableTaskId || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.tableStatus)">{{ row.tableStatus || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="行数" width="140">
              <template #default="{ row }">
                {{ row.syncedRowCount || 0 }} / {{ row.totalRowCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="成功 / 失败" width="140">
              <template #default="{ row }">
                {{ row.successRowCount || 0 }} / {{ row.failedRowCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="断点" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.checkpointValue || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="120">
              <template #default="{ row }">
                {{ formatDuration(row.durationMillis) }}
              </template>
            </el-table-column>
            <el-table-column label="错误" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.errorMessage || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="openTableLogs(row)">表日志</el-button>
              </template>
            </el-table-column>
          </el-table>
          <StateEmpty
            v-if="!loading && !detail.tableRuns.length"
            title="还没有表级进度"
            description="这个 run 还没有写入表级执行结果，或者执行还没开始。"
            hint="表级进度会展示每张表的状态、耗时、断点和错误。"
            button-text="返回历史"
            @action="goBack"
          />
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>执行日志</h2>
          <el-space>
            <el-input v-model="filters.keyword" placeholder="关键词" clearable style="width: 160px;" />
            <el-select v-model="filters.logLevel" placeholder="级别" clearable style="width: 120px;">
              <el-option label="INFO" value="INFO" />
              <el-option label="WARN" value="WARN" />
              <el-option label="ERROR" value="ERROR" />
            </el-select>
            <el-select v-model="filters.tableName" placeholder="表名" clearable filterable style="width: 160px;">
              <el-option v-for="item in tableNameOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-button @click="loadLogs">筛选</el-button>
          </el-space>
        </div>
        <div class="table-shell">
          <el-table :data="logs" border stripe v-loading="loading">
            <el-table-column label="时间" width="220">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column prop="runId" label="run_id" width="220" show-overflow-tooltip />
            <el-table-column prop="tableName" label="表" width="160" />
            <el-table-column prop="logLevel" label="级别" width="120" />
            <el-table-column prop="logMessage" label="日志内容" min-width="440" />
          </el-table>
          <StateEmpty
            v-if="!loading && !logs.length"
            title="还没有关联日志"
            description="当前 run 没有匹配的日志记录。"
            hint="你可以切换到执行历史或同步任务页面查看更多上下文。"
            button-text="查看日志页"
            @action="goToLogs"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTaskRun, listTaskRunLogs } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = reactive({
  run: {},
  tableRuns: []
})
const logs = ref([])
const filters = reactive({
  keyword: '',
  logLevel: '',
  tableName: ''
})
const tableNameOptions = computed(function () {
  return detail.tableRuns
    .map(function (row) {
      return row.sourceTableName || row.targetTableName || ''
    })
    .filter(function (value, index, array) {
      return value && array.indexOf(value) === index
    })
})

const currentRunTableSummary = computed(function () {
  if (!detail.tableRuns.length) {
    return '暂无表级数据'
  }
  const finished = detail.tableRuns.filter(function (row) {
    return row.tableStatus === 'SUCCESS'
  }).length
  return finished + ' / ' + detail.tableRuns.length + ' 已完成'
})

onMounted(function () {
  loadDetail()
})

async function loadDetail() {
  const taskId = Number(route.query.taskId)
  const runId = route.query.runId
  if (!taskId || !runId) {
    ElMessage.warning('缺少 taskId 或 runId')
    return
  }
  loading.value = true
  try {
    const response = await getTaskRun(taskId, runId, 100)
    detail.run = response.run || {}
    detail.tableRuns = response.tableRuns || []
    logs.value = response.logs || []
    await loadLogs()
  } catch (error) {
    ElMessage.error(error.message || '加载 run 详情失败')
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  const taskId = Number(route.query.taskId)
  const runId = route.query.runId
  if (!taskId || !runId) {
    logs.value = []
    return
  }
  try {
    logs.value = await listTaskRunLogs(taskId, runId, {
      syncRunId: detail.run.id,
      tableName: filters.tableName,
      logLevel: filters.logLevel,
      keyword: filters.keyword,
      limit: 200
    })
  } catch (error) {
    ElMessage.error(error.message || '加载日志失败')
  }
}

function goBack() {
  router.push({
    path: '/execution-history',
    query: {
      taskId: route.query.taskId,
      runId: route.query.runId,
      tab: route.query.tab || ''
    }
  })
}

function goToLogs() {
  router.push({
    path: '/logs',
    query: {
      taskId: route.query.taskId,
      runId: route.query.runId
    }
  })
}

function openTableLogs(row) {
  if (!row || !detail.run.runId) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: route.query.taskId,
      runId: detail.run.runId,
      tableName: row.sourceTableName || row.targetTableName || ''
    }
  })
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
