<template>
  <div class="page-section field-mapping-workbench">
    <div class="page-header field-mapping-workbench__header">
      <div class="field-mapping-workbench__titleblock">
        <h1>字段映射</h1>
        <p>按同步任务维护源字段到目标字段的映射、忽略字段和默认值，用于同步执行。</p>
      </div>
      <div class="field-mapping-workbench__toolbar">
        <div class="field-mapping-workbench__toolbar-actions">
          <el-select
            v-model="selectedTaskId"
            placeholder="选择任务"
            class="field-mapping-workbench__task-select"
            @change="handleTaskChange"
          >
            <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
          </el-select>
          <el-tooltip :disabled="canRegenerate" :content="regenerateDisabledReason" placement="bottom">
            <span>
              <el-button
                :disabled="!canRegenerate"
                @click="handleRegenerate"
              >
                重新推荐
              </el-button>
            </span>
          </el-tooltip>
          <el-tooltip :disabled="canCreateMapping" :content="createMappingDisabledReason" placement="bottom">
            <span>
              <el-button
                type="primary"
                :disabled="!canCreateMapping"
                @click="openCreateDialog"
              >
                新建映射
              </el-button>
            </span>
          </el-tooltip>
        </div>
        <div class="field-mapping-workbench__toolbar-note">
          {{ toolbarHint }}
        </div>
      </div>
    </div>

    <div class="field-mapping-workbench__overview page-overview page-overview--five">
      <div class="page-overview__item">
        <div class="page-overview__label">当前任务</div>
        <div class="page-overview__value">{{ selectedTaskName || '未选择' }}</div>
        <div class="page-overview__hint">{{ taskRouteText }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">映射覆盖率</div>
        <div class="page-overview__value">{{ mappingCoverageText }}</div>
        <div class="page-overview__hint">{{ coverageHint }}</div>
        <el-progress :percentage="mappingCoverage" :show-text="false" :stroke-width="8" class="field-mapping-workbench__coverage-progress" />
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">已映射</div>
        <div class="page-overview__value">{{ confirmedCount }}</div>
        <div class="page-overview__hint">已确认的字段映射规则</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">待确认</div>
        <div class="page-overview__value">{{ pendingConfirmCount }}</div>
        <div class="page-overview__hint">中低置信度推荐需人工确认</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">已忽略</div>
        <div class="page-overview__value">{{ ignoredMappingCount }}</div>
        <div class="page-overview__hint">不会参与同步的源字段</div>
      </div>
    </div>

    <div class="panel-card glass-panel field-mapping-workbench__recommendation">
      <div class="section-title">
        <div class="section-title__left">
          <h2>智能推荐</h2>
          <el-tag :type="recommendationStatusTagType" effect="light">{{ recommendationStatusLabel }}</el-tag>
        </div>
        <el-space>
          <el-tag type="warning" effect="light">{{ recommendationSummary }}</el-tag>
          <el-button
            size="small"
            :disabled="!canRegenerate || loadingSuggestions"
            :title="regenerateDisabledReason"
            :loading="loadingSuggestions"
            @click="handleRegenerate"
          >
            重新推荐
          </el-button>
        </el-space>
      </div>

      <div class="field-mapping-workbench__panel-hint">
        <span>{{ recommendationHint }}</span>
      </div>

      <div v-if="selectedTaskId" class="field-mapping-workbench__table-shell table-shell">
        <el-table :data="suggestions" border stripe v-loading="loadingSuggestions" :empty-text="selectedTaskId ? '暂无推荐映射，请点击“重新推荐”。' : ''">
          <el-table-column prop="sourceColumnName" label="源字段" min-width="180">
            <template #default="{ row }">
              <div class="field-cell">
                <span class="field-cell__title">{{ row.sourceColumnName || '—' }}</span>
                <span class="field-cell__sub">{{ row.ignored ? '已忽略' : '待处理' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="源类型" width="120">
            <template #default="{ row }">
              {{ row.sourceColumnType || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="推荐目标字段" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.targetColumnName" size="small" placeholder="手动修改目标字段" @input="markSuggestionDirty(row)" />
            </template>
          </el-table-column>
          <el-table-column label="目标类型" width="120">
            <template #default="{ row }">
              {{ row.targetColumnType || '—' }}
            </template>
          </el-table-column>
          <el-table-column label="置信度" width="120">
            <template #default="{ row }">
              <el-tag :type="confidenceTagType(row.confidence)" effect="light">{{ formatConfidence(row.confidence) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="suggestionStatusTagType(row)" effect="light">{{ suggestionStatusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-switch v-model="row.ignored" @change="markSuggestionDirty(row)" />
                <el-button size="small" type="primary" @click="acceptSuggestion(row)">确认</el-button>
                <el-button size="small" @click="saveSuggestion(row)">{{ row.saved ? '更新' : '保存' }}</el-button>
                <el-button size="small" type="danger" :disabled="!row.id" @click="removeMapping(row)">删除</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else class="field-mapping-workbench__empty-state">
        <div class="field-mapping-workbench__empty-title">请先选择同步任务</div>
        <div class="field-mapping-workbench__empty-desc">选择任务后，系统会读取源表和目标表字段，并自动生成推荐映射。</div>
        <ol class="field-mapping-workbench__guide-list">
          <li>选择同步任务</li>
          <li>系统读取源表和目标表字段</li>
          <li>自动生成推荐映射</li>
          <li>人工确认低置信度字段</li>
          <li>保存映射规则</li>
        </ol>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact field-mapping-workbench__bottom">
      <div class="panel-card glass-panel field-mapping-workbench__mapping-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>映射规则</h2>
            <el-tag type="info" effect="light">{{ mappingStatusLabel }}</el-tag>
          </div>
        </div>
        <div class="field-mapping-workbench__panel-hint">
          <span>{{ mappingRuleHint }}</span>
        </div>
        <div v-if="selectedTaskId" class="field-mapping-workbench__table-shell table-shell">
          <el-table :data="mappings" border stripe v-loading="loading" :empty-text="selectedTaskId ? '当前没有可保存的映射规则。' : ''">
            <el-table-column prop="sourceColumnName" label="源字段" min-width="180" />
            <el-table-column prop="targetColumnName" label="目标字段" min-width="180" />
            <el-table-column prop="transformRule" label="转换规则" min-width="200">
              <template #default="{ row }">
                {{ row.transformRule || '未配置' }}
              </template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值" min-width="140">
              <template #default="{ row }">
                {{ row.defaultValue || '—' }}
              </template>
            </el-table-column>
            <el-table-column label="是否忽略" width="110">
              <template #default="{ row }">
                <el-tag :type="row.ignored ? 'warning' : 'success'">
                  {{ row.ignored ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="mappingRowStatusTagType(row)" effect="light">{{ mappingRowStatusLabel(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" @click="editMapping(row)">编辑</el-button>
                  <el-button size="small" type="primary" :disabled="!row.id" @click="openTransformDrawer(row)">配置转换</el-button>
                  <el-button size="small" type="danger" @click="removeMapping(row)">删除</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="field-mapping-workbench__empty-state">
          <div class="field-mapping-workbench__empty-title">请先选择同步任务</div>
          <div class="field-mapping-workbench__empty-desc">选择任务后，这里会显示已确认并保存的字段映射规则。</div>
        </div>
      </div>

      <div class="panel-card glass-panel field-mapping-workbench__context-card">
        <div class="section-title">
          <div class="section-title__left">
            <h2>当前任务</h2>
            <el-tag :type="currentTask ? 'success' : 'info'" effect="light">{{ currentTask ? '已选择' : '未选择' }}</el-tag>
          </div>
          <el-tag type="success" effect="light">任务上下文</el-tag>
        </div>
        <div v-if="currentTask" class="field-mapping-context">
          <div class="field-mapping-context__status-strip">
            <div>
              <div class="field-mapping-context__status-label">{{ taskAvailabilityLabel }}</div>
              <div class="field-mapping-context__status-value">{{ taskAvailabilityHint }}</div>
            </div>
            <el-tag :type="mappingComplete ? 'success' : 'warning'" effect="light">
              {{ mappingComplete ? '可用于同步' : '等待确认' }}
            </el-tag>
          </div>
          <div class="field-mapping-context__summary">
            <div class="field-mapping-context__title">{{ currentTask.taskName }}</div>
            <div class="field-mapping-context__sub">{{ syncModeLabel(currentTask.syncMode) }}</div>
          </div>
          <div class="field-mapping-context__rows">
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">源数据源</span>
              <span class="field-mapping-context-row__value">{{ sourceDatasourceName || '—' }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">目标数据源</span>
              <span class="field-mapping-context-row__value">{{ targetDatasourceName || '—' }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">源表</span>
              <span class="field-mapping-context-row__value">{{ qualifiedTableName(currentTask.sourceSchemaName, currentTask.sourceTableName) }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">目标表</span>
              <span class="field-mapping-context-row__value">{{ qualifiedTableName(currentTask.targetSchemaName, currentTask.targetTableName) }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">字段数量</span>
              <span class="field-mapping-context-row__value">源 {{ sourceFieldCount }} / 目标 {{ targetFieldCount }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">任务状态</span>
              <span class="field-mapping-context-row__value">{{ currentTaskReadyText }}</span>
            </div>
          </div>

          <div class="field-mapping-context__missing">
            <div class="field-mapping-context__missing-title">还需要处理</div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">未映射源字段</span>
              <span class="field-mapping-context-row__value">{{ pendingSourceFieldCount }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">低置信度推荐</span>
              <span class="field-mapping-context-row__value">{{ lowConfidenceCount }}</span>
            </div>
            <div class="field-mapping-context-row">
              <span class="field-mapping-context-row__label">已忽略字段</span>
              <span class="field-mapping-context-row__value">{{ ignoredMappingCount }}</span>
            </div>
          </div>

          <div class="field-mapping-context__advice">
            <div class="field-mapping-context__advice-title">操作建议</div>
            <ul>
              <li>先处理低置信度推荐</li>
              <li>再确认未映射字段</li>
              <li>最后保存映射规则</li>
            </ul>
          </div>

          <div class="field-mapping-context__footer">
            <el-tag :type="mappingComplete ? 'success' : 'warning'" effect="light">
              {{ mappingComplete ? '字段映射已完成，可以保存并用于同步任务。' : '仍有字段未确认，请检查低置信度或未映射字段。' }}
            </el-tag>
          </div>
        </div>
        <div v-else class="field-mapping-context field-mapping-context--empty">
          <div class="field-mapping-context__title">请先选择一个同步任务</div>
          <div class="field-mapping-context__sub">选择任务后，这里会展示任务上下文、缺失项和操作建议。</div>
          <div class="field-mapping-context__empty-note">工作流会先确认上下文，再允许重新推荐和新建映射。</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="同步任务" prop="taskId">
              <el-select v-model="form.taskId" placeholder="请选择任务" style="width: 100%;" @change="syncTaskContext">
                <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="忽略字段">
              <el-switch v-model="form.ignored" />
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

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="源字段" prop="sourceColumnName">
              <el-input v-model="form.sourceColumnName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标字段" prop="targetColumnName">
              <el-input v-model="form.targetColumnName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="默认值">
          <el-input v-model="form.defaultValue" placeholder="可选" />
        </el-form-item>

        <el-form-item label="转换规则">
          <el-input :model-value="form.transformRule || '未配置'" disabled />
        </el-form-item>

        <el-form-item label="">
          <div class="field-mapping-workbench__rule-hint">
            转换规则请在保存字段映射后，通过表格中的「配置转换」维护。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitMapping">保存映射</el-button>
        </el-space>
      </template>
    </el-dialog>

    <TransformRuleDrawer
      v-model:visible="transformDrawerVisible"
      :task="currentTask"
      :mapping="activeTransformMapping"
      :source-schemas="sourceSchemas"
      :target-schemas="targetSchemas"
      @saved="handleTransformRulesSaved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFieldMapping, deleteTransformRule, listFieldMappings, listTasks, listTransformRules, saveFieldMapping, scanMetadata, suggestFieldMappings } from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'
import TransformRuleDrawer from '../components/TransformRuleDrawer.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const loadingSuggestions = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const tasks = ref([])
const mappings = ref([])
const suggestions = ref([])
const selectedTaskId = ref(null)
const selectedTaskName = ref('')
const editingId = ref(null)
const currentTask = ref(null)
const sourceSchemas = ref([])
const targetSchemas = ref([])
const sourceSchemaOptions = ref([])
const targetSchemaOptions = ref([])
const sourceTableOptions = ref([])
const targetTableOptions = ref([])
const transformDrawerVisible = ref(false)
const activeTransformMapping = ref(null)

const form = reactive(createEmptyForm())

const rules = {
  taskId: [{ required: true, message: '请选择同步任务', trigger: 'change' }],
  sourceTableName: [{ required: true, message: '请输入源表名', trigger: 'blur' }],
  targetTableName: [{ required: true, message: '请输入目标表名', trigger: 'blur' }],
  sourceColumnName: [{ required: true, message: '请输入源字段', trigger: 'blur' }],
  targetColumnName: [{ required: true, message: '请输入目标字段', trigger: 'blur' }]
}

const dialogTitle = computed(function () {
  return editingId.value ? '编辑字段映射' : '新建字段映射'
})

const ignoredMappingCount = computed(function () {
  return mappings.value.filter(function (item) {
    return !!item.ignored
  }).length
})

const suggestionCount = computed(function () {
  return suggestions.value.length
})

const confirmedCount = computed(function () {
  return mappings.value.filter(function (item) {
    return !item.ignored
  }).length
})

const lowConfidenceCount = computed(function () {
  return suggestions.value.filter(function (item) {
    return typeof item.confidence === 'number' && item.confidence < 0.6 && !item.ignored
  }).length
})

const pendingConfirmCount = computed(function () {
  return suggestions.value.filter(function (item) {
    return !item.saved && !item.ignored
  }).length
})

const sourceFieldCount = computed(function () {
  return countFields(sourceSchemas.value, form.sourceSchemaName, form.sourceTableName)
})

const targetFieldCount = computed(function () {
  return countFields(targetSchemas.value, form.targetSchemaName, form.targetTableName)
})

const mappingCoverage = computed(function () {
  if (!sourceFieldCount.value) {
    return 0
  }
  return Math.min(100, Math.round((confirmedCount.value / sourceFieldCount.value) * 100))
})

const mappingCoverageText = computed(function () {
  return mappingCoverage.value + '%'
})

const coverageHint = computed(function () {
  if (!selectedTaskId.value) {
    return '选择任务后计算覆盖率'
  }
  return '已确认 ' + confirmedCount.value + ' / ' + sourceFieldCount.value + ' 个源字段'
})

const pendingSourceFieldCount = computed(function () {
  if (!sourceFieldCount.value) {
    return 0
  }
  return Math.max(0, sourceFieldCount.value - mappings.value.length)
})

const canCreateMapping = computed(function () {
  return !!selectedTaskId.value && hasFieldContext.value
})

const canRegenerate = computed(function () {
  return !!selectedTaskId.value && hasFieldContext.value
})

const hasFieldContext = computed(function () {
  return !!currentTask.value && !!currentTask.value.sourceDatasourceId && !!currentTask.value.targetDatasourceId
})

const recommendationStatusLabel = computed(function () {
  if (!selectedTaskId.value) {
    return '未选择任务'
  }
  if (suggestionCount.value === 0) {
    return '0 条推荐'
  }
  if (pendingConfirmCount.value > 0) {
    return '需要确认'
  }
  return '已完成'
})

const recommendationStatusTagType = computed(function () {
  if (!selectedTaskId.value) {
    return 'info'
  }
  if (pendingConfirmCount.value > 0) {
    return 'warning'
  }
  if (suggestionCount.value > 0) {
    return 'success'
  }
  return 'info'
})

const recommendationHint = computed(function () {
  if (!selectedTaskId.value) {
    return '请先选择同步任务'
  }
  if (!hasFieldContext.value) {
    return '当前任务缺少源/目标字段信息，无法重新推荐'
  }
  if (suggestionCount.value === 0) {
    return '可以点击重新推荐，或手动新建映射。'
  }
  return '系统会根据字段名归一化、常见别名和相似度匹配生成推荐映射。'
})

const regenerateDisabledReason = computed(function () {
  if (!selectedTaskId.value) {
    return '请先选择同步任务'
  }
  if (!hasFieldContext.value) {
    return '当前任务缺少源/目标字段信息，无法重新推荐'
  }
  return '重新生成推荐映射'
})

const createMappingDisabledReason = computed(function () {
  if (!selectedTaskId.value) {
    return '请先选择同步任务'
  }
  if (!hasFieldContext.value) {
    return '当前任务缺少源/目标字段信息，无法新建映射'
  }
  return '新建字段映射'
})

const mappingStatusLabel = computed(function () {
  if (!selectedTaskId.value) {
    return '未选择任务'
  }
  if (mappings.value.length === 0) {
    return '0 条规则'
  }
  return '已配置 ' + mappings.value.length + ' 条'
})

const mappingRuleHint = computed(function () {
  if (!selectedTaskId.value) {
    return '选择任务后，这里会显示已确认并保存的字段映射规则。'
  }
  if (mappings.value.length === 0) {
    return '还没有确认映射规则，请先从智能推荐中确认映射，或手动新建映射。'
  }
  return '已确认并保存的字段映射规则会用于同步执行。'
})

const sourceDatasourceName = computed(function () {
  return currentTask.value ? currentTask.value.sourceDatasourceName || '' : ''
})

const targetDatasourceName = computed(function () {
  return currentTask.value ? currentTask.value.targetDatasourceName || '' : ''
})

const recommendationSummary = computed(function () {
  if (!selectedTaskId.value) {
    return '请先选择同步任务'
  }
  if (suggestionCount.value === 0) {
    return '还没有推荐映射'
  }
  return suggestionCount.value + ' 条推荐'
})

const mappingComplete = computed(function () {
  return !!selectedTaskId.value && sourceFieldCount.value > 0 && pendingSourceFieldCount.value === 0 && pendingConfirmCount.value === 0 && lowConfidenceCount.value === 0
})

const taskRouteText = computed(function () {
  if (!currentTask.value) {
    return '请先选择一个同步任务，再为该任务配置字段映射。'
  }
  return qualifiedTableName(currentTask.value.sourceSchemaName, currentTask.value.sourceTableName) + ' → ' + qualifiedTableName(currentTask.value.targetSchemaName, currentTask.value.targetTableName)
})

const toolbarHint = computed(function () {
  if (!selectedTaskId.value) {
    return '先选任务，再查看推荐、补齐映射并保存。'
  }
  if (!hasFieldContext.value) {
    return '当前任务上下文不足，先补齐源/目标字段信息。'
  }
  return mappingComplete.value ? '映射已完成，可继续保存或微调规则。' : '优先处理低置信度项，再补齐未映射字段。'
})

const recommendationEmptyTitle = computed(function () {
  return selectedTaskId.value ? '暂无推荐映射，请点击“重新推荐”。' : '请先选择同步任务'
})

const recommendationEmptyHint = computed(function () {
  return selectedTaskId.value ? '可以点击重新推荐，或直接新建映射。' : '选择任务后，系统会自动加载推荐映射。'
})

const mappingEmptyTitle = computed(function () {
  return selectedTaskId.value ? '还没有确认映射规则' : '请先选择同步任务'
})

const mappingEmptyHint = computed(function () {
  return selectedTaskId.value ? '请先从智能推荐中确认映射，或手动新建映射。' : '选择任务后，这里会显示已确认并保存的字段映射规则。'
})

function suggestionStatusLabel(row) {
  if (row.ignored) {
    return '已忽略'
  }
  if (row.saved) {
    return '已保存'
  }
  return '待确认'
}

function suggestionStatusTagType(row) {
  if (row.ignored) {
    return 'info'
  }
  if (row.saved) {
    return 'success'
  }
  return 'warning'
}

function mappingRowStatusLabel(row) {
  if (row.ignored) {
    return '已忽略'
  }
  const summary = normalizeTransformRuleSummary(row.transformRule)
  if (summary === '未配置') {
    return '待配置'
  }
  if (summary === '已停用') {
    return '已停用'
  }
  return '已配置'
}

function mappingRowStatusTagType(row) {
  if (row.ignored) {
    return 'info'
  }
  const summary = normalizeTransformRuleSummary(row.transformRule)
  if (summary === '未配置') {
    return 'warning'
  }
  if (summary === '已停用') {
    return 'info'
  }
  return 'success'
}

const taskAvailabilityLabel = computed(function () {
  return hasFieldContext.value ? '上下文完整' : '上下文不足'
})

const taskAvailabilityHint = computed(function () {
  if (!currentTask.value) {
    return '未选择任务'
  }
  if (!hasFieldContext.value) {
    return '缺少源/目标字段信息'
  }
  return '可继续生成推荐和新建映射'
})

const currentTaskReadyText = computed(function () {
  if (!currentTask.value) {
    return '未选择任务'
  }
  if (!hasFieldContext.value) {
    return '字段信息不足'
  }
  return mappingComplete.value ? '映射已准备就绪' : '映射可继续完善'
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  loading.value = true
  try {
    tasks.value = await listTasks()
    resolveSelectedTask()
    await loadTableOptions()
    if (selectedTaskId.value) {
      await loadMappings(selectedTaskId.value)
      await loadSuggestions(selectedTaskId.value)
    } else {
      mappings.value = []
      suggestions.value = []
    }
  } catch (error) {
    ElMessage.error(error.message || '加载字段映射失败')
  } finally {
    loading.value = false
  }
}

function resolveSelectedTask() {
  const queryTaskId = route.query.taskId ? Number(route.query.taskId) : null
  if (queryTaskId) {
    selectedTaskId.value = queryTaskId
  }
  if (!selectedTaskId.value && tasks.value.length > 0) {
    selectedTaskId.value = tasks.value[0].id
  }
  syncTaskContext(selectedTaskId.value)
}

async function handleTaskChange(taskId) {
  syncRoute(taskId)
  syncTaskContext(taskId)
  await loadMappings(taskId)
  await loadSuggestions(taskId)
}

function syncRoute(taskId) {
  const nextQuery = Object.assign({}, route.query)
  if (taskId) {
    nextQuery.taskId = String(taskId)
  } else {
    delete nextQuery.taskId
  }
  router.replace({ path: '/mapping', query: nextQuery })
}

function syncTaskContext(taskId) {
  const task = tasks.value.find(function (item) {
    return item.id === taskId
  })
  currentTask.value = task || null
  selectedTaskName.value = task ? task.taskName : ''
  syncContextToForm()
}

async function loadTableOptions() {
  if (!currentTask.value || !currentTask.value.sourceDatasourceId || !currentTask.value.targetDatasourceId) {
    sourceSchemas.value = []
    targetSchemas.value = []
    sourceSchemaOptions.value = []
    targetSchemaOptions.value = []
    sourceTableOptions.value = []
    targetTableOptions.value = []
    return
  }
  sourceSchemas.value = await scanMetadata(currentTask.value.sourceDatasourceId)
  targetSchemas.value = currentTask.value.targetDatasourceId === currentTask.value.sourceDatasourceId
    ? sourceSchemas.value
    : await scanMetadata(currentTask.value.targetDatasourceId)
  sourceSchemaOptions.value = toSchemaOptions(sourceSchemas.value)
  targetSchemaOptions.value = toSchemaOptions(targetSchemas.value)
  sourceTableOptions.value = toTableOptions(sourceSchemas.value, form.sourceSchemaName)
  targetTableOptions.value = toTableOptions(targetSchemas.value, form.targetSchemaName)
}

function toSchemaOptions(schemas) {
  return (schemas || []).map(function (schema) {
    return {
      label: schema.schemaName,
      value: schema.schemaName,
      schemaName: schema.schemaName
    }
  })
}

function toTableOptions(schemas, schemaName) {
  if (!schemaName) {
    return []
  }
  const schema = (schemas || []).find(function (item) {
    return item.schemaName && item.schemaName.toLowerCase() === String(schemaName).toLowerCase()
  })
  return schema && schema.tables ? schema.tables.map(function (table) {
    return {
      label: table.tableName,
      value: table.tableName,
      tableName: table.tableName
    }
  }) : []
}

async function loadMappings(taskId) {
  if (!taskId) {
    mappings.value = []
    return
  }
  loading.value = true
  try {
    const loadedMappings = await listFieldMappings(taskId)
    mappings.value = loadedMappings
    await hydrateTransformRuleSummaries(taskId, mappings.value)
  } catch (error) {
    ElMessage.error(error.message || '加载映射失败')
  } finally {
    loading.value = false
  }
}

async function loadSuggestions(taskId) {
  if (!taskId) {
    suggestions.value = []
    return
  }
  loadingSuggestions.value = true
  try {
    const recommended = await suggestFieldMappings(taskId)
    suggestions.value = mergeSuggestionsWithMappings(recommended || [], mappings.value)
  } catch (error) {
    ElMessage.error(error.message || '加载推荐失败')
    suggestions.value = []
  } finally {
    loadingSuggestions.value = false
  }
}

async function handleRegenerate() {
  if (!canRegenerate.value) {
    ElMessage.warning(recommendationHint.value)
    return
  }
  await loadSuggestions(selectedTaskId.value)
}

function openCreateDialog() {
  if (!canCreateMapping.value) {
    ElMessage.warning(canRegenerate.value ? '请先选择一个任务' : '当前任务缺少源/目标字段信息，无法新建映射')
    return
  }
  editingId.value = null
  resetForm()
  form.taskId = selectedTaskId.value
  syncContextToForm()
  dialogVisible.value = true
}

function editMapping(row) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function acceptSuggestion(row) {
  row.ignored = false
  row.targetColumnName = row.targetColumnName || row.sourceColumnName
  saveSuggestion(row)
}

async function saveSuggestion(row) {
  if (!selectedTaskId.value) {
    ElMessage.warning('请先选择一个任务')
    return
  }
  if (!row.sourceColumnName || !row.targetColumnName) {
    ElMessage.warning('源字段和目标字段不能为空')
    return
  }
  try {
    await saveFieldMapping(normalizeSuggestionPayload(row))
    row.saved = true
    ElMessage.success('字段映射已保存')
    await loadMappings(selectedTaskId.value)
    await loadSuggestions(selectedTaskId.value)
  } catch (error) {
    ElMessage.error(error.message || '保存推荐失败')
  }
}

function markSuggestionDirty(row) {
  row.saved = false
}

async function submitMapping() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate()
  saving.value = true
  try {
    await saveFieldMapping(normalizeForm())
    ElMessage.success('字段映射已保存')
    dialogVisible.value = false
    await loadMappings(selectedTaskId.value)
    await loadSuggestions(selectedTaskId.value)
  } catch (error) {
    ElMessage.error(error.message || '保存映射失败')
  } finally {
    saving.value = false
  }
}

async function removeMapping(row) {
  try {
    await ElMessageBox.confirm('确定删除这个字段映射吗？', '提示', {
      type: 'warning'
    })
  } catch (error) {
    return
  }
  try {
    if (row.id) {
      let taskRules = []
      let transformRules = []
      try {
        taskRules = await listTransformRules({
          taskId: Number(selectedTaskId.value)
        })
      } catch (error) {
        taskRules = []
      }
      try {
        transformRules = await listTransformRules({
          taskId: Number(selectedTaskId.value),
          fieldMappingId: row.id
        })
      } catch (error) {
        transformRules = []
      }
      const allRules = mergeTransformRuleLists(transformRules || [], (taskRules || []).filter(function (rule) {
        return matchesMappingRule(rule, row)
      }))
      for (let i = 0; i < allRules.length; i += 1) {
        if (allRules[i] && allRules[i].id) {
          await deleteTransformRule(allRules[i].id)
        }
      }
    }
    await deleteFieldMapping(row.id)
    ElMessage.success('字段映射已删除')
    await loadMappings(selectedTaskId.value)
    await loadSuggestions(selectedTaskId.value)
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function normalizeSuggestionPayload(row) {
  return {
    id: row.id,
    taskId: Number(selectedTaskId.value),
    sourceSchemaName: row.sourceSchemaName || (currentTask.value ? currentTask.value.sourceSchemaName : ''),
    targetSchemaName: row.targetSchemaName || (currentTask.value ? currentTask.value.targetSchemaName : ''),
    sourceTableName: row.sourceTableName || (currentTask.value ? currentTask.value.sourceTableName : ''),
    targetTableName: row.targetTableName || (currentTask.value ? currentTask.value.targetTableName : ''),
    sourceColumnName: row.sourceColumnName,
    targetColumnName: row.targetColumnName,
    ignored: !!row.ignored,
    defaultValue: row.defaultValue || '',
    transformRule: row.transformRule || ''
  }
}

function mergeSuggestionsWithMappings(recommended, savedMappings) {
  const mappingIndex = {}
  const result = []

  for (let i = 0; i < savedMappings.length; i += 1) {
    mappingIndex[savedMappings[i].sourceColumnName] = savedMappings[i]
  }

  for (let j = 0; j < recommended.length; j += 1) {
    const item = recommended[j] || {}
    const saved = mappingIndex[item.sourceColumnName]
    result.push({
      id: saved ? saved.id : null,
      taskId: selectedTaskId.value,
      sourceSchemaName: saved ? saved.sourceSchemaName : (currentTask.value ? currentTask.value.sourceSchemaName : ''),
      targetSchemaName: saved ? saved.targetSchemaName : (currentTask.value ? currentTask.value.targetSchemaName : ''),
      sourceTableName: saved ? saved.sourceTableName : (currentTask.value ? currentTask.value.sourceTableName : ''),
      targetTableName: saved ? saved.targetTableName : (currentTask.value ? currentTask.value.targetTableName : ''),
      sourceColumnName: item.sourceColumnName,
      targetColumnName: saved ? saved.targetColumnName : (item.targetColumnName || item.sourceColumnName),
      confidence: item.confidence,
      matchReason: item.matchReason,
      ignored: saved ? !!saved.ignored : !!item.ignored,
      defaultValue: saved ? (saved.defaultValue || '') : '',
      transformRule: saved ? (saved.transformRule || '') : '',
      saved: !!saved
    })
    delete mappingIndex[item.sourceColumnName]
  }

  for (const key in mappingIndex) {
    if (Object.prototype.hasOwnProperty.call(mappingIndex, key)) {
      const saved = mappingIndex[key]
      result.push({
        id: saved.id,
        taskId: selectedTaskId.value,
        sourceSchemaName: saved.sourceSchemaName,
        targetSchemaName: saved.targetSchemaName,
        sourceTableName: saved.sourceTableName,
        targetTableName: saved.targetTableName,
        sourceColumnName: saved.sourceColumnName,
        targetColumnName: saved.targetColumnName,
        confidence: null,
        matchReason: '已保存映射',
        ignored: !!saved.ignored,
        defaultValue: saved.defaultValue || '',
        transformRule: saved.transformRule || '',
        saved: true
      })
    }
  }

  return result
}

function normalizeForm() {
  return {
    id: editingId.value,
    taskId: Number(form.taskId),
    sourceSchemaName: form.sourceSchemaName,
    targetSchemaName: form.targetSchemaName,
    sourceTableName: form.sourceTableName,
    targetTableName: form.targetTableName,
    sourceColumnName: form.sourceColumnName,
    targetColumnName: form.targetColumnName,
    ignored: !!form.ignored,
    defaultValue: form.defaultValue,
    transformRule: form.transformRule
  }
}

function openTransformDrawer(row) {
  if (!row || !row.id) {
    ElMessage.warning('请先保存字段映射')
    return
  }
  activeTransformMapping.value = row
  transformDrawerVisible.value = true
}

async function handleTransformRulesSaved(payload) {
  if (!selectedTaskId.value) {
    return
  }
  await loadMappings(selectedTaskId.value)
  await loadSuggestions(selectedTaskId.value)
  if (payload && payload.mappingId && activeTransformMapping.value && activeTransformMapping.value.id === payload.mappingId) {
    activeTransformMapping.value.transformRule = payload.summary || '未配置'
  }
}

async function hydrateTransformRuleSummaries(taskId, mappingRows) {
  if (!taskId || !mappingRows || mappingRows.length === 0) {
    return
  }
  let taskLevelRules = []
  try {
    taskLevelRules = await listTransformRules({
      taskId: taskId
    })
  } catch (error) {
    taskLevelRules = []
  }
  const tasks = []
  for (let i = 0; i < mappingRows.length; i += 1) {
    const row = mappingRows[i]
    if (!row || !row.id) {
      row.transformRule = '未配置'
      continue
    }
    tasks.push(loadTransformRuleSummary(taskId, row, taskLevelRules).catch(function () {
      return {
        row: row,
        value: normalizeTransformRuleSummary(row.transformRule)
      }
    }))
  }
  const summaries = await Promise.all(tasks)
  for (let j = 0; j < summaries.length; j += 1) {
    const summary = summaries[j]
    if (summary && summary.row) {
      summary.row.transformRule = summary.value
    }
  }
}

async function loadTransformRuleSummary(taskId, row, taskLevelRules) {
  const fieldRules = await listTransformRules({
    taskId: taskId,
    fieldMappingId: row.id
  })
  const matchedTaskRules = (taskLevelRules || []).filter(function (rule) {
    return matchesMappingRule(rule, row)
  })
  const merged = mergeTransformRuleLists(fieldRules || [], matchedTaskRules || [])
  return {
    row: row,
    value: normalizeTransformRuleSummary(buildTransformRuleSummaryFromRules(merged))
  }
}

function matchesMappingRule(rule, row) {
  if (!rule || !row) {
    return false
  }
  if (rule.fieldMappingId && row.id && Number(rule.fieldMappingId) === Number(row.id)) {
    return true
  }
  return normalizeText(rule.sourceField) === normalizeText(row.sourceColumnName) &&
    normalizeText(rule.targetField) === normalizeText(row.targetColumnName)
}

function mergeTransformRuleLists(leftRules, rightRules) {
  const seen = {}
  const result = []
  ;(leftRules || []).forEach(function (item) {
    if (item && item.id != null && !seen[item.id]) {
      seen[item.id] = true
      result.push(item)
    }
  })
  ;(rightRules || []).forEach(function (item) {
    if (item && item.id != null && !seen[item.id]) {
      seen[item.id] = true
      result.push(item)
    }
  })
  result.sort(function (left, right) {
    const leftOrder = left && left.transformOrder != null ? Number(left.transformOrder) : 0
    const rightOrder = right && right.transformOrder != null ? Number(right.transformOrder) : 0
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    const leftId = left && left.id != null ? Number(left.id) : 0
    const rightId = right && right.id != null ? Number(right.id) : 0
    return leftId - rightId
  })
  return result
}

function buildTransformRuleSummaryFromRules(rules) {
  if (!rules || rules.length === 0) {
    return '未配置'
  }
  const activeRules = rules.filter(function (item) {
    return item && item.enabled !== false
  })
  if (activeRules.length === 0) {
    return '已停用'
  }
  if (activeRules.length <= 2) {
    return activeRules.map(function (item) {
      return item.transformType
    }).join(' + ')
  }
  return '已配置 ' + rules.length + ' 条'
}

function normalizeTransformRuleSummary(value) {
  if (!value || String(value).trim().length === 0) {
    return '未配置'
  }
  return String(value)
}

function normalizeText(value) {
  return value == null ? '' : String(value).trim().toLowerCase()
}

function confidenceTagType(value) {
  if (typeof value !== 'number') {
    return 'info'
  }
  if (value >= 0.85) {
    return 'success'
  }
  if (value >= 0.6) {
    return 'warning'
  }
  return 'info'
}

function formatConfidence(value) {
  if (typeof value !== 'number') {
    return '-'
  }
  return Math.round(value * 100) + '%'
}

function syncContextToForm() {
  if (!currentTask.value) {
    return
  }
  if (!form.sourceSchemaName) {
    form.sourceSchemaName = currentTask.value.sourceSchemaName || ''
  }
  if (!form.targetSchemaName) {
    form.targetSchemaName = currentTask.value.targetSchemaName || ''
  }
  if (!form.sourceTableName) {
    form.sourceTableName = currentTask.value.sourceTableName || ''
  }
  if (!form.targetTableName) {
    form.targetTableName = currentTask.value.targetTableName || ''
  }
  sourceTableOptions.value = toTableOptions(sourceSchemas.value, form.sourceSchemaName)
  targetTableOptions.value = toTableOptions(targetSchemas.value, form.targetSchemaName)
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function createEmptyForm() {
  return {
    taskId: null,
    sourceSchemaName: '',
    targetSchemaName: '',
    sourceTableName: '',
    targetTableName: '',
    sourceColumnName: '',
    targetColumnName: '',
    ignored: false,
    defaultValue: '',
    transformRule: ''
  }
}

function qualifiedTableName(schemaName, tableName) {
  if (!tableName) {
    return '-'
  }
  if (!schemaName) {
    return tableName
  }
  return schemaName + '.' + tableName
}

function handleSourceSchemaChange() {
  sourceTableOptions.value = toTableOptions(sourceSchemas.value, form.sourceSchemaName)
  if (form.sourceTableName && sourceTableOptions.value.length > 0) {
    const sourceTableMatch = sourceTableOptions.value.some(function (item) {
      return item.value === form.sourceTableName
    })
    if (!sourceTableMatch) {
      form.sourceTableName = ''
    }
  }
}

function handleTargetSchemaChange() {
  targetTableOptions.value = toTableOptions(targetSchemas.value, form.targetSchemaName)
  if (form.targetTableName && targetTableOptions.value.length > 0) {
    const targetTableMatch = targetTableOptions.value.some(function (item) {
      return item.value === form.targetTableName
    })
    if (!targetTableMatch) {
      form.targetTableName = ''
    }
  }
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

function countFields(schemas, schemaName, tableName) {
  if (!schemaName || !tableName) {
    return 0
  }
  const normalizedSchemaName = String(schemaName).toLowerCase()
  const normalizedTableName = String(tableName).toLowerCase()
  const copyFallbackTableName = normalizedTableName.endsWith('_copy') && normalizedTableName.length > 5
    ? normalizedTableName.slice(0, -5)
    : ''
  let fallbackTable = null
  const schema = (schemas || []).find(function (item) {
    return item.schemaName && item.schemaName.toLowerCase() === normalizedSchemaName
  })
  if (schema && schema.tables) {
    const exactTable = schema.tables.find(function (item) {
      return item.tableName && (
        item.tableName.toLowerCase() === normalizedTableName ||
        (copyFallbackTableName && item.tableName.toLowerCase() === copyFallbackTableName)
      )
    })
    if (exactTable && exactTable.columns) {
      return exactTable.columns.length
    }
  }
  ;(schemas || []).forEach(function (item) {
    if (!item || !item.tables) {
      return
    }
    item.tables.forEach(function (table) {
      if (!table || !table.tableName || !table.columns) {
        return
      }
      const tableNameLower = table.tableName.toLowerCase()
      if (tableNameLower !== normalizedTableName && (!copyFallbackTableName || tableNameLower !== copyFallbackTableName)) {
        return
      }
      if (!fallbackTable) {
        fallbackTable = table
      }
    })
  })
  if (!fallbackTable || !fallbackTable.columns) {
    return 0
  }
  return fallbackTable.columns.length
}
</script>
