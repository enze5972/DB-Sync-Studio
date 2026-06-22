<template>
  <div class="page-section datasource-center">
    <div class="page-header datasource-center__header">
      <div>
        <h1>数据源管理</h1>
        <p>创建、测试和管理 MySQL / PostgreSQL / DM 数据源连接，为表扫描和同步任务提供基础。</p>
      </div>
      <el-space>
        <el-button round @click="openConnectionGuide">查看连接配置建议</el-button>
        <el-button type="primary" round @click="openCreateDialog">新建数据源</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--four datasource-center__overview">
      <div class="page-overview__item datasource-center__overview-item">
        <div class="page-overview__label">数据源总数</div>
        <div class="page-overview__value">{{ datasourceSummary.total }}</div>
        <div class="page-overview__hint">所有已保存连接配置</div>
      </div>
      <div class="page-overview__item datasource-center__overview-item">
        <div class="page-overview__label">可用连接</div>
        <div class="page-overview__value">{{ datasourceSummary.available }}</div>
        <div class="page-overview__hint">已测试且当前可连接</div>
      </div>
      <div class="page-overview__item datasource-center__overview-item">
        <div class="page-overview__label">异常连接</div>
        <div class="page-overview__value">{{ datasourceSummary.failed }}</div>
        <div class="page-overview__hint">最近测试失败或待确认</div>
      </div>
      <div class="page-overview__item datasource-center__overview-item">
        <div class="page-overview__label">支持类型</div>
        <div class="page-overview__value">MySQL / PostgreSQL / DM</div>
        <div class="page-overview__hint">用于同步、扫描与预览</div>
      </div>
    </div>

    <div class="dashboard-panels datasource-center__workspace">
      <div class="panel-card glass-panel datasource-center__list-panel">
        <div class="section-title section-title--compact datasource-center__section-head">
          <h2>连接列表</h2>
          <el-tag :type="datasources.length ? 'success' : 'info'" effect="dark">
            {{ datasources.length ? datasources.length + ' 个连接' : '暂无数据源' }}
          </el-tag>
        </div>

        <div class="datasource-center__status-row">
          <div class="status-item">
            <span class="status-item__label">当前连接</span>
            <span class="status-item__value">{{ currentDatasourceLabel }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">连接状态</span>
            <span class="status-item__value">{{ connectionStateLabel }}</span>
          </div>
        </div>

        <div v-if="loading || datasources.length" class="table-shell datasource-center__table-shell">
          <el-table :data="datasources" border stripe v-loading="loading">
            <el-table-column prop="name" label="名称" min-width="140" />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag type="info" effect="plain">{{ datasourceTypeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="host" label="主机" width="160" />
            <el-table-column prop="port" label="端口" width="90" />
            <el-table-column prop="databaseName" label="数据库" min-width="140" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" @click="editDatasource(row)">编辑</el-button>
                  <el-button size="small" type="success" @click="testDatasource(row)">测试连接</el-button>
                  <el-button size="small" type="danger" @click="removeDatasource(row)">删除</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-else class="datasource-center__empty-shell">
          <StateEmpty
            class="datasource-center__empty"
            title="创建第一个数据源连接"
            description="连接源库和目标库后，即可继续进行表结构扫描、字段映射和同步任务配置。"
            hint="1. 选择数据库类型 · 2. 填写主机、端口和认证信息 · 3. 测试连接 · 4. 保存到本地 SQLite · 5. 用于表扫描和同步任务"
            footer="建议先测试连接，再保存配置；这样后续的扫描、任务和日志页面才能直接复用。"
            button-text="新建数据源"
            :wide="true"
            @action="openCreateDialog"
          />
          <div class="datasource-center__tips panel-card glass-panel">
            <div class="section-title section-title--compact datasource-center__tips-head">
              <h2>连接建议</h2>
              <el-tag type="info" effect="dark">静态提示</el-tag>
            </div>
            <div class="status-stack">
              <div class="status-item">
                <span class="status-item__label">MySQL 默认端口</span>
                <span class="status-item__value">3306</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">PostgreSQL 默认端口</span>
                <span class="status-item__value">5432</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">DM 默认端口</span>
                <span class="status-item__value">5236</span>
              </div>
              <div class="status-item">
                <span class="status-item__label">推荐顺序</span>
                <span class="status-item__value">先测试连接，再保存配置</span>
              </div>
            </div>
            <div class="datasource-center__tips-note">
              这些建议只用于快速填写，不会修改你的连接配置。
            </div>
          </div>
          <div class="datasource-center__empty-actions">
            <el-button plain round @click="openConnectionGuide">查看连接配置建议</el-button>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel datasource-center__detail-panel">
        <div class="section-title section-title--compact datasource-center__section-head">
          <h2>连接详情</h2>
          <el-tag :type="datasources.length ? 'success' : 'info'" effect="dark">
            {{ datasources.length ? '可管理' : '未选择' }}
          </el-tag>
        </div>
        <div class="status-stack">
          <div class="status-item">
            <span class="status-item__label">状态</span>
            <span class="status-item__value">{{ datasources.length ? '已加载连接列表' : '未选择数据源' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">最后操作</span>
            <span class="status-item__value">{{ datasources.length ? '可编辑、可测试、可删除' : '等待创建第一个连接' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">用途</span>
            <span class="status-item__value">表扫描、任务配置、同步执行</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">提示</span>
            <span class="status-item__value">选择数据源后，可直接进行测试和管理</span>
          </div>
        </div>
        <div class="datasource-center__detail-note">
          数据源管理是同步工作台的基础入口。连接保存后，会被表扫描、任务向导和同步任务复用。
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%;">
                <el-option label="MySQL" value="MYSQL" />
                <el-option label="PostgreSQL" value="POSTGRESQL" />
                <el-option label="达梦 DM" value="DM" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主机" prop="host">
              <el-input v-model="form.host" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="端口" prop="port">
              <el-input-number v-model="form.port" :min="1" :max="65535" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数据库" prop="databaseName">
              <el-input v-model="form.databaseName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="testCurrentConnection" :loading="testing">测试连接</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="saving">保存</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteDatasource, listDatasources, listDatasourceConnectionMetrics, saveDatasource, testDatasourceConnection } from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const datasources = ref([])
const datasourceMetrics = ref([])
const currentEditingId = ref(null)

const form = reactive(createEmptyForm())

const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'change' }],
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const dialogTitle = computed(function () {
  return currentEditingId.value ? '编辑数据源' : '新建数据源'
})

const datasourceSummary = computed(function () {
  const total = datasources.value.length
  const metricsById = latestDatasourceMetricMap.value
  let available = 0
  let failed = 0
  datasources.value.forEach(function (item) {
    const metric = metricsById[item.id]
    if (!metric) {
      return
    }
    if (metric.connectionStatus === 'SUCCESS') {
      available += 1
      return
    }
    if (metric.connectionStatus === 'FAILED') {
      failed += 1
    }
  })
  return {
    total: total,
    available: available,
    failed: failed
  }
})

const latestDatasourceMetricMap = computed(function () {
  const map = {}
  ;(datasourceMetrics.value || []).forEach(function (item) {
    if (!item || item.datasourceId === null || item.datasourceId === undefined) {
      return
    }
    const current = map[item.datasourceId]
    if (!current) {
      map[item.datasourceId] = item
      return
    }
    const currentTime = Number(current.metricTime || 0)
    const nextTime = Number(item.metricTime || 0)
    if (nextTime >= currentTime) {
      map[item.datasourceId] = item
    }
  })
  return map
})

const connectionStateLabel = computed(function () {
  if (!datasources.value.length) {
    return '未加载'
  }
  if (!datasourceMetrics.value.length) {
    return '待测试'
  }
  if (datasourceSummary.value.failed > 0) {
    return '存在异常'
  }
  if (datasourceSummary.value.available > 0) {
    return '已可用'
  }
  return '待测试'
})

const currentDatasourceLabel = computed(function () {
  if (!datasources.value.length) {
    return '未选择'
  }
  return datasources.value[0].name || '未命名连接'
})

onMounted(function () {
  loadDatasources()
})

async function loadDatasources() {
  loading.value = true
  try {
    const result = await Promise.all([
      listDatasources(),
      listDatasourceConnectionMetrics({ limit: 200 })
    ])
    datasources.value = result[0] || []
    datasourceMetrics.value = Array.isArray(result[1]) ? result[1] : []
  } catch (error) {
    ElMessage.error(error.message || '加载数据源失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  currentEditingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openConnectionGuide() {
  ElMessage.info('请先选择数据库类型，填写主机、端口和认证信息，然后测试连接再保存。')
}

function editDatasource(row) {
  currentEditingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
    saving.value = true
    const payload = normalizePayload()
    await saveDatasource(payload)
    ElMessage.success('数据源已保存')
    dialogVisible.value = false
    await loadDatasources()
  } catch (error) {
    if (error !== false && error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || error || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

async function testCurrentConnection() {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch (error) {
    return
  }
  await testDatasource(form)
}

async function testDatasource(row) {
  testing.value = true
  try {
    const result = await testDatasourceConnection(normalizeRow(row))
    if (result.success) {
      ElMessage.success(result.message || '连接成功')
    } else {
      ElMessage.warning(result.message || '连接失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '连接测试失败')
  } finally {
    testing.value = false
  }
}

async function removeDatasource(row) {
  try {
    await ElMessageBox.confirm('确定删除这个数据源吗？', '提示', {
      type: 'warning'
    })
    await deleteDatasource(row.id)
    ElMessage.success('数据源已删除')
    await loadDatasources()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

function normalizePayload() {
  return normalizeRow(Object.assign({ id: currentEditingId.value }, form))
}

function normalizeRow(row) {
  return {
    id: row.id,
    name: row.name,
    type: row.type,
    host: row.host,
    port: Number(row.port),
    databaseName: row.databaseName,
    username: row.username,
    password: row.password,
    remark: row.remark
  }
}

function datasourceTypeLabel(type) {
  if (type === 'POSTGRESQL') {
    return 'PostgreSQL'
  }
  if (type === 'DM') {
    return '达梦 DM'
  }
  return 'MySQL'
}

function resetForm() {
  Object.assign(form, createEmptyForm())
}

function createEmptyForm() {
  return {
    name: '',
    type: 'MYSQL',
    host: '127.0.0.1',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
    remark: ''
  }
}
</script>

<style scoped>
.datasource-center {
  display: grid;
  gap: 18px;
}

.datasource-center__workspace {
  grid-template-columns: 1.08fr 0.92fr;
  align-items: start;
}

.datasource-center__overview {
  margin-top: 0;
}

.datasource-center__section-head {
  margin-top: 0;
}

.datasource-center__status-row {
  display: grid;
  gap: 12px;
}

.datasource-center__table-shell {
  margin-top: 14px;
}

.datasource-center__empty-shell {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.datasource-center__empty {
  margin-top: 0;
}

.datasource-center__tips {
  border-radius: 16px;
  padding: 16px;
}

.datasource-center__tips-head {
  margin-top: 0;
}

.datasource-center__tips-note,
.datasource-center__detail-note {
  margin-top: 10px;
  color: var(--text-sub);
  font-size: 12px;
  line-height: 1.7;
}

.datasource-center__empty-actions {
  display: flex;
  justify-content: flex-start;
}

.datasource-center__detail-panel {
  display: grid;
  gap: 14px;
}

.page-overview--four {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 1280px) {
  .datasource-center__workspace {
    grid-template-columns: 1fr;
  }

  .page-overview--four {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-overview--four {
    grid-template-columns: 1fr;
  }
}
</style>
