<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>数据源管理</h1>
        <p>创建、测试和保存 MySQL / PostgreSQL / DM 数据源连接。</p>
      </div>
      <el-button type="primary" round @click="openCreateDialog">新建数据源</el-button>
    </div>

    <div class="page-overview">
      <div class="page-overview__item">
        <div class="page-overview__label">数据源数量</div>
        <div class="page-overview__value">{{ datasources.length }}</div>
        <div class="page-overview__hint">本地保存后可直接复用连接配置</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">支持类型</div>
        <div class="page-overview__value">MySQL / PostgreSQL / DM</div>
        <div class="page-overview__hint">保持同一套连接管理界面</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">操作方式</div>
        <div class="page-overview__value">测试后保存</div>
        <div class="page-overview__hint">先验证连通性，再写入 SQLite</div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="table-shell">
        <el-table :data="datasources" border stripe v-loading="loading">
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column prop="host" label="主机" width="160" />
          <el-table-column prop="port" label="端口" width="90" />
          <el-table-column prop="databaseName" label="数据库" min-width="140" />
          <el-table-column prop="username" label="用户名" width="140" />
          <el-table-column prop="remark" label="备注" min-width="180" />
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
import { deleteDatasource, listDatasources, saveDatasource, testDatasourceConnection } from '../services/backend'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const dialogVisible = ref(false)
const formRef = ref()
const datasources = ref([])
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

onMounted(function () {
  loadDatasources()
})

async function loadDatasources() {
  loading.value = true
  try {
    datasources.value = await listDatasources()
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
