<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>数据预览</h1>
        <p>查看指定数据源中某张表的前 100 条记录，支持分页、过滤和排序。</p>
      </div>
      <el-space>
        <el-button round @click="goBack">返回表结构</el-button>
        <el-button type="primary" round :loading="loading" @click="runPreview">查询预览</el-button>
      </el-space>
    </div>

    <div class="page-overview">
      <div class="page-overview__item">
        <div class="page-overview__label">当前数据源</div>
        <div class="page-overview__value">{{ selectedDatasourceName || '未选择' }}</div>
        <div class="page-overview__hint">预览前先选择连接和表名</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">当前结果</div>
        <div class="page-overview__value">{{ result.totalRowCount }} 行</div>
        <div class="page-overview__hint">{{ result.columns.length }} 个字段，支持分页查看</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">排序条件</div>
        <div class="page-overview__value">{{ form.sortColumn || '默认主键/首列' }}</div>
        <div class="page-overview__hint">{{ form.sortDirection === 'DESC' ? '降序' : '升序' }}</div>
      </div>
    </div>

    <div v-if="!hasQueried && !loading" class="panel-card glass-panel preview-intro">
      <div class="status-item">
        <span class="status-item__label">提示</span>
        <span class="status-item__value">先选择数据源和表名，再点击“查询预览”查看前 100 条记录</span>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>查询条件</h2>
          <el-tag type="info" effect="dark">{{ selectedDatasourceName || '请选择数据源' }}</el-tag>
        </div>

        <el-form :model="form" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="数据源">
                <el-select v-model="form.datasourceId" placeholder="请选择数据源" style="width: 100%;" @change="handleDatasourceChange">
                  <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Schema">
                <CreatableSelect v-model="form.schemaName" :options="schemaOptions" placeholder="可选" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="表名">
                <CreatableSelect v-model="form.tableName" :options="tableOptions" placeholder="可输入或选择" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="页码">
                <el-input-number v-model="form.pageNumber" :min="1" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="页大小">
                <el-select v-model="form.pageSize" style="width: 100%;">
                  <el-option v-for="size in pageSizeOptions" :key="size" :label="String(size)" :value="size" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label=" ">
                <span class="sql-hint">建议 20 - 50，便于快速扫读</span>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="排序字段">
                <el-select v-model="form.sortColumn" filterable clearable placeholder="默认使用主键或首列" style="width: 100%;">
                  <el-option v-for="column in availableColumns" :key="column" :label="column" :value="column" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序方式">
                <el-select v-model="form.sortDirection" style="width: 100%;">
                  <el-option label="升序" value="ASC" />
                  <el-option label="降序" value="DESC" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="section-title section-title--compact">
          <h2>过滤条件</h2>
          <el-button size="small" @click="addFilter">新增条件</el-button>
        </div>

        <div class="preview-filters">
          <div v-for="(filter, index) in form.filters" :key="index" class="preview-filter-row">
            <el-select v-model="filter.columnName" filterable placeholder="字段名" style="min-width: 180px;">
              <el-option v-for="column in availableColumns" :key="column" :label="column" :value="column" />
            </el-select>
            <el-select v-model="filter.operator" style="width: 150px;">
              <el-option label="等于" value="EQ" />
              <el-option label="不等于" value="NE" />
              <el-option label="大于" value="GT" />
              <el-option label="大于等于" value="GTE" />
              <el-option label="小于" value="LT" />
              <el-option label="小于等于" value="LTE" />
              <el-option label="包含" value="LIKE" />
              <el-option label="为空" value="IS_NULL" />
              <el-option label="非空" value="IS_NOT_NULL" />
            </el-select>
            <el-input v-model="filter.value" placeholder="条件值" />
            <el-button type="danger" plain @click="removeFilter(index)">删除</el-button>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>结果</h2>
          <el-tag type="success" effect="dark">
            共 {{ result.totalRowCount }} 行
          </el-tag>
        </div>
        <div class="status-stack preview-summary">
          <div class="status-item">
            <span class="status-item__label">当前页</span>
            <span class="status-item__value">{{ result.pageNumber || form.pageNumber }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">页大小</span>
            <span class="status-item__value">{{ result.pageSize || form.pageSize }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">字段数</span>
            <span class="status-item__value">{{ result.columns.length }}</span>
          </div>
        </div>

        <div class="table-shell" v-loading="loading">
          <el-skeleton v-if="loading && !hasQueried" :rows="6" animated />
          <el-alert
            v-else-if="previewError"
            :title="previewError"
            type="error"
            show-icon
            :closable="false"
          />
          <el-empty
            v-else-if="!hasQueried"
            description="请选择数据源和表名后查询预览"
          />
          <el-empty
            v-else-if="!result.columns.length || !result.rows.length"
            description="当前查询没有返回记录"
          />
          <el-table v-else :data="result.rows" border stripe>
            <el-table-column
              v-for="column in result.columns"
              :key="column"
              :prop="column"
              :label="column"
              min-width="160"
              show-overflow-tooltip
            />
          </el-table>
        </div>

        <div class="preview-pagination">
          <el-pagination
            layout="total, prev, pager, next, sizes"
            :total="result.totalRowCount"
            :current-page="form.pageNumber"
            :page-size="form.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listDatasources, previewTableData, scanMetadata } from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const hasQueried = ref(false)
const previewError = ref('')
const autoPreviewTriggered = ref(false)
const datasources = ref([])
const selectedDatasourceName = ref('')
const availableColumns = ref([])
const schemaOptions = ref([])
const tableOptions = ref([])
const result = reactive({
  columns: [],
  rows: [],
  totalRowCount: 0,
  pageNumber: 1,
  pageSize: 100
})
const pageSizeOptions = [10, 20, 50, 100]

const form = reactive({
  datasourceId: null,
  schemaName: '',
  tableName: '',
  pageNumber: 1,
  pageSize: 100,
  sortColumn: '',
  sortDirection: 'ASC',
  filters: []
})

onMounted(function () {
  loadDatasources()
})

async function loadDatasources() {
  try {
    datasources.value = await listDatasources()
    applyRouteDefaults()
    if (form.datasourceId) {
      syncDatasourceLabel(form.datasourceId)
      await loadMetadataOptions(form.datasourceId)
    }
    if (!autoPreviewTriggered.value && shouldAutoPreview()) {
      autoPreviewTriggered.value = true
      await runPreview()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载数据源失败，请稍后重试')
  }
}

function applyRouteDefaults() {
  if (route.query.datasourceId) {
    form.datasourceId = Number(route.query.datasourceId)
  }
  if (route.query.schemaName) {
    form.schemaName = String(route.query.schemaName)
  }
  if (route.query.tableName) {
    form.tableName = String(route.query.tableName)
  }
}

function shouldAutoPreview() {
  return !!(form.datasourceId && form.tableName)
}

function handleDatasourceChange(value) {
  syncDatasourceLabel(value)
  availableColumns.value = []
  schemaOptions.value = []
  tableOptions.value = []
  previewError.value = ''
  hasQueried.value = false
  result.columns = []
  result.rows = []
  result.totalRowCount = 0
  result.pageNumber = form.pageNumber
  result.pageSize = form.pageSize
  loadMetadataOptions(value)
}

function syncDatasourceLabel(id) {
  const datasource = datasources.value.find(function (item) {
    return item.id === id
  })
  selectedDatasourceName.value = datasource ? datasource.name : ''
}

function addFilter() {
  form.filters.push({
    columnName: '',
    operator: 'EQ',
    value: ''
  })
}

function removeFilter(index) {
  form.filters.splice(index, 1)
}

async function runPreview() {
  if (!form.datasourceId) {
    ElMessage.warning('请选择数据源')
    return
  }
  if (!form.tableName) {
    ElMessage.warning('请输入表名')
    return
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === form.datasourceId
  })
  if (!datasource) {
    ElMessage.warning('数据源不存在')
    return
  }
  loading.value = true
  hasQueried.value = true
  previewError.value = ''
  try {
    const response = await previewTableData({
      datasource: datasource,
      schemaName: form.schemaName,
      tableName: form.tableName,
      pageNumber: form.pageNumber,
      pageSize: form.pageSize,
      sortColumn: form.sortColumn,
      sortDirection: form.sortDirection,
      filters: form.filters.filter(function (item) {
        return item.columnName && item.operator
      })
    })
    result.columns = response.columns || []
    result.rows = response.rows || []
    result.totalRowCount = response.totalRowCount || 0
    result.pageNumber = response.pageNumber || form.pageNumber
    result.pageSize = response.pageSize || form.pageSize
    availableColumns.value = result.columns.slice()
    if (!form.sortColumn && availableColumns.value.length > 0) {
      form.sortColumn = availableColumns.value[0]
    }
    if (result.rows.length === 0) {
      ElMessage.info('查询完成，但没有返回记录')
    } else {
      ElMessage.success('预览完成')
    }
  } catch (error) {
    const message = error.message || '预览失败，请稍后重试'
    previewError.value = message
    result.columns = []
    result.rows = []
    result.totalRowCount = 0
    result.pageNumber = form.pageNumber
    result.pageSize = form.pageSize
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

async function loadMetadataOptions(datasourceId) {
  if (!datasourceId) {
    return
  }
  try {
    const schemas = await scanMetadata(datasourceId)
    schemaOptions.value = (schemas || []).map(function (schema) {
      return schema.schemaName
    })
    tableOptions.value = (schemas || []).reduce(function (acc, schema) {
      const tables = schema.tables || []
      for (let i = 0; i < tables.length; i += 1) {
        acc.push(tables[i].tableName)
      }
      return acc
    }, [])
  } catch (error) {
    schemaOptions.value = []
    tableOptions.value = []
    ElMessage.error(error.message || '加载表结构失败，请稍后重试')
  }
}

function handlePageChange(value) {
  form.pageNumber = value
  runPreview()
}

function handleSizeChange(value) {
  form.pageSize = value
  form.pageNumber = 1
  runPreview()
}

function goBack() {
  router.push('/metadata')
}
</script>

<style scoped>
.preview-intro {
  margin-top: 16px;
}
</style>
