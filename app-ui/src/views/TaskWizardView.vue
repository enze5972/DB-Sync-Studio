<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>创建同步任务向导</h1>
        <p>按步骤选择数据源、表和同步模式，自动生成字段映射并保存到本地 SQLite。</p>
      </div>
      <el-space>
        <el-button round @click="goBack">返回任务列表</el-button>
        <el-button type="primary" round :loading="saving" @click="saveWizard">保存任务</el-button>
      </el-space>
    </div>

    <div class="panel-card glass-panel">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="选择源数据源" />
        <el-step title="选择目标数据源" />
        <el-step title="选择 schema / table" />
        <el-step title="生成字段映射" />
        <el-step title="同步模式" />
        <el-step title="确认保存" />
      </el-steps>
      <div class="wizard-step-note">
        <strong>{{ currentStepMeta.title }}：</strong>{{ currentStepMeta.hint }}
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <template v-if="activeStep === 0">
          <div class="section-title">
            <h2>源数据源</h2>
            <el-tag type="info" effect="dark">Step 1</el-tag>
          </div>
          <el-select v-model="form.sourceDatasourceId" placeholder="请选择源数据源" style="width: 100%;" @change="handleSourceDatasourceChange">
            <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </template>

        <template v-else-if="activeStep === 1">
          <div class="section-title">
            <h2>目标数据源</h2>
            <el-tag type="info" effect="dark">Step 2</el-tag>
          </div>
          <el-select v-model="form.targetDatasourceId" placeholder="请选择目标数据源" style="width: 100%;" @change="handleTargetDatasourceChange">
            <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </template>

        <template v-else-if="activeStep === 2">
          <div class="section-title">
            <h2>选择表</h2>
            <el-tag type="success" effect="dark">Step 3</el-tag>
          </div>
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="wizard-card">
                <h3>源表</h3>
                <CreatableSelect
                  v-model="form.sourceSchemaName"
                  :options="sourceSchemaOptions"
                  placeholder="Schema"
                  style="width: 100%; margin-bottom: 12px;"
                  @change="handleSourceSchemaChange"
                />
                <CreatableSelect
                  v-model="form.sourceTableName"
                  :options="sourceTableOptions"
                  placeholder="Table"
                  style="width: 100%;"
                  @change="generateMappings"
                />
              </div>
            </el-col>
            <el-col :span="12">
              <div class="wizard-card">
                <h3>目标表</h3>
                <CreatableSelect
                  v-model="form.targetSchemaName"
                  :options="targetSchemaOptions"
                  placeholder="Schema"
                  style="width: 100%; margin-bottom: 12px;"
                  @change="handleTargetSchemaChange"
                />
                <CreatableSelect
                  v-model="form.targetTableName"
                  :options="targetTableOptions"
                  placeholder="Table"
                  style="width: 100%;"
                  @change="generateMappings"
                />
              </div>
            </el-col>
          </el-row>
        </template>

        <template v-else-if="activeStep === 3">
          <div class="section-title">
            <h2>字段映射</h2>
            <el-tag type="success" effect="dark">Step 4</el-tag>
          </div>
          <div class="table-shell">
            <el-table :data="mappings" border stripe>
              <el-table-column prop="sourceColumnName" label="源字段" min-width="180" />
              <el-table-column label="目标字段" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.targetColumnName" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="置信度" width="110">
                <template #default="{ row }">
                  {{ formatConfidence(row.confidence) }}
                </template>
              </el-table-column>
              <el-table-column label="原因" min-width="130">
                <template #default="{ row }">
                  {{ row.matchReason || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="ignored" label="忽略" width="100">
                <template #default="{ row }">
                  <el-switch v-model="row.ignored" />
                </template>
              </el-table-column>
              <el-table-column prop="defaultValue" label="默认值" min-width="160" />
            </el-table>
          </div>
        </template>

        <template v-else-if="activeStep === 4">
          <div class="section-title">
            <h2>同步模式</h2>
            <el-tag type="warning" effect="dark">Step 5</el-tag>
          </div>
          <el-radio-group v-model="form.syncMode">
            <el-radio label="FULL">全量同步</el-radio>
            <el-radio label="INCREMENTAL">增量同步</el-radio>
          </el-radio-group>

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
        </template>

        <template v-else>
          <div class="section-title">
            <h2>确认保存</h2>
            <el-tag type="success" effect="dark">Step 6</el-tag>
          </div>
          <div class="status-stack">
            <div class="status-item">
              <span class="status-item__label">任务名</span>
              <span class="status-item__value">{{ form.taskName }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">源表</span>
              <span class="status-item__value">{{ qualifiedTableName(form.sourceSchemaName, form.sourceTableName) }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">目标表</span>
              <span class="status-item__value">{{ qualifiedTableName(form.targetSchemaName, form.targetTableName) }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">映射条数</span>
              <span class="status-item__value">{{ mappings.length }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">调度状态</span>
              <span class="status-item__value">{{ scheduleSummary(form) }}</span>
            </div>
          </div>
        </template>

        <div class="wizard-actions">
          <el-button :disabled="activeStep === 0" @click="previousStep">上一步</el-button>
          <el-button v-if="activeStep < 5" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-else type="primary" :loading="saving" @click="saveWizard">保存任务</el-button>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>任务预览</h2>
          <el-tag type="info" effect="dark">Wizard</el-tag>
        </div>
        <div class="wizard-preview-summary">
          <div class="mini-summary">
            <div class="mini-summary__label">当前步骤</div>
            <div class="mini-summary__value">{{ currentStepMeta.title }}</div>
            <div class="mini-summary__hint">{{ currentStepMeta.hint }}</div>
          </div>
          <div class="mini-summary">
            <div class="mini-summary__label">映射策略</div>
            <div class="mini-summary__value">智能推荐</div>
            <div class="mini-summary__hint">字段名归一化 + 常见别名 + 相似度补位</div>
          </div>
        </div>
        <div class="status-stack">
          <div class="status-item">
            <span class="status-item__label">源数据源</span>
            <span class="status-item__value">{{ sourceDatasourceName || '未选择' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">目标数据源</span>
            <span class="status-item__value">{{ targetDatasourceName || '未选择' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">同步模式</span>
            <span class="status-item__value">{{ syncModeLabel(form.syncMode) }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">生成规则</span>
            <span class="status-item__value">字段名归一化 + 常见别名 + 相似度匹配</span>
          </div>
        </div>

        <div class="table-shell">
          <el-table :data="mappings.slice(0, 8)" border stripe>
            <el-table-column prop="sourceColumnName" label="源字段" min-width="150" />
            <el-table-column prop="targetColumnName" label="目标字段" min-width="150" />
            <el-table-column label="置信度" width="100">
              <template #default="{ row }">
                {{ formatConfidence(row.confidence) }}
              </template>
            </el-table-column>
            <el-table-column prop="ignored" label="忽略" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.ignored" />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDatasources, saveFieldMapping, saveTask, scanMetadata, suggestFieldMappings } from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'

const router = useRouter()
  const route = useRoute()

const datasources = ref([])
const sourceSchemas = ref([])
const targetSchemas = ref([])
const sourceSchemaOptions = ref([])
const targetSchemaOptions = ref([])
const sourceTableOptions = ref([])
const targetTableOptions = ref([])
const sourceDatasourceName = ref('')
const targetDatasourceName = ref('')
const saving = ref(false)
const activeStep = ref(0)
const mappings = ref([])
const stepMeta = [
  {
    title: '选择源数据源',
    hint: '先确定从哪里读取表结构和数据，后续的 schema、表和字段都会跟随它展开。'
  },
  {
    title: '选择目标数据源',
    hint: '目标库决定写入位置，也会影响后续 SQL 方言和字段映射建议。'
  },
  {
    title: '选择 schema / table',
    hint: '先锁定源表与目标表，向导会据此加载字段并生成映射。'
  },
  {
    title: '生成字段映射',
    hint: '这里可以人工确认、改名或忽略系统推荐的映射关系。'
  },
  {
    title: '同步模式',
    hint: '可选择全量或增量，同步任务的调度配置也在这一段完成。'
  },
  {
    title: '确认保存',
    hint: '最后检查任务名、表名、映射数量和调度配置，再一次性保存。'
  }
]

const form = reactive({
  taskName: '',
  sourceDatasourceId: null,
  targetDatasourceId: null,
  sourceSchemaName: '',
  sourceTableName: '',
  targetSchemaName: '',
  targetTableName: '',
  syncMode: 'FULL',
  incrementalMode: 'NONE',
  incrementalColumnName: '',
  incrementalTieBreakerColumnName: '',
  incrementalCompositeColumnName: '',
  scheduleEnabled: false,
  scheduleType: 'MANUAL',
  scheduleCronExpression: '',
  scheduleIntervalSeconds: 300
})

const currentStepMeta = computed(function () {
  return stepMeta[activeStep.value] || stepMeta[0]
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    datasources.value = await listDatasources()
    applyQueryDefaults()
    if (form.sourceDatasourceId) {
      await loadSourceMetadata()
    }
    if (form.targetDatasourceId) {
      await loadTargetMetadata()
    }
    if (form.sourceTableName && form.targetTableName) {
      await generateMappings()
    }
    updatePreview()
  } catch (error) {
    ElMessage.error(error.message || '加载向导数据失败')
  }
}

function applyQueryDefaults() {
  if (route.query.sourceDatasourceId) {
    form.sourceDatasourceId = Number(route.query.sourceDatasourceId)
  }
  if (route.query.targetDatasourceId) {
    form.targetDatasourceId = Number(route.query.targetDatasourceId)
  }
  if (route.query.schemaName) {
    form.sourceSchemaName = String(route.query.schemaName)
  }
  if (route.query.tableName) {
    form.sourceTableName = String(route.query.tableName)
    if (!form.targetTableName) {
      form.targetTableName = String(route.query.tableName)
    }
  }
}

async function handleSourceDatasourceChange(value) {
  sourceDatasourceName.value = datasourceName(value)
  form.sourceSchemaName = ''
  form.sourceTableName = ''
  sourceSchemas.value = []
  mappings.value = []
  await loadSourceMetadata()
  await generateMappings()
}

async function handleTargetDatasourceChange(value) {
  targetDatasourceName.value = datasourceName(value)
  form.targetSchemaName = ''
  form.targetTableName = ''
  targetSchemas.value = []
  mappings.value = []
  await loadTargetMetadata()
  await generateMappings()
}

async function loadSourceMetadata() {
  if (!form.sourceDatasourceId) {
    return
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === form.sourceDatasourceId
  })
  if (!datasource) {
    return
  }
  sourceDatasourceName.value = datasource.name
  sourceSchemas.value = await scanMetadata(datasource.id)
  sourceSchemaOptions.value = (sourceSchemas.value || []).map(function (schema) {
    return schema.schemaName
  })
  if (!form.sourceSchemaName && sourceSchemas.value.length > 0) {
    form.sourceSchemaName = sourceSchemas.value[0].schemaName
  }
  syncSourceTableDefault()
}

async function loadTargetMetadata() {
  if (!form.targetDatasourceId) {
    return
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === form.targetDatasourceId
  })
  if (!datasource) {
    return
  }
  targetDatasourceName.value = datasource.name
  targetSchemas.value = await scanMetadata(datasource.id)
  targetSchemaOptions.value = (targetSchemas.value || []).map(function (schema) {
    return schema.schemaName
  })
  if (!form.targetSchemaName && targetSchemas.value.length > 0) {
    form.targetSchemaName = targetSchemas.value[0].schemaName
  }
  syncTargetTableDefault()
}

function handleSourceSchemaChange() {
  form.sourceTableName = ''
  syncSourceTableDefault()
  generateMappings()
}

function handleTargetSchemaChange() {
  form.targetTableName = ''
  syncTargetTableDefault()
  generateMappings()
}

function syncSourceTableDefault() {
  const tables = sourceTables.value
  sourceTableOptions.value = tables.map(function (table) {
    return table.tableName
  })
  if (!form.sourceTableName && tables.length > 0) {
    form.sourceTableName = tables[0].tableName
  }
}

function syncTargetTableDefault() {
  const tables = targetTables.value
  targetTableOptions.value = tables.map(function (table) {
    return table.tableName
  })
  if (!form.targetTableName && tables.length > 0) {
    form.targetTableName = tables[0].tableName
  }
}

const sourceTables = computed(function () {
  const schema = sourceSchemas.value.find(function (item) {
    return item.schemaName && form.sourceSchemaName && item.schemaName.toLowerCase() === form.sourceSchemaName.toLowerCase()
  })
  return schema ? (schema.tables || []) : []
})

const targetTables = computed(function () {
  const schema = targetSchemas.value.find(function (item) {
    return item.schemaName && form.targetSchemaName && item.schemaName.toLowerCase() === form.targetSchemaName.toLowerCase()
  })
  return schema ? (schema.tables || []) : []
})

async function generateMappings() {
  const sourceTable = findTable(sourceSchemas.value, form.sourceSchemaName, form.sourceTableName)
  const targetTable = findTable(targetSchemas.value, form.targetSchemaName, form.targetTableName)
  if (!sourceTable || !targetTable) {
    mappings.value = []
    return
  }
  try {
    const suggestions = await suggestFieldMappings(selectedTaskId.value || 0)
    if (suggestions && suggestions.length > 0) {
      mappings.value = suggestions.map(function (item) {
        return {
          sourceTableName: form.sourceTableName,
          targetTableName: form.targetTableName,
          sourceColumnName: item.sourceColumnName,
          targetColumnName: item.targetColumnName || item.sourceColumnName,
          confidence: item.confidence,
          matchReason: item.matchReason,
          ignored: !!item.ignored,
          defaultValue: '',
          transformRule: ''
        }
      })
    } else {
      mappings.value = buildFallbackMappings(sourceTable, targetTable)
    }
  } catch (error) {
    mappings.value = buildFallbackMappings(sourceTable, targetTable)
  }
  updatePreview()
}

function buildFallbackMappings(sourceTable, targetTable) {
  const targetColumns = targetTable.columns || []
  return (sourceTable.columns || []).map(function (column) {
    const match = findMatchingColumn(targetColumns, column.name)
    return {
      sourceTableName: form.sourceTableName,
      targetTableName: form.targetTableName,
      sourceColumnName: column.name,
      targetColumnName: match ? match.name : column.name,
      confidence: match ? 1 : 0,
      matchReason: match ? 'exact' : 'none',
      ignored: !match,
      defaultValue: '',
      transformRule: ''
    }
  })
}

function findTable(schemas, schemaName, tableName) {
  const schema = schemas.find(function (item) {
    return item.schemaName && schemaName && item.schemaName.toLowerCase() === schemaName.toLowerCase()
  })
  if (!schema || !schema.tables) {
    return null
  }
  return schema.tables.find(function (item) {
    return item.tableName && tableName && item.tableName.toLowerCase() === tableName.toLowerCase()
  }) || null
}

function findMatchingColumn(columns, sourceColumnName) {
  const exact = columns.find(function (column) {
    return column.name === sourceColumnName
  })
  if (exact) {
    return exact
  }
  return columns.find(function (column) {
    return column.name && sourceColumnName && column.name.toLowerCase() === sourceColumnName.toLowerCase()
  }) || null
}

function formatConfidence(value) {
  if (typeof value !== 'number') {
    return '-'
  }
  return Math.round(value * 100) + '%'
}

function updatePreview() {
  form.taskName = defaultTaskName()
}

function defaultTaskName() {
  if (form.sourceTableName && form.targetTableName) {
    return form.sourceTableName + ' -> ' + form.targetTableName
  }
  return '新建同步任务'
}

function nextStep() {
  if (activeStep.value === 0 && !form.sourceDatasourceId) {
    ElMessage.warning('请先选择源数据源')
    return
  }
  if (activeStep.value === 1 && !form.targetDatasourceId) {
    ElMessage.warning('请先选择目标数据源')
    return
  }
  if (activeStep.value === 2) {
    if (!form.sourceTableName || !form.targetTableName) {
      ElMessage.warning('请选择源表和目标表')
      return
    }
    generateMappings()
  }
  if (activeStep.value < 5) {
    activeStep.value += 1
  }
}

function previousStep() {
  if (activeStep.value > 0) {
    activeStep.value -= 1
  }
}

async function saveWizard() {
  if (!form.sourceDatasourceId || !form.targetDatasourceId || !form.sourceTableName || !form.targetTableName) {
    ElMessage.warning('请补全向导信息')
    return
  }
  saving.value = true
  try {
    const task = await saveTask({
      taskName: form.taskName || defaultTaskName(),
      sourceDatasourceId: Number(form.sourceDatasourceId),
      targetDatasourceId: Number(form.targetDatasourceId),
      sourceSchemaName: form.sourceSchemaName,
      sourceTableName: form.sourceTableName,
      targetSchemaName: form.targetSchemaName,
      targetTableName: form.targetTableName,
      syncMode: form.syncMode,
      taskStatus: 'PENDING',
      incrementalMode: form.incrementalMode,
      incrementalColumnName: form.incrementalColumnName,
      incrementalTieBreakerColumnName: form.incrementalTieBreakerColumnName,
      incrementalCompositeColumnName: form.incrementalCompositeColumnName,
      scheduleEnabled: form.scheduleEnabled,
      scheduleType: form.scheduleType,
      scheduleCronExpression: form.scheduleCronExpression,
      scheduleIntervalSeconds: form.scheduleIntervalSeconds
    })
    for (let i = 0; i < mappings.value.length; i += 1) {
      const mapping = mappings.value[i]
      await saveFieldMapping({
        taskId: task.id,
        sourceTableName: mapping.sourceTableName,
        targetTableName: mapping.targetTableName,
        sourceColumnName: mapping.sourceColumnName,
        targetColumnName: mapping.targetColumnName,
        ignored: mapping.ignored,
        defaultValue: mapping.defaultValue,
        transformRule: mapping.transformRule
      })
    }
    ElMessage.success('任务已保存')
    router.push('/tasks')
  } catch (error) {
    ElMessage.error(error.message || '保存任务失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/tasks')
}

function datasourceName(id) {
  const datasource = datasources.value.find(function (item) {
    return item.id === id
  })
  return datasource ? datasource.name : ''
}

function qualifiedTableName(schemaName, tableName) {
  if (!tableName) {
    return '-'
  }
  return schemaName ? schemaName + '.' + tableName : tableName
}

function syncModeLabel(value) {
  if (value === 'INCREMENTAL') {
    return '增量同步'
  }
  return '全量同步'
}

function scheduleSummary(task) {
  if (!task || !task.scheduleEnabled) {
    return '未启用'
  }
  if (task.scheduleType === 'CRON') {
    return 'Cron'
  }
  if (task.scheduleType === 'INTERVAL') {
    return '间隔 ' + (task.scheduleIntervalSeconds || 0) + ' 秒'
  }
  return '手动'
}
</script>
