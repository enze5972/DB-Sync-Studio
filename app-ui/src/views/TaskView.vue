<template>
  <div class="page-section task-workbench">
    <div class="page-header task-workbench__header">
      <div>
        <h1>同步任务</h1>
        <p>管理同步任务、执行调度、查看日志与断点，形成完整的数据同步闭环。</p>
      </div>
      <el-space>
        <el-button round @click="goToWizard">创建向导</el-button>
        <el-button type="primary" round @click="openCreateDialog">新建同步任务</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--five task-workbench__overview">
      <div v-for="item in overviewCards" :key="item.label" class="page-overview__item task-workbench__overview-item">
        <div class="page-overview__label">{{ item.label }}</div>
        <div class="page-overview__value task-workbench__overview-value">{{ item.value }}</div>
        <div class="page-overview__hint">{{ item.hint }}</div>
      </div>
    </div>

    <div class="dashboard-panels task-workbench__panels">
      <div class="panel-card glass-panel task-workbench__panel task-workbench__panel--left">
        <div class="section-title section-title--compact task-workbench__section-head">
          <h2>任务工作区</h2>
          <el-tag type="info" effect="dark">{{ taskCountLabel }}</el-tag>
        </div>

        <div class="task-workbench__status-row">
          <div class="status-item task-workbench__status-item">
            <span class="status-item__label">当前任务</span>
            <span class="status-item__value">{{ selectedTaskName || '未选择' }}</span>
          </div>
          <div class="status-item task-workbench__status-item">
            <span class="status-item__label">调度概览</span>
            <span class="status-item__value">{{ scheduleOverviewText }}</span>
          </div>
        </div>

        <div v-if="!loading && !tasks.length" class="task-workbench__empty-shell">
          <StateEmpty
            class="task-workbench__empty"
            title="创建第一个同步任务"
            description="连接源库和目标库，配置表映射后即可开始同步。"
            hint="1. 选择源数据源 · 2. 选择目标数据源 · 3. 扫描表结构 · 4. 配置字段映射 · 5. 执行同步并查看日志"
            footer="同步任务、执行日志、断点恢复会在这里形成完整工作闭环。"
            button-text="新建同步任务"
            :wide="true"
            @action="openCreateDialog"
          />
          <div class="task-workbench__empty-secondary">
            <el-button plain round @click="goToWizard">使用创建向导</el-button>
          </div>
        </div>

        <div v-else class="table-shell task-workbench__table-shell">
          <el-table :data="tasks" border stripe v-loading="loading" @row-click="selectTask">
            <el-table-column prop="taskName" label="任务名" min-width="170" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.taskStatus)">{{ statusLabel(row.taskStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="同步模式" width="120">
              <template #default="{ row }">
                <el-tag>{{ syncModeLabel(row.syncMode) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="调度" width="150">
              <template #default="{ row }">
                {{ scheduleSummary(row) }}
              </template>
            </el-table-column>
            <el-table-column label="进度" min-width="220">
              <template #default="{ row }">
                <div class="task-progress">
                  <div class="task-progress__line">
                    <span>{{ row.syncedRowCount || 0 }} / {{ row.totalRowCount || 0 }}</span>
                    <span>{{ progressPercent(row) }}%</span>
                  </div>
                  <el-progress :percentage="progressPercent(row)" :stroke-width="10" :show-text="false" />
                  <div class="task-progress__hint">{{ row.progressMessage || '等待执行' }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="下次执行" width="180">
              <template #default="{ row }">
                {{ formatTime(row.scheduleNextRunAt) }}
              </template>
            </el-table-column>
            <el-table-column label="最近结果" width="120">
              <template #default="{ row }">
                <el-tag :type="scheduleResultTagType(row.scheduleLastResult)">{{ row.scheduleLastResult || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-space wrap>
                  <el-button size="small" type="success" @click.stop="startSelectedTask(row)">开始</el-button>
                  <el-button size="small" type="primary" plain @click.stop="runBatchSelectedTask(row)">批量</el-button>
                  <el-button size="small" @click.stop="openExecutionHistory(row)">历史</el-button>
                  <el-button size="small" @click.stop="openTaskEditor(row, false)">编辑</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
          <StateEmpty
            v-if="!loading && !tasks.length"
            title="创建第一个同步任务"
            description="连接源库和目标库，配置表映射后即可开始同步。"
            hint="1. 选择源数据源 · 2. 选择目标数据源 · 3. 扫描表结构 · 4. 配置字段映射 · 5. 执行同步并查看日志"
            footer="同步任务、执行日志、断点恢复会在这里形成完整工作闭环。"
            button-text="新建同步任务"
            :wide="true"
            @action="openCreateDialog"
          />
        </div>
      </div>

      <div class="panel-card glass-panel task-workbench__panel task-workbench__panel--right">
        <div class="section-title section-title--compact task-workbench__section-head">
          <h2>任务详情工作区</h2>
          <el-space>
            <el-tag :type="selectedTaskRow ? 'success' : 'info'" effect="dark">
              {{ selectedTaskRow ? '已选择任务' : '未选择任务' }}
            </el-tag>
            <el-button size="small" @click="openExecutionHistory(selectedTaskRow)" :disabled="!selectedTaskRow">执行历史</el-button>
            <el-button size="small" @click="goToTaskLogs" :disabled="!selectedTaskRow">日志页</el-button>
          </el-space>
        </div>

        <div v-if="!selectedTaskRow" class="task-workbench__detail-empty">
          <div class="status-item task-workbench__detail-empty-card">
            <span class="status-item__label">请选择左侧同步任务</span>
            <span class="status-item__value">选择任务后，可以查看实时日志、执行历史和同步断点。</span>
          </div>
          <div class="task-workbench__detail-grid">
            <div class="task-workbench__detail-card">
              <span class="task-workbench__detail-label">实时日志</span>
              <span class="task-workbench__detail-value">选择任务后显示</span>
            </div>
            <div class="task-workbench__detail-card">
              <span class="task-workbench__detail-label">执行历史</span>
              <span class="task-workbench__detail-value">选择任务后可查看</span>
            </div>
            <div class="task-workbench__detail-card">
              <span class="task-workbench__detail-label">当前断点</span>
              <span class="task-workbench__detail-value">断点值、更新时间、同步模式和最近状态</span>
            </div>
          </div>
        </div>

        <div v-else class="task-workbench__detail-content">
          <div class="task-workbench__detail-tabs">
            <el-segmented v-model="detailTab" :options="detailTabOptions" />
          </div>

          <div v-if="detailTab === 'logs'" class="task-workbench__detail-stack">
            <div class="status-item task-workbench__detail-summary">
              <span class="status-item__label">实时日志</span>
              <span class="status-item__value">
                {{ selectedTaskProgress.syncedRowCount }} / {{ selectedTaskProgress.totalRowCount }} · {{ selectedTaskProgress.durationText }}
              </span>
            </div>
            <div v-for="entry in taskLogs" :key="entry.id" class="capability-item task-workbench__log-item">
              <div class="capability-item__title">{{ formatTime(entry.createdAt) }}</div>
              <div class="capability-item__desc">
                <div>{{ entry.logLevel }} - {{ entry.logMessage }}</div>
                <div class="task-log-meta">
                  <span>run_id: {{ entry.runId || '-' }}</span>
                  <span>table: {{ entry.tableName || '-' }}</span>
                </div>
              </div>
            </div>
            <div v-if="!taskLogs.length" class="status-item">
              <span class="status-item__label">提示</span>
              <span class="status-item__value">暂无日志记录</span>
            </div>
          </div>

          <div v-else-if="detailTab === 'history'" class="task-workbench__detail-stack">
            <div class="status-item">
              <span class="status-item__label">执行历史</span>
              <span class="status-item__value">可通过顶部按钮进入完整执行历史页面。</span>
            </div>
            <div class="task-workbench__detail-grid task-workbench__detail-grid--history">
              <div class="task-workbench__detail-card">
                <span class="task-workbench__detail-label">最近状态</span>
                <span class="task-workbench__detail-value">{{ statusLabel(selectedTaskRow.taskStatus) }}</span>
              </div>
              <div class="task-workbench__detail-card">
                <span class="task-workbench__detail-label">最近结果</span>
                <span class="task-workbench__detail-value">{{ selectedTaskRow.scheduleLastResult || '—' }}</span>
              </div>
              <div class="task-workbench__detail-card">
                <span class="task-workbench__detail-label">最近消息</span>
                <span class="task-workbench__detail-value">{{ selectedTaskRow.scheduleLastMessage || '—' }}</span>
              </div>
            </div>
          </div>

          <div v-else class="task-workbench__detail-stack">
            <div class="status-item">
              <span class="status-item__label">日志页</span>
              <span class="status-item__value">选择任务后可打开完整日志页继续排查执行细节。</span>
            </div>
            <el-button type="primary" @click="goToTaskLogs" :disabled="!selectedTaskRow">打开日志页</el-button>
          </div>

          <div class="task-workbench__detail-stack">
            <div class="section-title section-title--compact task-workbench__subsection-head">
              <h2>当前断点</h2>
              <el-tag type="info">{{ selectedTaskRow.incrementalMode || '默认' }}</el-tag>
            </div>
            <div class="task-workbench__checkpoint-grid">
              <div class="task-workbench__checkpoint-item">
                <span class="task-workbench__checkpoint-label">断点值</span>
                <span class="task-workbench__checkpoint-value">{{ selectedTaskCheckpoint.value }}</span>
              </div>
              <div class="task-workbench__checkpoint-item">
                <span class="task-workbench__checkpoint-label">更新时间</span>
                <span class="task-workbench__checkpoint-value">{{ selectedTaskCheckpoint.updatedAt }}</span>
              </div>
              <div class="task-workbench__checkpoint-item">
                <span class="task-workbench__checkpoint-label">同步模式</span>
                <span class="task-workbench__checkpoint-value">{{ syncModeLabel(selectedTaskRow.syncMode) }}</span>
              </div>
              <div class="task-workbench__checkpoint-item">
                <span class="task-workbench__checkpoint-label">最近状态</span>
                <span class="task-workbench__checkpoint-value">{{ statusLabel(selectedTaskRow.taskStatus) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="980px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务名称" prop="taskName">
              <el-input v-model="form.taskName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="同步模式" prop="syncMode">
              <el-select v-model="form.syncMode" style="width: 100%;">
                <el-option label="全量同步" value="FULL" />
                <el-option label="增量同步" value="INCREMENTAL" />
                <el-option label="手动执行" value="MANUAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="源数据源" prop="sourceDatasourceId">
              <el-select v-model="form.sourceDatasourceId" placeholder="请选择源数据源" style="width: 100%;">
                <el-option v-for="item in datasourceOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标数据源" prop="targetDatasourceId">
              <el-select v-model="form.targetDatasourceId" placeholder="请选择目标数据源" style="width: 100%;">
                <el-option v-for="item in datasourceOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
            <el-col :span="12">
            <el-form-item label="源 schema">
              <CreatableSelect v-model="form.sourceSchemaName" :options="sourceSchemaOptions" placeholder="可选" @change="handleSourceSchemaChange" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标 schema">
              <CreatableSelect v-model="form.targetSchemaName" :options="targetSchemaOptions" placeholder="可选" @change="handleTargetSchemaChange" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="源表名" prop="sourceTableName">
              <CreatableSelect v-model="form.sourceTableName" :options="sourceTableOptions" placeholder="源表名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标表名" prop="targetTableName">
              <CreatableSelect v-model="form.targetTableName" :options="targetTableOptions" placeholder="目标表名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">增量配置</el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="增量模式">
              <el-select v-model="form.incrementalMode" style="width: 100%;">
                <el-option label="无" value="NONE" />
                <el-option label="更新时间" value="TIMESTAMP" />
                <el-option label="自增 ID" value="AUTO_INCREMENT_ID" />
                <el-option label="组合断点" value="COMPOSITE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="增量字段">
              <el-input v-model="form.incrementalColumnName" placeholder="例如 updated_at 或 id" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" v-if="form.incrementalMode === 'COMPOSITE'">
          <el-col :span="12">
            <el-form-item label="断点字段 2">
              <el-input v-model="form.incrementalTieBreakerColumnName" placeholder="例如 id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="组合表达式">
              <el-input v-model="form.incrementalCompositeColumnName" placeholder="例如 updated_at,id" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">调度配置</el-divider>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="启用调度">
              <el-switch v-model="form.scheduleEnabled" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调度类型">
              <el-select v-model="form.scheduleType" style="width: 100%;">
                <el-option label="手动" value="MANUAL" />
                <el-option label="Cron" value="CRON" />
                <el-option label="固定间隔" value="INTERVAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" v-if="form.scheduleType === 'CRON'">
          <el-col :span="24">
            <el-form-item label="Cron 表达式">
              <el-input v-model="form.scheduleCronExpression" placeholder="例如 0 9 * * *" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16" v-else-if="form.scheduleType === 'INTERVAL'">
          <el-col :span="24">
            <el-form-item label="间隔秒数">
              <el-input-number v-model="form.scheduleIntervalSeconds" :min="1" :step="60" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="上次执行">
              <el-input :model-value="formatTime(form.scheduleLastRunAt)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次执行">
              <el-input :model-value="formatTime(form.scheduleNextRunAt)" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="最近结果">
              <el-input :model-value="form.scheduleLastResult || '-'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最近消息">
              <el-input :model-value="form.scheduleLastMessage || '-'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">多表配置</el-divider>

        <div class="table-shell" style="margin-bottom: 12px;">
          <el-table :data="taskTables" border stripe>
            <el-table-column label="启用" width="80">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" />
              </template>
            </el-table-column>
            <el-table-column label="源 schema" min-width="140">
              <template #default="{ row }">
                <CreatableSelect v-model="row.sourceSchemaName" :options="sourceSchemasForRow(row)" placeholder="可选" />
              </template>
            </el-table-column>
            <el-table-column label="源表" min-width="150">
              <template #default="{ row }">
                <CreatableSelect v-model="row.sourceTableName" :options="sourceTablesForRow(row)" placeholder="源表名" />
              </template>
            </el-table-column>
            <el-table-column label="目标 schema" min-width="140">
              <template #default="{ row }">
                <CreatableSelect v-model="row.targetSchemaName" :options="targetSchemasForRow(row)" placeholder="可选" />
              </template>
            </el-table-column>
            <el-table-column label="目标表" min-width="150">
              <template #default="{ row }">
                <CreatableSelect v-model="row.targetTableName" :options="targetTablesForRow(row)" placeholder="目标表名" />
              </template>
            </el-table-column>
            <el-table-column label="同步模式" width="130">
              <template #default="{ row }">
                <el-select v-model="row.syncMode" style="width: 100%;">
                  <el-option label="继承任务" value="" />
                  <el-option label="全量" value="FULL" />
                  <el-option label="增量" value="INCREMENTAL" />
                  <el-option label="手动" value="MANUAL" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="增量模式" width="140">
              <template #default="{ row }">
                <el-select v-model="row.incrementalMode" style="width: 100%;">
                  <el-option label="继承任务" value="" />
                  <el-option label="无" value="NONE" />
                  <el-option label="更新时间" value="TIMESTAMP" />
                  <el-option label="自增 ID" value="AUTO_INCREMENT_ID" />
                  <el-option label="组合" value="COMPOSITE" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="增量字段" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.incrementalColumnName" placeholder="例如 updated_at" />
              </template>
            </el-table-column>
            <el-table-column label="断点字段 2" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.incrementalTieBreakerColumnName" placeholder="例如 id" />
              </template>
            </el-table-column>
            <el-table-column label="组合表达式" min-width="160">
              <template #default="{ row }">
                <el-input v-model="row.incrementalCompositeColumnName" placeholder="例如 updated_at,id" />
              </template>
            </el-table-column>
            <el-table-column label="批量" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.batchSize" :min="1" :step="100" style="width: 100%;" />
              </template>
            </el-table-column>
            <el-table-column label="顺序" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.tableOrder" :min="1" :step="1" style="width: 100%;" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ $index }">
                <el-button size="small" type="danger" @click="removeTaskTable($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-space>
          <el-button @click="addTaskTable">新增表</el-button>
          <el-button type="primary" plain @click="syncMainTableToBatch">复用主表</el-button>
        </el-space>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitTask" :loading="saving">保存</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteTask, deleteTaskTable, listDatasources, listTaskLogs, listTaskTables, listTasks, pauseTask, resumeTask, saveTask, saveTaskTable, startTask, stopTask, scanMetadata } from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const savingTaskTables = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const tasks = ref([])
const datasourceOptions = ref([])
const datasourceSchemas = ref({})
const sourceSchemaOptions = ref([])
const targetSchemaOptions = ref([])
const sourceTableOptions = ref([])
const targetTableOptions = ref([])
const selectedTaskId = ref(null)
const selectedTaskName = ref('')
const taskLogs = ref([])
const taskTables = ref([])
const editingId = ref(null)
const editingTaskTablesOnly = ref(false)
const refreshTimer = ref(null)
const detailTab = ref('logs')
const detailTabOptions = [
  { label: '实时日志', value: 'logs' },
  { label: '执行历史', value: 'history' },
  { label: '日志页', value: 'logsPage' }
]

const form = reactive(createEmptyForm())

const rules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  sourceDatasourceId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  targetDatasourceId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  sourceTableName: [{ required: true, message: '请输入源表名', trigger: 'blur' }],
  targetTableName: [{ required: true, message: '请输入目标表名', trigger: 'blur' }]
}

const dialogTitle = computed(function () {
  return editingId.value ? '编辑同步任务' : '新建同步任务'
})

const selectedTaskRow = computed(function () {
  return tasks.value.find(function (item) {
    return item.id === selectedTaskId.value
  }) || null
})

const taskCountLabel = computed(function () {
  return tasks.value.length + ' 个任务'
})

const overviewCards = computed(function () {
  const total = tasks.value.length
  const running = tasks.value.filter(function (item) {
    return item.taskStatus === 'RUNNING'
  }).length
  const waiting = tasks.value.filter(function (item) {
    return item.taskStatus === 'PENDING' || item.taskStatus === 'WAITING'
  }).length
  const success = tasks.value.filter(function (item) {
    return item.taskStatus === 'SUCCESS'
  }).length
  const failed = tasks.value.filter(function (item) {
    return item.taskStatus === 'FAILED'
  }).length
  return [
    { label: '总任务', value: total, hint: '当前同步任务总数' },
    { label: '运行中', value: running, hint: '正在执行中的任务' },
    { label: '等待中', value: waiting, hint: '待启动或待调度任务' },
    { label: '成功', value: success, hint: '最近执行成功的任务' },
    { label: '失败', value: failed, hint: '最近执行失败的任务' }
  ]
})

onMounted(function () {
  loadPageData()
  startPolling()
})

onBeforeUnmount(function () {
  stopPolling()
})

async function loadPageData() {
  loading.value = true
  try {
    const [taskList, datasources] = await Promise.all([listTasks(), listDatasources()])
    tasks.value = taskList
    datasourceOptions.value = datasources
    await loadDatasourceSchemas()
    if (selectedTaskId.value !== null) {
      const selectedRow = tasks.value.find(function (row) {
        return row.id === selectedTaskId.value
      })
      if (selectedRow) {
        selectedTaskName.value = selectedRow.taskName
        taskLogs.value = await listTaskLogs(selectedRow.id)
      }
    }
    if (tasks.value.length > 0 && selectedTaskId.value === null) {
      selectTask(tasks.value[0])
    }
  } catch (error) {
    ElMessage.error(error.message || '加载任务失败')
  } finally {
    loading.value = false
  }
}

function startPolling() {
  stopPolling()
  refreshTimer.value = window.setInterval(function () {
    loadTasksOnly()
  }, 3000)
}

function stopPolling() {
  if (refreshTimer.value) {
    window.clearInterval(refreshTimer.value)
    refreshTimer.value = null
  }
}

async function loadTasksOnly() {
  try {
    tasks.value = await listTasks()
    if (selectedTaskId.value !== null) {
      const selectedRow = tasks.value.find(function (row) {
        return row.id === selectedTaskId.value
      })
      if (selectedRow) {
        selectedTaskName.value = selectedRow.taskName
        taskLogs.value = await listTaskLogs(selectedRow.id)
      }
    }
  } catch (error) {
    ElMessage.error(error.message || '刷新任务状态失败')
  }
}

async function loadDatasourceSchemas() {
  const entries = datasourceOptions.value || []
  const map = {}
  for (let i = 0; i < entries.length; i += 1) {
    const datasource = entries[i]
    if (!datasource || !datasource.id) {
      continue
    }
    const cached = datasourceSchemas.value[datasource.id]
    if (cached) {
      map[datasource.id] = cached
      continue
    }
    try {
      map[datasource.id] = await scanMetadata(datasource.id)
    } catch (error) {
      map[datasource.id] = []
    }
  }
  datasourceSchemas.value = map
}

function goToWizard() {
  router.push('/task-wizard')
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  syncFormSchemaOptions()
  dialogVisible.value = true
}

async function openTaskEditor(row, tablesOnly) {
  if (!row || !row.id) {
    return
  }
  editingId.value = row.id
  editingTaskTablesOnly.value = !!tablesOnly
  Object.assign(form, row)
  await loadTaskTables(row.id)
  syncFormSchemaOptions()
  dialogVisible.value = true
}

function editTask(row) {
  return openTaskEditor(row, false)
}

async function submitTask() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  saving.value = true
  try {
    await saveTask(normalizeTask())
    if (editingId.value) {
      await saveTaskTables(editingId.value)
    }
    ElMessage.success('任务已保存')
    dialogVisible.value = false
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '保存任务失败')
  } finally {
    saving.value = false
  }
}

async function startSelectedTask(row) {
  try {
    const result = await startTask(row.id)
    ElMessage.success(result.progressMessage || '任务已开始')
    selectTask(row)
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '任务开始失败')
  }
}

async function pauseSelectedTask(row) {
  try {
    await pauseTask(row.id)
    ElMessage.success('已请求暂停')
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '暂停失败')
  }
}

async function resumeSelectedTask(row) {
  try {
    await resumeTask(row.id)
    ElMessage.success('已请求恢复')
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '恢复失败')
  }
}

async function stopSelectedTask(row) {
  try {
    await stopTask(row.id)
    ElMessage.success('已请求停止')
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '停止失败')
  }
}

