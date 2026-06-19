<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>表结构比较</h1>
        <p>选择源表和目标表，查看字段、类型、长度、主键和索引差异，并生成修复 SQL。</p>
      </div>
      <el-space>
        <el-button round @click="loadDatasources">刷新数据源</el-button>
        <el-button type="primary" round :loading="comparing" @click="runCompare">开始比较</el-button>
      </el-space>
    </div>

    <div class="page-overview">
      <div class="page-overview__item">
        <div class="page-overview__label">差异项</div>
        <div class="page-overview__value">{{ diffCount }}</div>
        <div class="page-overview__hint">显示字段、类型、长度、主键和索引差异</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">SQL 预览</div>
        <div class="page-overview__value">{{ suggestedSqlList.length }} 条</div>
        <div class="page-overview__hint">只生成建议，执行前必须确认</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">执行状态</div>
        <div class="page-overview__value">{{ previewMessage || '等待比较' }}</div>
        <div class="page-overview__hint">支持 MySQL / PostgreSQL / DM 方言</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>比较配置</h2>
          <el-tag type="info" effect="dark">DDL</el-tag>
        </div>
        <el-form label-width="120px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="源数据源">
                <el-select v-model="form.sourceDatasourceId" style="width: 100%;" @change="handleSourceDatasourceChange">
                  <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="目标数据源">
                <el-select v-model="form.targetDatasourceId" style="width: 100%;" @change="handleTargetDatasourceChange">
                  <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="源 schema">
                <CreatableSelect v-model="form.sourceSchemaName" :options="sourceSchemaOptions" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="目标 schema">
                <CreatableSelect v-model="form.targetSchemaName" :options="targetSchemaOptions" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="源表名">
                <CreatableSelect v-model="form.sourceTableName" :options="sourceTableOptions" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="目标表名">
                <CreatableSelect v-model="form.targetTableName" :options="targetTableOptions" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>差异结果</h2>
          <el-tag type="success" effect="dark">{{ diffCount }} 项</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="result.diffEntries" border stripe v-loading="comparing">
            <el-table-column prop="diffType" label="类型" width="160" />
            <el-table-column prop="description" label="描述" min-width="280" />
            <el-table-column prop="sourceColumnName" label="源字段" min-width="160" />
            <el-table-column prop="targetColumnName" label="目标字段" min-width="160" />
            <el-table-column label="建议" width="120">
              <template #default="{ row }">
                <el-tag :type="diffTagType(row.diffType)" effect="dark">
                  {{ diffTypeLabel(row.diffType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="SQL" min-width="360" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.suggestedSql || '-' }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>SQL 预览</h2>
        <el-space>
          <el-button :disabled="suggestedSqlList.length === 0" @click="copySql">复制 SQL</el-button>
          <el-button type="warning" :disabled="suggestedSqlList.length === 0" :loading="executing" @click="executeSql">
            确认执行
          </el-button>
        </el-space>
      </div>
      <el-input v-model="sqlText" type="textarea" :rows="8" />
      <div class="status-stack" style="margin-top: 16px;">
        <div class="status-item">
          <span class="status-item__label">预览状态</span>
          <span class="status-item__value">{{ previewMessage || '等待比较' }}</span>
        </div>
        <div class="status-item">
          <span class="status-item__label">执行规则</span>
          <span class="status-item__value">仅生成 SQL，执行前必须确认，不自动删除目标字段</span>
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>比较历史</h2>
        <el-tag type="info" effect="dark">{{ historyEntries.length }} 条</el-tag>
      </div>
      <div class="table-shell">
        <el-table :data="historyEntries" border stripe v-loading="historyLoading">
          <el-table-column label="时间" width="200">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="源表" min-width="220">
            <template #default="{ row }">
              {{ formatQualifiedTable(row.sourceSchemaName, row.sourceTableName) }}
            </template>
          </el-table-column>
          <el-table-column label="目标表" min-width="220">
            <template #default="{ row }">
              {{ formatQualifiedTable(row.targetSchemaName, row.targetTableName) }}
            </template>
          </el-table-column>
          <el-table-column label="摘要" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatHistorySummary(row.diffSummary) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  compareSchema,
  executeSchemaSql,
  listDatasources,
  listSchemaComparisonHistory,
  scanMetadata,
  previewSchemaSql
} from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'

const datasources = ref([])
const comparing = ref(false)
const executing = ref(false)
const historyLoading = ref(false)
const previewMessage = ref('')
const historyEntries = ref([])
const sourceSchemaOptions = ref([])
const targetSchemaOptions = ref([])
const sourceTableOptions = ref([])
const targetTableOptions = ref([])
const result = reactive({
  diffEntries: [],
  suggestedSqlList: []
})
const form = reactive({
  sourceDatasourceId: null,
  targetDatasourceId: null,
  sourceSchemaName: '',
  targetSchemaName: '',
  sourceTableName: '',
  targetTableName: ''
})

const sqlText = computed(function () {
  return result.suggestedSqlList.join(';\n')
})

const diffCount = computed(function () {
  return result.diffEntries.length
})

const suggestedSqlList = computed(function () {
  return result.suggestedSqlList
})

onMounted(function () {
  loadDatasources()
  loadComparisonHistory()
})

async function loadDatasources() {
  try {
    datasources.value = await listDatasources()
    if (datasources.value.length > 0 && !form.sourceDatasourceId) {
      form.sourceDatasourceId = datasources.value[0].id
    }
    if (datasources.value.length > 1 && !form.targetDatasourceId) {
      form.targetDatasourceId = datasources.value[1].id
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据源失败')
  }
}

async function handleSourceDatasourceChange(value) {
  form.sourceDatasourceId = value
  sourceSchemaOptions.value = []
  sourceTableOptions.value = []
  await loadMetadataOptions('source')
}

async function handleTargetDatasourceChange(value) {
  form.targetDatasourceId = value
  targetSchemaOptions.value = []
  targetTableOptions.value = []
  await loadMetadataOptions('target')
}

async function loadComparisonHistory() {
  historyLoading.value = true
  try {
    historyEntries.value = await listSchemaComparisonHistory(20)
  } catch (error) {
    ElMessage.error(error.message || '加载比较历史失败')
  } finally {
    historyLoading.value = false
  }
}

async function loadMetadataOptions(side) {
  const datasourceId = side === 'target' ? form.targetDatasourceId : form.sourceDatasourceId
  if (!datasourceId) {
    return
  }
  try {
    const schemas = await scanMetadata(datasourceId)
    const schemaNames = (schemas || []).map(function (schema) {
      return schema.schemaName
    })
    const tableNames = (schemas || []).reduce(function (acc, schema) {
      const tables = schema.tables || []
      for (let i = 0; i < tables.length; i += 1) {
        acc.push(tables[i].tableName)
      }
      return acc
    }, [])
    if (side === 'target') {
      targetSchemaOptions.value = schemaNames
      targetTableOptions.value = tableNames
    } else {
      sourceSchemaOptions.value = schemaNames
      sourceTableOptions.value = tableNames
    }
  } catch (error) {
    if (side === 'target') {
      targetSchemaOptions.value = []
      targetTableOptions.value = []
    } else {
      sourceSchemaOptions.value = []
      sourceTableOptions.value = []
    }
    ElMessage.error(error.message || '加载结构失败')
  }
}

async function runCompare() {
  if (!form.sourceDatasourceId || !form.targetDatasourceId) {
    ElMessage.warning('请先选择源和目标数据源')
    return
  }
  comparing.value = true
  try {
    const response = await compareSchema(form)
    result.diffEntries = response.diffEntries || []
    result.suggestedSqlList = response.suggestedSqlList || []
    await loadComparisonHistory()
    const targetDatasource = findTargetDatasource()
    if (targetDatasource) {
      const preview = await previewSchemaSql({
        datasource: targetDatasource,
        sql: result.suggestedSqlList.join(';\n'),
        allowDangerousSql: true
      })
      previewMessage.value = preview.message
    } else {
      previewMessage.value = '请先选择目标数据源'
    }
    ElMessage.success('比较完成')
  } catch (error) {
    ElMessage.error(error.message || '比较失败')
  } finally {
    comparing.value = false
  }
}

function findTargetDatasource() {
  return datasources.value.find(function (item) {
    return item.id === form.targetDatasourceId
  }) || null
}

async function copySql() {
  try {
    await navigator.clipboard.writeText(sqlText.value)
    ElMessage.success('已复制 SQL')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

async function executeSql() {
  if (!sqlText.value) {
    return
  }
  try {
    await ElMessageBox.confirm('确认执行这组 DDL SQL 吗？', '执行确认', {
      type: 'warning'
    })
  } catch (error) {
    return
  }
  const targetDatasource = findTargetDatasource()
  if (!targetDatasource) {
    ElMessage.warning('请选择目标数据源')
    return
  }
  executing.value = true
  try {
    await executeSchemaSql({
      datasource: targetDatasource,
      sql: sqlText.value,
      allowDangerousSql: true
    })
    ElMessage.success('DDL 执行成功')
  } catch (error) {
    ElMessage.error(error.message || 'DDL 执行失败')
  } finally {
    executing.value = false
  }
}

function diffTagType(diffType) {
  if (diffType === 'MISSING_COLUMN') {
    return 'warning'
  }
  if (diffType === 'EXTRA_COLUMN') {
    return 'info'
  }
  return 'danger'
}

function diffTypeLabel(diffType) {
  if (diffType === 'MISSING_COLUMN') {
    return '新增字段'
  }
  if (diffType === 'EXTRA_COLUMN') {
    return '多余字段'
  }
  if (diffType === 'TYPE_DIFF') {
    return '类型差异'
  }
  if (diffType === 'LENGTH_DIFF') {
    return '长度差异'
  }
  if (diffType === 'NULLABLE_DIFF') {
    return '空值差异'
  }
  if (diffType === 'DEFAULT_DIFF') {
    return '默认值差异'
  }
  if (diffType === 'PRIMARY_KEY_DIFF') {
    return '主键差异'
  }
  if (diffType === 'INDEX_DIFF') {
    return '索引差异'
  }
  return diffType || '-'
}

function formatQualifiedTable(schemaName, tableName) {
  const prefix = schemaName ? schemaName + '.' : ''
  return prefix + (tableName || '-')
}

function formatHistorySummary(diffSummary) {
  if (diffSummary === null || diffSummary === undefined || diffSummary === '') {
    return '-'
  }
  return diffSummary + ' 项差异'
}
</script>
