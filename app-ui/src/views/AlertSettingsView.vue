<template>
  <div class="page-section alert-settings">
    <div class="page-header">
      <div>
        <h1>告警设置</h1>
        <p>管理告警规则、告警渠道与渠道测试，敏感信息不会明文展示。</p>
      </div>
      <el-space>
        <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
        <el-button type="primary" round @click="openRuleDialog()">新增规则</el-button>
        <el-button type="primary" plain round @click="openChannelDialog()">新增渠道</el-button>
      </el-space>
    </div>

    <div class="page-overview page-overview--three">
      <div class="page-overview__item">
        <div class="page-overview__label">告警规则</div>
        <div class="page-overview__value">{{ rules.length }}</div>
        <div class="page-overview__hint">支持启用/停用、冷却时间和任务/表级绑定</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">告警渠道</div>
        <div class="page-overview__value">{{ channels.length }}</div>
        <div class="page-overview__hint">SMTP 与 Webhook 最小可用通道</div>
      </div>
      <div class="page-overview__item">
        <div class="page-overview__label">可用任务</div>
        <div class="page-overview__value">{{ tasks.length }}</div>
        <div class="page-overview__hint">规则可直接绑定已有同步任务</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>告警规则</h2>
          <el-tag type="warning" effect="dark">{{ enabledRuleCount }} 个启用</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="rules" border stripe v-loading="loading">
            <el-table-column prop="ruleName" label="规则名" min-width="180" />
            <el-table-column prop="alertType" label="类型" width="180" />
            <el-table-column label="任务" width="120">
              <template #default="{ row }">
                {{ resolveTaskName(row.taskId) }}
              </template>
            </el-table-column>
            <el-table-column prop="tableName" label="表名" min-width="140" />
            <el-table-column label="级别" width="100">
              <template #default="{ row }">
                <el-tag :type="alertLevelTagType(row.alertLevel)">{{ row.alertLevel || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="cooldownSeconds" label="冷却" width="100">
              <template #default="{ row }">
                {{ row.cooldownSeconds || 600 }}s
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="渠道" min-width="180">
              <template #default="{ row }">
                {{ resolveChannelNames(row.channelIdsJson) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" @click.stop="openRuleDialog(row)">编辑</el-button>
                  <el-button
                    size="small"
                    type="danger"
                    :loading="deletingRuleId === row.id"
                    :disabled="deletingRuleId !== null || deletingChannelId !== null || testingChannelId !== null"
                    @click.stop="removeRule(row)"
                  >
                    删除
                  </el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
          <StateEmpty
            v-if="!loading && !rules.length"
            title="还没有告警规则"
            description="先创建一个规则，才能按任务或表触发告警。"
            hint="建议先去帮助文档看一下告警配置流程。"
            button-text="新增规则"
            @action="openRuleDialog()"
          />
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>告警渠道</h2>
          <el-tag type="success" effect="dark">{{ enabledChannelCount }} 个启用</el-tag>
        </div>
        <div class="table-shell">
          <el-table :data="channels" border stripe v-loading="loading">
            <el-table-column prop="channelName" label="名称" min-width="160" />
            <el-table-column prop="channelType" label="类型" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="SMTP 收件人" min-width="180">
              <template #default="{ row }">
                {{ maskDisplay(row.smtpToAddress) }}
              </template>
            </el-table-column>
            <el-table-column label="Webhook" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ maskDisplay(row.webhookUrl) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" type="primary" plain @click.stop="openChannelDialog(row)">编辑</el-button>
                  <el-button
                    size="small"
                    :loading="testingChannelId === row.id"
                    :disabled="deletingRuleId !== null || deletingChannelId !== null || testingChannelId !== null"
                    @click.stop="testChannel(row)"
                  >
                    测试
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    :loading="deletingChannelId === row.id"
                    :disabled="deletingRuleId !== null || deletingChannelId !== null || testingChannelId !== null"
                    @click.stop="removeChannel(row)"
                  >
                    删除
                  </el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
          <StateEmpty
            v-if="!loading && !channels.length"
            title="还没有告警渠道"
            description="先配置一个 SMTP 或 Webhook 渠道，规则才能发送通知。"
            hint="渠道保存后可以立即测试发送。"
            button-text="新增渠道"
            @action="openChannelDialog()"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="ruleDialogVisible" :title="ruleForm.id ? '编辑告警规则' : '新增告警规则'" width="760px">
      <el-form :model="ruleForm" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则名称">
              <el-input v-model="ruleForm.ruleName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="告警类型">
              <el-select v-model="ruleForm.alertType" style="width: 100%;">
                <el-option v-for="item in alertTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="任务 ID">
              <el-select v-model="ruleForm.taskId" clearable filterable style="width: 100%;">
                <el-option v-for="task in tasks" :key="task.id" :label="task.taskName" :value="task.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="表名">
              <el-input v-model="ruleForm.tableName" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="告警级别">
              <el-select v-model="ruleForm.alertLevel" style="width: 100%;">
                <el-option label="INFO" value="INFO" />
                <el-option label="WARNING" value="WARNING" />
                <el-option label="ERROR" value="ERROR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="冷却秒数">
              <el-input-number v-model="ruleForm.cooldownSeconds" :min="60" :step="60" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="通知渠道">
          <el-select v-model="ruleForm.channelIds" multiple collapse-tags filterable style="width: 100%;">
            <el-option v-for="channel in channels" :key="channel.id" :label="channel.channelName" :value="channel.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容模板">
          <el-input v-model="ruleForm.alertContentTemplate" :rows="4" type="textarea" placeholder="支持 ${taskId}、${runId}、${tableName}、${content}" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-space>
          <el-button @click="ruleDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingRule" @click="saveRule">保存</el-button>
        </el-space>
      </template>
    </el-dialog>

    <el-dialog v-model="channelDialogVisible" :title="channelForm.id ? '编辑告警渠道' : '新增告警渠道'" width="780px">
      <el-form :model="channelForm" label-width="140px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="渠道名称">
              <el-input v-model="channelForm.channelName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道类型">
              <el-select v-model="channelForm.channelType" style="width: 100%;">
                <el-option label="SMTP" value="SMTP" />
                <el-option label="Webhook" value="WEBHOOK" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="启用">
          <el-switch v-model="channelForm.enabled" />
        </el-form-item>
        <template v-if="channelForm.channelType === 'SMTP'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="SMTP 主机">
                <el-input v-model="channelForm.smtpHost" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SMTP 端口">
                <el-input-number v-model="channelForm.smtpPort" :min="1" :max="65535" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="SMTP 用户名">
                <el-input v-model="channelForm.smtpUsername" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SMTP 密码">
                <el-input v-model="channelForm.smtpPassword" type="password" show-password placeholder="留空则保留原值" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="收件人">
                <el-input v-model="channelForm.smtpToAddress" placeholder="可留空或编辑时保留原值" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发件人">
                <el-input v-model="channelForm.smtpFromAddress" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <template v-else>
          <el-form-item label="Webhook 地址">
            <el-input v-model="channelForm.webhookUrl" />
          </el-form-item>
          <el-form-item label="Webhook Token">
            <el-input v-model="channelForm.webhookToken" type="password" show-password placeholder="留空则保留原值" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-space>
          <el-button @click="channelDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingChannel" @click="saveChannel">保存</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteAlertChannel,
  deleteAlertRule,
  listAlertChannels,
  listAlertRules,
  listTasks,
  saveAlertChannel,
  saveAlertRule,
  testAlertChannel
} from '../services/backend'
import StateEmpty from '../components/StateEmpty.vue'

const router = useRouter()
const loading = ref(false)
const savingRule = ref(false)
const savingChannel = ref(false)
const testingChannelId = ref(null)
const deletingRuleId = ref(null)
const deletingChannelId = ref(null)
const ruleDialogVisible = ref(false)
const channelDialogVisible = ref(false)
const tasks = ref([])
const rules = ref([])
const channels = ref([])

const alertTypeOptions = [
  { label: '任务执行失败', value: 'TASK_EXECUTION_FAILED' },
  { label: '任务连续失败 N 次', value: 'TASK_CONSECUTIVE_FAILED' },
  { label: '数据校验不一致', value: 'VALIDATION_INCONSISTENT' },
  { label: '数据修复失败', value: 'REPAIR_FAILED' },
  { label: '数据源连接失败', value: 'DATASOURCE_CONNECTION_FAILED' },
  { label: '同步耗时超过阈值', value: 'TASK_DURATION_EXCEEDED' },
  { label: '同步延迟超过阈值', value: 'SYNC_DELAY_EXCEEDED' },
  { label: '表同步失败', value: 'TABLE_SYNC_FAILED' },
  { label: '调度任务跳过执行', value: 'SCHEDULE_SKIPPED' }
]

const ruleForm = reactive(createEmptyRuleForm())
const channelForm = reactive(createEmptyChannelForm())

const enabledRuleCount = computed(function () {
  return rules.value.filter(function (item) {
    return !!item.enabled
  }).length
})

const enabledChannelCount = computed(function () {
  return channels.value.filter(function (item) {
    return !!item.enabled
  }).length
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  loading.value = true
  try {
    const result = await Promise.all([listTasks(), listAlertRules(), listAlertChannels()])
    tasks.value = result[0] || []
    rules.value = result[1] || []
    channels.value = result[2] || []
  } catch (error) {
    ElMessage.error(error.message || '加载告警设置失败')
  } finally {
    loading.value = false
  }
}

function openRuleDialog(row) {
  Object.assign(ruleForm, createEmptyRuleForm())
  if (row) {
    Object.assign(ruleForm, {
      id: row.id,
      ruleName: row.ruleName || '',
      alertType: row.alertType || 'TASK_EXECUTION_FAILED',
      taskId: row.taskId || null,
      tableName: row.tableName || '',
      alertLevel: row.alertLevel || 'ERROR',
      alertContentTemplate: row.alertContentTemplate || '',
      channelIds: parseChannelIds(row.channelIdsJson),
      enabled: row.enabled !== false,
      cooldownSeconds: row.cooldownSeconds || 600
    })
  }
  ruleDialogVisible.value = true
}

function openChannelDialog(row) {
  Object.assign(channelForm, createEmptyChannelForm())
  if (row) {
    Object.assign(channelForm, {
      id: row.id,
      channelName: row.channelName || '',
      channelType: row.channelType || 'SMTP',
      enabled: row.enabled !== false,
      smtpHost: row.smtpHost || '',
      smtpPort: row.smtpPort || 25,
      smtpUsername: row.smtpUsername || '',
      smtpPassword: '',
      smtpToAddress: '',
      smtpFromAddress: row.smtpFromAddress || '',
      webhookUrl: row.webhookUrl || '',
      webhookToken: ''
    })
  }
  channelDialogVisible.value = true
}

async function saveRule() {
  savingRule.value = true
  try {
    await saveAlertRule({
      id: ruleForm.id,
      ruleName: ruleForm.ruleName,
      alertType: ruleForm.alertType,
      taskId: ruleForm.taskId,
      tableName: ruleForm.tableName,
      alertLevel: ruleForm.alertLevel,
      alertContentTemplate: ruleForm.alertContentTemplate,
      channelIdsJson: JSON.stringify(ruleForm.channelIds || []),
      enabled: ruleForm.enabled,
      cooldownSeconds: ruleForm.cooldownSeconds
    })
    ElMessage.success('规则已保存')
    ruleDialogVisible.value = false
    await loadPageData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存规则失败'))
  } finally {
    savingRule.value = false
  }
}

async function saveChannel() {
  savingChannel.value = true
  try {
    await saveAlertChannel({
      id: channelForm.id,
      channelName: channelForm.channelName,
      channelType: channelForm.channelType,
      enabled: channelForm.enabled,
      smtpHost: channelForm.smtpHost,
      smtpPort: channelForm.smtpPort,
      smtpUsername: channelForm.smtpUsername,
      smtpPassword: channelForm.smtpPassword,
      smtpToAddress: channelForm.smtpToAddress,
      smtpFromAddress: channelForm.smtpFromAddress,
      webhookUrl: channelForm.webhookUrl,
      webhookToken: channelForm.webhookToken
    })
    ElMessage.success('渠道已保存')
    channelDialogVisible.value = false
    await loadPageData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '保存渠道失败'))
  } finally {
    savingChannel.value = false
  }
}

async function testChannel(row) {
  try {
    await ElMessageBox.confirm('确认向该渠道发送测试告警吗？', '提示', {
      type: 'warning'
    })
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(getErrorMessage(error, '测试告警确认失败'))
    return
  }

  testingChannelId.value = row.id
  try {
    await testAlertChannel({
      channelId: row.id,
      content: '这是一条来自 DB Sync Studio 的测试告警'
    })
    ElMessage.success('测试告警已发送')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '测试告警失败'))
  } finally {
    testingChannelId.value = null
  }
}