function openMappings(row) {
  router.push({ path: '/mapping', query: { taskId: String(row.id) } })
}

function openExecutionHistory(row) {
  const task = row || selectedTaskRow.value
  if (!task || !task.id) {
    return
  }
  router.push({ path: '/execution-history', query: { taskId: String(task.id) } })
}

function goToTaskLogs() {
  if (!selectedTaskRow.value) {
    return
  }
  router.push({
    path: '/logs',
    query: {
      taskId: String(selectedTaskRow.value.id)
    }
  })
}

async function runBatchSelectedTask(row) {
  try {
    const response = window.prompt('请输入并发数，默认 3：', '3')
    const maxConcurrency = Math.max(1, Number(response || 3) || 3)
    await runBatchTask(row.id, {
      maxConcurrency: maxConcurrency,
      tables: normalizeTaskTables(taskTables.value).map(function (item) {
        return {
          sourceSchemaName: item.sourceSchemaName,
          sourceTableName: item.sourceTableName,
          targetSchemaName: item.targetSchemaName,
          targetTableName: item.targetTableName,
          syncMode: item.syncMode,
          incrementalMode: item.incrementalMode,
          incrementalColumnName: item.incrementalColumnName,
          incrementalTieBreakerColumnName: item.incrementalTieBreakerColumnName,
          incrementalCompositeColumnName: item.incrementalCompositeColumnName,
          batchSize: item.batchSize
        }
      })
    })
    ElMessage.success('批量执行已提交')
    await loadPageData()
  } catch (error) {
    if (String(error && error.message || '').indexOf('prompt') >= 0) {
      return
    }
    ElMessage.error(error.message || '批量执行失败')
  }
}

