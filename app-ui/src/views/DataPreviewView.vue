<template>
  <div class="page-section preview-workbench">
    <div class="page-header preview-workbench__header">
      <div class="preview-workbench__header-copy">
        <h1>数据预览</h1>
        <p>选择数据源和表后，预览前 100 条记录，并支持分页、排序和过滤。</p>
      </div>
      <el-space>
        <el-button round @click="goBack">返回表结构</el-button>
        <el-tooltip :disabled="canQuery" content="请先选择数据源、Schema 和表名" placement="bottom">
          <span>
            <el-button type="primary" round :loading="loading" :disabled="!canQuery" @click="runPreview">
              查询预览
            </el-button>
          </span>
        </el-tooltip>
      </el-space>
    </div>

    <div class="preview-workbench__overview">
      <div class="preview-overview-card">
        <div class="preview-overview-card__label">当前数据源</div>
        <div class="preview-overview-card__value">{{ selectedDatasourceName || '未选择' }}</div>
        <div class="preview-overview-card__hint">{{ selectedDatasourceHint }}</div>
      </div>
      <div class="preview-overview-card">
        <div class="preview-overview-card__label">当前表</div>
        <div class="preview-overview-card__value">{{ currentTableLabel }}</div>
        <div class="preview-overview-card__hint">{{ currentTableHint }}</div>
      </div>
      <div class="preview-overview-card">
        <div class="preview-overview-card__label">预览结果</div>
        <div class="preview-overview-card__value">{{ previewResultSummary }}</div>
        <div class="preview-overview-card__hint">{{ previewResultHint }}</div>
      </div>
      <div class="preview-overview-card">
        <div class="preview-overview-card__label">查询限制</div>
        <div class="preview-overview-card__value">前 {{ form.pageSize }} 条</div>
        <div class="preview-overview-card__hint">适合快速预览前 100 条记录</div>
      </div>
    </div>

    <div class="panel-card preview-banner" :class="'is-' + queryStatus">
      <div class="status-item">
        <span class="status-item__label">提示</span>
        <span class="status-item__value">{{ statusMessage }}</span>
      </div>
    </div>

    <div class="preview-workbench__workspace">
      <div class="preview-workbench__builder">
        <div class="panel-card glass-panel preview-panel preview-panel--builder">
          <div class="preview-panel__head">
            <div>
              <h2>查询条件</h2>
              <p>按数据源、Schema、表、分页、排序和过滤条件组合预览查询。</p>
            </div>
            <el-tag :type="queryStatusTagType" effect="dark">{{ queryStatusLabel }}</el-tag>
          </div>

          <div class="preview-section">
            <div class="preview-section__title">基础信息</div>
            <el-form :model="form" label-width="96px">
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
                    <CreatableSelect
                      v-model="form.schemaName"
                      :options="schemaOptions"
                      :disabled="!form.datasourceId"
                      placeholder="请选择或输入 Schema"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="表名">
                    <CreatableSelect
                      v-model="form.tableName"
                      :options="tableOptions"
                      :disabled="!form.schemaName"
                      placeholder="请选择或输入表名"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="当前页">
                    <el-input-number v-model="form.pageNumber" :min="1" style="width: 100%;" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <div class="preview-section">
            <div class="preview-section__title">分页排序</div>
            <el-form :model="form" label-width="96px">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="页大小">
                    <el-select v-model="form.pageSize" style="width: 100%;">
                      <el-option v-for="size in pageSizeOptions" :key="size" :label="String(size)" :value="size" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <div class="preview-hint">
                    适合快速预览前 100 条记录
                  </div>
                </el-col>
              </el-row>

              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="排序字段">
                    <el-select
                      v-model="form.sortColumn"
                      filterable
                      clearable
                      placeholder="未选择表时不可用"
                      style="width: 100%;"
                      :disabled="!form.tableName"
                    >
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
          </div>

          <div class="preview-section">
            <div class="preview-section__head">
              <div class="preview-section__title">过滤条件</div>
              <el-space>
                <el-button size="small" text @click="filtersExpanded = !filtersExpanded">
                  {{ filtersExpanded ? '收起条件' : '展开条件' }}
                </el-button>
                <el-button size="small" @click="addFilter">新增条件</el-button>
              </el-space>
            </div>
            <div v-if="!form.filters.length && !filtersExpanded" class="preview-empty-note">
              当前未设置过滤条件。
            </div>
            <div v-else-if="form.filters.length" class="preview-filters">
              <div v-for="(filter, index) in form.filters" :key="index" class="preview-filter-row">
                <el-select v-model="filter.columnName" filterable placeholder="字段名" style="min-width: 180px;" :disabled="!canUseColumns">
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
            <div v-else class="preview-empty-note">
              当前未设置过滤条件。
            </div>
          </div>

          <div class="preview-actions">
            <el-button @click="resetPreview">重置</el-button>
            <el-tooltip :disabled="canQuery" content="请先选择数据源、Schema 和表名" placement="top">
              <span>
                <el-button type="primary" :loading="loading" :disabled="!canQuery" @click="runPreview">查询预览</el-button>
              </span>
            </el-tooltip>
          </div>
        </div>
      </div>

      <div class="preview-workbench__results">
        <div class="panel-card glass-panel preview-panel preview-panel--results">
          <div class="preview-panel__head">
            <div>
              <h2>结果预览</h2>
              <p>这里显示查询成功后的当前页数据、统计信息和分页结果。</p>
            </div>
            <el-tag :type="queryStatusTagType" effect="dark">{{ queryStatusChip }}</el-tag>
          </div>

          <div class="preview-results-summary">
            <div>当前页 {{ result.pageNumber || form.pageNumber }}</div>
            <div>页大小 {{ result.pageSize || form.pageSize }}</div>
            <div>字段数 {{ result.columns.length }}</div>
            <div>总数 {{ result.totalRowCount }}</div>
          </div>

          <div class="preview-results-meta">
            <div class="status-item">
              <span class="status-item__label">当前状态</span>
              <span class="status-item__value">{{ queryStateLabel }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">结果来源</span>
              <span class="status-item__value">{{ currentTableLabel }}</span>
            </div>
          </div>

          <div class="preview-results-workspace" :class="'is-' + queryStatus">
            <div v-if="queryStatus === 'idle'" class="preview-hero-empty">
              <div class="preview-hero-empty__title">还没有开始预览</div>
              <div class="preview-hero-empty__text">选择数据源、schema 和表名后，点击“查询预览”查看记录。</div>
              <ol class="preview-hero-empty__list">
                <li>选择数据源</li>
                <li>选择 Schema / table</li>
                <li>配置分页和排序</li>
                <li>查询预览数据</li>
              </ol>
            </div>

            <div v-else-if="queryStatus === 'ready'" class="preview-hero-empty">
              <div class="preview-hero-empty__title">查询条件已就绪</div>
              <div class="preview-hero-empty__text">点击“查询预览”查看当前表的前 {{ form.pageSize }} 条记录。</div>
            </div>

            <div v-else-if="queryStatus === 'loading'" class="preview-loading">
              正在查询数据，请稍候。
            </div>

            <div v-else-if="queryStatus === 'error'" class="preview-error-box">
              <el-alert
                :title="previewError || '查询失败，请检查数据源连接、表名、权限或过滤条件。'"
                type="error"
                show-icon
                :closable="false"
              />
            </div>

            <div v-else-if="queryStatus === 'success' || queryStatus === 'empty'" class="preview-table-shell">
              <el-table
                :data="result.rows"
                border
                stripe
                height="100%"
                :empty-text="queryStatus === 'empty' ? '查询完成，当前条件下没有返回数据。' : ' '"
              >
                <el-table-column
                  v-for="column in result.columns"
                  :key="column"
                  :prop="column"
                  :label="column"
                  min-width="160"
                  show-overflow-tooltip
                />
              </el-table>
              <div v-if="queryStatus === 'empty'" class="preview-empty-note preview-empty-note--result">
                选择表后点击查询预览，这里显示前 100 条记录。
              </div>
            </div>
          </div>

          <div v-if="showPagination" class="preview-pagination">
            <el-pagination
              layout="total, prev, pager, next, sizes"
              :total="result.totalRowCount"
              :current-page="form.pageNumber"
              :page-size="form.pageSize"
              :page-sizes="pageSizeOptions"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
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
const filtersExpanded = ref(false)
const datasources = ref([])
const selectedDatasourceName = ref('')
const selectedDatasourceType = ref('')
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

const canUseColumns = computed(function () {
  return !!form.tableName && availableColumns.value.length > 0
})

const canQuery = computed(function () {
  return !!form.datasourceId && !!form.schemaName && !!form.tableName && !loading.value
})

const queryStatus = computed(function () {
  if (loading.value) {
    return 'loading'
  }
  if (previewError.value) {
    return 'error'
  }
  if (!hasQueried.value) {
    return canQuery.value ? 'ready' : 'idle'
  }
  if (!result.rows.length) {
    return 'empty'
  }
  return 'success'
})

const queryStatusLabel = computed(function () {
  if (queryStatus.value === 'loading') {
    return '查询中'
  }
  if (queryStatus.value === 'error') {
    return '查询失败'
  }
  if (queryStatus.value === 'ready') {
    return '可查询'
  }
  if (queryStatus.value === 'success') {
    return '已返回数据'
  }
  if (queryStatus.value === 'empty') {
    return '无数据'
  }
  return '未就绪'
})

const queryStatusChip = computed(function () {
  if (queryStatus.value === 'loading') {
    return '查询中'
  }
  if (queryStatus.value === 'error') {
    return '查询失败'
  }
  if (queryStatus.value === 'ready') {
    return '可查询'
  }
  if (queryStatus.value === 'success') {
    return '已返回 ' + result.rows.length + ' 行'
  }
  if (queryStatus.value === 'empty') {
    return '无数据'
  }
  return '未查询'
})

const queryStatusTagType = computed(function () {
  if (queryStatus.value === 'loading') {
    return 'warning'
  }
  if (queryStatus.value === 'error') {
    return 'danger'
  }
  if (queryStatus.value === 'ready') {
    return 'info'
  }
  if (queryStatus.value === 'success') {
    return 'success'
  }
  if (queryStatus.value === 'empty') {
    return 'info'
  }
  return 'info'
})

const queryStateLabel = computed(function () {
  if (queryStatus.value === 'loading') {
    return '正在查询数据，请稍候。'
  }
  if (queryStatus.value === 'error') {
    return '查询失败，请检查数据源连接、表名、权限或过滤条件。'
  }
  if (queryStatus.value === 'ready') {
    return '查询条件已就绪，点击“查询预览”查看数据。'
  }
  if (queryStatus.value === 'success') {
    return '查询完成，已返回当前页数据。'
  }
  if (queryStatus.value === 'empty') {
    return '查询完成，当前条件下没有返回数据。'
  }
  return '请选择数据源、Schema 和表名，然后点击“查询预览”查看前 100 条记录。'
})

const statusMessage = computed(function () {
  if (queryStatus.value === 'loading') {
    return '正在查询数据，请稍候。'
  }
  if (queryStatus.value === 'error') {
    return '查询失败，请检查数据源连接、表名、权限或过滤条件。'
  }
  if (queryStatus.value === 'ready') {
    return '查询条件已就绪，点击“查询预览”查看数据。'
  }
  if (queryStatus.value === 'success') {
    return '查询完成，已返回当前页数据。'
  }
  if (queryStatus.value === 'empty') {
    return '查询完成，当前条件下没有返回数据。'
  }
  if (form.datasourceId && (!form.schemaName || !form.tableName)) {
    return '请选择 Schema 和表名后继续预览。'
  }
  return '请选择数据源、Schema 和表名，然后点击“查询预览”查看前 100 条记录。'
})

const selectedDatasourceHint = computed(function () {
  if (!form.datasourceId) {
    return '未选择数据源'
  }
  return selectedDatasourceType.value ? '类型：' + selectedDatasourceType.value : '已选择数据源'
})

const currentTableLabel = computed(function () {
  if (!form.tableName) {
    return '未选择'
  }
  return form.schemaName ? form.schemaName + '.' + form.tableName : form.tableName
})

const currentTableHint = computed(function () {
  if (!form.tableName) {
    return form.datasourceId ? '请先选择 Schema' : '请先选择数据源'
  }
  return '已选择表，可继续预览'
})

const previewResultSummary = computed(function () {
  return result.rows.length + ' 行'
})

const previewResultHint = computed(function () {
  if (!hasQueried.value) {
    return '尚未执行查询'
  }
  return result.rows.length ? '当前页已返回数据' : '当前条件下没有返回数据'
})

const showPagination = computed(function () {
  return hasQueried.value || queryStatus.value === 'loading' || !!previewError.value
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
  return !!(form.datasourceId && form.schemaName && form.tableName)
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
  form.schemaName = ''
  form.tableName = ''
  form.sortColumn = ''
  filtersExpanded.value = false
  loadMetadataOptions(value)
}

function syncDatasourceLabel(id) {
  const datasource = datasources.value.find(function (item) {
    return item.id === id
  })
  selectedDatasourceName.value = datasource ? datasource.name : ''
  selectedDatasourceType.value = datasource ? datasource.type : ''
}

function addFilter() {
  form.filters.push({
    columnName: '',
    operator: 'EQ',
    value: ''
  })
  filtersExpanded.value = true
}

function removeFilter(index) {
  form.filters.splice(index, 1)
}

async function runPreview() {
  if (!form.datasourceId) {
    ElMessage.warning('请选择数据源')
    return
  }
  if (!form.schemaName) {
    ElMessage.warning('请选择 Schema')
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

function resetPreview() {
  form.schemaName = ''
  form.tableName = ''
  form.pageNumber = 1
  form.pageSize = 100
  form.sortColumn = ''
  form.sortDirection = 'ASC'
  form.filters = []
  hasQueried.value = false
  previewError.value = ''
  result.columns = []
  result.rows = []
  result.totalRowCount = 0
  result.pageNumber = 1
  result.pageSize = 100
  availableColumns.value = []
  filtersExpanded.value = false
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
.preview-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: calc(100vh - 32px);
}

.preview-workbench__header {
  align-items: flex-start;
  gap: 16px;
}

.preview-workbench__header-copy h1 {
  margin-bottom: 6px;
}

.preview-workbench__header-copy p {
  max-width: 760px;
}

.preview-workbench__overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.preview-overview-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.preview-overview-card__label {
  font-size: 12px;
  color: #64748b;
}

.preview-overview-card__value {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.preview-overview-card__hint {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.45;
  color: #64748b;
}

.preview-banner {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.preview-banner.is-loading {
  background: #fffbeb;
  border-color: #f59e0b;
}

.preview-banner.is-error {
  background: #fef2f2;
  border-color: #fca5a5;
}

.preview-banner.is-success {
  background: #f0fdf4;
  border-color: #86efac;
}

.preview-banner.is-empty {
  background: #f8fafc;
}

.preview-banner.is-ready {
  background: #eef4ff;
  border-color: #bfdbfe;
}

.preview-workbench__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.4fr);
  gap: 16px;
  align-items: start;
  min-height: 0;
}

.preview-workbench__builder,
.preview-workbench__results {
  min-width: 0;
  min-height: 0;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.preview-panel--builder {
  padding-bottom: 16px;
}

.preview-panel--results {
  height: 100%;
  min-height: 560px;
  padding-bottom: 16px;
}

.preview-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.preview-panel__head h2 {
  margin-bottom: 4px;
}

.preview-panel__head p {
  margin: 0;
  color: #64748b;
  line-height: 1.5;
}

.preview-section {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fbfdff;
  padding: 14px;
}

.preview-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.preview-section__title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 12px;
}

.preview-hint {
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  border: 1px solid #e2e8f0;
  min-height: 42px;
  display: flex;
  align-items: center;
}

.preview-empty-note {
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  background: #fbfdff;
  color: #64748b;
  line-height: 1.5;
}

.preview-empty-note--result {
  margin-top: 12px;
  background: #f8fafc;
}

.preview-filters {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-filter-row {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) 150px minmax(180px, 1.2fr) auto;
  gap: 10px;
  align-items: center;
}

.preview-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.preview-results-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 13px;
}

.preview-results-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.preview-results-workspace {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 360px;
  flex: 1 1 auto;
}

.preview-hero-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 320px;
  padding: 24px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: #fbfdff;
  color: #475569;
}

.preview-hero-empty__title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
}

.preview-hero-empty__text {
  line-height: 1.6;
  color: #475569;
  margin-bottom: 14px;
}

.preview-hero-empty__list {
  margin: 0;
  padding-left: 18px;
  color: #64748b;
  line-height: 1.7;
}

.preview-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: #fbfdff;
  color: #475569;
}

.preview-error-box {
  min-height: 320px;
  display: flex;
  align-items: flex-start;
}

.preview-table-shell {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1 1 auto;
}

.preview-table-shell :deep(.el-table) {
  width: 100%;
}

.preview-table-shell :deep(.el-table__inner-wrapper) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.preview-table-shell :deep(.el-table__body-wrapper) {
  flex: 1 1 auto;
}

.preview-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}

.preview-workbench :deep(.el-form-item) {
  margin-bottom: 12px;
}

@media (max-width: 1280px) {
  .preview-workbench__workspace,
  .preview-workbench__overview {
    grid-template-columns: 1fr;
  }

  .preview-panel--results {
    min-height: 520px;
  }
}

@media (max-width: 960px) {
  .preview-workbench__header {
    flex-direction: column;
  }

  .preview-filter-row {
    grid-template-columns: 1fr;
  }

  .preview-results-meta {
    grid-template-columns: 1fr;
  }

  .preview-actions,
  .preview-pagination {
    justify-content: stretch;
  }
}
</style>
