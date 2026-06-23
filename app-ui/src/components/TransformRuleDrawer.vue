<template>
  <el-drawer
    v-model="drawerVisible"
    :close-on-click-modal="false"
    :destroy-on-close="false"
    size="82%"
    class="transform-rule-drawer"
    :before-close="handleBeforeClose"
    :title="drawerTitle"
  >
    <div v-if="mapping" class="transform-rule-drawer__content">
      <el-descriptions :column="2" border size="small" class="transform-rule-drawer__descriptions">
        <el-descriptions-item label="源字段">{{ mapping.sourceColumnName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="目标字段">{{ mapping.targetColumnName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="源字段类型">{{ sourceFieldType }}</el-descriptions-item>
        <el-descriptions-item label="目标字段类型">{{ targetFieldType }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        class="transform-rule-drawer__tip"
        type="info"
        show-icon
        :closable="false"
        title="转换规则按顺序执行，保存后会同步回字段映射摘要。"
        description="支持多个规则、启停、排序、删除、恢复未配置和测试转换。"
      />

      <div class="transform-rule-drawer__grid">
        <div class="transform-rule-drawer__panel transform-rule-drawer__panel--rules">
          <div class="transform-rule-drawer__section-header">
            <div>
              <h3>规则列表</h3>
              <p>{{ ruleSummaryText }}</p>
            </div>
            <el-space>
              <el-button size="small" @click="openAddRule">新增规则</el-button>
              <el-button size="small" type="warning" plain :disabled="rules.length === 0" @click="restoreUnconfigured">
                恢复未配置
              </el-button>
            </el-space>
          </div>

          <div class="transform-rule-drawer__table-shell">
            <el-table :data="rules" v-loading="loading" border stripe max-height="360">
              <el-table-column label="顺序" width="80" align="center">
                <template #default="{ row }">
                  {{ row.transformOrder }}
                </template>
              </el-table-column>
              <el-table-column label="规则类型" min-width="180">
                <template #default="{ row }">
                  <div class="transform-rule-drawer__type-cell">
                    <div class="transform-rule-drawer__type-title">{{ ruleTypeLabel(row.transformType) }}</div>
                    <div class="transform-rule-drawer__type-hint">{{ ruleTypeDescription(row.transformType) }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="规则说明" min-width="180">
                <template #default="{ row }">
                  {{ ruleTypeScenario(row.transformType) }}
                </template>
              </el-table-column>
              <el-table-column label="启用" width="90" align="center">
                <template #default="{ row }">
                  <el-switch
                    :model-value="row.enabled"
                    :disabled="saving"
                    @change="function (value) { toggleEnabled(row, value) }"
                  />
                </template>
              </el-table-column>
              <el-table-column label="错误策略" min-width="160">
                <template #default="{ row }">
                  <div class="transform-rule-drawer__type-cell">
                    <div class="transform-rule-drawer__type-title">{{ errorStrategyLabel(row.onError) }}</div>
                    <div class="transform-rule-drawer__type-hint">{{ errorStrategyDescription(row.onError) }}</div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="配置摘要" min-width="200">
                <template #default="{ row }">
                  {{ formatRuleConfigSummary(row) }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="260" fixed="right">
                <template #default="{ row, $index }">
                  <el-space wrap>
                    <el-button size="small" :disabled="$index === 0" @click="moveRuleUp($index)">上移</el-button>
                    <el-button size="small" :disabled="$index === rules.length - 1" @click="moveRuleDown($index)">下移</el-button>
                    <el-button size="small" @click="openEditRule(row, $index)">编辑</el-button>
                    <el-button size="small" type="danger" @click="deleteRule(row, $index)">删除</el-button>
                  </el-space>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="transform-rule-drawer__panel transform-rule-drawer__panel--test">
          <div class="transform-rule-drawer__section-header">
            <div>
              <h3>测试转换</h3>
              <p>输入原始值后，使用当前规则列表立即测试。</p>
            </div>
            <el-space>
              <el-button size="small" :loading="testing" type="primary" @click="runTest">测试</el-button>
              <el-button size="small" @click="clearTest">清空</el-button>
            </el-space>
          </div>

          <el-form label-width="88px" class="transform-rule-drawer__test-form">
            <el-form-item label="原始值">
              <el-input
                v-model="testInput"
                type="textarea"
                :rows="6"
                placeholder="例如  abc-123  "
              />
            </el-form-item>
          </el-form>

          <div v-if="testResult" class="transform-rule-drawer__test-result">
            <el-alert
              :type="testResult.success ? 'success' : 'warning'"
              :title="testResult.success ? '测试完成' : '测试失败'"
              :description="testResult.success ? '转换已按当前规则执行完成。' : testFailureHint"
              show-icon
              :closable="false"
            />

            <div class="transform-rule-drawer__result-summary">
              <div class="transform-rule-drawer__result-item">
                <span>原始值</span>
                <strong>{{ formatPreviewValue(testResult.originalValue) }}</strong>
              </div>
              <div class="transform-rule-drawer__result-item">
                <span>最终结果</span>
                <strong>{{ formatPreviewValue(testResult.resultValue) }}</strong>
              </div>
            </div>

            <div class="transform-rule-drawer__result-note" v-if="!testResult.success && failureRule">
              当前失败步骤已按 <strong>{{ errorStrategyLabel(failureRule.onError) }}</strong> 处理。
            </div>

            <el-table :data="testResult.steps || []" border stripe max-height="280" class="transform-rule-drawer__steps">
              <el-table-column label="步骤" width="80" align="center">
                <template #default="{ $index }">
                  {{ $index + 1 }}
                </template>
              </el-table-column>
              <el-table-column prop="transformType" label="规则" min-width="140">
                <template #default="{ row }">
                  {{ ruleTypeLabel(row.transformType) }}
                </template>
              </el-table-column>
              <el-table-column label="前值" min-width="180">
                <template #default="{ row }">
                  {{ formatPreviewValue(row.before) }}
                </template>
              </el-table-column>
              <el-table-column label="后值" min-width="180">
                <template #default="{ row }">
                  {{ formatPreviewValue(row.after) }}
                </template>
              </el-table-column>
              <el-table-column label="结果" width="96" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.success ? 'success' : 'warning'" effect="light">
                    {{ row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="错误" min-width="180">
                <template #default="{ row }">
                  {{ row.errorMessage || '—' }}
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-else class="transform-rule-drawer__empty">
            先输入样例值并点击“测试”，这里会展示逐步转换结果。
          </div>
        </div>
      </div>

      <div class="transform-rule-drawer__footer">
        <el-space>
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveAll">保存</el-button>
        </el-space>
      </div>
    </div>

    <el-dialog
      v-model="editorVisible"
      :title="editorTitle"
      width="760px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-form ref="editorFormRef" :model="editorForm" :rules="editorRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则类型" prop="transformType">
              <el-select v-model="editorForm.transformType" style="width: 100%;" @change="handleTypeChange">
                <el-option
                  v-for="option in transformTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="执行顺序" prop="transformOrder">
              <el-input-number v-model="editorForm.transformOrder" :min="1" :step="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="错误策略" prop="onError">
              <el-select v-model="editorForm.onError" style="width: 100%;">
                <el-option
                  v-for="option in errorStrategyOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认值">
              <el-input v-model="editorForm.defaultValue" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="启用规则">
          <el-switch v-model="editorForm.enabled" />
        </el-form-item>

        <div class="transform-rule-drawer__form-hint">
          {{ ruleTypeScenario(editorForm.transformType) }}
        </div>

        <template v-if="editorForm.transformType === 'null_to_default'">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="null_to_default 会优先使用上面的默认值。"
            description="留空会显式写入空字符串，表示将 null 转成空字符串。"
          />
        </template>

        <template v-else-if="editorForm.transformType === 'empty_to_null' || editorForm.transformType === 'trim' || editorForm.transformType === 'uppercase' || editorForm.transformType === 'lowercase'">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="当前规则无需额外配置。"
            description="直接保存即可。"
          />
        </template>

        <template v-else-if="editorForm.transformType === 'prefix'">
          <el-form-item label="前缀" prop="prefix">
            <el-input v-model="editorForm.prefix" placeholder="请输入前缀" />
          </el-form-item>
        </template>

        <template v-else-if="editorForm.transformType === 'suffix'">
          <el-form-item label="后缀" prop="suffix">
            <el-input v-model="editorForm.suffix" placeholder="请输入后缀" />
          </el-form-item>
        </template>

        <template v-else-if="editorForm.transformType === 'replace'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="from" prop="from">
                <el-input v-model="editorForm.from" placeholder="必填" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="to">
                <el-input v-model="editorForm.to" placeholder="可为空字符串" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else-if="editorForm.transformType === 'date_format'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="fromPattern" prop="fromPattern">
                <el-input v-model="editorForm.fromPattern" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="toPattern" prop="toPattern">
                <el-input v-model="editorForm.toPattern" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else-if="editorForm.transformType === 'number_scale'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="scale" prop="scale">
                <el-input-number v-model="editorForm.scale" :min="0" :max="10" :step="1" style="width: 100%;" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="roundingMode" prop="roundingMode">
                <el-select v-model="editorForm.roundingMode" style="width: 100%;">
                  <el-option
                    v-for="option in roundingModeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-else-if="editorForm.transformType === 'dict_map'">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="字典映射的默认值使用上方“默认值”字段。"
          />
          <div class="transform-rule-drawer__mapping-head">
            <div class="transform-rule-drawer__mapping-title">映射项</div>
            <el-button size="small" @click="addMappingEntry">新增映射项</el-button>
          </div>
          <div class="transform-rule-drawer__mapping-list">
            <div
              v-for="(item, index) in editorForm.mappingEntries"
              :key="index"
              class="transform-rule-drawer__mapping-row"
            >
              <el-row :gutter="12">
                <el-col :span="10">
                  <el-input v-model="item.key" placeholder="key" />
                </el-col>
                <el-col :span="10">
                  <el-input v-model="item.value" placeholder="value" />
                </el-col>
                <el-col :span="4">
                  <el-button style="width: 100%;" @click="removeMappingEntry(index)">删除</el-button>
                </el-col>
              </el-row>
            </div>
          </div>
        </template>

        <template v-else-if="editorForm.transformType === 'constant'">
          <el-form-item label="value" prop="value">
            <el-input v-model="editorForm.value" placeholder="固定写入的值" />
          </el-form-item>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="无论输入值是什么，该规则都会写入固定值。"
          />
        </template>

        <template v-else-if="editorForm.transformType === 'script_js'">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            title="脚本只能访问 value、source、target 和 context。对象/数组会被转成 JSON 字符串，不会保持对象类型。"
            description="示例：return { raw: value, normalized: String(value).trim() }"
          />

          <el-form-item label="JavaScript 脚本" prop="script">
            <el-input
              v-model="editorForm.script"
              type="textarea"
              :rows="10"
              placeholder="return value == null ? '' : String(value).trim();"
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="结果类型" prop="resultType">
                <el-select v-model="editorForm.resultType" style="width: 100%;">
                  <el-option
                    v-for="option in resultTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="超时(ms)" prop="timeoutMs">
                <el-input-number v-model="editorForm.timeoutMs" :min="1" :max="1000" :step="50" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="允许 null" prop="allowNull">
                <el-switch v-model="editorForm.allowNull" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最大输出" prop="maxOutputLength">
                <el-input-number v-model="editorForm.maxOutputLength" :min="1" :step="100" style="width: 100%;" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </el-form>

      <template #footer>
        <el-space>
          <el-button @click="editorVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmEditor">确定</el-button>
        </el-space>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteTransformRule,
  listTransformRules,
  saveFieldMapping,
  saveTransformRule,
  setTransformRuleEnabled,
  testTransformRules
} from '../services/backend'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  task: {
    type: Object,
    default: null
  },
  mapping: {
    type: Object,
    default: null
  },
  sourceSchemas: {
    type: Array,
    default: function () {
      return []
    }
  },
  targetSchemas: {
    type: Array,
    default: function () {
      return []
    }
  }
})

const emit = defineEmits(['update:visible', 'saved'])

const transformTypeOptions = [
  { value: 'null_to_default', label: 'null_to_default · null 转默认值', description: '将 null 替换成默认值', scenario: '目标字段允许自定义默认值时使用' },
  { value: 'empty_to_null', label: 'empty_to_null · 空字符串转 null', description: '将空字符串转为 null', scenario: '清洗空白内容时使用' },
  { value: 'trim', label: 'trim · 去除左右空格', description: '去除前后空白字符', scenario: '文本字段标准化时使用' },
  { value: 'uppercase', label: 'uppercase · 转大写', description: '将文本转成大写', scenario: '编码、标识字段统一时使用' },
  { value: 'lowercase', label: 'lowercase · 转小写', description: '将文本转成小写', scenario: '邮箱、用户名等字段统一时使用' },
  { value: 'prefix', label: 'prefix · 添加前缀', description: '在值前面追加文本', scenario: '拼接业务编码、表前缀时使用' },
  { value: 'suffix', label: 'suffix · 添加后缀', description: '在值后面追加文本', scenario: '拼接后缀标识时使用' },
  { value: 'replace', label: 'replace · 字符串替换', description: '按 from/to 替换文本', scenario: '清理分隔符或字符时使用' },
  { value: 'date_format', label: 'date_format · 日期格式转换', description: '日期字符串或时间类型格式化', scenario: '跨系统日期格式不一致时使用' },
  { value: 'number_scale', label: 'number_scale · 数字精度处理', description: '调整小数位和舍入模式', scenario: '金额、数量等数值字段使用' },
  { value: 'dict_map', label: 'dict_map · 字典映射', description: '按 key/value 做字典转换', scenario: '状态码、枚举值转换时使用' },
  { value: 'constant', label: 'constant · 固定值', description: '始终输出固定值', scenario: '补默认业务标记时使用' },
  { value: 'script_js', label: 'script_js · JavaScript 脚本', description: '运行沙箱化 JavaScript 脚本', scenario: '需要自定义复杂转换逻辑时使用' }
]

const errorStrategyOptions = [
  { value: 'FAIL', label: 'fail · 失败即终止', description: '转换失败时本行失败' },
  { value: 'USE_ORIGINAL', label: 'use_original · 回退原始值', description: '转换失败时保留原始值' },
  { value: 'USE_DEFAULT', label: 'use_default · 回退默认值', description: '转换失败时写入默认值' },
  { value: 'SET_NULL', label: 'set_null · 回退 null', description: '转换失败时写入 null' }
]

const roundingModeOptions = [
  { value: 'UP', label: 'UP' },
  { value: 'DOWN', label: 'DOWN' },
  { value: 'CEILING', label: 'CEILING' },
  { value: 'FLOOR', label: 'FLOOR' },
  { value: 'HALF_UP', label: 'HALF_UP' },
  { value: 'HALF_DOWN', label: 'HALF_DOWN' },
  { value: 'HALF_EVEN', label: 'HALF_EVEN' }
]

const resultTypeOptions = [
  { value: 'auto', label: 'auto · 智能处理' },
  { value: 'text', label: 'text · 文本输出' },
  { value: 'json', label: 'json · 严格 JSON' }
]

const drawerVisible = computed({
  get: function () {
    return props.visible
  },
  set: function (value) {
    emit('update:visible', value)
  }
})

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const editorVisible = ref(false)
const editorFormRef = ref(null)
const rules = ref([])
const originalRules = ref([])
const originalRuleIds = ref([])
const testInput = ref('')
const testResult = ref(null)
const currentEditorIndex = ref(-1)

const editorForm = reactive(createEmptyEditorForm())

const editorRules = {
  transformType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  onError: [{ required: true, message: '请选择错误策略', trigger: 'change' }],
  transformOrder: [
    {
      validator: function (rule, value, callback) {
        if (value === null || value === undefined || value === '') {
          callback(new Error('请输入执行顺序'))
          return
        }
        const number = Number(value)
        if (!Number.isInteger(number) || number < 1) {
          callback(new Error('执行顺序必须是正整数'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const drawerTitle = computed(function () {
  if (!props.mapping) {
    return '转换规则配置'
  }
  return '转换规则配置 · ' + (props.mapping.sourceColumnName || '—') + ' → ' + (props.mapping.targetColumnName || '—')
})

const ruleSummaryText = computed(function () {
  return buildRuleSummary(rules.value)
})

const sourceFieldType = computed(function () {
  return resolveFieldType('source')
})

const targetFieldType = computed(function () {
  return resolveFieldType('target')
})

const failureRule = computed(function () {
  if (!testResult.value || !testResult.value.steps || testResult.value.success) {
    return null
  }
  const failedStepIndex = findFailedStepIndex(testResult.value.steps)
  if (failedStepIndex < 0) {
    return null
  }
  const enabledRules = getEnabledRulesInOrder()
  return enabledRules[failedStepIndex] || null
})

const testFailureHint = computed(function () {
  if (!testResult.value) {
    return ''
  }
  if (testResult.value.success) {
    return '转换已按当前规则执行完成。'
  }
  return testResult.value.errorMessage || '转换失败，请检查规则配置。'
})

watch(
  function () {
    return [props.visible, props.mapping && props.mapping.id, props.mapping && props.mapping.sourceColumnName, props.mapping && props.mapping.targetColumnName]
  },
  function (value) {
    if (value[0] && props.mapping) {
      loadRules()
    }
    if (!value[0]) {
      resetState()
    }
  },
  { immediate: true }
)

function resetState() {
  rules.value = []
  originalRules.value = []
  originalRuleIds.value = []
  testInput.value = ''
  testResult.value = null
  currentEditorIndex.value = -1
  editorVisible.value = false
  Object.assign(editorForm, createEmptyEditorForm())
}

async function loadRules() {
  if (!props.task || !props.task.id || !props.mapping || !props.mapping.id) {
    rules.value = []
    originalRules.value = []
    originalRuleIds.value = []
    return
  }
  loading.value = true
  try {
    const taskRules = await listTransformRules({ taskId: props.task.id })
    const fieldRules = await listTransformRules({ taskId: props.task.id, fieldMappingId: props.mapping.id })
    const merged = mergeRules(taskRules, fieldRules)
    rules.value = merged.map(function (rule, index) {
      return normalizeLoadedRule(rule, index)
    })
    originalRules.value = cloneRules(rules.value)
    originalRuleIds.value = rules.value.map(function (item) {
      return item.id
    }).filter(Boolean)
    testResult.value = null
  } catch (error) {
    ElMessage.error(error.message || '加载转换规则失败')
    rules.value = []
  } finally {
    loading.value = false
  }
}

function mergeRules(taskRules, fieldRules) {
  const result = []
  const seen = {}
  ;(fieldRules || []).forEach(function (item) {
    if (item && item.id != null) {
      seen[item.id] = true
      result.push(item)
    }
  })
  ;(taskRules || []).forEach(function (item) {
    if (!item || item.id == null || seen[item.id]) {
      return
    }
    if (matchesMapping(item, props.mapping)) {
      result.push(item)
    }
  })
  result.sort(function (left, right) {
    const leftOrder = left && left.transformOrder ? Number(left.transformOrder) : 0
    const rightOrder = right && right.transformOrder ? Number(right.transformOrder) : 0
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    const leftId = left && left.id ? Number(left.id) : 0
    const rightId = right && right.id ? Number(right.id) : 0
    return leftId - rightId
  })
  return result
}

function matchesMapping(rule, mapping) {
  if (!rule || !mapping) {
    return false
  }
  const sourceField = normalizeText(rule.sourceField)
  const targetField = normalizeText(rule.targetField)
  const mappingSource = normalizeText(mapping.sourceColumnName)
  const mappingTarget = normalizeText(mapping.targetColumnName)
  if (rule.fieldMappingId && mapping.id && Number(rule.fieldMappingId) === Number(mapping.id)) {
    return true
  }
  return sourceField === mappingSource && targetField === mappingTarget
}

function normalizeLoadedRule(rule, index) {
  const config = parseConfig(rule && rule.transformConfig)
  return {
    id: rule && rule.id != null ? Number(rule.id) : null,
    taskId: rule && rule.taskId != null ? Number(rule.taskId) : (props.task ? Number(props.task.id) : null),
    tableTaskId: rule && rule.tableTaskId != null ? Number(rule.tableTaskId) : null,
    fieldMappingId: rule && rule.fieldMappingId != null ? Number(rule.fieldMappingId) : null,
    sourceField: rule && rule.sourceField ? rule.sourceField : (props.mapping ? props.mapping.sourceColumnName : ''),
    targetField: rule && rule.targetField ? rule.targetField : (props.mapping ? props.mapping.targetColumnName : ''),
    transformType: rule && rule.transformType ? rule.transformType : 'trim',
    transformConfig: rule && rule.transformConfig ? rule.transformConfig : '{}',
    transformOrder: rule && rule.transformOrder != null ? Number(rule.transformOrder) : index + 1,
    enabled: rule ? rule.enabled !== false : true,
    onError: rule && rule.onError ? rule.onError : 'FAIL',
    defaultValue: rule && rule.defaultValue != null ? String(rule.defaultValue) : '',
    script: stringConfigValue(config, 'script'),
    timeoutMs: numberConfigValue(config, 'timeoutMs') == null ? 1000 : numberConfigValue(config, 'timeoutMs'),
    resultType: stringConfigValue(config, 'resultType') || 'auto',
    allowNull: config && config.allowNull !== undefined ? !!config.allowNull : true,
    maxOutputLength: numberConfigValue(config, 'maxOutputLength') == null ? 10000 : numberConfigValue(config, 'maxOutputLength'),
    config: config
  }
}

function cloneRules(sourceRules) {
  return (sourceRules || []).map(function (rule) {
    return normalizeLoadedRule(rule, 0)
  })
}

function createEmptyEditorForm() {
  return {
    id: null,
    taskId: null,
    tableTaskId: null,
    fieldMappingId: null,
    transformType: 'trim',
    transformOrder: 1,
    enabled: true,
    onError: 'FAIL',
    defaultValue: '',
    prefix: '',
    suffix: '',
    from: '',
    to: '',
    fromPattern: 'yyyy/MM/dd HH:mm:ss',
    toPattern: 'yyyy-MM-dd HH:mm:ss',
    scale: 2,
    roundingMode: 'HALF_UP',
    mappingEntries: [
      { key: 'Y', value: '启用' },
      { key: 'N', value: '禁用' }
    ],
    value: '',
    script: '',
    timeoutMs: 1000,
    resultType: 'auto',
    allowNull: true,
    maxOutputLength: 10000
  }
}

function openAddRule() {
  currentEditorIndex.value = -1
  Object.assign(editorForm, createEmptyEditorForm())
  editorVisible.value = true
}

function openEditRule(rule, index) {
  currentEditorIndex.value = index
  Object.assign(editorForm, createEmptyEditorForm())
  editorForm.id = rule.id
  editorForm.taskId = rule.taskId
  editorForm.tableTaskId = rule.tableTaskId
  editorForm.fieldMappingId = rule.fieldMappingId
  editorForm.transformType = rule.transformType
  editorForm.transformOrder = rule.transformOrder
  editorForm.enabled = rule.enabled !== false
  editorForm.onError = rule.onError || 'FAIL'
  editorForm.defaultValue = rule.defaultValue || ''
  applyConfigToForm(rule)
  editorVisible.value = true
}

function applyConfigToForm(rule) {
  const config = rule && rule.config ? rule.config : parseConfig(rule ? rule.transformConfig : null)
  if (editorForm.transformType === 'prefix') {
    editorForm.prefix = stringConfigValue(config, 'prefix')
  } else if (editorForm.transformType === 'suffix') {
    editorForm.suffix = stringConfigValue(config, 'suffix')
  } else if (editorForm.transformType === 'replace') {
    editorForm.from = stringConfigValue(config, 'from')
    editorForm.to = stringConfigValue(config, 'to')
  } else if (editorForm.transformType === 'date_format') {
    editorForm.fromPattern = stringConfigValue(config, 'fromPattern') || 'yyyy/MM/dd HH:mm:ss'
    editorForm.toPattern = stringConfigValue(config, 'toPattern') || 'yyyy-MM-dd HH:mm:ss'
  } else if (editorForm.transformType === 'number_scale') {
    const scale = numberConfigValue(config, 'scale')
    editorForm.scale = scale == null ? 2 : scale
    editorForm.roundingMode = stringConfigValue(config, 'roundingMode') || 'HALF_UP'
  } else if (editorForm.transformType === 'dict_map') {
    editorForm.mappingEntries = mappingEntriesFromConfig(config)
  } else if (editorForm.transformType === 'constant') {
    editorForm.value = stringConfigValue(config, 'value')
  } else if (editorForm.transformType === 'script_js') {
    editorForm.script = stringConfigValue(config, 'script')
    editorForm.timeoutMs = numberConfigValue(config, 'timeoutMs') == null ? 1000 : numberConfigValue(config, 'timeoutMs')
    editorForm.resultType = stringConfigValue(config, 'resultType') || 'auto'
    editorForm.allowNull = config && config.allowNull !== undefined ? !!config.allowNull : true
    editorForm.maxOutputLength = numberConfigValue(config, 'maxOutputLength') == null ? 10000 : numberConfigValue(config, 'maxOutputLength')
  }
}

function handleTypeChange() {
  const currentId = editorForm.id
  const currentTaskId = editorForm.taskId
  const currentTableTaskId = editorForm.tableTaskId
  const currentFieldMappingId = editorForm.fieldMappingId
  const currentOrder = editorForm.transformOrder
  const currentEnabled = editorForm.enabled
  const currentOnError = editorForm.onError
  const currentDefaultValue = editorForm.defaultValue
  Object.assign(editorForm, createEmptyEditorForm())
  editorForm.id = currentId
  editorForm.taskId = currentTaskId
  editorForm.tableTaskId = currentTableTaskId
  editorForm.fieldMappingId = currentFieldMappingId
  editorForm.transformOrder = currentOrder
  editorForm.enabled = currentEnabled
  editorForm.onError = currentOnError
  editorForm.defaultValue = currentDefaultValue
}

function addMappingEntry() {
  if (!editorForm.mappingEntries) {
    editorForm.mappingEntries = []
  }
  editorForm.mappingEntries.push({ key: '', value: '' })
}

function removeMappingEntry(index) {
  editorForm.mappingEntries.splice(index, 1)
}

function confirmEditor() {
  if (!editorFormRef.value) {
    return
  }
  editorFormRef.value.validate(function (valid) {
    if (!valid) {
      return
    }
    const validationError = validateConfigByType()
    if (validationError) {
      ElMessage.error(validationError)
      return
    }
    saveEditorRule()
  })
}

async function saveEditorRule() {
  try {
    const localRule = normalizeLoadedRule(buildRulePayloadFromForm(editorForm), currentEditorIndex.value < 0 ? rules.value.length : currentEditorIndex.value)
    upsertLocalRule(localRule)
    editorVisible.value = false
    ElMessage.success('规则已暂存')
  } catch (error) {
    ElMessage.error(error.message || '保存规则失败')
  }
}

function buildRulePayload(form) {
  return buildRulePayloadFromForm(form)
}

function buildRulePayloadFromForm(form) {
  return {
    id: form.id,
    taskId: form.taskId != null ? form.taskId : (props.task ? props.task.id : null),
    tableTaskId: form.tableTaskId != null ? form.tableTaskId : null,
    fieldMappingId: form.fieldMappingId != null ? form.fieldMappingId : (props.mapping ? props.mapping.id : null),
    sourceField: props.mapping ? props.mapping.sourceColumnName : '',
    targetField: props.mapping ? props.mapping.targetColumnName : '',
    transformType: form.transformType,
    transformConfig: JSON.stringify(buildTransformConfig(form)),
    transformOrder: Number(form.transformOrder),
    enabled: !!form.enabled,
    onError: form.onError || 'FAIL',
    defaultValue: normalizeDefaultValue(form.defaultValue)
  }
}

function buildRulePayloadFromRule(rule, transformOrder) {
  return {
    id: rule.id,
    taskId: props.task ? props.task.id : null,
    tableTaskId: rule.tableTaskId != null ? rule.tableTaskId : null,
    fieldMappingId: rule.fieldMappingId != null ? rule.fieldMappingId : null,
    sourceField: rule.sourceField || (props.mapping ? props.mapping.sourceColumnName : ''),
    targetField: rule.targetField || (props.mapping ? props.mapping.targetColumnName : ''),
    transformType: rule.transformType,
    transformConfig: JSON.stringify(buildTransformConfigFromRule(rule)),
    transformOrder: Number(transformOrder == null ? rule.transformOrder : transformOrder),
    enabled: !!rule.enabled,
    onError: rule.onError || 'FAIL',
    defaultValue: normalizeDefaultValue(rule.defaultValue)
  }
}

function buildTransformConfig(form) {
  return buildTransformConfigFromForm(form)
}

function buildTransformConfigFromForm(form) {
  if (form.transformType === 'prefix') {
    return { prefix: form.prefix || '' }
  }
  if (form.transformType === 'suffix') {
    return { suffix: form.suffix || '' }
  }
  if (form.transformType === 'replace') {
    return { from: form.from || '', to: form.to || '' }
  }
  if (form.transformType === 'date_format') {
    return {
      fromPattern: form.fromPattern || 'yyyy/MM/dd HH:mm:ss',
      toPattern: form.toPattern || 'yyyy-MM-dd HH:mm:ss'
    }
  }
  if (form.transformType === 'number_scale') {
    return {
      scale: Number(form.scale == null ? 2 : form.scale),
      roundingMode: form.roundingMode || 'HALF_UP'
    }
  }
  if (form.transformType === 'dict_map') {
    const mapping = {}
    ;(form.mappingEntries || []).forEach(function (entry) {
      if (!entry || entry.key == null) {
        return
      }
      mapping[String(entry.key).trim()] = entry.value == null ? '' : String(entry.value)
    })
    return {
      mapping: mapping
    }
  }
  if (form.transformType === 'constant') {
    return {
      value: form.value == null ? '' : String(form.value)
    }
  }
  if (form.transformType === 'script_js') {
    return {
      script: form.script == null ? '' : String(form.script),
      timeoutMs: Number(form.timeoutMs == null ? 1000 : form.timeoutMs),
      resultType: form.resultType || 'auto',
      allowNull: form.allowNull !== false,
      maxOutputLength: Number(form.maxOutputLength == null ? 10000 : form.maxOutputLength)
    }
  }
  if (form.transformType === 'null_to_default') {
    return {}
  }
  return {}
}

function buildTransformConfigFromRule(rule) {
  const config = rule && rule.config ? rule.config : {}
  if (rule.transformType === 'prefix') {
    return { prefix: config.prefix || '' }
  }
  if (rule.transformType === 'suffix') {
    return { suffix: config.suffix || '' }
  }
  if (rule.transformType === 'replace') {
    return { from: config.from || '', to: config.to || '' }
  }
  if (rule.transformType === 'date_format') {
    return {
      fromPattern: config.fromPattern || 'yyyy/MM/dd HH:mm:ss',
      toPattern: config.toPattern || 'yyyy-MM-dd HH:mm:ss'
    }
  }
  if (rule.transformType === 'number_scale') {
    return {
      scale: Number(config.scale == null ? 2 : config.scale),
      roundingMode: config.roundingMode || 'HALF_UP'
    }
  }
  if (rule.transformType === 'dict_map') {
    const mapping = {}
    Object.keys(config.mapping || {}).forEach(function (key) {
      mapping[String(key).trim()] = config.mapping[key] == null ? '' : String(config.mapping[key])
    })
    return {
      mapping: mapping
    }
  }
  if (rule.transformType === 'constant') {
    return {
      value: config.value == null ? '' : String(config.value)
    }
  }
  if (rule.transformType === 'script_js') {
    return {
      script: config.script == null ? '' : String(config.script),
      timeoutMs: Number(config.timeoutMs == null ? 1000 : config.timeoutMs),
      resultType: config.resultType || 'auto',
      allowNull: config.allowNull !== false,
      maxOutputLength: Number(config.maxOutputLength == null ? 10000 : config.maxOutputLength)
    }
  }
  return {}
}

function normalizeDefaultValue(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value)
}

function validateConfigByType() {
  if (editorForm.transformType === 'prefix' && !stringHasContent(editorForm.prefix)) {
    return '请输入前缀'
  }
  if (editorForm.transformType === 'suffix' && !stringHasContent(editorForm.suffix)) {
    return '请输入后缀'
  }
  if (editorForm.transformType === 'replace' && !stringHasContent(editorForm.from)) {
    return 'replace 需要填写 from'
  }
  if (editorForm.transformType === 'date_format') {
    if (!stringHasContent(editorForm.fromPattern)) {
      return '请填写 fromPattern'
    }
    if (!stringHasContent(editorForm.toPattern)) {
      return '请填写 toPattern'
    }
  }
  if (editorForm.transformType === 'number_scale') {
    if (!Number.isInteger(Number(editorForm.scale)) || Number(editorForm.scale) < 0 || Number(editorForm.scale) > 10) {
      return 'scale 必须是 0 到 10 的整数'
    }
    if (!stringHasContent(editorForm.roundingMode)) {
      return '请选择 roundingMode'
    }
  }
  if (editorForm.transformType === 'dict_map') {
    if (!editorForm.mappingEntries || editorForm.mappingEntries.length === 0) {
      return '请至少新增一条字典映射'
    }
    for (let i = 0; i < editorForm.mappingEntries.length; i += 1) {
      const entry = editorForm.mappingEntries[i]
      if (!entry || !stringHasContent(entry.key)) {
        return '字典映射的 key 不能为空'
      }
    }
  }
  if (editorForm.transformType === 'script_js') {
    if (!stringHasContent(editorForm.script)) {
      return '请编写 JavaScript 脚本'
    }
    if (!Number.isInteger(Number(editorForm.timeoutMs)) || Number(editorForm.timeoutMs) < 1 || Number(editorForm.timeoutMs) > 1000) {
      return '超时必须是 1 到 1000 的整数'
    }
    if (!stringHasContent(editorForm.resultType) || findOption(resultTypeOptions, editorForm.resultType) == null) {
      return '请选择合法的结果类型'
    }
    if (!Number.isInteger(Number(editorForm.maxOutputLength)) || Number(editorForm.maxOutputLength) < 1) {
      return '最大输出长度必须是大于 0 的整数'
    }
  }
  return ''
}

function upsertLocalRule(rule) {
  const nextRules = rules.value.slice()
  if (currentEditorIndex.value >= 0 && currentEditorIndex.value < nextRules.length) {
    nextRules[currentEditorIndex.value] = rule
  } else {
    nextRules.push(rule)
  }
  nextRules.sort(function (left, right) {
    const leftOrder = left.transformOrder == null ? 0 : Number(left.transformOrder)
    const rightOrder = right.transformOrder == null ? 0 : Number(right.transformOrder)
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    const leftId = left.id == null ? 0 : Number(left.id)
    const rightId = right.id == null ? 0 : Number(right.id)
    return leftId - rightId
  })
  rules.value = nextRules
}

async function deleteRule(rule, index) {
  try {
    await ElMessageBox.confirm('确定删除这条转换规则吗？', '提示', { type: 'warning' })
  } catch (error) {
    return
  }
  if (rule && rule.id != null) {
    await deleteTransformRule(rule.id)
  }
  rules.value.splice(index, 1)
  originalRules.value = cloneRules(rules.value)
  ElMessage.success('规则已删除')
}

async function toggleEnabled(rule, enabled) {
  if (!rule || rule.id == null) {
    rule.enabled = !!enabled
    return
  }
  try {
    const saved = await setTransformRuleEnabled(rule.id, !!enabled)
    rule.enabled = saved && saved.enabled !== undefined ? !!saved.enabled : !!enabled
    rule.onError = saved && saved.onError ? saved.onError : rule.onError
    originalRules.value = cloneRules(rules.value)
    ElMessage.success(!!enabled ? '已启用规则' : '已停用规则')
  } catch (error) {
    ElMessage.error(error.message || '切换规则状态失败')
  }
}

function moveRuleUp(index) {
  if (index <= 0) {
    return
  }
  swapRules(index, index - 1)
}

function moveRuleDown(index) {
  if (index < 0 || index >= rules.value.length - 1) {
    return
  }
  swapRules(index, index + 1)
}

function swapRules(leftIndex, rightIndex) {
  const nextRules = rules.value.slice()
  const left = nextRules[leftIndex]
  nextRules[leftIndex] = nextRules[rightIndex]
  nextRules[rightIndex] = left
  nextRules.forEach(function (item, index) {
    item.transformOrder = index + 1
  })
  rules.value = nextRules
}

async function restoreUnconfigured() {
  try {
    await ElMessageBox.confirm('确定恢复为未配置吗？这会删除当前映射的全部转换规则。', '提示', { type: 'warning' })
  } catch (error) {
    return
  }
  const current = rules.value.slice()
  for (let i = 0; i < current.length; i += 1) {
    if (current[i] && current[i].id != null) {
      await deleteTransformRule(current[i].id)
    }
  }
  rules.value = []
  originalRules.value = []
  testResult.value = null
  ElMessage.success('已恢复为未配置')
}

function clearTest() {
  testInput.value = ''
  testResult.value = null
}

async function runTest() {
  if (!props.task || !props.task.id || !props.mapping) {
    ElMessage.warning('请先选择一个字段映射')
    return
  }
  testing.value = true
  try {
    const payload = {
      taskId: props.task.id,
      fieldMappingId: props.mapping.id,
      sourceField: props.mapping.sourceColumnName,
      targetField: props.mapping.targetColumnName,
      value: testInput.value,
      rules: rules.value.map(function (rule, index) {
        return buildRulePayloadFromRule(rule, index + 1)
      })
    }
    testResult.value = await testTransformRules(payload)
  } catch (error) {
    ElMessage.error(error.message || '测试转换失败')
  } finally {
    testing.value = false
  }
}

async function saveAll() {
  saving.value = true
  try {
    const savedRules = []
    for (let i = 0; i < rules.value.length; i += 1) {
      const rule = rules.value[i]
      rule.transformOrder = i + 1
      const saved = await saveTransformRule(buildRulePayloadFromRule(rule, rule.transformOrder))
      savedRules.push(normalizeLoadedRule(saved, i))
    }
    rules.value = savedRules
    originalRules.value = cloneRules(savedRules)
    originalRuleIds.value = savedRules.map(function (item) {
      return item.id
    }).filter(Boolean)
    const summary = buildRuleSummary(savedRules)
    if (props.mapping && props.mapping.id != null) {
      await saveFieldMapping({
        id: props.mapping.id,
        taskId: props.mapping.taskId,
        sourceSchemaName: props.mapping.sourceSchemaName,
        targetSchemaName: props.mapping.targetSchemaName,
        sourceTableName: props.mapping.sourceTableName,
        targetTableName: props.mapping.targetTableName,
        sourceColumnName: props.mapping.sourceColumnName,
        targetColumnName: props.mapping.targetColumnName,
        ignored: !!props.mapping.ignored,
        defaultValue: props.mapping.defaultValue || '',
        transformRule: summary
      })
    }
    emit('saved', {
      summary: summary,
      mappingId: props.mapping ? props.mapping.id : null
    })
    ElMessage.success('转换规则已保存')
    drawerVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '保存转换规则失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  if (!isDirty()) {
    drawerVisible.value = false
    return
  }
  ElMessageBox.confirm('当前有未保存的修改，确定放弃吗？', '提示', { type: 'warning' })
    .then(function () {
      drawerVisible.value = false
    })
    .catch(function () {
    })
}

function handleBeforeClose(done) {
  if (!isDirty()) {
    done()
    return
  }
  ElMessageBox.confirm('当前有未保存的修改，确定放弃吗？', '提示', { type: 'warning' })
    .then(function () {
      done()
    })
    .catch(function () {
    })
}

function isDirty() {
  return JSON.stringify(stripRuntimeFields(rules.value)) !== JSON.stringify(stripRuntimeFields(originalRules.value))
}

function stripRuntimeFields(ruleList) {
  return (ruleList || []).map(function (item) {
    return {
      id: item.id,
      taskId: item.taskId,
      tableTaskId: item.tableTaskId,
      fieldMappingId: item.fieldMappingId,
      sourceField: item.sourceField,
      targetField: item.targetField,
      transformType: item.transformType,
      transformConfig: item.transformConfig,
      transformOrder: item.transformOrder,
      enabled: item.enabled,
      onError: item.onError,
      defaultValue: item.defaultValue
    }
  })
}

function buildRuleSummary(ruleList) {
  const activeRules = (ruleList || []).filter(function (item) {
    return item && item.enabled !== false
  })
  if (!ruleList || ruleList.length === 0) {
    return '未配置'
  }
  if (activeRules.length === 0) {
    return '已停用'
  }
  if (activeRules.length <= 2) {
    return activeRules.map(function (item) {
      return item.transformType
    }).join(' + ')
  }
  return '已配置 ' + ruleList.length + ' 条'
}

function formatRuleConfigSummary(rule) {
  if (!rule) {
    return '—'
  }
  if (rule.transformType === 'null_to_default') {
    return stringHasContent(rule.defaultValue) ? ('defaultValue=' + rule.defaultValue) : 'defaultValue=空字符串'
  }
  if (rule.transformType === 'empty_to_null' || rule.transformType === 'trim' || rule.transformType === 'uppercase' || rule.transformType === 'lowercase') {
    return '无额外配置'
  }
  if (rule.transformType === 'prefix') {
    return 'prefix=' + (rule.config && rule.config.prefix ? rule.config.prefix : '')
  }
  if (rule.transformType === 'suffix') {
    return 'suffix=' + (rule.config && rule.config.suffix ? rule.config.suffix : '')
  }
  if (rule.transformType === 'replace') {
    return 'from=' + (rule.config && rule.config.from ? rule.config.from : '') + ', to=' + (rule.config && rule.config.to != null ? rule.config.to : '')
  }
  if (rule.transformType === 'date_format') {
    return (rule.config && rule.config.fromPattern ? rule.config.fromPattern : '') + ' -> ' + (rule.config && rule.config.toPattern ? rule.config.toPattern : '')
  }
  if (rule.transformType === 'number_scale') {
    return 'scale=' + (rule.config && rule.config.scale != null ? rule.config.scale : 2) + ', roundingMode=' + (rule.config && rule.config.roundingMode ? rule.config.roundingMode : 'HALF_UP')
  }
  if (rule.transformType === 'dict_map') {
    const entries = mappingEntriesFromConfig(rule.config)
    if (!entries.length) {
      return '未配置映射项'
    }
    const mappingSummary = entries.slice(0, 2).map(function (entry) {
      return entry.key + '->' + entry.value
    }).join(', ')
    if (stringHasContent(rule.defaultValue)) {
      return mappingSummary + ', defaultValue=' + rule.defaultValue
    }
    return mappingSummary
  }
  if (rule.transformType === 'constant') {
    return stringHasContent(rule.config && rule.config.value) ? ('value=' + rule.config.value) : 'value=空字符串'
  }
  if (rule.transformType === 'script_js') {
    const resultType = rule.config && rule.config.resultType ? rule.config.resultType : 'auto'
    const timeoutMs = rule.config && rule.config.timeoutMs != null ? rule.config.timeoutMs : 1000
    const maxOutputLength = rule.config && rule.config.maxOutputLength != null ? rule.config.maxOutputLength : 10000
    const allowNull = rule.config && rule.config.allowNull !== false
    return 'resultType=' + resultType + ', timeoutMs=' + timeoutMs + 'ms, allowNull=' + allowNull + ', maxOutputLength=' + maxOutputLength
  }
  return '—'
}

function ruleTypeLabel(type) {
  const option = findOption(transformTypeOptions, type)
  return option ? option.label : (type || '—')
}

function ruleTypeDescription(type) {
  const option = findOption(transformTypeOptions, type)
  return option ? option.description : '—'
}

function ruleTypeScenario(type) {
  const option = findOption(transformTypeOptions, type)
  return option ? option.scenario : '—'
}

function errorStrategyLabel(value) {
  const option = findOption(errorStrategyOptions, value)
  return option ? option.label : (value || 'FAIL')
}

function errorStrategyDescription(value) {
  const option = findOption(errorStrategyOptions, value)
  return option ? option.description : '失败即终止'
}

function findOption(options, value) {
  for (let i = 0; i < options.length; i += 1) {
    if (options[i].value === value) {
      return options[i]
    }
  }
  return null
}

function formatPreviewValue(value) {
  if (value === null) {
    return 'null'
  }
  if (value === undefined) {
    return 'undefined'
  }
  if (typeof value === 'string') {
    return value.length === 0 ? '空字符串' : value
  }
  return String(value)
}

function findFailedStepIndex(steps) {
  for (let i = 0; i < steps.length; i += 1) {
    if (steps[i] && steps[i].success === false) {
      return i
    }
  }
  return -1
}

function getEnabledRulesInOrder() {
  return rules.value
    .filter(function (item) {
      return item && item.enabled !== false
    })
    .slice()
    .sort(function (left, right) {
      const leftOrder = left && left.transformOrder != null ? Number(left.transformOrder) : 0
      const rightOrder = right && right.transformOrder != null ? Number(right.transformOrder) : 0
      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder
      }
      const leftId = left && left.id != null ? Number(left.id) : 0
      const rightId = right && right.id != null ? Number(right.id) : 0
      return leftId - rightId
    })
}

function resolveFieldType(side) {
  if (!props.mapping) {
    return '—'
  }
  const schemaName = side === 'source' ? props.mapping.sourceSchemaName : props.mapping.targetSchemaName
  const tableName = side === 'source' ? props.mapping.sourceTableName : props.mapping.targetTableName
  const columnName = side === 'source' ? props.mapping.sourceColumnName : props.mapping.targetColumnName
  const schemas = side === 'source' ? props.sourceSchemas : props.targetSchemas
  const table = findTable(schemas, schemaName, tableName)
  if (!table || !table.columns) {
    return '—'
  }
  for (let i = 0; i < table.columns.length; i += 1) {
    const column = table.columns[i]
    if (column && column.name && columnName && normalizeText(column.name) === normalizeText(columnName)) {
      return column.type || column.columnType || column.dataType || '—'
    }
  }
  return '—'
}

function findTable(schemas, schemaName, tableName) {
  if (!schemaName || !tableName) {
    return null
  }
  for (let i = 0; i < (schemas || []).length; i += 1) {
    const schema = schemas[i]
    if (!schema || !schema.schemaName || normalizeText(schema.schemaName) !== normalizeText(schemaName)) {
      continue
    }
    const tables = schema.tables || []
    for (let j = 0; j < tables.length; j += 1) {
      const table = tables[j]
      if (table && table.tableName && normalizeText(table.tableName) === normalizeText(tableName)) {
        return table
      }
    }
  }
  return null
}

function normalizeText(value) {
  return value == null ? '' : String(value).trim().toLowerCase()
}

function stringConfigValue(config, key) {
  if (!config || config[key] === null || config[key] === undefined) {
    return ''
  }
  return String(config[key])
}

function numberConfigValue(config, key) {
  if (!config || config[key] === null || config[key] === undefined || config[key] === '') {
    return null
  }
  const number = Number(config[key])
  return Number.isNaN(number) ? null : number
}

function parseConfig(json) {
  if (!json) {
    return {}
  }
  try {
    const parsed = JSON.parse(json)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (error) {
    return {}
  }
}

function mappingEntriesFromConfig(config) {
  const mapping = config && config.mapping && typeof config.mapping === 'object' ? config.mapping : {}
  return Object.keys(mapping).map(function (key) {
    return {
      key: key,
      value: mapping[key] == null ? '' : String(mapping[key])
    }
  })
}

function stringHasContent(value) {
  return value !== null && value !== undefined && String(value).trim().length > 0
}
</script>

<style scoped>
.transform-rule-drawer__content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.transform-rule-drawer__tip {
  margin-bottom: 2px;
}

.transform-rule-drawer__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 16px;
}

.transform-rule-drawer__panel {
  min-width: 0;
  padding: 16px;
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.transform-rule-drawer__section-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.transform-rule-drawer__section-header h3 {
  margin: 0;
  font-size: 16px;
}

.transform-rule-drawer__section-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.transform-rule-drawer__type-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.transform-rule-drawer__type-title {
  font-weight: 600;
}

.transform-rule-drawer__type-hint {
  color: #64748b;
  font-size: 12px;
}

.transform-rule-drawer__table-shell {
  overflow: hidden;
  border-radius: 12px;
}

.transform-rule-drawer__test-form {
  margin-top: 8px;
}

.transform-rule-drawer__test-result {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.transform-rule-drawer__result-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.transform-rule-drawer__result-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.transform-rule-drawer__result-item span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.transform-rule-drawer__result-item strong {
  display: block;
  margin-top: 4px;
  word-break: break-all;
}

.transform-rule-drawer__result-note {
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 13px;
}

.transform-rule-drawer__empty {
  padding: 24px 12px;
  color: #64748b;
  text-align: center;
  background: #f8fafc;
  border-radius: 12px;
}

.transform-rule-drawer__footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.transform-rule-drawer__form-hint {
  margin: 0 0 12px;
  color: #64748b;
  font-size: 12px;
}

.transform-rule-drawer__mapping-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.transform-rule-drawer__mapping-title {
  font-weight: 600;
}

.transform-rule-drawer__mapping-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.transform-rule-drawer__mapping-row {
  padding: 10px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.transform-rule-drawer__descriptions {
  background: #fff;
}

@media (max-width: 1200px) {
  .transform-rule-drawer__grid {
    grid-template-columns: 1fr;
  }
}
</style>