function previewTable(row, side) {
  const datasourceId = side === 'target' ? row.targetDatasourceId : row.sourceDatasourceId
  const schemaName = side === 'target' ? row.targetSchemaName : row.sourceSchemaName
  const tableName = side === 'target' ? row.targetTableName : row.sourceTableName
  router.push({
    path: '/preview',
    query: {
      datasourceId: datasourceId ? String(datasourceId) : '',
      schemaName: schemaName || '',
      tableName: tableName || ''
    }
  })
}

async function removeTask(row) {
  await ElMessageBox.confirm('确定删除这个任务吗？', '提示', {
    type: 'warning'
  })
  try {
    await deleteTask(row.id)
    ElMessage.success('任务已删除')
    if (selectedTaskId.value === row.id) {
      selectedTaskId.value = null
      selectedTaskName.value = ''
      taskLogs.value = []
    }
    await loadPageData()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

async function selectTask(row) {
  if (!row || !row.id) {
    return
  }
  selectedTaskId.value = row.id
  selectedTaskName.value = row.taskName
  detailTab.value = 'logs'
  try {
    taskLogs.value = await listTaskLogs(row.id)
  } catch (error) {
    ElMessage.error(error.message || '加载日志失败')
  }
}

function normalizeTask() {
  return {
    id: editingId.value,
    taskName: form.taskName,
    sourceDatasourceId: Number(form.sourceDatasourceId),
    targetDatasourceId: Number(form.targetDatasourceId),
    syncMode: form.syncMode,
    taskStatus: 'PENDING',
    sourceSchemaName: form.sourceSchemaName,
    sourceTableName: form.sourceTableName,
    targetSchemaName: form.targetSchemaName,
    targetTableName: form.targetTableName,
    incrementalMode: form.incrementalMode,
    incrementalColumnName: form.incrementalColumnName,
    incrementalTieBreakerColumnName: form.incrementalTieBreakerColumnName,
    incrementalCompositeColumnName: form.incrementalCompositeColumnName,
    scheduleEnabled: form.scheduleEnabled,
    scheduleType: form.scheduleType,
    scheduleCronExpression: form.scheduleCronExpression,
    scheduleIntervalSeconds: form.scheduleIntervalSeconds,
    scheduleLastRunAt: form.scheduleLastRunAt,
    scheduleNextRunAt: form.scheduleNextRunAt,
    scheduleLastResult: form.scheduleLastResult,
    scheduleLastMessage: form.scheduleLastMessage
  }
}

function createEmptyForm() {
  return {
    taskName: '',
    sourceDatasourceId: null,
    targetDatasourceId: null,
    syncMode: 'FULL',
    taskStatus: 'PENDING',
    sourceSchemaName: '',
    sourceTableName: '',
    targetSchemaName: '',
    targetTableName: '',
    incrementalMode: 'NONE',
    incrementalColumnName: '',
    incrementalTieBreakerColumnName: '',
    incrementalCompositeColumnName: '',
    scheduleEnabled: false,
    scheduleType: 'MANUAL',
    scheduleCronExpression: '',
    scheduleIntervalSeconds: 300,
    scheduleLastRunAt: null,
    scheduleNextRunAt: null,
    scheduleLastResult: '',
    scheduleLastMessage: ''
  }
}

function resetForm() {
  Object.assign(form, createEmptyForm())
  syncFormSchemaOptions()
}

function addTaskTable() {
  taskTables.value.push({
    id: null,
    taskId: null,
    sourceSchemaName: '',
    sourceTableName: '',
    targetSchemaName: '',
    targetTableName: '',
    tableOrder: taskTables.value.length + 1,
    enabled: true,
    syncMode: '',
    incrementalMode: '',
    incrementalColumnName: '',
    incrementalTieBreakerColumnName: '',
    incrementalCompositeColumnName: '',
    batchSize: 500
  })
}

function syncFormSchemaOptions() {
  const sourceSchemas = currentSchemaList(form.sourceDatasourceId)
  const targetSchemas = currentSchemaList(form.targetDatasourceId)
  sourceSchemaOptions.value = sourceSchemas.map(function (item) {
    return item.schemaName
  })
  targetSchemaOptions.value = targetSchemas.map(function (item) {
    return item.schemaName
  })
  sourceTableOptions.value = currentTableList(form.sourceDatasourceId, form.sourceSchemaName).map(function (item) {
    return item.tableName
  })
  targetTableOptions.value = currentTableList(form.targetDatasourceId, form.targetSchemaName).map(function (item) {
    return item.tableName
  })
}

function handleSourceSchemaChange() {
  sourceTableOptions.value = currentTableList(form.sourceDatasourceId, form.sourceSchemaName).map(function (item) {
    return item.tableName
  })
}

function handleTargetSchemaChange() {
  targetTableOptions.value = currentTableList(form.targetDatasourceId, form.targetSchemaName).map(function (item) {
    return item.tableName
  })
}

function currentSchemaList(datasourceId) {
  const schemas = datasourceSchemas.value[datasourceId] || []
  return schemas || []
}

function currentTableList(datasourceId, schemaName) {
  const schemas = currentSchemaList(datasourceId)
  if (!schemaName) {
    return []
  }
  const schema = schemas.find(function (item) {
    return item.schemaName && item.schemaName.toLowerCase() === String(schemaName).toLowerCase()
  })
  return schema ? (schema.tables || []) : []
}

function sourceSchemasForRow(row) {
  return datasourceSchemas.value[form.sourceDatasourceId] ? (datasourceSchemas.value[form.sourceDatasourceId] || []).map(function (item) {
    return item.schemaName
  }) : sourceSchemaOptions.value
}

function targetSchemasForRow(row) {
  return datasourceSchemas.value[form.targetDatasourceId] ? (datasourceSchemas.value[form.targetDatasourceId] || []).map(function (item) {
    return item.schemaName
  }) : targetSchemaOptions.value
}

function sourceTablesForRow(row) {
  return currentTableList(form.sourceDatasourceId, row.sourceSchemaName).map(function (item) {
    return item.tableName
  })
}

function targetTablesForRow(row) {
  return currentTableList(form.targetDatasourceId, row.targetSchemaName).map(function (item) {
    return item.tableName
  })
}

function removeTaskTable(index) {
  if (taskTables.value.length <= 1) {
    return
  }
  taskTables.value.splice(index, 1)
}

function syncMainTableToBatch() {
  taskTables.value.splice(0, taskTables.value.length, {
    id: null,
    taskId: null,
    sourceSchemaName: form.sourceSchemaName,
    sourceTableName: form.sourceTableName,
    targetSchemaName: form.targetSchemaName,
    targetTableName: form.targetTableName,
    syncMode: '',
    incrementalMode: '',
    incrementalColumnName: '',
    incrementalTieBreakerColumnName: '',
    incrementalCompositeColumnName: '',
    batchSize: 500,
    tableOrder: 1,
    enabled: true
  })
}

async function loadTaskTables(taskId) {
  try {
    const rows = await listTaskTables(taskId)
    taskTables.value = normalizeTaskTables(rows)
    if (taskTables.value.length === 0) {
      taskTables.value = [createEmptyTaskTable(1)]
    }
  } catch (error) {
    ElMessage.error(error.message || '加载多表配置失败')
    taskTables.value = [createEmptyTaskTable(1)]
  }
}

async function saveTaskTables(taskId) {
  if (!taskId) {
    return
  }
  savingTaskTables.value = true
  try {
    const existingRows = await listTaskTables(taskId)
    const normalizedRows = normalizeTaskTables(taskTables.value).map(function (item, index) {
      return {
        id: item.id,
        taskId: taskId,
        sourceSchemaName: item.sourceSchemaName,
        sourceTableName: item.sourceTableName,
        targetSchemaName: item.targetSchemaName,
        targetTableName: item.targetTableName,
        syncMode: item.syncMode,
        incrementalMode: item.incrementalMode,
        incrementalColumnName: item.incrementalColumnName,
        incrementalTieBreakerColumnName: item.incrementalTieBreakerColumnName,
        incrementalCompositeColumnName: item.incrementalCompositeColumnName,
        batchSize: item.batchSize || 500,
        tableOrder: item.tableOrder || index + 1,
        enabled: item.enabled !== false
      }
    })

    for (let i = 0; i < normalizedRows.length; i += 1) {
      await saveTaskTable(taskId, normalizedRows[i])
    }

    const savedIds = {}
    normalizedRows.forEach(function (item) {
      if (item.id) {
        savedIds[item.id] = true
      }
    })
    for (let j = 0; j < existingRows.length; j += 1) {
      const existingRow = existingRows[j]
      if (existingRow && existingRow.id && !savedIds[existingRow.id]) {
        await deleteTaskTable(taskId, existingRow.id)
      }
    }
  } finally {
    savingTaskTables.value = false
  }
}

function createEmptyTaskTable(order) {
  return {
    id: null,
    taskId: null,
    sourceSchemaName: '',
    sourceTableName: '',
    targetSchemaName: '',
    targetTableName: '',
    syncMode: '',
    incrementalMode: '',
    incrementalColumnName: '',
    incrementalTieBreakerColumnName: '',
    incrementalCompositeColumnName: '',
    batchSize: 500,
    tableOrder: order || 1,
    enabled: true
  }
}

function normalizeTaskTables(rows) {
  if (!rows || !rows.length) {
    return []
  }
  return rows.slice().sort(function (a, b) {
    return Number(a.tableOrder || 0) - Number(b.tableOrder || 0)
  }).map(function (item, index) {
    return {
      id: item.id || null,
      taskId: item.taskId || null,
      sourceSchemaName: item.sourceSchemaName || '',
      sourceTableName: item.sourceTableName || '',
      targetSchemaName: item.targetSchemaName || '',
      targetTableName: item.targetTableName || '',
      syncMode: item.syncMode || '',
      incrementalMode: item.incrementalMode || '',
      incrementalColumnName: item.incrementalColumnName || '',
      incrementalTieBreakerColumnName: item.incrementalTieBreakerColumnName || '',
      incrementalCompositeColumnName: item.incrementalCompositeColumnName || '',
      batchSize: item.batchSize || 500,
      tableOrder: item.tableOrder || index + 1,
      enabled: item.enabled !== false
    }
  })
}

function syncModeLabel(value) {
  if (value === 'INCREMENTAL') {
    return '增量同步'
  }
  if (value === 'MANUAL') {
    return '手动执行'
  }
  return '全量同步'
}

function statusLabel(value) {
  if (value === 'RUNNING') {
    return '执行中'
  }
  if (value === 'SUCCESS') {
    return '成功'
  }
  if (value === 'PARTIAL_SUCCESS') {
    return '部分成功'
  }
  if (value === 'FAILED') {
    return '失败'
  }
  if (value === 'PAUSED') {
    return '暂停'
  }
  if (value === 'STOPPED') {
    return '已停止'
  }
  return '待执行'
}

function statusTagType(value) {
  if (value === 'RUNNING') {
    return 'warning'
  }
  if (value === 'SUCCESS') {
    return 'success'
  }
  if (value === 'PARTIAL_SUCCESS') {
    return 'warning'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  if (value === 'PAUSED' || value === 'STOPPED') {
    return 'warning'
  }
  return 'info'
}

function incrementalSummary(row) {
  if (!row) {
    return '-'
  }
  const parts = []
  if (row.syncMode) {
    parts.push(syncModeLabel(row.syncMode))
  }
  if (row.incrementalMode && row.incrementalMode !== 'NONE') {
    parts.push(row.incrementalMode)
  }
  if (row.incrementalColumnName) {
    parts.push(row.incrementalColumnName)
  }
  return parts.length ? parts.join(' · ') : '继承任务配置'
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function progressPercent(row) {
  const total = Number(row.totalRowCount || 0)
  const synced = Number(row.syncedRowCount || 0)
  if (!total) {
    return 0
  }
  return Math.min(100, Math.round((synced / total) * 100))
}

const selectedTaskProgress = computed(function () {
  const row = selectedTaskRow.value
  if (!row) {
    return {
      syncedRowCount: 0,
      totalRowCount: 0,
      durationText: '-'
    }
  }
  return {
    syncedRowCount: row.syncedRowCount || 0,
    totalRowCount: row.totalRowCount || 0,
    durationText: formatDuration(row.durationMillis)
  }
})

const selectedTaskCheckpoint = computed(function () {
  const row = selectedTaskRow.value
  if (!row) {
    return {
      value: '—',
      updatedAt: '—'
    }
  }
  return {
    value: row.incrementalCheckpointValue || '—',
    updatedAt: formatTime(row.incrementalCheckpointUpdatedAt)
  }
})

const scheduleOverviewText = computed(function () {
  const selectedRow = selectedTaskRow.value
  if (!selectedRow) {
    return '暂无任务'
  }
  if (!selectedRow.scheduleEnabled) {
    return '未启动'
  }
  return scheduleSummary(selectedRow) + ' · 下次 ' + formatTime(selectedRow.scheduleNextRunAt)
})

function formatDuration(value) {
  if (!value) {
    return '-'
  }
  const seconds = Math.max(0, Math.round(Number(value) / 1000))
  if (seconds < 60) {
    return seconds + ' 秒'
  }
  const minutes = Math.floor(seconds / 60)
  const remain = seconds % 60
  return minutes + ' 分 ' + remain + ' 秒'
}

function scheduleSummary(row) {
  if (!row || !row.scheduleEnabled) {
    return '未启动'
  }
  if (row.scheduleType === 'CRON') {
    return 'Cron'
  }
  if (row.scheduleType === 'INTERVAL') {
    return '间隔 ' + (row.scheduleIntervalSeconds || 0) + ' 秒'
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

<style scoped>
.task-workbench {
  gap: 18px;
}

.task-workbench__header p {
  max-width: 760px;
}

.task-workbench__overview {
  margin-top: 0;
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.task-workbench__overview-item {
  min-height: 108px;
  padding: 14px 16px;
}

.task-workbench__overview-value {
  font-size: 22px;
}

.task-workbench__panels {
  align-items: start;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.05fr);
}

.task-workbench__panel {
  display: grid;
  gap: 14px;
  min-height: 100%;
  align-content: start;
}

.task-workbench__status-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.task-workbench__status-item {
  min-height: 76px;
}

.task-workbench__empty-shell {
  display: grid;
  gap: 12px;
}

.task-workbench__empty {
  margin: 0;
}

.task-workbench__empty-secondary {
  display: flex;
  justify-content: center;
}

.task-workbench__table-shell {
  margin-top: 0;
}

.task-workbench__detail-empty,
.task-workbench__detail-content,
.task-workbench__detail-stack {
  display: grid;
  gap: 12px;
}

.task-workbench__detail-empty-card {
  min-height: 92px;
}

.task-workbench__detail-grid,
.task-workbench__checkpoint-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.task-workbench__checkpoint-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.task-workbench__detail-card,
.task-workbench__checkpoint-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.task-workbench__detail-label,
.task-workbench__checkpoint-label {
  display: block;
  color: var(--text-sub);
  font-size: 12px;
  margin-bottom: 8px;
}

.task-workbench__detail-value,
.task-workbench__checkpoint-value {
  display: block;
  font-weight: 700;
  line-height: 1.6;
}

.task-workbench__detail-tabs {
  display: flex;
  justify-content: flex-start;
}

.task-workbench__log-item {
  padding: 12px 14px;
}

.task-workbench__detail-summary {
  margin-bottom: 2px;
}

@media (max-width: 1280px) {
  .task-workbench__overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-workbench__panels {
    grid-template-columns: minmax(0, 1fr);
  }

  .task-workbench__detail-grid,
  .task-workbench__checkpoint-grid,
  .task-workbench__status-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .task-workbench__overview,
  .task-workbench__detail-grid,
  .task-workbench__checkpoint-grid,
  .task-workbench__status-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
