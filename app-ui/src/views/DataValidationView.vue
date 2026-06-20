<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>数据校验</h1>
        <p>按任务执行行数、主键、抽样和 Hash 校验，并基于差异生成安全修复方案。</p>
      </div>
      <el-space>
        <el-button round @click="loadPageData">刷新</el-button>
        <el-button type="primary" round :loading="running" @click="runValidation">开始校验</el-button>
      </el-space>
    </div>

    <div class="page-overview">
      <div class="page-overview__item">
        <div class="page-overview__label">校验状态</div>
        <div class="page-overview__value">{{ currentRun.status || '待执行' }}</div>
        <div class="page-overview__hint">最近一次校验结果</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">源 / 目标行数</div>
        <div class="page-overview__value">{{ currentRun.sourceRowCount || 0 }} / {{ currentRun.targetRowCount || 0 }}</div>
        <div class="page-overview__hint">支持按条件和增量条件校验</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">缺失 / 不一致</div>
        <div class="page-overview__value">{{ currentRun.missingCount || 0 }} / {{ currentRun.inconsistentCount || 0 }}</div>
        <div class="page-overview__hint">可生成修复方案</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">耗时</div>
        <div class="page-overview__value">{{ formatDuration(currentRun.elapsedMillis) }}</div>
        <div class="page-overview__hint">最近校验运行时间</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>校验配置</h2>
          <el-tag type="info" effect="dark">P2-1</el-tag>
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

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>最近校验</h2>
          <el-tag type="success" effect="dark">{{ validationRuns.length }} 条</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="validationRuns" border stripe @row-click="selectValidationRun">
            <el-table-column prop="validationMethod" label="方式" width="150" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.status || '-' }}</el-tag>
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

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>差异明细</h2>
          <el-space>
            <el-tag type="warning" effect="dark">{{ validationDifferences.length }} 项</el-tag>
            <el-button :disabled="!selectedValidationRunId" @click="loadRepairPreview">生成修复方案</el-button>
          </el-space>
        </div>
        <div class="table-shell">
          <el-table :data="validationDifferences" border stripe>
            <el-table-column prop="differenceType" label="差异类型" width="160" />
            <el-table-column prop="primaryKeyJson" label="主键" min-width="200" show-overflow-tooltip />
            <el-table-column prop="sourceRowJson" label="源行" min-width="240" show-overflow-tooltip />
            <el-table-column prop="targetRowJson" label="目标行" min-width="240" show-overflow-tooltip />
            <el-table-column prop="suggestedRepairType" label="建议修复" width="160" />
          </el-table>
          <StateEmpty
            v-if="!validationDifferences.length"
            title="还没有差异记录"
            description="校验完成后，这里才会列出不一致、缺失和异常项。"
            hint="可以先运行校验，再生成修复方案。"
            button-text="开始校验"
            @action="runValidation"
          />
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>修复历史</h2>
          <el-tag type="info" effect="dark">{{ repairRuns.length }} 条</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="repairRuns" border stripe>
            <el-table-column prop="repairType" label="类型" width="160" />
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column prop="repairCount" label="数量" width="100" />
            <el-table-column prop="successCount" label="成功" width="100" />
            <el-table-column prop="failedCount" label="失败" width="100" />
            <el-table-column label="时间" width="200">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
          <StateEmpty
            v-if="!repairRuns.length"
            title="还没有修复记录"
            description="生成并执行修复后，这里会展示修复批次。"
            hint="修复历史会记录类型、状态、数量和耗时。"
            button-text="生成修复方案"
            @action="loadRepairPreview"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="repairDialogVisible" title="修复方案" width="960px">
      <div class="status-stack" style="margin-bottom: 16px;">
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
      <el-table :data="repairPreview.details" border stripe @selection-change="handleRepairSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="repairType" label="类型" width="160" />
        <el-table-column prop="primaryKeyJson" label="主键" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sqlPreview" label="SQL 预览" min-width="360" show-overflow-tooltip />
        <el-table-column prop="parameterJson" label="参数" min-width="240" show-overflow-tooltip />
      </el-table>
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
import { onMounted, reactive, ref } from 'vue'
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
</script>
