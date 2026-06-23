<template>
  <div class="page-section wizard-page">
    <div class="page-header wizard-page__header">
      <div class="wizard-page__header-copy">
        <h1>创建同步任务向导</h1>
        <p>按步骤选择数据源、表和同步模式，自动生成字段映射并保存到本地 SQLite。</p>
      </div>
      <el-space>
        <el-button round @click="goBack">返回任务列表</el-button>
        <el-tooltip :disabled="canSaveTask" content="请先完成全部步骤再保存任务" placement="bottom">
          <span>
            <el-button type="primary" round :disabled="!canSaveTask" :loading="saving" @click="saveWizard">
              保存任务
            </el-button>
          </span>
        </el-tooltip>
      </el-space>
    </div>

    <div class="wizard-page__stepper panel-card glass-panel">
      <div class="wizard-stepper">
        <div
          v-for="(step, index) in stepMeta"
          :key="step.title"
          class="wizard-stepper__item"
          :class="{
            'is-active': activeStep === index,
            'is-complete': activeStep > index
          }"
        >
          <div class="wizard-stepper__index">{{ index + 1 }}</div>
          <div class="wizard-stepper__body">
            <div class="wizard-stepper__title">{{ step.title }}</div>
            <div class="wizard-stepper__hint">{{ step.shortHint }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="wizard-page__workspace">
      <div class="wizard-page__main">
        <div class="panel-card glass-panel wizard-panel wizard-panel--content">
          <template v-if="activeStep === 0">
            <div class="wizard-panel__head">
              <div>
                <h2>源数据源</h2>
                <p>源数据源用于读取 schema、table 和字段信息，后续表选择和字段映射会基于它展开。</p>
              </div>
              <el-tag type="info" effect="dark">步骤 1 / 6</el-tag>
            </div>

            <div class="wizard-status-card">
              <div class="wizard-status-card__title">当前选择信息</div>
              <div class="wizard-status-grid">
                <div class="status-item">
                  <span class="status-item__label">类型</span>
                  <span class="status-item__value">{{ sourceDatasourceInfo.type }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">主机</span>
                  <span class="status-item__value">{{ sourceDatasourceInfo.host }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">数据库</span>
                  <span class="status-item__value">{{ sourceDatasourceInfo.databaseName }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">连接状态</span>
                  <span class="status-item__value">{{ sourceDatasourceInfo.status }}</span>
                </div>
              </div>
            </div>

            <div class="wizard-form-block">
              <div class="wizard-form-block__label">请选择源数据源</div>
              <el-select
                v-model="form.sourceDatasourceId"
                placeholder="请选择源数据源"
                style="width: 100%;"
                @change="handleSourceDatasourceChange"
              >
                <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </div>

            <div class="wizard-note">
              选择一个可用源数据源后才能进入下一步。
            </div>
          </template>

          <template v-else-if="activeStep === 1">
            <div class="wizard-panel__head">
              <div>
                <h2>目标数据源</h2>
                <p>目标数据源用于写入同步结果，字段映射和表结构比较会基于源库与目标库生成。</p>
              </div>
              <el-tag type="info" effect="dark">步骤 2 / 6</el-tag>
            </div>

            <div class="wizard-status-card">
              <div class="wizard-status-card__title">当前选择信息</div>
              <div class="wizard-status-grid">
                <div class="status-item">
                  <span class="status-item__label">类型</span>
                  <span class="status-item__value">{{ targetDatasourceInfo.type }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">主机</span>
                  <span class="status-item__value">{{ targetDatasourceInfo.host }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">数据库</span>
                  <span class="status-item__value">{{ targetDatasourceInfo.databaseName }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">连接状态</span>
                  <span class="status-item__value">{{ targetDatasourceInfo.status }}</span>
                </div>
              </div>
            </div>

            <div class="wizard-form-block">
              <div class="wizard-form-block__label">请选择目标数据源</div>
              <el-select
                v-model="form.targetDatasourceId"
                placeholder="请选择目标数据源"
                style="width: 100%;"
                @change="handleTargetDatasourceChange"
              >
                <el-option v-for="item in datasources" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </div>

            <div class="wizard-note">
              目标数据源不能和源数据源冲突时，按现有业务规则处理。
            </div>
          </template>

          <template v-else-if="activeStep === 2">
            <div class="wizard-panel__head">
              <div>
                <h2>选择 schema / table</h2>
                <p>选择需要同步的 schema 和 table。字段映射将在选择表后自动生成或进入下一步生成。</p>
              </div>
              <el-tag type="success" effect="dark">步骤 3 / 6</el-tag>
            </div>

            <div class="wizard-status-card">
              <div class="wizard-status-card__title">已选择表数量</div>
              <div class="wizard-stat">
                <div class="wizard-stat__value">{{ selectedTableCount }}</div>
                <div class="wizard-stat__hint">当前选择 {{ qualifiedTableName(form.sourceSchemaName, form.sourceTableName) }} → {{ qualifiedTableName(form.targetSchemaName, form.targetTableName) }}</div>
              </div>
            </div>

            <el-row :gutter="16">
              <el-col :span="12">
                <div class="wizard-card">
                  <h3>源表</h3>
                  <CreatableSelect
                    v-model="form.sourceSchemaName"
                    :options="sourceSchemaOptions"
                    placeholder="Schema"
                    style="width: 100%; margin-bottom: 12px;"
                    @change="handleSourceSchemaChange"
                  />
                  <CreatableSelect
                    v-model="form.sourceTableName"
                    :options="sourceTableOptions"
                    placeholder="Table"
                    style="width: 100%;"
                    @change="generateMappings"
                  />
                </div>
              </el-col>
              <el-col :span="12">
                <div class="wizard-card">
                  <h3>目标表</h3>
                  <CreatableSelect
                    v-model="form.targetSchemaName"
                    :options="targetSchemaOptions"
                    placeholder="Schema"
                    style="width: 100%; margin-bottom: 12px;"
                    @change="handleTargetSchemaChange"
                  />
                  <CreatableSelect
                    v-model="form.targetTableName"
                    :options="targetTableOptions"
                    placeholder="Table"
                    style="width: 100%;"
                    @change="generateMappings"
                  />
                </div>
              </el-col>
            </el-row>

            <div class="wizard-note">
              请先完成源数据源和目标数据源选择，然后扫描或选择可用表。
            </div>
          </template>

          <template v-else-if="activeStep === 3">
            <div class="wizard-panel__head">
              <div>
                <h2>字段映射</h2>
                <p>系统会根据字段名归一化、常见别名和相似度匹配生成字段映射。</p>
              </div>
              <el-tag type="success" effect="dark">步骤 4 / 6</el-tag>
            </div>

            <div class="wizard-status-card">
              <div class="wizard-status-card__title">映射策略</div>
              <div class="status-stack">
                <div class="status-item">
                  <span class="status-item__label">规则</span>
                  <span class="status-item__value">字段名归一化 + 常见别名 + 相似度匹配</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">当前状态</span>
                  <span class="status-item__value">{{ mappings.length ? '已有映射结果' : '等待生成字段映射' }}</span>
                </div>
              </div>
            </div>

            <div v-if="mappings.length" class="table-shell wizard-table-shell">
              <el-table :data="mappings" border stripe>
                <el-table-column prop="sourceColumnName" label="源字段" min-width="180" />
                <el-table-column label="目标字段" min-width="180">
                  <template #default="{ row }">
                    <el-input v-model="row.targetColumnName" size="small" />
                  </template>
                </el-table-column>
                <el-table-column label="置信度" width="110">
                  <template #default="{ row }">
                    {{ formatConfidence(row.confidence) }}
                  </template>
                </el-table-column>
                <el-table-column label="原因" min-width="130">
                  <template #default="{ row }">
                    {{ row.matchReason || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="ignored" label="忽略" width="100">
                  <template #default="{ row }">
                    <el-switch v-model="row.ignored" />
                  </template>
                </el-table-column>
                <el-table-column prop="defaultValue" label="默认值" min-width="160" />
              </el-table>
            </div>

            <div v-else class="wizard-empty-state">
              选择表后生成字段映射，这里会展示源字段、目标字段、置信度和忽略状态。
            </div>

            <div class="wizard-note">
              重新生成映射会复用现有字段推荐逻辑，不会改变保存逻辑。
            </div>
          </template>

          <template v-else-if="activeStep === 4">
            <div class="wizard-panel__head">
              <div>
                <h2>同步模式</h2>
                <p>选择全量同步或增量同步，并配置必要的断点字段或更新时间字段。</p>
              </div>
              <el-tag type="warning" effect="dark">步骤 5 / 6</el-tag>
            </div>

            <div class="wizard-status-card">
              <div class="wizard-status-card__title">当前同步模式</div>
              <div class="wizard-stat">
                <div class="wizard-stat__value">{{ syncModeLabel(form.syncMode) }}</div>
                <div class="wizard-stat__hint">如果当前只支持全量同步，则会明确显示当前使用全量同步。</div>
              </div>
            </div>

            <el-radio-group v-model="form.syncMode" class="wizard-radio-group">
              <el-radio label="FULL">全量同步</el-radio>
              <el-radio label="INCREMENTAL">增量同步</el-radio>
            </el-radio-group>

            <el-divider content-position="left">增量配置</el-divider>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="增量模式">
                  <el-select v-model="form.incrementalMode" style="width: 100%;">
                    <el-option label="无" value="NONE" />
                    <el-option label="更新时间" value="TIMESTAMP" />
                    <el-option label="自增 ID" value="AUTO_INCREMENT_ID" />
                    <el-option label="组合断点" value="COMPOSITE" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="增量字段">
                  <el-input v-model="form.incrementalColumnName" placeholder="例如 updated_at 或 id" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16" v-if="form.incrementalMode === 'COMPOSITE'">
              <el-col :span="12">
                <el-form-item label="断点字段 2">
                  <el-input v-model="form.incrementalTieBreakerColumnName" placeholder="例如 id" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="组合表达式">
                  <el-input v-model="form.incrementalCompositeColumnName" placeholder="例如 updated_at,id" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">调度配置</el-divider>

            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="启用调度">
                  <el-switch v-model="form.scheduleEnabled" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="调度类型">
                  <el-select v-model="form.scheduleType" style="width: 100%;">
                    <el-option label="手动" value="MANUAL" />
                    <el-option label="Cron" value="CRON" />
                    <el-option label="固定间隔" value="INTERVAL" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16" v-if="form.scheduleType === 'CRON'">
              <el-col :span="24">
                <el-form-item label="Cron 表达式">
                  <el-input v-model="form.scheduleCronExpression" placeholder="例如 0 9 * * *" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="16" v-else-if="form.scheduleType === 'INTERVAL'">
              <el-col :span="24">
                <el-form-item label="间隔秒数">
                  <el-input-number v-model="form.scheduleIntervalSeconds" :min="1" :step="60" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>

          <template v-else>
            <div class="wizard-panel__head">
              <div>
                <h2>确认保存</h2>
                <p>请确认数据源、表、字段映射和同步模式配置无误后保存任务。</p>
              </div>
              <el-tag type="success" effect="dark">步骤 6 / 6</el-tag>
            </div>

            <div class="wizard-status-card">
              <div class="wizard-status-card__title">最终摘要</div>
              <div class="status-stack">
                <div class="status-item">
                  <span class="status-item__label">源数据源</span>
                  <span class="status-item__value">{{ sourceDatasourceName || '未选择' }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">目标数据源</span>
                  <span class="status-item__value">{{ targetDatasourceName || '未选择' }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">已选 schema</span>
                  <span class="status-item__value">{{ form.sourceSchemaName || '—' }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">已选表数量</span>
                  <span class="status-item__value">{{ selectedTableCount }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">字段映射数量</span>
                  <span class="status-item__value">{{ mappings.length }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">同步模式</span>
                  <span class="status-item__value">{{ syncModeLabel(form.syncMode) }}</span>
                </div>
                <div class="status-item">
                  <span class="status-item__label">保存位置</span>
                  <span class="status-item__value">本地 SQLite</span>
                </div>
              </div>
            </div>

            <div class="wizard-empty-state wizard-empty-state--success">
              {{ canSaveTask ? '配置已完成，可以保存任务。' : '还需要完成：' }}
              <ul v-if="!canSaveTask" class="wizard-missing-list">
                <li v-for="item in missingSaveItems" :key="item">{{ item }}</li>
              </ul>
            </div>
          </template>

          <div class="wizard-actions">
            <el-button :disabled="activeStep === 0" @click="previousStep">上一步</el-button>
            <el-tooltip v-if="activeStep < 5" :disabled="canGoNext" :content="nextDisabledReason" placement="top">
              <span>
                <el-button type="primary" :disabled="!canGoNext" @click="nextStep">下一步</el-button>
              </span>
            </el-tooltip>
            <el-button v-else type="primary" :disabled="!canSaveTask" :loading="saving" @click="saveWizard">保存任务</el-button>
          </div>
        </div>
      </div>

      <div class="wizard-page__summary">
        <div class="panel-card glass-panel wizard-panel wizard-panel--summary">
          <div class="wizard-panel__head wizard-panel__head--summary">
            <div>
              <h2>任务预览</h2>
              <p>始终展示当前任务配置进度和最终保存摘要。</p>
            </div>
            <el-tag type="info" effect="dark">配置中 · 步骤 {{ activeStep + 1 }} / 6</el-tag>
          </div>

          <div class="wizard-summary-metrics">
            <div class="wizard-metric">
              <div class="wizard-metric__label">完成度</div>
              <div class="wizard-metric__value">{{ progressLabel }}</div>
            </div>
            <div class="wizard-metric">
              <div class="wizard-metric__label">当前状态</div>
              <div class="wizard-metric__value">{{ currentStatus }}</div>
            </div>
          </div>

          <div class="wizard-summary-list">
            <div class="status-item">
              <span class="status-item__label">源数据源</span>
              <span class="status-item__value">{{ sourceDatasourceName || '未选择' }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">目标数据源</span>
              <span class="status-item__value">{{ targetDatasourceName || '未选择' }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">Schema</span>
              <span class="status-item__value">{{ form.sourceSchemaName || '—' }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">已选表</span>
              <span class="status-item__value">{{ selectedTableCount }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">字段映射</span>
              <span class="status-item__value">{{ mappings.length }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">同步模式</span>
              <span class="status-item__value">{{ syncModeLabel(form.syncMode) }}</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">生成规则</span>
              <span class="status-item__value">字段名归一化 + 常见别名 + 相似度匹配</span>
            </div>
            <div class="status-item">
              <span class="status-item__label">保存位置</span>
              <span class="status-item__value">本地 SQLite</span>
            </div>
          </div>

          <div class="wizard-summary-state" :class="{ 'is-complete': canSaveTask }">
            <div class="wizard-summary-state__title">{{ canSaveTask ? '配置已完成' : '还需要完成' }}</div>
            <ul v-if="!canSaveTask" class="wizard-missing-list">
              <li v-for="item in missingSummaryItems" :key="item">{{ item }}</li>
            </ul>
            <div v-else class="wizard-summary-state__body">可以保存任务。</div>
          </div>

          <div class="table-shell wizard-table-shell wizard-table-shell--summary">
            <div v-if="showMappingPreview" class="wizard-summary-table-head">
              <div class="wizard-summary-table-head__title">映射预览</div>
              <div class="wizard-summary-table-head__hint">Step 4 之后展示源字段、目标字段、置信度和忽略状态。</div>
            </div>
            <el-table v-if="showMappingPreview && mappings.length" :data="mappings.slice(0, 8)" border stripe>
              <el-table-column prop="sourceColumnName" label="源字段" min-width="150" />
              <el-table-column prop="targetColumnName" label="目标字段" min-width="150" />
              <el-table-column prop="transformRule" label="转换规则" min-width="160">
                <template #default="{ row }">
                  {{ row.transformRule || '待配置' }}
                </template>
              </el-table-column>
              <el-table-column label="置信度" width="100">
                <template #default="{ row }">
                  {{ formatConfidence(row.confidence) }}
                </template>
              </el-table-column>
              <el-table-column prop="ignored" label="忽略" width="90">
                <template #default="{ row }">
                  <el-switch v-model="row.ignored" />
                </template>
              </el-table-column>
            </el-table>
            <div v-else class="wizard-empty-state wizard-empty-state--summary">
              完成字段映射后，这里将展示源字段、目标字段、置信度和忽略状态。
            </div>
            <div class="wizard-note">
              字段级转换规则请在保存任务后，到「字段映射」页中的“配置转换”里继续维护。
            </div>
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
import { listDatasources, saveFieldMapping, saveTask, scanMetadata, suggestFieldMappings } from '../services/backend'
import CreatableSelect from '../components/CreatableSelect.vue'

const router = useRouter()
const route = useRoute()

const datasources = ref([])
const sourceSchemas = ref([])
const targetSchemas = ref([])
const sourceSchemaOptions = ref([])
const targetSchemaOptions = ref([])
const sourceTableOptions = ref([])
const targetTableOptions = ref([])
const sourceDatasourceName = ref('')
const targetDatasourceName = ref('')
const saving = ref(false)
const activeStep = ref(0)
const mappings = ref([])
const selectedTaskId = ref(0)
const stepMeta = [
  {
    title: '选择源数据源',
    shortHint: '读取 schema、table 和字段信息',
    hint: '先确定从哪里读取表结构和数据，后续的 schema、表和字段都会跟随它展开。'
  },
  {
    title: '选择目标数据源',
    shortHint: '确定写入位置与方言',
    hint: '目标库决定写入位置，也会影响后续 SQL 方言和字段映射建议。'
  },
  {
    title: '选择 schema / table',
    shortHint: '锁定源表与目标表',
    hint: '先锁定源表与目标表，向导会据此加载字段并生成映射。'
  },
  {
    title: '生成字段映射',
    shortHint: '确认或调整推荐映射',
    hint: '这里可以人工确认、改名或忽略系统推荐的映射关系。'
  },
  {
    title: '同步模式',
    shortHint: '配置全量或增量',
    hint: '可选择全量或增量，同步任务的调度配置也在这一段完成。'
  },
  {
    title: '确认保存',
    shortHint: '检查后提交任务',
    hint: '最后检查任务名、表名、映射数量和调度配置，再一次性保存。'
  }
]

const form = reactive({
  taskName: '',
  sourceDatasourceId: null,
  targetDatasourceId: null,
  sourceSchemaName: '',
  sourceTableName: '',
  targetSchemaName: '',
  targetTableName: '',
  syncMode: 'FULL',
  incrementalMode: 'NONE',
  incrementalColumnName: '',
  incrementalTieBreakerColumnName: '',
  incrementalCompositeColumnName: '',
  scheduleEnabled: false,
  scheduleType: 'MANUAL',
  scheduleCronExpression: '',
  scheduleIntervalSeconds: 300
})

const sourceDatasourceInfo = computed(function () {
  return buildDatasourceInfo(form.sourceDatasourceId, sourceDatasourceName.value)
})

const targetDatasourceInfo = computed(function () {
  return buildDatasourceInfo(form.targetDatasourceId, targetDatasourceName.value)
})

const selectedTableCount = computed(function () {
  return form.sourceTableName && form.targetTableName ? 1 : 0
})

const canGoNext = computed(function () {
  if (activeStep.value === 0) {
    return !!form.sourceDatasourceId
  }
  if (activeStep.value === 1) {
    return !!form.targetDatasourceId
  }
  if (activeStep.value === 2) {
    return !!form.sourceTableName && !!form.targetTableName
  }
  if (activeStep.value === 3) {
    return mappings.value.length > 0
  }
  if (activeStep.value === 4) {
    return !!form.syncMode
  }
  return false
})

const canSaveTask = computed(function () {
  return !!form.sourceDatasourceId &&
    !!form.targetDatasourceId &&
    !!form.sourceTableName &&
    !!form.targetTableName &&
    mappings.value.length > 0 &&
    !!form.syncMode &&
    !!form.scheduleType
})

const missingSaveItems = computed(function () {
  const items = []
  if (!form.sourceDatasourceId) {
    items.push('未选择源数据源')
  }
  if (!form.targetDatasourceId) {
    items.push('未选择目标数据源')
  }
  if (!form.sourceTableName || !form.targetTableName) {
    items.push('未选择 table')
  }
  if (!mappings.value.length) {
    items.push('未生成字段映射')
  }
  if (!form.syncMode) {
    items.push('未选择同步模式')
  }
  return items
})

const missingSummaryItems = computed(function () {
  if (canSaveTask.value) {
    return []
  }
  return missingSaveItems.value
})

const progressLabel = computed(function () {
  return (activeStep.value + 1) + ' / 6'
})

const currentStatus = computed(function () {
  if (activeStep.value === 0) {
    return form.sourceDatasourceId ? '可以继续选择目标数据源' : '等待选择源数据源'
  }
  if (activeStep.value === 1) {
    return form.targetDatasourceId ? '可以继续选择 schema / table' : '等待选择目标数据源'
  }
  if (activeStep.value === 2) {
    return form.sourceTableName && form.targetTableName ? '可以生成字段映射' : '等待选择表'
  }
  if (activeStep.value === 3) {
    return mappings.value.length ? '可以调整映射并继续' : '等待生成字段映射'
  }
  if (activeStep.value === 4) {
    return '等待选择同步模式'
  }
  return canSaveTask.value ? '可以保存任务' : '等待确认保存'
})

const showMappingPreview = computed(function () {
  return activeStep.value >= 3 || mappings.value.length > 0
})

const nextDisabledReason = computed(function () {
  if (activeStep.value === 0) {
    return '请选择一个可用源数据源'
  }
  if (activeStep.value === 1) {
    return '请选择一个可用目标数据源'
  }
  if (activeStep.value === 2) {
    return '请至少选择一张表'
  }
  if (activeStep.value === 3) {
    return '请先确认字段映射'
  }
  if (activeStep.value === 4) {
    return '请先配置同步策略'
  }
  return '请先完成当前步骤'
})

onMounted(function () {
  loadPageData()
})

async function loadPageData() {
  try {
    datasources.value = await listDatasources()
    applyQueryDefaults()
    if (form.sourceDatasourceId) {
      await loadSourceMetadata()
    }
    if (form.targetDatasourceId) {
      await loadTargetMetadata()
    }
    if (form.sourceTableName && form.targetTableName) {
      await generateMappings()
    }
    updatePreview()
  } catch (error) {
    ElMessage.error(error.message || '加载向导数据失败')
  }
}

function applyQueryDefaults() {
  if (route.query.sourceDatasourceId) {
    form.sourceDatasourceId = Number(route.query.sourceDatasourceId)
  }
  if (route.query.targetDatasourceId) {
    form.targetDatasourceId = Number(route.query.targetDatasourceId)
  }
  if (route.query.schemaName) {
    form.sourceSchemaName = String(route.query.schemaName)
  }
  if (route.query.tableName) {
    form.sourceTableName = String(route.query.tableName)
    if (!form.targetTableName) {
      form.targetTableName = String(route.query.tableName)
    }
  }
}

async function handleSourceDatasourceChange(value) {
  sourceDatasourceName.value = datasourceName(value)
  form.sourceSchemaName = ''
  form.sourceTableName = ''
  sourceSchemas.value = []
  mappings.value = []
  await loadSourceMetadata()
  await generateMappings()
}

async function handleTargetDatasourceChange(value) {
  targetDatasourceName.value = datasourceName(value)
  form.targetSchemaName = ''
  form.targetTableName = ''
  targetSchemas.value = []
  mappings.value = []
  await loadTargetMetadata()
  await generateMappings()
}

async function loadSourceMetadata() {
  if (!form.sourceDatasourceId) {
    return
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === form.sourceDatasourceId
  })
  if (!datasource) {
    return
  }
  sourceDatasourceName.value = datasource.name
  sourceSchemas.value = await scanMetadata(datasource.id)
  sourceSchemaOptions.value = (sourceSchemas.value || []).map(function (schema) {
    return schema.schemaName
  })
  if (!form.sourceSchemaName && sourceSchemas.value.length > 0) {
    form.sourceSchemaName = sourceSchemas.value[0].schemaName
  }
  syncSourceTableDefault()
}

async function loadTargetMetadata() {
  if (!form.targetDatasourceId) {
    return
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === form.targetDatasourceId
  })
  if (!datasource) {
    return
  }
  targetDatasourceName.value = datasource.name
  targetSchemas.value = await scanMetadata(datasource.id)
  targetSchemaOptions.value = (targetSchemas.value || []).map(function (schema) {
    return schema.schemaName
  })
  if (!form.targetSchemaName && targetSchemas.value.length > 0) {
    form.targetSchemaName = targetSchemas.value[0].schemaName
  }
  syncTargetTableDefault()
}

function handleSourceSchemaChange() {
  form.sourceTableName = ''
  syncSourceTableDefault()
  generateMappings()
}

function handleTargetSchemaChange() {
  form.targetTableName = ''
  syncTargetTableDefault()
  generateMappings()
}

function syncSourceTableDefault() {
  const tables = sourceTables.value
  sourceTableOptions.value = tables.map(function (table) {
    return table.tableName
  })
  if (!form.sourceTableName && tables.length > 0) {
    form.sourceTableName = tables[0].tableName
  }
}

function syncTargetTableDefault() {
  const tables = targetTables.value
  targetTableOptions.value = tables.map(function (table) {
    return table.tableName
  })
  if (!form.targetTableName && tables.length > 0) {
    form.targetTableName = tables[0].tableName
  }
}

const sourceTables = computed(function () {
  const schema = sourceSchemas.value.find(function (item) {
    return item.schemaName && form.sourceSchemaName && item.schemaName.toLowerCase() === form.sourceSchemaName.toLowerCase()
  })
  return schema ? (schema.tables || []) : []
})

const targetTables = computed(function () {
  const schema = targetSchemas.value.find(function (item) {
    return item.schemaName && form.targetSchemaName && item.schemaName.toLowerCase() === form.targetSchemaName.toLowerCase()
  })
  return schema ? (schema.tables || []) : []
})

async function generateMappings() {
  const sourceTable = findTable(sourceSchemas.value, form.sourceSchemaName, form.sourceTableName)
  const targetTable = findTable(targetSchemas.value, form.targetSchemaName, form.targetTableName)
  if (!sourceTable || !targetTable) {
    mappings.value = []
    updatePreview()
    return
  }
  try {
    const suggestions = await suggestFieldMappings(selectedTaskId.value || 0)
    if (suggestions && suggestions.length > 0) {
      mappings.value = suggestions.map(function (item) {
        return {
          sourceTableName: form.sourceTableName,
          targetTableName: form.targetTableName,
          sourceColumnName: item.sourceColumnName,
          targetColumnName: item.targetColumnName || item.sourceColumnName,
          confidence: item.confidence,
          matchReason: item.matchReason,
          ignored: !!item.ignored,
          defaultValue: '',
          transformRule: ''
        }
      })
    } else {
      mappings.value = buildFallbackMappings(sourceTable, targetTable)
    }
  } catch (error) {
    mappings.value = buildFallbackMappings(sourceTable, targetTable)
  }
  updatePreview()
}

function buildFallbackMappings(sourceTable, targetTable) {
  const targetColumns = targetTable.columns || []
  return (sourceTable.columns || []).map(function (column) {
    const match = findMatchingColumn(targetColumns, column.name)
    return {
      sourceTableName: form.sourceTableName,
      targetTableName: form.targetTableName,
      sourceColumnName: column.name,
      targetColumnName: match ? match.name : column.name,
      confidence: match ? 1 : 0,
      matchReason: match ? 'exact' : 'none',
      ignored: !match,
      defaultValue: '',
      transformRule: ''
    }
  })
}

function findTable(schemas, schemaName, tableName) {
  const schema = schemas.find(function (item) {
    return item.schemaName && schemaName && item.schemaName.toLowerCase() === schemaName.toLowerCase()
  })
  if (!schema || !schema.tables) {
    return null
  }
  return schema.tables.find(function (item) {
    return item.tableName && tableName && item.tableName.toLowerCase() === tableName.toLowerCase()
  }) || null
}

function findMatchingColumn(columns, sourceColumnName) {
  const exact = columns.find(function (column) {
    return column.name === sourceColumnName
  })
  if (exact) {
    return exact
  }
  return columns.find(function (column) {
    return column.name && sourceColumnName && column.name.toLowerCase() === sourceColumnName.toLowerCase()
  }) || null
}

function formatConfidence(value) {
  if (typeof value !== 'number') {
    return '-'
  }
  return Math.round(value * 100) + '%'
}

function updatePreview() {
  form.taskName = defaultTaskName()
}

function defaultTaskName() {
  if (form.sourceTableName && form.targetTableName) {
    return form.sourceTableName + ' -> ' + form.targetTableName
  }
  return '新建同步任务'
}

function nextStep() {
  if (activeStep.value === 0 && !form.sourceDatasourceId) {
    ElMessage.warning('请先选择源数据源')
    return
  }
  if (activeStep.value === 1 && !form.targetDatasourceId) {
    ElMessage.warning('请先选择目标数据源')
    return
  }
  if (activeStep.value === 2) {
    if (!form.sourceTableName || !form.targetTableName) {
      ElMessage.warning('请选择源表和目标表')
      return
    }
    generateMappings()
  }
  if (activeStep.value === 3 && !mappings.value.length) {
    ElMessage.warning('请先生成字段映射')
    return
  }
  if (activeStep.value === 4 && !form.syncMode) {
    ElMessage.warning('请选择同步模式')
    return
  }
  if (activeStep.value < 5) {
    activeStep.value += 1
  }
}

function previousStep() {
  if (activeStep.value > 0) {
    activeStep.value -= 1
  }
}

async function saveWizard() {
  if (!canSaveTask.value) {
    ElMessage.warning('请补全向导信息')
    return
  }
  saving.value = true
  try {
    const task = await saveTask({
      taskName: form.taskName || defaultTaskName(),
      sourceDatasourceId: Number(form.sourceDatasourceId),
      targetDatasourceId: Number(form.targetDatasourceId),
      sourceSchemaName: form.sourceSchemaName,
      sourceTableName: form.sourceTableName,
      targetSchemaName: form.targetSchemaName,
      targetTableName: form.targetTableName,
      syncMode: form.syncMode,
      taskStatus: 'PENDING',
      incrementalMode: form.incrementalMode,
      incrementalColumnName: form.incrementalColumnName,
      incrementalTieBreakerColumnName: form.incrementalTieBreakerColumnName,
      incrementalCompositeColumnName: form.incrementalCompositeColumnName,
      scheduleEnabled: form.scheduleEnabled,
      scheduleType: form.scheduleType,
      scheduleCronExpression: form.scheduleCronExpression,
      scheduleIntervalSeconds: form.scheduleIntervalSeconds
    })
    for (let i = 0; i < mappings.value.length; i += 1) {
      const mapping = mappings.value[i]
      await saveFieldMapping({
        taskId: task.id,
        sourceTableName: mapping.sourceTableName,
        targetTableName: mapping.targetTableName,
        sourceColumnName: mapping.sourceColumnName,
        targetColumnName: mapping.targetColumnName,
        ignored: mapping.ignored,
        defaultValue: mapping.defaultValue,
        transformRule: mapping.transformRule
      })
    }
    ElMessage.success('任务已保存')
    router.push('/tasks')
  } catch (error) {
    ElMessage.error(error.message || '保存任务失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/tasks')
}

function datasourceName(id) {
  const datasource = datasources.value.find(function (item) {
    return item.id === id
  })
  return datasource ? datasource.name : ''
}

function buildDatasourceInfo(id, fallbackName) {
  if (!id) {
    return {
      type: '—',
      host: '—',
      databaseName: '—',
      status: '未选择'
    }
  }
  const datasource = datasources.value.find(function (item) {
    return item.id === id
  })
  if (!datasource) {
    return {
      type: '—',
      host: '—',
      databaseName: '—',
      status: fallbackName ? '已选择' : '未选择'
    }
  }
  return {
    type: datasource.type || '—',
    host: datasource.host || '—',
    databaseName: datasource.databaseName || '—',
    status: '已选择'
  }
}

function qualifiedTableName(schemaName, tableName) {
  if (!tableName) {
    return '—'
  }
  return schemaName ? schemaName + '.' + tableName : tableName
}

function syncModeLabel(value) {
  if (value === 'INCREMENTAL') {
    return '增量同步'
  }
  return '全量同步'
}

function scheduleSummary(task) {
  if (!task || !task.scheduleEnabled) {
    return '未启用'
  }
  if (task.scheduleType === 'CRON') {
    return 'Cron'
  }
  if (task.scheduleType === 'INTERVAL') {
    return '间隔 ' + (task.scheduleIntervalSeconds || 0) + ' 秒'
  }
  return '手动'
}
</script>

<style scoped>
.wizard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: calc(100vh - 32px);
}

.wizard-page__header {
  align-items: flex-start;
  gap: 16px;
}

.wizard-page__header-copy h1 {
  margin-bottom: 6px;
}

.wizard-page__header-copy p {
  max-width: 760px;
}

.wizard-page__stepper {
  padding: 12px 16px;
}

.wizard-stepper {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.wizard-stepper__item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
  padding: 8px 10px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.wizard-stepper__item.is-active {
  background: #eef4ff;
  border-color: #84a9ff;
  color: #1e3a8a;
}

.wizard-stepper__item.is-complete {
  background: #f0fdf4;
  border-color: #86efac;
  color: #166534;
}

.wizard-stepper__index {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 auto;
  background: rgba(15, 23, 42, 0.08);
}

.wizard-stepper__item.is-active .wizard-stepper__index {
  background: #3b82f6;
  color: #fff;
}

.wizard-stepper__item.is-complete .wizard-stepper__index {
  background: #16a34a;
  color: #fff;
}

.wizard-stepper__body {
  min-width: 0;
}

.wizard-stepper__title {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.3;
}

.wizard-stepper__hint {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.35;
  color: inherit;
  opacity: 0.8;
}

.wizard-page__workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.9fr);
  gap: 16px;
  align-items: start;
  min-height: 0;
}

.wizard-page__main,
.wizard-page__summary {
  min-width: 0;
  min-height: 0;
}

.wizard-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
}

.wizard-panel--content {
  padding-bottom: 16px;
}

.wizard-panel--summary {
  position: sticky;
  top: 16px;
  max-height: calc(100vh - 124px);
  overflow: auto;
  padding-bottom: 16px;
}

.wizard-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.wizard-panel__head h2 {
  margin-bottom: 4px;
}

.wizard-panel__head p {
  margin: 0;
  color: #64748b;
  line-height: 1.5;
}

.wizard-status-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fafbff;
  padding: 14px;
}

.wizard-status-card__title {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.wizard-status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.wizard-form-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wizard-form-block__label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}

.wizard-note {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  color: #475569;
  line-height: 1.6;
  border: 1px solid #e2e8f0;
}

.wizard-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.wizard-card h3 {
  margin: 0 0 12px;
  font-size: 14px;
}

.wizard-radio-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.wizard-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.wizard-stat__value {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
}

.wizard-stat__hint {
  color: #64748b;
  line-height: 1.5;
}

.wizard-table-shell {
  max-height: 380px;
  overflow: auto;
}

.wizard-table-shell--summary {
  max-height: 320px;
}

.wizard-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  padding: 20px;
  border: 1px dashed #cbd5e1;
  border-radius: 14px;
  background: #fbfdff;
  color: #64748b;
  text-align: center;
  line-height: 1.6;
}

.wizard-empty-state--success {
  justify-content: flex-start;
  align-items: flex-start;
  flex-direction: column;
  text-align: left;
}

.wizard-summary-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.wizard-metric {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.wizard-metric__label {
  font-size: 12px;
  color: #64748b;
}

.wizard-metric__value {
  margin-top: 6px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
}

.wizard-summary-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.wizard-summary-state {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
  padding: 14px;
}

.wizard-summary-state.is-complete {
  background: #f0fdf4;
  border-color: #86efac;
}

.wizard-summary-state__title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 8px;
}

.wizard-summary-state__body {
  color: #166534;
  line-height: 1.6;
}

.wizard-summary-table-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 8px;
}

.wizard-summary-table-head__title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.wizard-summary-table-head__hint {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.wizard-missing-list {
  margin: 0;
  padding-left: 18px;
  color: #b45309;
}

.wizard-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 4px;
}

:deep(.status-item) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

:deep(.status-item__label) {
  font-size: 12px;
  color: #64748b;
}

:deep(.status-item__value) {
  font-size: 13px;
  color: #0f172a;
  word-break: break-word;
  line-height: 1.45;
}

.wizard-page :deep(.el-form-item) {
  margin-bottom: 12px;
}

@media (max-width: 1280px) {
  .wizard-page__workspace {
    grid-template-columns: 1fr;
  }

  .wizard-panel--summary {
    position: static;
    max-height: none;
  }

  .wizard-stepper {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .wizard-stepper {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .wizard-status-grid,
  .wizard-summary-metrics {
    grid-template-columns: 1fr;
  }

  .wizard-page__header {
    flex-direction: column;
  }

  .wizard-actions {
    flex-wrap: wrap;
  }
}
</style>