async function removeRule(row) {
  try {
    await ElMessageBox.confirm('确认删除该告警规则吗？', '提示', { type: 'warning' })
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(getErrorMessage(error, '删除告警规则确认失败'))
    return
  }

  deletingRuleId.value = row.id
  try {
    await deleteAlertRule(row.id)
    ElMessage.success('规则已删除')
    await loadPageData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除规则失败'))
  } finally {
    deletingRuleId.value = null
  }
}

async function removeChannel(row) {
  try {
    await ElMessageBox.confirm('确认删除该告警渠道吗？', '提示', { type: 'warning' })
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(getErrorMessage(error, '删除告警渠道确认失败'))
    return
  }

  deletingChannelId.value = row.id
  try {
    await deleteAlertChannel(row.id)
    ElMessage.success('渠道已删除')
    await loadPageData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '删除渠道失败'))
  } finally {
    deletingChannelId.value = null
  }
}

function createEmptyRuleForm() {
  return {
    id: null,
    ruleName: '',
    alertType: 'TASK_EXECUTION_FAILED',
    taskId: null,
    tableName: '',
    alertLevel: 'ERROR',
    alertContentTemplate: '${content}',
    channelIds: [],
    enabled: true,
    cooldownSeconds: 600
  }
}

function createEmptyChannelForm() {
  return {
    id: null,
    channelName: '',
    channelType: 'SMTP',
    enabled: true,
    smtpHost: '',
    smtpPort: 25,
    smtpUsername: '',
    smtpPassword: '',
    smtpToAddress: '',
    smtpFromAddress: '',
    webhookUrl: '',
    webhookToken: ''
  }
}

function parseChannelIds(value) {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

function resolveTaskName(taskId) {
  const task = tasks.value.find(function (item) {
    return item.id === taskId
  })
  return task ? task.taskName : (taskId || '-')
}

function resolveChannelNames(channelIdsJson) {
  const ids = parseChannelIds(channelIdsJson)
  if (!ids.length) {
    return '-'
  }
  return ids.map(function (id) {
    const channel = channels.value.find(function (item) {
      return item.id === id
    })
    return channel ? channel.channelName : ('#' + id)
  }).join('，')
}

function alertLevelTagType(level) {
  if (level === 'ERROR') {
    return 'danger'
  }
  if (level === 'WARNING') {
    return 'warning'
  }
  return 'info'
}

function maskDisplay(value) {
  if (!value) {
    return '-'
  }
  if (value.length <= 8) {
    return '****'
  }
  return value.slice(0, 3) + '****' + value.slice(-3)
}

function getErrorMessage(error, fallback) {
  if (error && error.message) {
    return error.message
  }
  return fallback
}
</script>
