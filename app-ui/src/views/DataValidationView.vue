<template>
  <div class="page-section data-validation-workbench">
    <div class="page-header data-validation-workbench__header">
      <div class="data-validation-workbench__titleblock">
        <h1>数据校验</h1>
        <p>按同步任务执行行数、主键、抽样和 Hash 校验，并基于差异生成安全修复方案。</p>
      </div>
      <div class="data-validation-workbench__toolbar">
        <div class="data-validation-workbench__toolbar-actions">
          <el-button round @click="loadPageData">刷新</el-button>
          <el-tooltip :disabled="canStartValidation" :content="startDisabledReason" placement="bottom">
            <span>
              <el-button
                type="primary"
                round
                :loading="running"
                :disabled="!canStartValidation"
                @click="runValidation"
              >
                开始校验
              </el-button>
            </span>
          </el-tooltip>
        </div>
        <div class="data-validation-workbench__toolbar-note">
          {{ toolbarHint }}
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel data-validation-workbench__flow-strip">
      <div
        v-for="(step, index) in validationFlow"
        :key="step"
        class="data-validation-workbench__flow-step"
        :class="{
          'is-active': validationFlowActiveIndex === index,
          'is-complete': validationFlowActiveIndex > index
        }"
      >
        <span class="data-validation-workbench__flow-index">{{ index + 1 }}</span>
        <span class="data-validation-workbench__flow-label">{{ step }}</span>
      </div>
    </div>

    <div class="page-overview page-overview--five data-validation-workbench__overview">
      <div class="page-overview__item">
        <div class="page-overview__label">校验状态</div>
        <div class="page-overview__value">{{ validationStatusLabel }}</div>
        <div class="page-overview__hint">{{ validationStatusHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">源 / 目标行数</div>
        <div class="page-overview__value">{{ currentRun.sourceRowCount || 0 }} / {{ currentRun.targetRowCount || 0 }}</div>
        <div class="page-overview__hint">{{ rowsHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">差异数量</div>
        <div class="page-overview__value">{{ differenceCount }}</div>
        <div class="page-overview__hint">{{ differenceSummaryText }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">耗时</div>
        <div class="page-overview__value">{{ formatDuration(currentRun.elapsedMillis) }}</div>
        <div class="page-overview__hint">最近校验运行时间</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">修复状态</div>
        <div class="page-overview__value">{{ repairStateLabel }}</div>
        <div class="page-overview__hint">{{ repairStateHint }}</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact data-validation-workbench__top-grid">
      <div class="panel-card glass-panel data-validation-workbench__config-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>校验配置</h2>
            <el-tag type="info" effect="dark">数据校验工作台</el-tag>
          </div>
          <el-tag :type="configReadinessTagType" effect="light">{{ configReadinessLabel }}</el-tag>
        </div>
        <div class="data-validation-workbench__panel-hint">
          <span>{{ validationConfigHint }}</span>
        </div>
        <el-form :model="form" label-width="120px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="同步任务">
                <el-select v-model="form.taskId" style="width: 100%;" @change="handleTaskChange">
                  <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="校验方式">
                <el-select v-model="form.validationMode" style="width: 100%;">
                  <el-option label="行数校验" value="ROW_COUNT" />
                  <el-option label="主键存在性" value="PRIMARY_KEY_EXISTS" />
                  <el-option label="抽样校验" value="SAMPLE" />
                  <el-option label="Hash 校验" value="HASH" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="抽样方式">
                <el-select v-model="form.sampleMode" style="width: 100%;">
                  <el-option label="随机抽样" value="RANDOM" />
                  <el-option label="主键范围" value="PRIMARY_KEY_RANGE" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="抽样数量">
                <el-input-number v-model="form.sampleCount" :min="1" :max="1000" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="Where 条件">
                <el-input v-model="form.whereClause" placeholder="可选" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="增量条件">
                <el-input v-model="form.incrementalCondition" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="Hash 算法">
                <el-select v-model="form.hashAlgorithm" style="width: 100%;">
                  <el-option label="MD5" value="MD5" />
                  <el-option label="CRC32" value="CRC32" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Hash 字段">
                <el-input v-model="form.hashColumnsText" placeholder="逗号分隔字段名" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <div class="panel-card glass-panel data-validation-workbench__summary-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>最近校验</h2>
            <el-tag type="success" effect="dark">{{ validationRuns.length }} 条</el-tag>
          </div>
          <el-tag :type="selectedValidationRunId ? 'success' : 'info'" effect="light">
            {{ selectedRunLabel }}
          </el-tag>
        </div>
        <div class="data-validation-workbench__panel-hint">
          <span>{{ recentValidationHint }}</span>
        </div>
        <div class="table-shell data-validation-workbench__table-shell">
          <el-table :data="validationRuns" border stripe @row-click="selectValidationRun">
            <el-table-column prop="validationMethod" label="方式" width="150" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="源 / 目标" width="160">
              <template #default="{ row }">
                {{ row.sourceRowCount || 0 }} / {{ row.targetRowCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="缺失 / 不一致" width="160">
              <template #default="{ row }">
                {{ row.missingCount || 0 }} / {{ row.inconsistentCount || 0 }}
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="120">
              <template #default="{ row }">
                {{ formatDuration(row.elapsedMillis) }}
              </template>
            </el-table-column>
            <el-table-column label="时间" width="200">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
          <StateEmpty
            v-if="!validationRuns.length"
            title="还没有校验记录"
            description="先选择任务并执行一次校验。"
            hint="这里会记录校验状态、源/目标行数和差异统计。"
            button-text="开始校验"
            @action="runValidation"
          />
        </div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact data-validation-workbench__bottom-grid">
      <div class="panel-card glass-panel data-validation-workbench__difference-card">
          <div class="section-title">
            <div class="section-title__left">
              <h2>差异明细</h2>
              <el-tag type="warning" effect="dark">{{ validationDifferences.length }} 项</el-tag>
            </div>
          <el-space>
            <el-tag :type="differenceCount > 0 ? 'warning' : 'success'" effect="light">{{ differenceOverviewLabel }}</el-tag>
            <el-tooltip :disabled="canLoadRepairPreview" :content="loadRepairDisabledReason" placement="top">
              <span>
                <el-button
                  :disabled="!canLoadRepairPreview"
                  @click="loadRepairPreview"
                >
                  生成修复方案
                </el-button>
              </span>
            </el-tooltip>
          </el-space>
          </div>
        <div class="data-validation-workbench__panel-hint">
          <span>{{ differenceHint }}</span>
        </div>
        <div class="table-shell data-validation-workbench__table-shell">
          <el-table :data="validationDifferences" border stripe>
            <el-table-column prop="differenceType" label="差异类型" width="160">
              <template #default="{ row }">
                <el-tag :type="differenceTypeTagType(row.differenceType)" effect="light">{{ differenceTypeLabel(row.differenceType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="primaryKeyJson" label="主键" min-width="200" show-overflow-tooltip />
            <el-table-column prop="sourceRowJson" label="源行" min-width="240" show-overflow-tooltip />
            <el-table-column prop="targetRowJson" label="目标行" min-width="240" show-overflow-tooltip />
            <el-table-column prop="suggestedRepairType" label="建议修复" width="160" />
          </el-table>
          <div v-if="!validationDifferences.length" class="data-validation-workbench__inline-note">
            完成校验后，如存在缺失、不一致或异常字段，将在这里显示。
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel data-validation-workbench__repair-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>修复历史</h2>
            <el-tag type="info" effect="dark">{{ repairRuns.length }} 条</el-tag>
          </div>
          <el-tag :type="repairRuns.length ? 'success' : 'info'" effect="light">{{ repairHistoryLabel }}</el-tag>
        </div>
        <div class="data-validation-workbench__panel-hint">
          <span>{{ repairHistoryHint }}</span>
        </div>
        <div class="table-shell data-validation-workbench__table-shell">
          <el-table :data="repairRuns" border stripe>
            <el-table-column prop="repairType" label="类型" width="160" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="repairTagType(row.status)" effect="light">{{ repairStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="repairCount" label="数量" width="100" />
            <el-table-column prop="successCount" label="成功" width="100" />
            <el-table-column prop="failedCount" label="失败" width="100" />
            <el-table-column label="时间" width="200">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!repairRuns.length" class="data-validation-workbench__inline-note">
            生成并执行修复方案后，这里会记录修复批次。
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="repairDialogVisible" title="修复方案" width="960px">
      <div class="data-validation-workbench__repair-banner">
        <div class="data-validation-workbench__repair-banner-main">
          <div class="data-validation-workbench__repair-banner-title">安全修复方案</div>
          <div class="data-validation-workbench__repair-banner-sub">{{ repairPreviewHint }}</div>
        </div>
        <el-tag type="warning" effect="light">{{ repairBannerTagLabel }}</el-tag>
      </div>
      <div class="status-stack data-validation-workbench__repair-meta">
        <div class="status-item">
          <span class="status-item__label">当前修复</span>
          <span class="status-item__value">
            <el-select v-model="repairForm.repairType" size="small" style="width: 180px;" @change="loadRepairPreview">
              <el-option label="插入缺失数据" value="INSERT_MISSING" />
              <el-option label="更新不一致字段" value="UPDATE_INCONSISTENT" />
              <el-option label="删除建议" value="DELETE_EXTRA" />
            </el-select>
          </span>
        </div>
        <div class="status-item">
          <span class="status-item__label">执行前确认</span>
          <span class="status-item__value">删除类操作默认禁止，必须额外确认</span>
        </div>
      </div>
      <div class="table-shell data-validation-workbench__repair-table">
        <el-table :data="repairPreview.details" border stripe @selection-change="handleRepairSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column prop="repairType" label="类型" width="160">
            <template #default="{ row }">
              <el-tag :type="repairTagType(row.status || 'PREVIEWED')" effect="light">{{ repairDetailLabel(row.repairType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="primaryKeyJson" label="主键" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sqlPreview" label="SQL 预览" min-width="360" show-overflow-tooltip />
          <el-table-column prop="parameterJson" label="参数" min-width="240" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-space>
          <el-switch v-model="repairForm.confirmDelete" inline-prompt active-text="删" inactive-text="禁" />
          <el-button @click="repairDialogVisible = false">取消</el-button>
          <el-button type="warning" :loading="repairing" @click="executeRepair">执行修复</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listRepairRuns, listTasks, listValidationDifferences, listValidationRuns, runRepair, runValidation as executeValidation } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const tasks = ref([])
const validationRuns = ref([])
const validationDifferences = ref([])
const repairRuns = ref([])
const running = ref(false)
const repairing = ref(false)
const repairDialogVisible = ref(false)
const selectedValidationRunId = ref(null)
const selectedRepairDetailIds = ref([])
const currentRun = reactive({
  status: '',
  sourceRowCount: 0,
  targetRowCount: 0,
  missingCount: 0,
  inconsistentCount: 0,
  elapsedMillis: 0
})
const repairPreview = reactive({
  details: []
})
const form = reactive({
  taskId: null,
  validationMode: 'PRIMARY_KEY_EXISTS',
  sampleMode: 'RANDOM',
  sampleCount: 20,
  whereClause: '',
  incrementalCondition: '',
  hashAlgorithm: 'MD5',
  hashColumnsText: ''
})
const repairForm = reactive({
  validationRunId: null,
  repairType: 'INSERT_MISSING',
  confirmDelete: false
})

const validationFlow = [
  '选择同步任务',
  '配置校验规则',
  '开始校验',
  '查看差异并生成修复方案'
]

const differenceCount = computed(function () {
  return (currentRun.missingCount || 0) + (currentRun.inconsistentCount || 0)
})

const canStartValidation = computed(function () {
  return !!form.taskId
})

const validationFlowActiveIndex = computed(function () {
  if (!form.taskId) {
    return 0
  }
  if (running.value) {
    return 2
  }
  if (repairRuns.value.length > 0 || validationDifferences.value.length > 0) {
    return 3
  }
  if (validationRuns.value.length > 0) {
    return 2
  }
  return 1
})

const validationStatusLabel = computed(function () {
  if (!form.taskId) {
    return '未配置'
  }
  if (running.value) {
    return '校验中'
  }
  if (!currentRun.status) {
    return '待执行'
  }
  if (currentRun.status === 'SUCCESS') {
    return '一致'
  }
  if (currentRun.status === 'FAILED') {
    return '校验失败'
  }
  if (differenceCount.value > 0) {
    return '发现差异'
  }
  return '待执行'
})

const validationStatusHint = computed(function () {
  if (!form.taskId) {
    return '请选择同步任务'
  }
  if (running.value) {
    return '正在比较源表和目标表'
  }
  if (!currentRun.status) {
    return '最近一次校验结果'
  }
  if (currentRun.status === 'SUCCESS' && differenceCount.value === 0) {
    return '未发现缺失或不一致'
  }
  if (currentRun.status === 'FAILED') {
    return '请检查任务配置或连接'
  }
  if (differenceCount.value > 0) {
    return '请查看差异明细'
  }
  return '最近一次校验结果'
})

const rowsHint = computed(function () {
  if (!form.taskId) {
    return '支持按条件和增量条件校验'
  }
  return '支持按条件和增量条件校验'
})

const differenceSummaryText = computed(function () {
  return '缺失 ' + (currentRun.missingCount || 0) + ' · 不一致 ' + (currentRun.inconsistentCount || 0)
})

const repairStateLabel = computed(function () {
  if (!selectedValidationRunId.value) {
    return '未生成'
  }
  if (!repairRuns.value.length) {
    return '未生成'
  }
  return repairRuns.value[0].status || '已生成'
})

const repairStateHint = computed(function () {
  if (!selectedValidationRunId.value) {
    return '发现差异后可生成修复方案'
  }
  if (!repairRuns.value.length) {
    return '发现差异后可生成修复方案'
  }
  return '最近修复批次状态'
})

const configReadinessLabel = computed(function () {
  return canStartValidation.value ? '可执行' : '未配置'
})

const configReadinessTagType = computed(function () {
  return canStartValidation.value ? 'success' : 'info'
})

const validationConfigHint = computed(function () {
  if (!form.taskId) {
    return '先选择同步任务，再配置校验方式、抽样、Hash 和条件。'
  }
  return '配置完整后可开始校验；校验完成后可基于差异生成修复方案。'
})

const toolbarHint = computed(function () {
  if (!form.taskId) {
    return '先选任务，再开始校验和查看结果。'
  }
  if (running.value) {
    return '校验执行中，请稍候。'
  }
  return '当前配置可以直接执行，也可以先调整校验方式。'
})

const startDisabledReason = computed(function () {
  if (!form.taskId) {
    return '请先选择同步任务'
  }
  return '开始校验'
})

const recentValidationHint = computed(function () {
  if (!form.taskId) {
    return '先选择任务并执行一次校验。'
  }
  return '这里会记录校验状态、源/目标行数和差异统计。'
})

const differenceOverviewLabel = computed(function () {
  if (!selectedValidationRunId.value) {
    return '等待校验'
  }
  return differenceCount.value > 0 ? '存在差异' : '未发现差异'
})

const differenceHint = computed(function () {
  if (!selectedValidationRunId.value) {
    return '先运行校验，再查看差异明细和修复建议。'
  }
  if (differenceCount.value > 0) {
    return '差异会按类型列出，方便快速判断是缺失还是不一致。'
  }
  return '当前校验结果未发现可处理差异。'
})

const loadRepairDisabledReason = computed(function () {
  if (!selectedValidationRunId.value) {
    return '请先选择最近一次校验结果'
  }
  if (differenceCount.value <= 0) {
    return '需要先完成一次校验并存在差异'
  }
  return '生成修复方案'
})

const canLoadRepairPreview = computed(function () {
  return !!selectedValidationRunId.value && differenceCount.value > 0
})

const repairHistoryLabel = computed(function () {
  if (!selectedValidationRunId.value) {
    return '未生成'
  }
  return repairRuns.value.length ? '有记录' : '未生成'
})

const repairHistoryHint = computed(function () {
  if (!selectedValidationRunId.value) {
    return '修复历史会记录类型、状态、数量和耗时。'
  }
  return '修复历史会记录类型、状态、数量和耗时。'
})

const repairPreviewHint = computed(function () {
  if (!differenceCount.value) {
    return '当前没有差异，仍可预览修复结构，但通常无需执行。'
  }
  return '只展示当前差异的安全修复 SQL 预览，执行前可勾选需要处理的条目。'
})

const repairBannerTagLabel = computed(function () {
  if (repairPreview.details.length === 0) {
    return '预览为空'
  }
  return repairPreview.details.length + ' 条预览'
})

const selectedRunLabel = computed(function () {
  if (!selectedValidationRunId.value) {
    return '未选择结果'
  }
  return '已选结果'
})

function differenceTypeLabel(value) {
  if (value === 'MISSING') {
    return '缺失'
  }
  if (value === 'INCONSISTENT') {
    return '不一致'
  }
  if (value === 'EXTRA') {
    return '多余'
  }
  if (value === 'HASH_MISMATCH') {
    return 'Hash 不一致'
  }
  if (value === 'PRIMARY_KEY_MISSING') {
    return '主键缺失'
  }
  return value || '差异'
}

function differenceTypeTagType(value) {
  if (value === 'MISSING') {
    return 'warning'
  }
  if (value === 'INCONSISTENT') {
    return 'danger'
  }
  if (value === 'EXTRA') {
    return 'info'
  }
  if (value === 'HASH_MISMATCH') {
    return 'warning'
  }
  if (value === 'PRIMARY_KEY_MISSING') {
    return 'danger'
  }
  return 'info'
}

function repairStatusLabel(value) {
  if (value === 'SUCCESS') {
    return '成功'
  }
  if (value === 'FAILED') {
    return '失败'
  }
  if (value === 'RUNNING') {
    return '执行中'
  }
  if (value === 'PREVIEWED') {
    return '预览'
  }
  return value || '-'
}

function repairTagType(value) {
  if (value === 'SUCCESS') {
    return 'success'
  }
  if (value === 'FAILED') {
    return 'danger'
  }
  if (value === 'RUNNING') {
    return 'warning'
  }
  if (value === 'PREVIEWED') {
    return 'info'
  }
  return 'info'
}

function repairDetailLabel(value) {
  if (value === 'INSERT_MISSING') {
    return '插入缺失'
  }
  if (value === 'UPDATE_INCONSISTENT') {
    return '更新不一致'
  }
  if (value === 'DELETE_EXTRA') {
    return '删除多余'
  }
  return value || '-'
}

function statusLabel(status) {
  if (status === 'RUNNING') {
    return '校验中'
  }
  if (status === 'SUCCESS') {
    return '一致'
  }
  if (status === 'FAILED') {
    return '校验失败'
  }
  if (status === 'PARTIAL_SUCCESS') {
    return '部分成功'
  }
  return status || '待执行'
}

function statusTagType(status) {
  if (status === 'RUNNING') {
    return 'warning'
  }
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  if (status === 'PARTIAL_SUCCESS') {
    return 'warning'
  }
  return 'info'
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
  return value + ' ms'
}

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    tasks.value = await listTasks()
    if (!form.taskId && tasks.value.length > 0) {
      form.taskId = tasks.value[0].id
    }
    await loadValidationRuns()
    await loadRepairRuns()
    if (selectedValidationRunId.value) {
      await loadValidationDifferences(selectedValidationRunId.value)
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据校验页面失败')
  }
}

async function loadValidationRuns() {
  if (!form.taskId) {
    validationRuns.value = []
    return
  }
  validationRuns.value = await listValidationRuns(form.taskId, 20)
  if (validationRuns.value.length > 0) {
    selectValidationRun(validationRuns.value[0])
  } else {
    selectedValidationRunId.value = null
    validationDifferences.value = []
    currentRun.status = ''
    currentRun.sourceRowCount = 0
    currentRun.targetRowCount = 0
    currentRun.missingCount = 0
    currentRun.inconsistentCount = 0
    currentRun.elapsedMillis = 0
  }
}

async function loadRepairRuns() {
  if (!selectedValidationRunId.value) {
    repairRuns.value = []
    return
  }
  repairRuns.value = await listRepairRuns(selectedValidationRunId.value, 20)
}

function handleTaskChange() {
  selectedValidationRunId.value = null
  validationDifferences.value = []
  repairRuns.value = []
  loadValidationRuns()
}

function selectValidationRun(row) {
  if (!row) {
    return
  }
  selectedValidationRunId.value = row.id
  currentRun.status = row.status || ''
  currentRun.sourceRowCount = row.sourceRowCount || 0
  currentRun.targetRowCount = row.targetRowCount || 0
  currentRun.missingCount = row.missingCount || 0
  currentRun.inconsistentCount = row.inconsistentCount || 0
  currentRun.elapsedMillis = row.elapsedMillis || 0
  repairForm.validationRunId = row.id
  loadValidationDifferences(row.id)
  loadRepairRuns()
}

async function loadValidationDifferences(validationRunId) {
  validationDifferences.value = await listValidationDifferences(validationRunId)
}

async function runValidation() {
  if (!form.taskId) {
    ElMessage.warning('请先选择同步任务')
    return
  }
  running.value = true
  try {
    const payload = {
      taskId: form.taskId,
      validationMode: form.validationMode,
      sampleMode: form.sampleMode,
      sampleCount: form.sampleCount,
      whereClause: form.whereClause,
      incrementalCondition: form.incrementalCondition,
      hashAlgorithm: form.hashAlgorithm,
      hashColumns: parseHashColumns(form.hashColumnsText)
    }
    const result = await executeValidation(payload)
    currentRun.status = result.run.status
    currentRun.sourceRowCount = result.run.sourceRowCount || 0
    currentRun.targetRowCount = result.run.targetRowCount || 0
    currentRun.missingCount = result.run.missingCount || 0
    currentRun.inconsistentCount = result.run.inconsistentCount || 0
    currentRun.elapsedMillis = result.run.elapsedMillis || 0
    selectedValidationRunId.value = result.run.id
    validationDifferences.value = result.differences || []
    await loadValidationRuns()
    await loadRepairRuns()
    ElMessage.success('校验完成')
  } catch (error) {
    ElMessage.error(error.message || '校验失败')
  } finally {
    running.value = false
  }
}

async function loadRepairPreview() {
  if (!selectedValidationRunId.value) {
    return
  }
  if (differenceCount.value <= 0) {
    ElMessage.warning('需要先完成一次校验并存在差异')
    return
  }
  repairForm.validationRunId = selectedValidationRunId.value
  if (!repairForm.repairType) {
    repairForm.repairType = repairTypeFromDifferences(validationDifferences.value)
  }
  try {
    const preview = await runRepair({
      validationRunId: repairForm.validationRunId,
      repairType: repairForm.repairType,
      validationDifferenceIds: [],
      execute: false,
      confirmDelete: false
    })
    repairPreview.details = preview.details || []
    selectedRepairDetailIds.value = []
    repairDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '生成修复方案失败')
  }
}

function handleRepairSelectionChange(selection) {
  selectedRepairDetailIds.value = selection.map(function (item) {
    return item.id
  })
}

async function executeRepair() {
  if (!repairForm.validationRunId) {
    ElMessage.warning('请先选择校验结果')
    return
  }
  repairing.value = true
  try {
    const result = await runRepair({
      validationRunId: repairForm.validationRunId,
      repairType: repairForm.repairType,
      targetTableName: null,
      validationDifferenceIds: selectedRepairDetailIds.value,
      execute: true,
      confirmDelete: repairForm.confirmDelete
    })
    repairRuns.value.unshift(result.run)
    repairDialogVisible.value = false
    ElMessage.success('修复完成')
  } catch (error) {
    ElMessage.error(error.message || '执行修复失败')
  } finally {
    repairing.value = false
  }
}

function parseHashColumns(text) {
  if (!text) {
    return []
  }
  return text.split(',').map(function (item) {
    return item.trim()
  }).filter(Boolean)
}

function repairTypeFromDifferences(differences) {
  if (!differences || !differences.length) {
    return 'INSERT_MISSING'
  }
  const first = differences[0]
  return first.suggestedRepairType || 'INSERT_MISSING'
}
</script>
