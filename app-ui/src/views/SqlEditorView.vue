<template>
  <div class="page-section sql-workbench">
    <div class="page-header sql-workbench__header">
      <div>
        <h1>SQL 编辑器</h1>
        <p>选择数据源后执行 SQL，查看结果集、影响行数、执行耗时和日志回放。</p>
      </div>
      <el-space wrap>
        <el-button round @click="loadLogs">刷新日志</el-button>
        <el-button
          type="primary"
          round
          :loading="executing"
          :disabled="!canExecute"
          @click="executeCurrentSql"
        >
          执行 SQL
        </el-button>
      </el-space>
    </div>

    <div class="sql-workbench__overview page-overview page-overview--four">
      <div class="page-overview__item">
        <div class="page-overview__label">当前数据源</div>
        <div class="page-overview__value">{{ selectedDatasourceName || '未选择' }}</div>
        <div class="page-overview__hint">{{ datasourceContextText }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">最近执行</div>
        <div class="page-overview__value">{{ recentStatusLabel }}</div>
        <div class="page-overview__hint">{{ recentStatusHint }}</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">执行日志</div>
        <div class="page-overview__value">{{ logs.length }} 条</div>
        <div class="page-overview__hint">保留最近 20 条执行记录</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">安全策略</div>
        <div class="page-overview__value">{{ safePolicyLabel }}</div>
        <div class="page-overview__hint">{{ safePolicyHint }}</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact sql-workbench__workspace">
      <div class="panel-card sql-console-card">
        <div class="section-title sql-console-card__title">
          <div class="section-title__left">
            <h2>SQL 控制台</h2>
            <el-tag :type="consoleStatusTagType" effect="light">{{ consoleStatusText }}</el-tag>
          </div>
          <el-tag :type="policyTagType" effect="light">{{ safePolicyLabel }}</el-tag>
        </div>

        <div class="sql-console-card__body">
          <div class="sql-console-block">
            <div class="sql-console-block__title">数据源</div>
            <div class="sql-console-block__field">
              <div class="sql-console-block__label">数据源</div>
              <el-select
                v-model="form.datasourceId"
                placeholder="请选择数据源"
                style="width: 100%;"
                @change="handleDatasourceChange"
              >
                <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </div>
            <div class="sql-console-block__meta">
              <div class="sql-console-info-row">
                <span class="sql-console-info-row__label">数据库类型</span>
                <span class="sql-console-info-row__value">{{ selectedDatasourceType || '—' }}</span>
              </div>
              <div class="sql-console-info-row">
                <span class="sql-console-info-row__label">数据库名</span>
                <span class="sql-console-info-row__value">{{ selectedDatasourceDatabase || '—' }}</span>
              </div>
              <div class="sql-console-info-row">
                <span class="sql-console-info-row__label">连接状态</span>
                <span class="sql-console-info-row__value">{{ selectedDatasourceName ? '已选择' : '未选择' }}</span>
              </div>
            </div>
          </div>

          <div class="sql-console-block sql-console-block--policy" :class="{ 'is-warning': allowDangerousSql }">
            <div class="sql-console-block__title">安全策略</div>
            <div class="sql-console-block__desc">
              默认仅允许安全查询，危险语句需要手动开启。
            </div>
            <div class="sql-console-block__toggle">
              <div>
                <div class="sql-console-block__label">允许危险语句</div>
                <div class="sql-console-block__toggle-hint">
                  开启后允许 INSERT / UPDATE / DELETE / DDL，请确认目标库、WHERE 条件和影响范围。
                </div>
              </div>
              <el-switch v-model="form.allowDangerousSql" />
            </div>
            <el-alert
              v-if="safeBlocked"
              type="warning"
              show-icon
              :closable="false"
              title="危险 SQL 已被拦截：当前未开启危险语句执行权限。"
              class="sql-console-block__alert"
            />
          </div>

          <div class="sql-console-block sql-console-block--editor">
            <div class="sql-console-block__title">SQL 语句</div>
            <el-input
              v-model="form.sql"
              type="textarea"
              :rows="14"
              placeholder="请输入 SELECT / INSERT / UPDATE / DELETE 语句"
              class="sql-editor"
            />
          </div>

          <div class="sql-console-footer">
            <div class="sql-console-footer__status">
              <span>状态：{{ statusLabel }}</span>
              <span>耗时：{{ executionSummary.elapsedText }}</span>
              <span>影响行数：{{ executionSummary.affectedRows }}</span>
            </div>
            <div class="sql-console-footer__actions">
              <el-tooltip :disabled="canExecute" :content="executeDisabledReason" placement="top">
                <span>
                  <el-button
                    type="primary"
                    :loading="executing"
                    :disabled="!canExecute"
                    @click="executeCurrentSql"
                  >
                    执行 SQL
                  </el-button>
                </span>
              </el-tooltip>
              <el-button @click="clearSql">清空</el-button>
            </div>
          </div>

          <el-alert
            v-if="executionError"
            :title="executionError"
            type="error"
            show-icon
            class="sql-console-block__alert"
          />
        </div>
      </div>

      <div class="panel-card sql-result-card">
        <div class="section-title sql-result-card__title">
          <div class="section-title__left">
            <h2>执行结果</h2>
            <el-tag :type="resultStatusTagType" effect="light">{{ resultStatusText }}</el-tag>
          </div>
          <el-space>
            <el-button text @click="loadLogs">刷新历史</el-button>
          </el-space>
        </div>

        <div class="sql-result-card__content">
          <el-tabs v-model="activeResultTab" class="sql-result-tabs">
            <el-tab-pane label="结果" name="result">
              <div class="sql-result-pane">
                <div class="sql-result-summary" :class="resultSummaryClass">
                  <div class="sql-result-summary__title">{{ resultTitle }}</div>
                  <div class="sql-result-summary__meta">{{ resultMetaText }}</div>
                  <div class="sql-result-summary__hint">{{ resultHint }}</div>
                </div>

                <div v-if="hasResultTable" class="table-shell sql-result-table">
                  <el-table :data="result.rows" border stripe>
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

                <div v-else class="sql-result-empty">
                  <el-empty :description="resultEmptyDescription" />
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="消息" name="message">
              <div class="sql-result-pane">
                <div v-if="messageItems.length" class="sql-message-list">
                  <div v-for="item in messageItems" :key="item.label" class="sql-message-item">
                    <span class="sql-message-item__label">{{ item.label }}</span>
                    <span class="sql-message-item__value">{{ item.value }}</span>
                  </div>
                </div>
                <el-empty v-else description="暂无执行消息" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="错误" name="error">
              <div class="sql-result-pane">
                <div v-if="executionError" class="sql-error-box">
                  <div class="sql-error-box__title">错误摘要</div>
                  <div class="sql-error-box__body">{{ executionError }}</div>
                  <div class="sql-error-box__hint">{{ errorHint }}</div>
                </div>
                <el-empty v-else description="暂无错误信息" />
              </div>
            </el-tab-pane>

            <el-tab-pane label="历史" name="history">
              <div class="sql-result-pane">
                <div class="table-shell sql-history-table">
                  <el-table :data="logs" border stripe v-loading="logsLoading">
                    <el-table-column prop="createdAt" label="时间" min-width="180">
                      <template #default="{ row }">
                        {{ formatTime(row.createdAt) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="datasourceName" label="数据源" min-width="150">
                      <template #default="{ row }">
                        {{ row.datasourceName || selectedDatasourceName || '—' }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="statementType" label="类型" width="110" />
                    <el-table-column prop="success" label="状态" width="120">
                      <template #default="{ row }">
                        <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="elapsedMillis" label="耗时" width="120">
                      <template #default="{ row }">
                        {{ formatDuration(row.elapsedMillis) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="affectedRows" label="影响行数" width="120" />
                    <el-table-column prop="sqlText" label="SQL 摘要" min-width="280" show-overflow-tooltip />
                    <el-table-column prop="errorMessage" label="错误摘要" min-width="240" show-overflow-tooltip />
                  </el-table>
                </div>
                <el-empty v-if="!logs.length && !logsLoading" description="暂无执行记录，执行 SQL 后会保留最近 20 条日志。" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel sql-history-panel">
      <div class="section-title">
        <div class="section-title__left">
          <h2>最近执行记录</h2>
          <el-tag type="info" effect="light">{{ logs.length }} 条</el-tag>
        </div>
        <div class="section-title__sub">时间 / 数据源 / 类型 / 状态 / 耗时 / 影响行数 / SQL 摘要 / 错误</div>
      </div>
      <div class="table-shell sql-history-panel__table">
        <el-table :data="logs" border stripe v-loading="logsLoading">
          <el-table-column prop="createdAt" label="时间" min-width="180">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="datasourceName" label="数据源" min-width="150">
            <template #default="{ row }">
              {{ row.datasourceName || selectedDatasourceName || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="statementType" label="SQL 类型" width="110" />
          <el-table-column prop="success" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="elapsedMillis" label="耗时" width="120">
            <template #default="{ row }">
              {{ formatDuration(row.elapsedMillis) }}
            </template>
          </el-table-column>
          <el-table-column prop="affectedRows" label="影响行数" width="120" />
          <el-table-column prop="sqlText" label="SQL 摘要" min-width="320" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="错误摘要" min-width="240" show-overflow-tooltip />
        </el-table>
      </div>
      <el-empty v-if="!logs.length && !logsLoading" description="暂无执行记录，执行 SQL 后会保留最近 20 条日志。" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { executeSql, listDatasources, listSqlExecutionLogs } from '../services/backend'

const datasources = ref([])
const selectedDatasourceName = ref('')
const selectedDatasourceType = ref('')
const selectedDatasourceDatabase = ref('')
const executing = ref(false)
const logsLoading = ref(false)
const executionError = ref('')
const logs = ref([])
const activeResultTab = ref('result')
const result = reactive({
  columns: [],
  rows: []
})
const executionSummary = reactive({
  elapsedText: '—',
  affectedRows: '—',
  statementType: '',
  success: false,
  blocked: false
})
const form = reactive({
  datasourceId: null,
  sql: '',
  allowDangerousSql: false
})

const canExecute = computed(function () {
  return !!form.datasourceId && !!String(form.sql || '').trim() && !executing.value && (form.allowDangerousSql || isReadonlySql(form.sql))
})

const executeDisabledReason = computed(function () {
  if (executing.value) {
    return '正在执行 SQL'
  }
  if (!form.datasourceId) {
    return '请先选择数据源'
  }
  if (!String(form.sql || '').trim()) {
    return '请输入 SQL'
  }
  if (!form.allowDangerousSql && !isReadonlySql(form.sql)) {
    return '当前为安全模式，仅允许 SELECT'
  }
  return '当前状态不可执行'
})

const safeBlocked = computed(function () {
  return !!executionSummary.blocked
})

const hasResultTable = computed(function () {
  return executionSummary.success && result.columns.length > 0 && result.rows.length > 0
})

const statusLabel = computed(function () {
  return consoleStatusText.value
})

const consoleStatusText = computed(function () {
  if (executing.value) {
    return '执行中'
  }
  if (!form.datasourceId) {
    return '未选择数据源'
  }
  if (safeBlocked.value) {
    return '被拦截'
  }
  if (executionSummary.success) {
    return '执行成功'
  }
  if (executionError.value) {
    return '执行失败'
  }
  if (canExecute.value) {
    return '可执行'
  }
  return '未执行'
})

const consoleStatusTagType = computed(function () {
  if (executing.value) {
    return 'warning'
  }
  if (safeBlocked.value || executionError.value) {
    return 'danger'
  }
  if (executionSummary.success) {
    return 'success'
  }
  if (canExecute.value) {
    return 'primary'
  }
  return 'info'
})

const resultStatusText = computed(function () {
  return consoleStatusText.value
})

const resultStatusTagType = computed(function () {
  return consoleStatusTagType.value
})

const recentStatusLabel = computed(function () {
  if (safeBlocked.value) {
    return '被安全策略拦截'
  }
  if (executionSummary.success) {
    return '执行成功'
  }
  if (executionError.value) {
    return '执行失败'
  }
  return '未执行'
})

const recentStatusHint = computed(function () {
  if (safeBlocked.value) {
    return '危险 SQL 未允许'
  }
  if (executionSummary.success) {
    return '耗时 ' + executionSummary.elapsedText + ' / 影响行数 ' + executionSummary.affectedRows
  }
  if (executionError.value) {
    return '查看错误详情'
  }
  return '耗时 — / 影响行数 —'
})

const safePolicyLabel = computed(function () {
  return form.allowDangerousSql ? '已允许危险语句' : '默认安全模式'
})

const safePolicyHint = computed(function () {
  return form.allowDangerousSql ? '危险 SQL 需要二次确认' : '默认仅允许 SELECT'
})

const policyTagType = computed(function () {
  return form.allowDangerousSql ? 'warning' : 'info'
})

const datasourceContextText = computed(function () {
  const type = selectedDatasourceType.value || '—'
  const database = selectedDatasourceDatabase.value || '—'
  return type + ' · ' + database
})

const resultTitle = computed(function () {
  if (safeBlocked.value) {
    return '危险 SQL 已被拦截'
  }
  if (executionError.value) {
    return '执行失败'
  }
  if (executionSummary.success) {
    if (result.columns.length > 0) {
      return '执行成功'
    }
    return '执行成功'
  }
  if (canExecute.value) {
    return 'SQL 已就绪'
  }
  return '还没有执行 SQL'
})

const resultMetaText = computed(function () {
  if (safeBlocked.value) {
    return '当前 SQL 命中了高风险规则，且未开启危险语句执行权限。'
  }
  if (executionError.value) {
    return errorSummary.value
  }
  if (executionSummary.success) {
    const rowText = result.rows.length > 0 ? '返回 ' + result.rows.length + ' 行' : '返回 0 行'
    const columnText = result.columns.length > 0 ? '字段 ' + result.columns.length + ' 个' : '字段 0 个'
    return rowText + ' · ' + columnText + ' · 耗时 ' + executionSummary.elapsedText
  }
  if (canExecute.value) {
    return '当前 SQL 可以执行。点击“执行 SQL”运行当前语句。'
  }
  return '选择数据源并输入 SQL 后，点击执行查看结果集、影响行数和执行耗时。'
})

const resultHint = computed(function () {
  if (safeBlocked.value) {
    return '处理建议：确认目标库和 WHERE 条件后，再手动开启危险语句。'
  }
  if (executionError.value) {
    return errorHint.value
  }
  if (executionSummary.success) {
    if (result.columns.length > 0) {
      return '结果表格展示当前查询返回的数据。'
    }
    return '非查询语句按影响行数和执行消息展示。'
  }
  if (canExecute.value) {
    return '主按钮位于左侧 Console 底部。'
  }
  return '主按钮位于左侧 Console 底部。'
})

const resultSummaryClass = computed(function () {
  return {
    'is-success': executionSummary.success,
    'is-warning': safeBlocked.value,
    'is-error': !!executionError.value
  }
})

const resultEmptyDescription = computed(function () {
  if (safeBlocked.value) {
    return '危险 SQL 已被拦截，切换到“消息”或“错误”查看原因。'
  }
  if (executionError.value) {
    return '执行失败，切换到“错误”查看详情。'
  }
  if (canExecute.value) {
    return 'SQL 已就绪，点击执行后这里会展示结果。'
  }
  return '选择数据源并输入 SQL 后，点击执行查看结果。'
})

const errorSummary = computed(function () {
  if (!executionError.value) {
    return '暂无错误信息'
  }
  return executionError.value
})

const errorHint = computed(function () {
  if (!executionError.value) {
    return '暂无错误信息'
  }
  return '可能原因：SQL 语法错误、数据源连接失败、权限不足、表不存在、危险语句限制。'
})

const messageItems = computed(function () {
  if (!executionSummary.statementType && !selectedDatasourceName.value && !executionSummary.success && !executionError.value) {
    return []
  }
  const items = []
  items.push({ label: 'SQL 已发送', value: executionSummary.statementType ? 'SQL 类型：' + executionSummary.statementType : '等待执行' })
  items.push({ label: '数据源', value: selectedDatasourceName.value || '未选择' })
  items.push({ label: '执行状态', value: resultStatusText.value })
  items.push({ label: '返回行数', value: executionSummary.success && result.rows.length > 0 ? String(result.rows.length) : '—' })
  items.push({ label: '影响行数', value: executionSummary.affectedRows })
  items.push({ label: '执行耗时', value: executionSummary.elapsedText })
  return items
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    datasources.value = await listDatasources()
    if (datasources.value.length > 0) {
      form.datasourceId = datasources.value[0].id
      applyDatasource(datasources.value[0])
    }
    await loadLogs()
  } catch (error) {
    ElMessage.error(error.message || '加载 SQL 编辑器失败')
  }
}

function applyDatasource(datasource) {
  selectedDatasourceName.value = datasource ? datasource.name : ''
  selectedDatasourceType.value = datasource ? (datasource.type || datasource.dbType || '—') : ''
  selectedDatasourceDatabase.value = datasource ? (datasource.databaseName || datasource.schemaName || datasource.database || '—') : ''
}

function handleDatasourceChange(value) {
  const datasource = datasources.value.find(function (item) {
    return item.id === value
  })
  applyDatasource(datasource)
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
    executionSummary.elapsedText = '—'
    executionSummary.affectedRows = '—'
    executionSummary.statementType = detectSqlType(form.sql)
    executionSummary.success = false
    executionSummary.blocked = true
    executionError.value = '危险 SQL 已被拦截：当前未开启危险语句执行权限。'
    result.columns = []
    result.rows = []
    activeResultTab.value = 'error'
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
  executionSummary.blocked = false
  activeResultTab.value = 'result'
  try {
    const response = await executeSql({
      datasource: datasource,
      sql: form.sql,
      allowDangerousSql: form.allowDangerousSql
    })
    result.columns = response.columns || []
    result.rows = response.rows || []
    executionSummary.elapsedText = formatDuration(response.elapsedMillis)
    executionSummary.affectedRows = response.affectedRows != null ? String(response.affectedRows) : '—'
    executionSummary.statementType = response.statementType || detectSqlType(form.sql)
    executionSummary.success = !!response.success
    ElMessage.success(response.message || '执行成功')
    await loadLogs()
  } catch (error) {
    executionError.value = error.message || 'SQL 执行失败'
    executionSummary.elapsedText = '—'
    executionSummary.affectedRows = '—'
    executionSummary.statementType = detectSqlType(form.sql)
    executionSummary.success = false
    executionSummary.blocked = false
    result.columns = []
    result.rows = []
    activeResultTab.value = 'error'
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
  executionSummary.elapsedText = '—'
  executionSummary.affectedRows = '—'
  executionSummary.statementType = ''
  executionSummary.success = false
  executionSummary.blocked = false
  activeResultTab.value = 'result'
}

function formatTime(value) {
  if (!value) {
    return '—'
  }
  return new Date(value).toLocaleString()
}

function formatDuration(value) {
  if (value == null && value !== 0) {
    return '—'
  }
  return Number(value) + ' ms'
}

function looksDangerousSql(sql) {
  if (!sql) {
    return false
  }
  const normalized = String(sql).toLowerCase()
  return !/^\s*select\b/.test(normalized)
}

function isReadonlySql(sql) {
  if (!sql) {
    return false
  }
  return /^\s*select\b/.test(String(sql).toLowerCase())
}

function detectSqlType(sql) {
  const normalized = String(sql || '').trim().toLowerCase()
  if (!normalized) {
    return ''
  }
  const firstWord = normalized.split(/\s+/)[0]
  return String(firstWord || '').toUpperCase()
}
</script>
