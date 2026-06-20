<template>
  <div class="page-section alert-history">
    <div class="page-header">
      <div>
        <h1>告警历史</h1>
        <p>查看每次告警发送结果、关联任务与运行批次，支持按条件筛选。</p>
      </div>
      <el-space>
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--three">
      <div class="page-overview__item">
        <div class="page-overview__label">告警总数</div>
        <div class="page-overview__value">{{ history.length }}</div>
        <div class="page-overview__hint">按当前筛选条件统计</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">成功发送</div>
        <div class="page-overview__value">{{ successCount }}</div>
        <div class="page-overview__hint">send_status = SUCCESS</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">发送失败</div>
        <div class="page-overview__value">{{ failedCount }}</div>
        <div class="page-overview__hint">便于定位渠道配置问题</div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>筛选条件</h2>
        <el-space>
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" :loading="loading" @click="loadPageData">查询</el-button>
        </el-space>
      </div>
      <el-row :gutter="16">
        <el-col :span="6">
          <el-select v-model="filters.taskId" clearable filterable placeholder="任务" style="width: 100%;">
            <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.alertType" clearable placeholder="类型" style="width: 100%;">
            <el-option v-for="item in alertTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.sendStatus" clearable placeholder="状态" style="width: 100%;">
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input v-model="filters.keyword" clearable placeholder="内容关键字" />
        </el-col>
      </el-row>
      <el-row :gutter="16" style="margin-top: 14px;">
        <el-col :span="8">
          <el-date-picker v-model="filters.timeRange" type="datetimerange" value-format="x" start-placeholder="开始时间" end-placeholder="结束时间" style="width: 100%;" />
        </el-col>
        <el-col :span="16" class="alert-history__actions">
          <el-input-number v-model="filters.limit" :min="1" :max="200" />
        </el-col>
      </el-row>
      <el-alert
        v-if="loadError"
        :title="loadError"
        type="error"
        show-icon
        :closable="false"
        style="margin-top: 14px;"
      />
    </div>

    <div class="panel-card glass-panel" style="margin-top: 16px;">
      <div class="table-shell" v-loading="loading">
        <el-table :data="history" border stripe>
          <el-table-column prop="createdTime" label="时间" width="190">
            <template #default="{ row }">
              {{ formatTime(row.createdTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="alertType" label="类型" width="180" />
          <el-table-column label="任务" width="140">
            <template #default="{ row }">
              {{ resolveTaskName(row.taskId) }}
            </template>
          </el-table-column>
          <el-table-column prop="runId" label="run_id" min-width="180" show-overflow-tooltip />
          <el-table-column prop="tableName" label="表名" min-width="140" />
          <el-table-column label="级别" width="100">
            <template #default="{ row }">
              <el-tag :type="alertLevelTagType(row.alertLevel)">{{ row.alertLevel || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="渠道" width="120">
            <template #default="{ row }">
              {{ row.channelType || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="sendStatusTagType(row.sendStatus)">{{ row.sendStatus || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="alertContent" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column label="结果" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.errorMessage || (row.sendStatus === 'SUCCESS' ? '发送成功' : '-') }}
            </template>
          </el-table-column>
        </el-table>
        <StateEmpty
          v-if="!loading && !history.length"
          title="还没有告警历史"
          :description="emptyDescription"
          hint="告警发送后会在这里记录时间、渠道、状态和内容。"
          button-text="去告警设置"
          @action="goToAlertSettings"
        />
      </div>
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
    return item.sendStatus === 'FAILED'
  }).length
})

const emptyDescription = computed(function () {
  return loadError.value ? '告警历史加载失败，请重试' : '当前筛选条件下没有告警历史'
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

function alertLevelTagType(level) {
  if (level === 'ERROR') {
    return 'danger'
  }
  if (level === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function sendStatusTagType(status) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  return 'info'
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
