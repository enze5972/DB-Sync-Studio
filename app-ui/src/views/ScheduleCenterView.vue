<template>
  <div class="page-section schedule-center">
      <div class="page-header">
      <div>
        <h1>调度中心</h1>
        <p>独立管理任务调度状态、执行策略、下次执行时间和最近执行结果。</p>
      </div>
      <el-space>
        <el-button @click="loadPageData">刷新</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--two">
      <div class="page-overview__item">
        <div class="page-overview__label">任务总数</div>
        <div class="page-overview__value">{{ tasks.length }}</div>
        <div class="page-overview__hint">只复用现有任务表，不新建调度表</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">已启用调度</div>
        <div class="page-overview__value">{{ enabledCount }}</div>
        <div class="page-overview__hint">Cron、固定间隔、手动三种配置统一管理</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>任务列表</h2>
          <el-tag type="info">{{ selectedTaskName || '未选择任务' }}</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="tasks" border stripe v-loading="loading" highlight-current-row @row-click="selectTask">
            <el-table-column prop="taskName" label="任务" min-width="180" />
            <el-table-column label="调度状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.scheduleEnabled ? 'success' : 'info'">
                  {{ row.scheduleEnabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="配置" min-width="220">
              <template #default="{ row }">
                {{ scheduleSummary(row) }}
              </template>
            </el-table-column>
            <el-table-column label="下次执行" width="180">
              <template #default="{ row }">
                {{ formatTime(row.scheduleNextRunAt) }}
              </template>
            </el-table-column>
            <el-table-column label="最近结果" width="140">
              <template #default="{ row }">
                <el-tag :type="scheduleResultTagType(row.scheduleLastResult)">{{ row.scheduleLastResult || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="340" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" @click.stop="selectTask(row)">详情</el-button>
                  <el-switch
                    :model-value="!!row.scheduleEnabled"
                    inline-prompt
                    active-text="开"
                    inactive-text="关"
                    @change="function (value) { toggleSchedule(row, value) }"
                  />
                  <el-button size="small" type="primary" @click.stop="runSelectedTask(row)">
                    执行
                  </el-button>
                  <el-button size="small" @click.stop="openExecutionHistory(row)">历史</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>调度详情</h2>
          <el-tag type="warning">{{ selectedTaskName || '未选择' }}</el-tag>
        </div>
        <div v-if="selectedTask" class="status-stack schedule-detail">
          <div class="status-item">
            <span class="status-item__label">当前状态</span>
            <span class="status-item__value">{{ selectedTask.scheduleEnabled ? '启用' : '停用' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">调度类型</span>
            <span class="status-item__value">{{ scheduleSummary(selectedTask) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">下次执行</span>
            <span class="status-item__value">{{ formatTime(selectedTask.scheduleNextRunAt) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">最近结果</span>
            <span class="status-item__value">{{ selectedTask.scheduleLastResult || '-' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">最近消息</span>
            <span class="status-item__value">{{ selectedTask.scheduleLastMessage || '-' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">run_id</span>
            <span class="status-item__value">{{ selectedTask.scheduleLastRunId || '-' }}</span>
          </div>
        </div>

        <el-divider content-position="left">调度配置</el-divider>
        <el-form :model="scheduleForm" label-width="120px">
          <el-form-item label="启用调度">
            <el-switch v-model="scheduleForm.enabled" />
          </el-form-item>
          <el-form-item label="调度类型">
            <el-select v-model="scheduleForm.scheduleType" style="width: 100%;">
              <el-option label="手动" value="MANUAL" />
              <el-option label="Cron" value="CRON" />
              <el-option label="固定间隔" value="INTERVAL" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="scheduleForm.scheduleType === 'CRON'" label="Cron">
            <el-input v-model="scheduleForm.cronExpression" placeholder="例如 0 9 * * *" />
          </el-form-item>
          <el-form-item v-else-if="scheduleForm.scheduleType === 'INTERVAL'" label="间隔秒数">
            <el-input-number v-model="scheduleForm.intervalSeconds" :min="1" :step="60" style="width: 100%;" />
          </el-form-item>
          <el-form-item>
            <el-space>
              <el-button type="primary" :loading="saving" @click="saveSchedule">保存配置</el-button>
              <el-button @click="runSelectedTask(selectedTask)">手动执行</el-button>
              <el-button @click="openExecutionHistory(selectedTask)" :disabled="!selectedTask">执行历史</el-button>
            </el-space>
          </el-form-item>
        </el-form>

        <el-divider content-position="left">执行历史</el-divider>
        <div class="status-stack">
          <div v-for="entry in scheduleHistory" :key="entry.id" class="capability-item">
            <div class="capability-item__title">{{ formatTime(entry.createdAt) }}</div>
            <div class="capability-item__desc">
              <div>{{ entry.logLevel }} - {{ entry.logMessage }}</div>
              <div class="task-log-meta">
                <span>run_id: {{ entry.runId || '-' }}</span>
                <span>table: {{ entry.tableName || '-' }}</span>
              </div>
            </div>
          </div>
          <div v-if="!scheduleHistory.length" class="status-item">
            <span class="status-item__label">提示</span>
            <span class="status-item__value">暂无历史记录</span>
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
import { listTaskScheduleHistory, listTasks, runTask, updateTaskScheduleState } from '../services/backend'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const tasks = ref([])
const selectedTaskId = ref(null)
const selectedTask = ref(null)
const selectedTaskName = ref('')
const scheduleHistory = ref([])

const scheduleForm = reactive({
  enabled: false,
  scheduleType: 'MANUAL',
  cronExpression: '',
  intervalSeconds: 300
})

const enabledCount = computed(function () {
  return tasks.value.filter(function (task) {
    return !!task.scheduleEnabled
  }).length
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  loading.value = true
  try {
    tasks.value = await listTasks()
    if (tasks.value.length > 0) {
      const current = selectedTaskId.value ? tasks.value.find(function (task) {
        return task.id === selectedTaskId.value
      }) : tasks.value[0]
      if (current) {
        selectTask(current)
      }
    } else {
      selectedTask.value = null
      selectedTaskName.value = ''
      scheduleHistory.value = []
    }
  } catch (error) {
    ElMessage.error(error.message || '加载调度中心失败')
  } finally {
    loading.value = false
  }
}

function selectTask(row) {
  if (!row) {
    return
  }
  selectedTaskId.value = row.id
  selectedTask.value = row
  selectedTaskName.value = row.taskName
  scheduleForm.enabled = !!row.scheduleEnabled
  scheduleForm.scheduleType = row.scheduleType || 'MANUAL'
  scheduleForm.cronExpression = row.scheduleCronExpression || ''
  scheduleForm.intervalSeconds = row.scheduleIntervalSeconds || 300
  loadScheduleHistory(row.id)
}

async function loadScheduleHistory(taskId) {
  try {
    scheduleHistory.value = await listTaskScheduleHistory(taskId)
  } catch (error) {
    ElMessage.error(error.message || '加载执行历史失败')
  }
}

async function saveSchedule() {
  if (!selectedTask.value) {
    ElMessage.warning('请先选择任务')
    return
  }
  saving.value = true
  try {
    const result = await updateTaskScheduleState(selectedTask.value.id, {
      enabled: scheduleForm.enabled,
      scheduleType: scheduleForm.scheduleType,
      cronExpression: scheduleForm.cronExpression,
      intervalSeconds: scheduleForm.intervalSeconds
    })
    selectedTask.value = result
    ElMessage.success('调度配置已保存')
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '保存调度失败')
  } finally {
    saving.value = false
  }
}

async function toggleSchedule(row, value) {
  try {
    await updateTaskScheduleState(row.id, {
      enabled: !!value,
      scheduleType: row.scheduleType || 'MANUAL',
      cronExpression: row.scheduleCronExpression || '',
      intervalSeconds: row.scheduleIntervalSeconds || 300
    })
    ElMessage.success('调度状态已更新')
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '更新调度状态失败')
  }
}

async function runSelectedTask(row) {
  if (!row) {
    return
  }
  try {
    await runTask(row.id)
    ElMessage.success('手动执行已触发')
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '执行失败')
  }
}

function openExecutionHistory(row) {
  if (!row || !row.id) {
    return
  }
  router.push({
    path: '/execution-history',
    query: {
      taskId: String(row.id)
    }
  })
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function scheduleSummary(row) {
  if (!row || !row.scheduleEnabled) {
    return '未启用'
  }
  if (row.scheduleType === 'CRON') {
    return 'Cron · ' + (row.scheduleCronExpression || '-')
  }
  if (row.scheduleType === 'INTERVAL') {
    return '间隔 · ' + (row.scheduleIntervalSeconds || 0) + ' 秒'
  }
  return '手动'
}

function scheduleResultTagType(value) {
  if (value === 'SUCCESS') {
    return 'success'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  if (value === 'RUNNING') {
    return 'warning'
  }
  return 'info'
}
</script>
