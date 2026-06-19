<template>
  <div class="page-section">
    <div class="page-header">
      <div>
        <h1>SQL 编辑器</h1>
        <p>选择一个数据源后执行 SQL，支持查询结果展示、影响行数和执行日志回写。</p>
      </div>
      <el-space>
        <el-button round @click="loadLogs">刷新日志</el-button>
        <el-button type="primary" round :loading="executing" :disabled="executing" @click="executeCurrentSql">执行 SQL</el-button>
      </el-space>
    </div>

    <div class="page-overview">
      <div class="page-overview__item">
        <div class="page-overview__label">当前数据源</div>
        <div class="page-overview__value">{{ selectedDatasourceName || '未选择' }}</div>
        <div class="page-overview__hint">执行前建议先确认目标库是否正确</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">最近执行</div>
        <div class="page-overview__value">{{ executionSummary.statementType || '等待执行' }}</div>
        <div class="page-overview__hint">{{ executionSummary.elapsedText }} / {{ executionSummary.affectedRows }} 行</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">执行日志</div>
        <div class="page-overview__value">{{ logs.length }} 条</div>
        <div class="page-overview__hint">保留最近 20 条执行记录</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>编辑器</h2>
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
              <el-form-item label="危险语句">
                <el-space alignment="center">
                  <el-switch v-model="form.allowDangerousSql" />
                  <el-tooltip
                    effect="dark"
                    placement="top"
                    content="默认禁止 DROP / TRUNCATE / ALTER，避免误删或破坏性执行；需要时再手动开启。"
                  >
                    <el-tag class="sql-hint-tag" type="warning" effect="light">默认禁止危险语句</el-tag>
                  </el-tooltip>
                </el-space>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <el-input
          v-model="form.sql"
          type="textarea"
          :rows="14"
          placeholder="请输入 SELECT / INSERT / UPDATE / DELETE 语句"
          class="sql-editor"
        />

        <div class="sql-editor__footer">
          <div class="sql-editor__meta">
            <span>执行耗时：{{ executionSummary.elapsedText }}</span>
            <span>影响行数：{{ executionSummary.affectedRows }}</span>
          </div>
            <el-space>
              <el-button @click="clearSql">清空</el-button>
            <el-button type="primary" :loading="executing" :disabled="executing" @click="executeCurrentSql">执行 SQL</el-button>
          </el-space>
        </div>

        <el-alert
          v-if="executionError"
          :title="executionError"
          type="error"
          show-icon
          class="sql-alert"
        />
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>执行结果</h2>
          <el-tag type="success" effect="dark">{{ executionSummary.statementType || '等待执行' }}</el-tag>
        </div>

        <div class="status-stack">
          <div class="status-item">
            <span class="status-item__label">状态</span>
            <span class="status-item__value">{{ executionSummary.success ? '成功' : '未执行 / 失败' }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">执行时间</span>
            <span class="status-item__value">{{ executionSummary.elapsedText }}</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">影响行数</span>
            <span class="status-item__value">{{ executionSummary.affectedRows }}</span>
          </div>
        </div>

        <div class="table-shell sql-result-table" v-loading="executing">
          <el-table v-if="result.columns.length > 0" :data="result.rows" border stripe>
            <el-table-column
              v-for="column in result.columns"
              :key="column"
              :prop="column"
              :label="column"
              min-width="160"
              show-overflow-tooltip
            />
          </el-table>
          <el-empty v-else description="执行后在这里展示查询结果" />
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>最近执行记录</h2>
        <el-tag type="info" effect="dark">SQL Logs</el-tag>
      </div>
      <div class="table-shell">
        <el-table :data="logs" border stripe v-loading="logsLoading">
          <el-table-column prop="createdAt" label="时间" min-width="180">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="statementType" label="类型" width="120" />
          <el-table-column prop="success" label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="affectedRows" label="影响行数" width="120" />
          <el-table-column prop="elapsedMillis" label="耗时(ms)" width="120" />
          <el-table-column prop="sqlText" label="SQL" min-width="320" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="错误" min-width="240" show-overflow-tooltip />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executeSql, listDatasources, listSqlExecutionLogs } from '../services/backend'

const datasources = ref([])
const selectedDatasourceName = ref('')
const executing = ref(false)
const logsLoading = ref(false)
const executionError = ref('')
const logs = ref([])
const result = reactive({
  columns: [],
  rows: []
})
const executionSummary = reactive({
  elapsedText: '-',
  affectedRows: '-',
  statementType: '',
  success: false
})

const form = reactive({
  datasourceId: null,
  sql: '',
  allowDangerousSql: false
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    datasources.value = await listDatasources()
    if (datasources.value.length > 0) {
      form.datasourceId = datasources.value[0].id
      selectedDatasourceName.value = datasources.value[0].name
    }
    await loadLogs()
  } catch (error) {
    ElMessage.error(error.message || '加载 SQL 编辑器失败')
  }
}

function handleDatasourceChange(value) {
  const datasource = datasources.value.find(function (item) {
    return item.id === value
  })
  selectedDatasourceName.value = datasource ? datasource.name : ''
}

async function executeCurrentSql() {
  if (!form.datasourceId) {
    ElMessage.warning('请选择数据源')
    return
  }
  if (!form.sql || !form.sql.trim()) {
    ElMessage.warning('请输入 SQL')
    return
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === form.datasourceId
  })
  if (!datasource) {
    ElMessage.warning('数据源不存在')
    return
  }
  if (looksDangerousSql(form.sql) && !form.allowDangerousSql) {
    ElMessage.warning('当前 SQL 含有危险操作，请先开启危险语句开关或修改 SQL')
    return
  }
  if (looksDangerousSql(form.sql)) {
    try {
      await ElMessageBox.confirm('确认执行这条包含危险操作的 SQL 吗？', '执行确认', { type: 'warning' })
    } catch (error) {
      return
    }
  }
  executing.value = true
  executionError.value = ''
  try {
    const response = await executeSql({
      datasource: datasource,
      sql: form.sql,
      allowDangerousSql: form.allowDangerousSql
    })
    result.columns = response.columns || []
    result.rows = response.rows || []
    executionSummary.elapsedText = formatDuration(response.elapsedMillis)
    executionSummary.affectedRows = response.affectedRows != null ? response.affectedRows : '-'
    executionSummary.statementType = response.statementType || ''
    executionSummary.success = !!response.success
    ElMessage.success(response.message || '执行成功')
    await loadLogs()
  } catch (error) {
    executionError.value = error.message || 'SQL 执行失败'
    executionSummary.elapsedText = '-'
    executionSummary.affectedRows = '-'
    executionSummary.statementType = ''
    executionSummary.success = false
    ElMessage.error(executionError.value)
  } finally {
    executing.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    logs.value = await listSqlExecutionLogs(20)
  } catch (error) {
    ElMessage.error(error.message || '加载 SQL 日志失败')
  } finally {
    logsLoading.value = false
  }
}

function clearSql() {
  form.sql = ''
  result.columns = []
  result.rows = []
  executionError.value = ''
  executionSummary.elapsedText = '-'
  executionSummary.affectedRows = '-'
  executionSummary.statementType = ''
  executionSummary.success = false
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function formatDuration(value) {
  if (value == null) {
    return '-'
  }
  return Number(value) + ' ms'
}

function looksDangerousSql(sql) {
  if (!sql) {
    return false
  }
  const normalized = String(sql).toLowerCase()
  return normalized.indexOf('drop ') !== -1 || normalized.indexOf('truncate ') !== -1 || normalized.indexOf('alter ') !== -1
}
</script>
