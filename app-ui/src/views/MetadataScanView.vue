<template>
  <div class="page-section metadata-scan-workbench">
    <div class="page-header metadata-scan-workbench__header">
      <div>
        <h1>表结构扫描</h1>
        <p>选择数据源并扫描 schema、table、columns 和 primary key 信息，用于后续字段映射和同步任务配置。</p>
      </div>
      <el-space>
        <el-select
          v-model="selectedDatasourceId"
          placeholder="选择数据源"
          style="min-width: 280px;"
          @change="handleDatasourceChange"
        >
          <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button type="primary" round :loading="scanning" :disabled="!selectedDatasourceId" @click="runScan">
          扫描结构
        </el-button>
      </el-space>
    </div>

    <div class="metadata-scan-workbench__flow">
      选择数据源 → 扫描结构 → 选择 table → 查看字段详情
    </div>

    <div class="stats-grid">
      <div class="stat-card glass-panel">
        <div class="stat-card__label">Schema 数</div>
        <div class="stat-card__value">{{ schemaCount }}</div>
        <div class="stat-card__hint">当前扫描结果中的 schema 总数</div>
      </div>
      <div class="stat-card glass-panel">
        <div class="stat-card__label">Table 数</div>
        <div class="stat-card__value">{{ tableCount }}</div>
        <div class="stat-card__hint">当前扫描结果中的 table 总数</div>
      </div>
      <div class="stat-card glass-panel">
        <div class="stat-card__label">Column 数</div>
        <div class="stat-card__value">{{ columnCount }}</div>
        <div class="stat-card__hint">所有字段和主键信息已加载</div>
      </div>
      <div class="stat-card glass-panel">
        <div class="stat-card__label">主键</div>
        <div class="stat-card__value">{{ primaryKeyCount }}</div>
        <div class="stat-card__hint">用于后续同步、更新和断点恢复</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact metadata-scan-workbench__panels">
      <div class="panel-card glass-panel metadata-scan-workbench__panel metadata-scan-workbench__panel--left">
        <div class="section-title section-title--compact metadata-scan-workbench__section-head">
          <h2>结构树</h2>
          <el-tag :type="treeStatusTagType" effect="dark">{{ treeStatusLabel }}</el-tag>
        </div>
        <div class="status-item metadata-scan-workbench__status">
          <span class="status-item__label">流程提示</span>
          <span class="status-item__value">{{ selectedDatasourceId ? '已选择数据源，下一步是扫描结构' : '未选择数据源，请先选择连接' }}</span>
        </div>
        <div class="metadata-scan-workbench__tree-shell">
          <el-tree
            v-loading="scanning"
            :data="treeData"
            :props="treeProps"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span>{{ data.label }}</span>
                <el-tag v-if="data.type === 'table'" size="small" type="success">TABLE</el-tag>
                <el-tag v-else-if="data.type === 'schema'" size="small" type="info">SCHEMA</el-tag>
              </span>
            </template>
          </el-tree>
          <StateEmpty
            v-if="!scanning && !treeData.length"
            class="metadata-scan-workbench__empty"
            title="还没有扫描结果"
            description="选择一个数据源并执行表结构扫描后，这里会显示 schema、table 和字段层级。"
            hint="未选择数据源时，请先切换到一个连接；已选择数据源时，可以直接开始扫描结构。"
            :button-text="selectedDatasourceId ? '开始扫描结构' : '去选择数据源'"
            @action="selectedDatasourceId ? runScan() : goToDatasource()"
          />
        </div>
      </div>

      <div class="panel-card glass-panel metadata-scan-workbench__panel metadata-scan-workbench__panel--right">
        <div class="section-title section-title--compact metadata-scan-workbench__section-head">
          <h2>字段详情{{ selectedTableLabel ? ' · ' + selectedTableLabel : '' }}</h2>
          <el-space>
          <el-tag :type="selectedTable ? 'success' : 'info'" effect="dark">{{ selectedTableStateLabel }}</el-tag>
            <el-button
              size="small"
              :disabled="!selectedTable || !selectedDatasourceId"
              :title="!selectedTable ? '请选择一个 table 后再进行结构比较' : ''"
              @click="openSchemaCompare"
            >
              结构比较
            </el-button>
            <el-button
              size="small"
              type="primary"
              :disabled="!selectedTable"
              :title="!selectedTable ? '请选择一个 table 后再进行数据预览' : ''"
              @click="openDataPreview"
            >
              数据预览
            </el-button>
          </el-space>
        </div>
        <div class="status-item metadata-scan-workbench__status">
          <span class="status-item__label">当前状态</span>
          <span class="status-item__value">{{ selectedTable ? '已选择 table，可查看字段详情' : '请选择一个 table' }}</span>
        </div>
        <div class="metadata-scan-workbench__detail-shell">
          <div v-if="selectedTable" class="table-shell">
            <el-table :data="selectedColumns" border stripe>
              <el-table-column prop="name" label="字段名" min-width="180" />
              <el-table-column prop="dataType" label="类型" min-width="140" />
              <el-table-column prop="columnSize" label="长度" width="100" />
              <el-table-column prop="decimalDigits" label="小数位" width="100" />
              <el-table-column label="主键" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.primaryKey ? 'warning' : 'info'">
                    {{ row.primaryKey ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="自增" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.autoIncrement ? 'success' : 'info'">
                    {{ row.autoIncrement ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="可空" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.nullable ? 'success' : 'danger'">
                    {{ row.nullable ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="defaultValue" label="默认值" min-width="160" />
            </el-table>
          </div>
          <StateEmpty
            v-else
            class="metadata-scan-workbench__empty"
            title="请选择一个 table"
            description="从左侧结构树选择 table 后，这里会显示字段名、类型、长度、小数位、主键和默认值。"
            hint="你也可以先扫描结构，再回到左侧树中选择目标表。"
            button-text="开始扫描结构"
            @action="runScan"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDatasources, scanMetadata } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const datasources = ref([])
const selectedDatasourceId = ref(null)
const currentDatasourceName = ref('')
const scanning = ref(false)
const scannedSchemas = ref([])
const selectedTable = ref(null)

const treeProps = {
  children: 'children',
  label: 'label'
}

const treeData = computed(function () {
  return scannedSchemas.value.map(function (schema) {
    return {
      id: 'schema-' + schema.schemaName,
      type: 'schema',
      label: schema.schemaName,
      schemaName: schema.schemaName,
      children: (schema.tables || []).map(function (table) {
        return {
          id: 'table-' + schema.schemaName + '-' + table.tableName,
          type: 'table',
          label: table.tableName,
          schemaName: schema.schemaName,
          tableName: table.tableName,
          columns: table.columns || []
        }
      })
    }
  })
})

const selectedColumns = computed(function () {
  return selectedTable.value ? selectedTable.value.columns || [] : []
})

const selectedTableLabel = computed(function () {
  if (!selectedTable.value) {
    return ''
  }
  return qualifiedTableName(selectedTable.value.schemaName, selectedTable.value.tableName)
})

const selectedTableStateLabel = computed(function () {
  return selectedTable.value ? '已选择 table' : '未选择 table'
})

const treeStatusLabel = computed(function () {
  if (!selectedDatasourceId.value) {
    return '未选择数据源'
  }
  if (!treeData.value.length) {
    return '未扫描'
  }
  return '已扫描'
})

const treeStatusTagType = computed(function () {
  if (!selectedDatasourceId.value) {
    return 'info'
  }
  if (!treeData.value.length) {
    return 'warning'
  }
  return 'success'
})

const schemaCount = computed(function () {
  return scannedSchemas.value.length
})

const tableCount = computed(function () {
  return scannedSchemas.value.reduce(function (count, schema) {
    return count + ((schema.tables || []).length)
  }, 0)
})

const columnCount = computed(function () {
  return scannedSchemas.value.reduce(function (total, schema) {
    return total + (schema.tables || []).reduce(function (tableTotal, table) {
      return tableTotal + ((table.columns || []).length)
    }, 0)
  }, 0)
})

const primaryKeyCount = computed(function () {
  return scannedSchemas.value.reduce(function (total, schema) {
    return total + (schema.tables || []).reduce(function (tableTotal, table) {
      return tableTotal + (table.columns || []).filter(function (column) {
        return column.primaryKey
      }).length
    }, 0)
  }, 0)
})

onMounted(function () {
  loadDatasources()
})

async function loadDatasources() {
  try {
    datasources.value = await listDatasources()
    if (datasources.value.length > 0) {
      selectedDatasourceId.value = datasources.value[0].id
      currentDatasourceName.value = datasources.value[0].name
      await runScan()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据源失败')
  }
}

async function runScan() {
  if (!selectedDatasourceId.value) {
    ElMessage.warning('请先选择一个数据源')
    return
  }
  scanning.value = true
  try {
    scannedSchemas.value = await scanMetadata(selectedDatasourceId.value)
    selectedTable.value = findFirstTable(scannedSchemas.value)
    currentDatasourceName.value = findDatasourceName(selectedDatasourceId.value)
    ElMessage.success('表结构扫描完成')
  } catch (error) {
    ElMessage.error(error.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

function handleDatasourceChange(value) {
  currentDatasourceName.value = findDatasourceName(value)
  scannedSchemas.value = []
  selectedTable.value = null
}

function handleNodeClick(data) {
  if (data.type !== 'table') {
    return
  }
  selectedTable.value = {
    schemaName: data.schemaName,
    tableName: data.tableName,
    columns: data.columns || []
  }
}

function openDataPreview() {
  if (!selectedDatasourceId.value || !selectedTable.value) {
    return
  }
  router.push({
    path: '/preview',
    query: {
      datasourceId: String(selectedDatasourceId.value),
      schemaName: selectedTable.value.schemaName || '',
      tableName: selectedTable.value.tableName || ''
    }
  })
}

function goToDatasource() {
  router.push('/datasource')
}

function openSchemaCompare() {
  if (!selectedDatasourceId.value || !selectedTable.value) {
    return
  }
  router.push({
    path: '/schema-compare',
    query: {
      sourceDatasourceId: String(selectedDatasourceId.value),
      sourceSchemaName: selectedTable.value.schemaName || '',
      sourceTableName: selectedTable.value.tableName || ''
    }
  })
}

function findFirstTable(schemas) {
  for (let i = 0; i < schemas.length; i += 1) {
    const schema = schemas[i]
    if (schema.tables && schema.tables.length > 0) {
      const table = schema.tables[0]
      return {
        schemaName: schema.schemaName,
        tableName: table.tableName,
        columns: table.columns || []
      }
    }
  }
  return null
}

function findDatasourceName(id) {
  const item = datasources.value.find(function (row) {
    return row.id === id
  })
  return item ? item.name : ''
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
</script>

<style scoped>
.metadata-scan-workbench {
  display: grid;
  gap: 18px;
}

.metadata-scan-workbench__flow {
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.6;
}

.metadata-scan-workbench__panels {
  align-items: start;
}

.metadata-scan-workbench__panel {
  min-height: 560px;
}

.metadata-scan-workbench__section-head {
  margin-top: 0;
}

.metadata-scan-workbench__status {
  margin-top: 10px;
}

.metadata-scan-workbench__tree-shell,
.metadata-scan-workbench__detail-shell {
  margin-top: 14px;
}

.metadata-scan-workbench__empty {
  margin-top: 14px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1280px) {
  .metadata-scan-workbench__panel {
    min-height: auto;
  }
}
</style>
