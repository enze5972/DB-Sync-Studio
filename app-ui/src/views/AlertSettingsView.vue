<template>
  <div class="page-section alert-settings-workbench">
    <div class="page-header alert-settings-workbench__header">
      <div class="alert-settings-workbench__titleblock">
        <h1>告警设置</h1>
        <p>管理告警规则、通知渠道和渠道测试，敏感信息不会明文展示。</p>
      </div>
      <div class="alert-settings-workbench__toolbar">
        <div class="alert-settings-workbench__toolbar-actions">
          <el-button round :loading="loading" @click="loadPageData">刷新</el-button>
          <el-button type="primary" round :disabled="!canCreateRule" @click="openRuleDialog()">
            新增规则
          </el-button>
          <el-button type="primary" plain round @click="openChannelDialog()">新增渠道</el-button>
        </div>
        <div class="alert-settings-workbench__toolbar-note">
          {{ toolbarHint }}
        </div>
      </div>
    </div>

    <div class="stats-grid alert-settings-workbench__stats">
      <div v-for="item in summaryCards" :key="item.label" class="stat-card alert-settings-workbench__stat-card">
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
        <div class="stat-card__hint">{{ item.hint }}</div>
      </div>
    </div>

    <div class="dashboard-panels dashboard-panels--compact alert-settings-workbench__workspace">
      <div class="panel-card alert-settings-workbench__panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>告警规则</h2>
            <el-tag :type="ruleSummaryTagType" effect="light">{{ ruleSummaryText }}</el-tag>
          </div>
          <el-tag effect="dark" type="warning">{{ enabledRuleCount }} 个启用</el-tag>
        </div>
        <div class="alert-settings-workbench__panel-hint">
          <span>{{ ruleHint }}</span>
        </div>
        <template v-if="rules.length">
          <div class="table-shell">
            <el-table :data="rules" border stripe v-loading="loading">
              <el-table-column label="规则" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="alert-settings-workbench__cell-stack">
                    <div class="alert-settings-workbench__primary">{{ row.ruleName || '-' }}</div>
                    <div class="alert-settings-workbench__secondary">{{ alertTypeLabel(row.alertType) }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="触发对象" min-width="160" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ resolveRuleTrigger(row) }}
                </template>
              </el-table-column>
              <el-table-column label="条件" min-width="210" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ resolveRuleCondition(row) }}
                </template>
              </el-table-column>
              <el-table-column label="渠道" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ resolveChannelNames(row.channelIdsJson) }}
                </template>
              </el-table-column>
              <el-table-column label="状态" width="160">
                <template #default="{ row }">
                  <div class="alert-settings-workbench__chips">
                    <el-tag :type="ruleStatusTagType(row)" effect="light">{{ ruleStatusLabel(row) }}</el-tag>
                    <el-tag v-if="!hasBoundTask(row)" type="warning" effect="light">未绑定任务</el-tag>
                    <el-tag v-if="!hasBoundChannel(row)" type="info" effect="light">未绑定渠道</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="280" fixed="right">
                <template #default="{ row }">
                  <el-space>
                    <el-button size="small" @click.stop="openRuleDialog(row)">编辑</el-button>
                    <el-button
                      size="small"
                      :type="row.enabled ? 'warning' : 'success'"
                      @click.stop="toggleRuleEnabled(row)"
                    >
                      {{ row.enabled ? '停用' : '启用' }}
                    </el-button>
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
          </div>
        </template>
        <StateEmpty
          v-else-if="!loading"
          title="还没有告警规则"
          description="先创建一个规则，绑定同步任务和通知渠道后即可在失败、异常或指标超限时发送告警。"
          hint="建议先配置告警渠道，再创建规则。"
          button-text="新增规则"
          @action="openRuleDialog()"
        />
      </div>

      <div class="panel-card alert-settings-workbench__panel">
        <div class="section-title">
          <div class="section-title__left">
            <h2>告警渠道</h2>
            <el-tag :type="channelSummaryTagType" effect="light">{{ channelSummaryText }}</el-tag>
          </div>
          <el-tag effect="dark" type="success">{{ enabledChannelCount }} 个启用</el-tag>
        </div>
        <div class="alert-settings-workbench__panel-hint">
          <span>{{ channelHint }}</span>
        </div>
        <template v-if="channels.length">
          <div class="table-shell">
            <el-table :data="channels" border stripe v-loading="loading">
              <el-table-column label="名称" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="alert-settings-workbench__cell-stack">
                    <div class="alert-settings-workbench__primary">{{ row.channelName || '-' }}</div>
                    <div class="alert-settings-workbench__secondary">{{ channelTypeLabel(row.channelType) }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="120">
                <template #default="{ row }">
                  <el-tag effect="light" type="info">{{ channelTypeLabel(row.channelType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">{{ row.enabled ? '已启用' : '已停用' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="最近测试" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ resolveChannelTestText(row) }}
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
                      测试发送
                    </el-button>
                    <el-button
                      size="small"
                      :type="row.enabled ? 'warning' : 'success'"
                      @click.stop="toggleChannelEnabled(row)"
                    >
                      {{ row.enabled ? '停用' : '启用' }}
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
          </div>
        </template>
        <StateEmpty
          v-else-if="!loading"
          title="还没有告警渠道"
          description="先配置一个 SMTP 或 Webhook 渠道，规则触发后才能发送通知。"
          hint="渠道保存后建议先测试发送。"
          button-text="新增渠道"
          @action="openChannelDialog()"
        />
      </div>
    </div>

    <div class="panel-card alert-settings-workbench__guide">
      <div class="section-title">
        <h2>配置建议</h2>
        <el-tag type="info" effect="light">敏感信息已隐藏</el-tag>
      </div>
      <div class="alert-settings-workbench__guide-grid">
        <div class="alert-settings-workbench__guide-item">
          <div class="alert-settings-workbench__guide-step">1</div>
          <div>
            <div class="alert-settings-workbench__guide-title">先新增告警渠道</div>
            <div class="alert-settings-workbench__guide-text">可先配置 SMTP 或 Webhook，规则触发后才能发送通知。</div>
          </div>
        </div>
        <div class="alert-settings-workbench__guide-item">
          <div class="alert-settings-workbench__guide-step">2</div>
          <div>
            <div class="alert-settings-workbench__guide-title">测试渠道发送</div>
            <div class="alert-settings-workbench__guide-text">建议先确认连接与鉴权，再启用规则。</div>
          </div>
        </div>
        <div class="alert-settings-workbench__guide-item">
          <div class="alert-settings-workbench__guide-step">3</div>
          <div>
            <div class="alert-settings-workbench__guide-title">新增告警规则并绑定任务</div>
            <div class="alert-settings-workbench__guide-text">支持任务失败、表级指标和延迟阈值等常见场景。</div>
          </div>
        </div>
        <div class="alert-settings-workbench__guide-item">
          <div class="alert-settings-workbench__guide-step">4</div>
          <div>
            <div class="alert-settings-workbench__guide-title">启用规则开始监听</div>
            <div class="alert-settings-workbench__guide-text">SMTP 密码、Webhook Secret 和 Token 不会明文展示。</div>
          </div>
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
                <el-option label="信息" value="INFO" />
                <el-option label="警告" value="WARNING" />
                <el-option label="错误" value="ERROR" />
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
                <el-option label="邮件 SMTP" value="SMTP" />
                <el-option label="Webhook 推送" value="WEBHOOK" />
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

const incompleteRuleCount = computed(function () {
  return rules.value.filter(function (item) {
    return !hasBoundTask(item) || !hasBoundChannel(item) || !item.ruleName
  }).length
})

const canCreateRule = computed(function () {
  return true
})

const canCreateChannel = computed(function () {
  return true
})

const canTestChannel = computed(function () {
  return channels.value.length > 0
})

const ruleSummaryText = computed(function () {
  if (!rules.value.length) {
    return '暂无规则'
  }
  if (incompleteRuleCount.value > 0) {
    return '配置不完整'
  }
  return '已配置完整'
})

const ruleSummaryTagType = computed(function () {
  if (!rules.value.length) {
    return 'info'
  }
  if (incompleteRuleCount.value > 0) {
    return 'warning'
  }
  return 'success'
})

const ruleHint = computed(function () {
  if (!channels.value.length) {
    return '建议先新增告警渠道，再创建规则。'
  }
  if (!rules.value.length) {
    return '可先创建规则并绑定任务、条件和渠道。'
  }
  return '规则支持启用、停用、编辑和删除，敏感信息不会出现在此处。'
})

const channelSummaryText = computed(function () {
  if (!channels.value.length) {
    return '暂无渠道'
  }
  return canTestChannel.value ? '可测试' : '未测试'
})

const channelSummaryTagType = computed(function () {
  if (!channels.value.length) {
    return 'info'
  }
  return canTestChannel.value ? 'success' : 'warning'
})

const channelHint = computed(function () {
  if (!channels.value.length) {
    return '先配置 SMTP 或 Webhook 渠道，规则触发后才能发送通知。'
  }
  return '渠道名称、类型、状态和最近测试结果会保持清晰展示，密码和 Token 不会明文展示。'
})

const toolbarHint = computed(function () {
  if (loading.value) {
    return '正在刷新告警配置。'
  }
  if (!channels.value.length) {
    return '建议先配置告警渠道，再创建规则。'
  }
  if (!rules.value.length) {
    return '建议先创建规则并绑定任务，再启用通知。'
  }
  return '可继续调整规则、渠道和测试发送。'
})

const summaryCards = computed(function () {
  return [
    { label: '告警规则', value: rules.value.length, hint: '支持启用 / 停用、任务绑定和冷却时间' },
    { label: '已启用规则', value: enabledRuleCount.value, hint: '当前可触发通知的规则' },
    { label: '告警渠道', value: channels.value.length, hint: 'SMTP / Webhook 通知通道' },
    { label: '可用任务', value: tasks.value.length, hint: '规则可绑定已有同步任务' }
  ]
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
  if (!canCreateRule.value && !row) {
    ElMessage.warning('建议先配置告警渠道，再创建规则')
    return
  }
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
  if (!canCreateChannel.value && !row) {
    ElMessage.warning('告警渠道暂不可创建')
    return
  }
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
  if (!canTestChannel.value) {
    ElMessage.warning('请先配置至少一个告警渠道')
    return
  }
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

async function toggleRuleEnabled(row) {
  try {
    await saveAlertRule({
      id: row.id,
      ruleName: row.ruleName,
      alertType: row.alertType,
      taskId: row.taskId,
      tableName: row.tableName,
      alertLevel: row.alertLevel,
      alertContentTemplate: row.alertContentTemplate,
      channelIdsJson: row.channelIdsJson,
      enabled: !row.enabled,
      cooldownSeconds: row.cooldownSeconds
    })
    ElMessage.success(row.enabled ? '规则已停用' : '规则已启用')
    await loadPageData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '更新规则状态失败'))
  }
}

async function toggleChannelEnabled(row) {
  try {
    await saveAlertChannel({
      id: row.id,
      channelName: row.channelName,
      channelType: row.channelType,
      enabled: !row.enabled,
      smtpHost: row.smtpHost,
      smtpPort: row.smtpPort,
      smtpUsername: row.smtpUsername,
      smtpPassword: '',
      smtpToAddress: row.smtpToAddress,
      smtpFromAddress: row.smtpFromAddress,
      webhookUrl: row.webhookUrl,
      webhookToken: ''
    })
    ElMessage.success(row.enabled ? '渠道已停用' : '渠道已启用')
    await loadPageData()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '更新渠道状态失败'))
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

function hasBoundTask(row) {
  return row && row.taskId !== null && row.taskId !== undefined
}

function hasBoundChannel(row) {
  return parseChannelIds(row && row.channelIdsJson).length > 0
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

function resolveRuleTrigger(row) {
  if (!row) {
    return '-'
  }
  if (row.taskId) {
    return '任务 #' + row.taskId
  }
  if (row.tableName) {
    return '表级：' + row.tableName
  }
  return '全局'
}

function resolveRuleCondition(row) {
  if (!row) {
    return '-'
  }
  const parts = []
  if (row.alertType) {
    parts.push(alertTypeLabel(row.alertType))
  }
  if (row.alertLevel) {
    parts.push('级别 ' + row.alertLevel)
  }
  if (row.cooldownSeconds) {
    parts.push('冷却 ' + row.cooldownSeconds + 's')
  }
  return parts.length ? parts.join(' · ') : '-'
}

function resolveChannelTestText(row) {
  if (!row) {
    return '-'
  }
  if (!row.lastTestStatus) {
    return '未测试'
  }
  const status = row.lastTestStatus === 'SUCCESS' ? '测试成功' : '测试失败'
  if (row.lastTestAt) {
    return status + ' · ' + formatTime(row.lastTestAt)
  }
  return status
}

function ruleStatusLabel(row) {
  if (!row) {
    return '-'
  }
  if (!row.enabled) {
    return '已停用'
  }
  if (!hasBoundTask(row) || !hasBoundChannel(row)) {
    return '配置不完整'
  }
  return '已启用'
}

function ruleStatusTagType(row) {
  if (!row || !row.enabled) {
    return 'info'
  }
  if (!hasBoundTask(row) || !hasBoundChannel(row)) {
    return 'warning'
  }
  return 'success'
}

function channelTypeLabel(type) {
  if (type === 'WEBHOOK') {
    return 'Webhook'
  }
  return 'SMTP'
}

function alertTypeLabel(type) {
  const item = alertTypeOptions.find(function (option) {
    return option.value === type
  })
  return item ? item.label : (type || '-')
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
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

<style scoped>
.alert-settings-workbench {
  display: grid;
  gap: 16px;
}

.alert-settings-workbench__header {
  align-items: flex-start;
}

.alert-settings-workbench__titleblock {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.alert-settings-workbench__toolbar {
  display: grid;
  justify-items: end;
  gap: 8px;
  min-width: 0;
}

.alert-settings-workbench__toolbar-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.alert-settings-workbench__toolbar-note {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-align: right;
  max-width: 440px;
}

.alert-settings-workbench__stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.alert-settings-workbench__stat-card {
  min-height: 124px;
}

.alert-settings-workbench__workspace {
  align-items: stretch;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
}

.alert-settings-workbench__panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.alert-settings-workbench__panel-hint {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.alert-settings-workbench__cell-stack {
  display: grid;
  gap: 4px;
}

.alert-settings-workbench__primary {
  font-weight: 650;
  color: #0f172a;
}

.alert-settings-workbench__secondary {
  color: #64748b;
  font-size: 12px;
}

.alert-settings-workbench__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.alert-settings-workbench__guide {
  display: grid;
  gap: 14px;
}

.alert-settings-workbench__guide-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.alert-settings-workbench__guide-item {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid #e5eaf3;
  background: #fbfcfe;
}

.alert-settings-workbench__guide-step {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  background: #eef2ff;
  color: #334155;
  font-weight: 700;
}

.alert-settings-workbench__guide-title {
  font-weight: 650;
  color: #0f172a;
  margin-bottom: 4px;
}

.alert-settings-workbench__guide-text {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 1280px) {
  .alert-settings-workbench__workspace {
    grid-template-columns: 1fr;
  }

  .alert-settings-workbench__guide-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .alert-settings-workbench__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .alert-settings-workbench__toolbar,
  .alert-settings-workbench__toolbar-note {
    justify-items: start;
    text-align: left;
  }

  .alert-settings-workbench__toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .alert-settings-workbench__stats,
  .alert-settings-workbench__guide-grid {
    grid-template-columns: 1fr;
  }

  .alert-settings-workbench__toolbar-actions .el-button {
    width: 100%;
  }
}
</style>
