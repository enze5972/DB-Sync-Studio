<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>字段映射</h1>
        <p>按同步任务维护源字段到目标字段的映射、忽略字段和默认值。</p>
      </div>
      <el-space>
        <el-select
          v-model="selectedTaskId"
          placeholder="选择任务"
          style="min-width: 300px;"
          @change="handleTaskChange"
        >
          <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
        </el-select>
        <el-button type="primary" round :disabled="!selectedTaskId" @click="openCreateDialog">
          新建映射
        </el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--two">
      <div class="page-overview__item">
        <div class="page-overview__label">当前任务</div>
        <div class="page-overview__value">{{ selectedTaskName || '未选择' }}</div>
        <div class="page-overview__hint">{{ currentTask ? qualifiedTableName(currentTask.sourceSchemaName, currentTask.sourceTableName) + ' → ' + qualifiedTableName(currentTask.targetSchemaName, currentTask.targetTableName) : '先选任务，再维护映射' }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">映射数量</div>
        <div class="page-overview__value">{{ mappings.length }}</div>
        <div class="page-overview__hint">{{ ignoredMappingCount }} 个已忽略，支持手动确认和修改</div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>智能推荐</h2>
        <el-space>
          <el-tag type="warning" effect="dark">{{ suggestionCount }} 条推荐</el-tag>
          <el-button size="small" :disabled="!selectedTaskId || loadingSuggestions" :loading="loadingSuggestions" @click="loadSuggestions(selectedTaskId)">重新推荐</el-button>
        </el-space>
      </div>
      <div class="table-shell">
        <el-table :data="suggestions" border stripe v-loading="loadingSuggestions">
          <el-table-column prop="sourceColumnName" label="源字段" min-width="180" />
          <el-table-column label="目标字段" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row.targetColumnName" size="small" placeholder="手动修改目标字段" @input="markSuggestionDirty(row)" />
            </template>
          </el-table-column>
          <el-table-column label="置信度" width="120">
            <template #default="{ row }">
              <el-tag :type="confidenceTagType(row.confidence)" effect="light">{{ formatConfidence(row.confidence) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchReason" label="原因" min-width="180">
            <template #default="{ row }">
              <span class="table-muted">{{ row.matchReason || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="忽略" width="110">
            <template #default="{ row }">
              <el-switch v-model="row.ignored" @change="markSuggestionDirty(row)" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.saved ? 'success' : 'info'">{{ row.saved ? '已保存' : '未保存' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-space>
                <el-button size="small" type="primary" @click="acceptSuggestion(row)">采纳推荐</el-button>
                <el-button size="small" @click="saveSuggestion(row)">{{ row.saved ? '更新' : '保存' }}</el-button>
                <el-button size="small" type="danger" :disabled="!row.id" @click="removeMapping(row)">删除</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>映射规则</h2>
          <el-tag type="info" effect="dark">{{ selectedTaskName || '未选择任务' }}</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="mappings" border stripe v-loading="loading">
            <el-table-column prop="sourceColumnName" label="源字段" min-width="180" />
            <el-table-column prop="targetColumnName" label="目标字段" min-width="180" />
            <el-table-column label="忽略" width="100">
              <template #default="{ row }">
                <el-tag :type="row.ignored ? 'warning' : 'success'">
                  {{ row.ignored ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值" min-width="140" />
            <el-table-column prop="transformRule" label="转换规则" min-width="180" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" @click="editMapping(row)">编辑</el-button>
                  <el-button size="small" type="danger" @click="removeMapping(row)">删除</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>当前任务</h2>
          <el-tag type="success" effect="dark">Task Context</el-tag>
        </div>
        <div class="status-stack" v-if="currentTask">
          <div class="status-item">
            <span class="status-item__label">任务名</span>
            <span class="status-item__value">{{ currentTask.taskName }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">源表</span>
            <span class="status-item__value">{{ qualifiedTableName(currentTask.sourceSchemaName, currentTask.sourceTableName) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">目标表</span>
            <span class="status-item__value">{{ qualifiedTableName(currentTask.targetSchemaName, currentTask.targetTableName) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">同步模式</span>
            <span class="status-item__value">{{ syncModeLabel(currentTask.syncMode) }}</span>
          </div>
        </div>
        <div v-else class="status-item">
          <span class="status-item__label">提示</span>
          <span class="status-item__value">先选择一个同步任务，再为该任务配置字段映射。</span>
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
          <el-input
            v-model="form.transformRule"
            type="textarea"
            :rows="4"
            placeholder="例如：trim、toUpperCase、日期格式转换等"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="submitMapping">保存</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFieldMapping, listFieldMappings, listTasks, saveFieldMapping, scanMetadata, suggestFieldMappings } from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'

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
    mappings.value = await listFieldMappings(taskId)
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

function openCreateDialog() {
  if (!selectedTaskId.value) {
    ElMessage.warning('请先选择一个任务')
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
</script>
