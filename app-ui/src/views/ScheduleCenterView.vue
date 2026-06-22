<template>
  <div class="page-section list-page schedule-center-workbench">
    <div class="page-header schedule-center-workbench__header">
      <div class="schedule-center-workbench__titleblock">
        <h1>调度中心</h1>
        <p>独立管理任务调度状态、执行策略、下次执行时间和最近执行结果。</p>
      </div>
      <div class="schedule-center-workbench__toolbar">
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
      </div>
    </div>

    <div class="page-overview page-overview--four schedule-center-workbench__overview">
      <div class="page-overview__item">
        <div class="page-overview__label">任务总数</div>
        <div class="page-overview__value">{{ tasks.length }}</div>
        <div class="page-overview__hint">当前可配置调度的同步任务</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">已启用调度</div>
        <div class="page-overview__value">{{ enabledCount }}</div>
        <div class="page-overview__hint">Cron / 固定间隔任务</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">待配置</div>
        <div class="page-overview__value">{{ pendingCount }}</div>
        <div class="page-overview__hint">未启用或未设置执行策略</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">最近执行</div>
        <div class="page-overview__value">{{ latestExecutionValue }}</div>
        <div class="page-overview__hint">{{ latestExecutionHint }}</div>
      </div>
    </div>

    <div class="panel-card glass-panel schedule-center-workbench__prompt-strip">
      <div class="schedule-center-workbench__prompt-main">
        <span class="schedule-center-workbench__prompt-label">提示</span>
        <span class="schedule-center-workbench__prompt-text">{{ promptText }}</span>
      </div>
      <div class="schedule-center-workbench__prompt-meta">
        <el-tag :type="selectedTask ? 'success' : 'info'" effect="light">
          {{ selectedTask ? '已选择任务' : '未选择任务' }}
        </el-tag>
        <el-tag :type="selectedTask && selectedTask.scheduleEnabled ? 'success' : 'info'" effect="light">
          {{ selectedTask && selectedTask.scheduleEnabled ? '调度已启用' : '调度未启用' }}
        </el-tag>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact schedule-center-workbench__workspace">
      <div class="panel-card glass-panel schedule-center-workbench__task-panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>任务列表</h2>
            <el-tag :type="tasks.length ? 'success' : 'info'" effect="dark">{{ tasks.length }} 个</el-tag>
          </div>
          <el-space>
            <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
              <span>
                <el-button size="small" @click="openExecutionHistory(selectedTask)" :disabled="!selectedTask">执行历史</el-button>
              </span>
            </el-tooltip>
            <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
              <span>
                <el-button size="small" type="primary" plain @click="runSelectedTask(selectedTask)" :disabled="!selectedTask">
                  手动执行
                </el-button>
              </span>
            </el-tooltip>
          </el-space>
        </div>
        <div class="schedule-center-workbench__task-summary">
          <div class="status-item">
            <span class="status-item__label">当前任务</span>
            <span class="status-item__value">{{ selectedTaskName || '未选择任务' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">调度状态</span>
            <span class="status-item__value">{{ selectedTask ? scheduleStatusLabel(selectedTask) : '请先选择任务' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">调度类型</span>
            <span class="status-item__value">{{ selectedTask ? scheduleSummary(selectedTask) : '-' }}</span>
          </div>
        </div>

        <div class="table-shell schedule-center-workbench__table-shell">
          <el-table
            :data="tasks"
            border
            stripe
            v-loading="loading"
            highlight-current-row
            :empty-text="taskTableEmptyText"
            @row-click="selectTask"
          >
            <el-table-column prop="taskName" label="任务" min-width="180" show-overflow-tooltip />
            <el-table-column label="调度状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.scheduleEnabled ? 'success' : 'info'" effect="light">
                  {{ row.scheduleEnabled ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="配置" min-width="220" show-overflow-tooltip>
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
                <el-tag :type="scheduleResultTagType(row.scheduleLastResult)" effect="light">
                  {{ row.scheduleLastResult || '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
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
          <StateEmpty
            v-if="emptyStateType === 'no-tasks'"
            title="当前还没有可配置的任务"
            description="先创建同步任务，再回到调度中心配置执行策略、下次执行时间和最近结果。"
            hint="任务创建完成后，这里会显示调度开关、类型和历史入口。"
            button-text="去同步任务"
            @action="goToTasks"
          />
        </div>
      </div>

      <div class="panel-card glass-panel schedule-center-workbench__detail-panel">
          <div class="section-title">
            <div class="section-title__left">
              <h2>调度详情</h2>
              <el-tag :type="selectedTask ? (selectedTask.scheduleEnabled ? 'success' : 'warning') : 'info'" effect="dark">
                {{ selectedTask ? (selectedTask.scheduleEnabled ? '已启用' : '未启用') : '未选择' }}
              </el-tag>
            </div>
          <el-space>
            <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
              <span>
                <el-button size="small" @click="openExecutionHistory(selectedTask)" :disabled="!selectedTask">执行历史</el-button>
              </span>
            </el-tooltip>
            <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
              <span>
                <el-button size="small" type="primary" plain :loading="saving" :disabled="!selectedTask" @click="saveSchedule">
                  保存配置
                </el-button>
              </span>
            </el-tooltip>
          </el-space>
        </div>

        <StateEmpty
          v-if="!selectedTask"
          title="请选择一个任务"
          description="右侧会展示当前任务的调度配置、执行策略、最近执行结果和历史摘要。"
          hint="先从左侧列表选择任务，再启用或调整调度。"
        />

        <div v-else class="schedule-center-workbench__detail-stack">
          <div class="status-stack">
            <div class="status-item">
              <span class="status-item__label">任务名称</span>
              <span class="status-item__value">{{ selectedTaskName }}</span>
            </div>
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

          <div class="schedule-center-workbench__context-card">
            <div class="section-title section-title--compact">
              <div class="section-title__left">
                <h3>任务上下文</h3>
                <el-tag type="info" effect="light">{{ taskContextStatus }}</el-tag>
              </div>
            </div>
            <div class="status-stack">
              <div class="status-item">
                <span class="status-item__label">源表</span>
                <span class="status-item__value">{{ taskContextText.source }}</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">目标表</span>
                <span class="status-item__value">{{ taskContextText.target }}</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">同步模式</span>
                <span class="status-item__value">{{ taskContextText.mode }}</span>
              </div>
            </div>
          </div>

          <div class="schedule-center-workbench__form-card">
            <div class="section-title section-title--compact">
              <div class="section-title__left">
                <h3>调度配置</h3>
                <el-tag :type="configSummaryTagType" effect="light">{{ configSummaryText }}</el-tag>
              </div>
            </div>
            <el-form :model="scheduleForm" label-width="120px" class="schedule-center-workbench__form">
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
                  <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
                    <span>
                      <el-button type="primary" :loading="saving" :disabled="!selectedTask" @click="saveSchedule">
                        保存配置
                      </el-button>
                    </span>
                  </el-tooltip>
                  <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
                    <span>
                      <el-button :disabled="!selectedTask" @click="runSelectedTask(selectedTask)">手动执行</el-button>
                    </span>
                  </el-tooltip>
                  <el-tooltip :disabled="!!selectedTask" content="请先选择任务" placement="top">
                    <span>
                      <el-button :disabled="!selectedTask" @click="openExecutionHistory(selectedTask)">执行历史</el-button>
                    </span>
                  </el-tooltip>
                </el-space>
              </el-form-item>
            </el-form>
          </div>

          <div class="schedule-center-workbench__history-card">
            <div class="section-title section-title--compact">
              <div class="section-title__left">
                <h3>最近执行历史</h3>
                <el-tag type="info" effect="light">{{ scheduleHistory.length }} 条</el-tag>
              </div>
              <el-button size="small" @click="loadScheduleHistory(selectedTask.id)">刷新历史</el-button>
            </div>
            <div v-if="scheduleHistory.length" class="status-stack">
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
            </div>
            <div v-else class="status-item">
              <span class="status-item__label">提示</span>
              <span class="status-item__value">当前任务暂无历史记录</span>
            </div>
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
import StateEmpty from '../components/StateEmpty.vue'

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

const pendingCount = computed(function () {
  return tasks.value.filter(function (task) {
    return !task.scheduleEnabled || !task.scheduleType || task.scheduleType === 'MANUAL'
  }).length
})

const latestExecution = computed(function () {
  let latest = null
  tasks.value.forEach(function (task) {
    if (!task || !task.scheduleLastRunAt) {
      return
    }
    if (!latest || Number(task.scheduleLastRunAt) > Number(latest.scheduleLastRunAt)) {
      latest = task
    }
  })
  return latest
})

const latestExecutionValue = computed(function () {
  if (!latestExecution.value) {
    return '—'
  }
  return scheduleResultLabel(latestExecution.value.scheduleLastResult)
})

const latestExecutionHint = computed(function () {
  if (!latestExecution.value) {
    return '暂无执行记录'
  }
  const task = latestExecution.value
  return formatTime(task.scheduleLastRunAt)
})

const promptText = computed(function () {
  if (!tasks.value.length) {
    return '先创建同步任务，再为任务配置调度策略、执行时间和历史查看入口。'
  }
  if (!selectedTask.value) {
    return '请选择一个同步任务，再查看调度配置、执行策略和最近执行结果。'
  }
  return '可以启用或停用调度，修改 Cron / 固定间隔配置，或直接触发一次手动执行。'
})

const taskTableEmptyText = computed(function () {
  if (loading.value) {
    return '加载中'
  }
  return '当前没有可配置的任务'
})

const emptyStateType = computed(function () {
  if (!tasks.value.length) {
    return 'no-tasks'
  }
  return ''
})

const taskContextText = computed(function () {
  const task = selectedTask.value
  if (!task) {
    return {
      source: '-',
      target: '-',
      mode: '-'
    }
  }
  const source = task.sourceSchemaName ? task.sourceSchemaName + '.' : ''
  const target = task.targetSchemaName ? task.targetSchemaName + '.' : ''
  return {
    source: source + (task.sourceTableName || '-'),
    target: target + (task.targetTableName || '-'),
    mode: task.syncMode || task.incrementalMode || (task.scheduleEnabled ? '启用中' : '手动')
  }
})

const taskContextStatus = computed(function () {
  if (!selectedTask.value) {
    return '未选择'
  }
  return selectedTask.value.scheduleEnabled ? '已启用' : '未启用'
})

const configSummaryText = computed(function () {
  if (!selectedTask.value) {
    return '未选择任务'
  }
  return scheduleSummary(selectedTask.value)
})

const configSummaryTagType = computed(function () {
  if (!selectedTask.value) {
    return 'info'
  }
  if (selectedTask.value.scheduleEnabled) {
    return 'success'
  }
  if (selectedTask.value.scheduleType === 'CRON' || selectedTask.value.scheduleType === 'INTERVAL') {
    return 'warning'
  }
  return 'info'
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
    selectedTaskName.value = result.taskName || selectedTaskName.value
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

function goToTasks() {
  router.push('/tasks')
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function scheduleSummary(row) {
  if (!row || !row.scheduleEnabled) {
    if (!row || !row.scheduleType || row.scheduleType === 'MANUAL') {
      return '手动'
    }
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

function scheduleStatusLabel(row) {
  if (!row) {
    return '-'
  }
  if (row.scheduleEnabled) {
    return '已启用'
  }
  return '已停用'
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
  if (!value) {
    return 'info'
  }
  return 'info'
}

function scheduleResultLabel(value) {
  if (!value) {
    return '—'
  }
  if (value === 'SUCCESS') {
    return '成功'
  }
  if (value === 'FAILED') {
    return '失败'
  }
  if (value === 'RUNNING') {
    return '运行中'
  }
  return value
}
</script>

<style scoped>
.schedule-center-workbench {
  gap: 16px;
}

.schedule-center-workbench__header {
  align-items: flex-start;
}

.schedule-center-workbench__titleblock {
  min-width: 0;
}

.schedule-center-workbench__titleblock h1 {
  margin-bottom: 4px;
}

.schedule-center-workbench__toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.schedule-center-workbench__overview .page-overview__value {
  font-size: 26px;
}

.schedule-center-workbench__prompt-strip {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 14px 18px;
}

.schedule-center-workbench__prompt-main {
  display: flex;
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.schedule-center-workbench__prompt-label {
  color: var(--text-sub);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.schedule-center-workbench__prompt-text {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.6;
}

.schedule-center-workbench__prompt-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.schedule-center-workbench__workspace {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 0.95fr);
  align-items: start;
}

.schedule-center-workbench__task-panel,
.schedule-center-workbench__detail-panel {
  min-height: 100%;
}

.schedule-center-workbench__task-summary {
  display: grid;
  gap: 10px;
  margin-bottom: 14px;
}

.schedule-center-workbench__table-shell {
  position: relative;
}

.schedule-center-workbench__detail-stack {
  display: grid;
  gap: 14px;
}

.schedule-center-workbench__context-card,
.schedule-center-workbench__form-card,
.schedule-center-workbench__history-card {
  display: grid;
  gap: 12px;
  padding: 14px 0 0;
  border-top: 1px solid rgba(148, 163, 184, 0.16);
}

.schedule-center-workbench__form :deep(.el-form-item) {
  margin-bottom: 16px;
}

@media (max-width: 1120px) {
  .schedule-center-workbench__workspace {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 760px) {
  .schedule-center-workbench__prompt-strip {
    flex-direction: column;
    align-items: flex-start;
  }

  .schedule-center-workbench__prompt-meta {
    justify-content: flex-start;
  }
}
</style>
