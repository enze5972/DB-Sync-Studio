<template>
  <div class="about-page standalone-page">
    <div class="about-page__hero glass-panel">
      <div class="about-page__hero-brand">
        <div class="about-page__logo" aria-hidden="true">
          <span class="about-page__logo-ring"></span>
          <span class="about-page__logo-core">DB</span>
        </div>
        <div class="about-page__hero-copy">
          <div class="about-page__eyebrow">产品信息中心</div>
          <h1>DB Sync Studio</h1>
          <p class="about-page__lead">
            本地化跨平台数据库同步工作台，用于管理数据源、配置同步任务、扫描表结构、执行全量与增量同步，并在本地查看日志与断点状态。
          </p>
          <div class="about-page__badges">
            <el-tag v-for="badge in badges" :key="badge" effect="dark" round>{{ badge }}</el-tag>
          </div>
          <div class="about-page__hero-actions">
            <el-button @click="goHome">回到首页</el-button>
            <el-button type="primary" @click="copyDiagnostics">复制诊断信息</el-button>
            <el-button @click="checkUpdate">检查更新</el-button>
            <el-button @click="openLogsDirectory">打开日志目录</el-button>
            <el-button @click="openDocs">查看文档</el-button>
          </div>
        </div>
      </div>

      <div class="about-page__hero-status glass-panel">
        <div class="about-page__hero-status-label">当前版本</div>
        <div class="about-page__hero-status-value">{{ displayVersion }}</div>
        <div class="about-page__hero-status-meta">运行模式：{{ runtimeModeLabel }}</div>
        <div class="about-page__hero-status-meta">当前状态：{{ backendConnected ? '运行正常' : '同步核心暂未连接' }}</div>
        <div class="about-page__hero-status-meta">最近启动：{{ lastStartupText }}</div>
      </div>
    </div>

    <div class="about-page__section">
      <div class="section-title">
        <h2>运行状态概览</h2>
        <el-tag type="info" effect="dark">运行状态</el-tag>
      </div>
      <div class="about-page__status-grid">
        <div v-for="item in statusCards" :key="item.title" class="about-page__status-card panel-card glass-panel">
          <div class="about-page__status-card-title">{{ item.title }}</div>
          <div class="about-page__status-card-state" :class="'about-page__status-card-state--' + item.stateType">
            {{ item.stateText }}
          </div>
          <div class="about-page__status-card-desc">{{ item.description }}</div>
        </div>
      </div>
    </div>

    <div class="about-page__section">
      <div class="section-title">
        <h2>产品能力</h2>
        <el-tag type="success" effect="dark">产品能力</el-tag>
      </div>
      <div class="about-page__capability-grid">
        <div v-for="item in capabilities" :key="item.title" class="about-page__capability-card panel-card glass-panel">
          <div class="about-page__capability-title">{{ item.title }}</div>
          <div class="about-page__capability-desc">{{ item.desc }}</div>
        </div>
      </div>
    </div>

    <div class="about-page__section">
      <div class="section-title">
        <h2>版本与构建信息</h2>
        <el-tag type="warning" effect="dark">构建信息</el-tag>
      </div>
      <div class="about-page__info-table panel-card glass-panel">
        <div v-for="item in infoRows" :key="item.label" class="about-page__info-row">
          <div class="about-page__info-label">{{ item.label }}</div>
          <div class="about-page__info-value" :title="item.value">{{ item.value }}</div>
          <el-button text size="small" @click="copyText(item.value)">复制</el-button>
        </div>
      </div>
    </div>

    <div class="about-page__section">
      <div class="section-title">
        <h2>帮助与支持</h2>
        <el-tag type="primary" effect="dark">支持</el-tag>
      </div>
      <div class="about-page__support-grid">
        <div v-for="item in supportCards" :key="item.title" class="about-page__support-card panel-card glass-panel">
          <div class="about-page__support-title">{{ item.title }}</div>
          <div class="about-page__support-desc">{{ item.desc }}</div>
          <div class="about-page__support-actions">
            <el-button v-for="action in item.actions" :key="action.label" :type="action.type || 'default'" :plain="!!action.plain" :disabled="action.disabled" @click="action.onClick">
              {{ action.label }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="about-page__footer panel-card glass-panel">
      <div class="about-page__footer-brand">DB Sync Studio</div>
      <div class="about-page__footer-subtitle">本地化跨平台数据库同步工作台</div>
      <div class="about-page__footer-copy">
        版权所有 © {{ currentYear }} DB Sync Studio。保留所有权利。
      </div>
      <div class="about-page__footer-note">为本地数据库同步、迁移和数据维护场景设计。</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAppSettings, getDiagnosticsStatus } from '../services/backend'

const router = useRouter()
const productName = ref('DB Sync Studio')
const appVersion = ref('当前版本未提供')
const frontendVersion = ref('当前版本未提供')
const backendVersion = ref('暂未提供')
const javaVersion = ref('内置 Java 17 Runtime')
const tauriVersion = ref('暂未提供')
const buildTime = ref('当前构建未写入')
const runtimeMode = ref('本地桌面版')
const osName = ref('暂未暴露')
const applicationDirectory = ref('暂未获取')
const logsDirectory = ref('暂未获取')
const databaseFilePath = ref('暂未获取')
const backendConnected = ref(false)
const backendPort = ref('暂未暴露')
const lastStartupText = ref('当前会话')
const diagnostics = ref({})
const checkingUpdate = ref(false)

const badges = [
  '本地优先',
  '跨平台',
  'Java 17 Runtime',
  'SQLite 本地存储',
  'MySQL / DM / PostgreSQL'
]

const currentYear = new Date().getFullYear()

const displayVersion = computed(function () {
  return appVersion.value || '当前版本未提供'
})

const runtimeModeLabel = computed(function () {
  return runtimeMode.value || '本地桌面版'
})

const infoRows = computed(function () {
  return [
    { label: '产品名称', value: productName.value },
    { label: '应用版本', value: appVersion.value },
    { label: '前端版本', value: frontendVersion.value },
    { label: '后端版本', value: backendVersion.value },
    { label: 'Java 版本', value: javaVersion.value },
    { label: 'Tauri 版本', value: tauriVersion.value },
    { label: '构建时间', value: buildTime.value },
    { label: '运行模式', value: runtimeModeLabel.value },
    { label: '操作系统', value: osName.value },
    { label: '后端端口', value: backendPort.value },
    { label: '数据目录', value: applicationDirectory.value },
    { label: '日志目录', value: logsDirectory.value },
    { label: '数据库文件', value: databaseFilePath.value }
  ]
})

const statusCards = computed(function () {
  return [
    {
      title: '前端服务',
      stateText: '已连接',
      stateType: 'success',
      description: 'Vue 3 / Tauri / ' + window.location.origin
    },
    {
      title: '同步核心',
      stateText: backendConnected.value ? '已连接' : '未连接',
      stateType: backendConnected.value ? 'success' : 'danger',
      description: 'Java 17 / Core Service / ' + (backendPort.value || '待后端提供接口')
    },
    {
      title: '本地数据',
      stateText: diagnostics.value.databaseFilePath ? '可用' : '不可用',
      stateType: diagnostics.value.databaseFilePath ? 'success' : 'warning',
      description: 'SQLite / ' + (applicationDirectory.value || '待后端提供接口')
    },
    {
      title: '平台环境',
      stateText: osName.value || '当前版本未提供',
      stateType: 'info',
      description: '系统版本 / 架构 / 用户目录'
    }
  ]
})

const capabilities = [
  {
    title: '数据源管理',
    desc: '统一管理 MySQL、达梦 DM、PostgreSQL 等数据源连接信息，支持连接测试和基础配置维护。'
  },
  {
    title: '表结构扫描',
    desc: '快速读取源库与目标库表结构，为字段映射和同步任务提供基础元数据。'
  },
  {
    title: '字段映射',
    desc: '在同步任务中维护源表、目标表、字段对应关系，降低手工配置成本。'
  },
  {
    title: '全量同步',
    desc: '适合初始化数据、历史数据迁移和一次性数据复制。'
  },
  {
    title: '增量同步',
    desc: '适合持续数据同步、周期同步和后续变更同步。'
  },
  {
    title: '同步日志',
    desc: '查看同步过程、执行结果、错误信息和排查线索。'
  },
  {
    title: '断点与恢复',
    desc: '为长时间任务和异常中断场景提供恢复基础。'
  },
  {
    title: '本地化运行',
    desc: '配置、日志、任务数据优先保存在本地，适合内网和私有环境使用。'
  }
]

const supportCards = computed(function () {
  return [
    {
      title: '使用文档',
      desc: '查看数据源配置、同步任务创建、字段映射、任务执行等使用说明。',
      actions: [
        { label: '查看文档', type: 'primary', onClick: openDocs }
      ]
    },
    {
      title: '问题反馈',
      desc: '遇到连接失败、同步失败、字段映射异常等问题时，可以复制诊断信息后提交反馈。',
      actions: [
        { label: '复制诊断信息', type: 'primary', onClick: copyDiagnostics }
      ]
    },
    {
      title: '故障排查',
      desc: '查看前后端连接状态、本地数据目录、日志目录和运行环境。',
      actions: [
        { label: '打开日志目录', onClick: openLogsDirectory },
        { label: '查看诊断信息', plain: true, onClick: copyDiagnostics }
      ]
    }
  ]
})

onMounted(function () {
  reload()
})

async function reload() {
  try {
    const response = await getAppSettings()
    const buildInfo = response && response.buildInfo ? response.buildInfo : {}
    const diag = await getDiagnosticsStatus()
    diagnostics.value = diag || {}
    productName.value = buildInfo.productName || 'DB Sync Studio'
    appVersion.value = buildInfo.appVersion || '当前版本未提供'
    frontendVersion.value = buildInfo.frontendVersion || '当前版本未提供'
    backendVersion.value = '暂未提供'
    javaVersion.value = buildInfo.javaCoreVersion || '内置 Java 17 Runtime'
    tauriVersion.value = buildInfo.tauriVersion || '暂未提供'
    buildTime.value = buildInfo.buildTime || '当前构建未写入'
    runtimeMode.value = window.location.port === '5173' ? '本地开发模式' : '本地桌面版'
    osName.value = await readOsName()
    applicationDirectory.value = response && response.applicationDirectory ? response.applicationDirectory : '待后端提供接口'
    logsDirectory.value = response && response.logsDirectory ? response.logsDirectory : '待后端提供接口'
    databaseFilePath.value = response && response.databaseFilePath ? response.databaseFilePath : '待后端提供接口'
    backendPort.value = '暂未暴露'
    backendConnected.value = true
    lastStartupText.value = diag && diag.generatedAt ? new Date(diag.generatedAt).toLocaleString() : '当前会话'
  } catch (error) {
    backendConnected.value = false
    ElMessage.error(error.message || '加载关于信息失败')
  }
}

async function readOsName() {
  try {
    if (navigator && navigator.userAgentData && navigator.userAgentData.platform) {
      return navigator.userAgentData.platform
    }
    if (navigator && navigator.platform) {
      return navigator.platform
    }
  } catch (error) {
    // ignore
  }
  return '暂未暴露'
}

async function copyDiagnostics() {
  try {
    const text = buildDiagnosticsText()
    await copyToClipboard(text)
    ElMessage.success('诊断信息已复制，可粘贴给技术支持。')
  } catch (error) {
    ElMessage.error('复制失败，请手动选择诊断信息。')
  }
}

async function copyText(value) {
  try {
    const text = value || ''
    if (!text || text === '-' || text.indexOf('暂未') === 0) {
      ElMessage.warning('当前信息暂不可复制')
      return
    }
    await copyToClipboard(text)
    ElMessage.success('已复制')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

async function copyToClipboard(text) {
  if (navigator && navigator.clipboard && navigator.clipboard.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  const ok = document.execCommand('copy')
  document.body.removeChild(textarea)
  if (!ok) {
    throw new Error('copy failed')
  }
}

function buildDiagnosticsText() {
  const lines = [
    '产品名称: ' + productName.value,
    '应用版本: ' + appVersion.value,
    '前端版本: ' + frontendVersion.value,
    '后端版本: ' + backendVersion.value,
    '运行模式: ' + runtimeModeLabel.value,
    '当前页面地址: ' + window.location.href,
    '当前时间: ' + new Date().toLocaleString(),
    '浏览器 User Agent: ' + navigator.userAgent,
    '操作系统: ' + osName.value,
    '后端连接状态: ' + (backendConnected.value ? '已连接' : '未连接'),
    '数据目录: ' + applicationDirectory.value,
    '日志目录: ' + logsDirectory.value
  ]
  return lines.join('\n')
}

async function checkUpdate() {
  if (checkingUpdate.value) {
    return
  }
  checkingUpdate.value = true
  ElMessage.info('当前版本暂未接入自动更新，请以后续发布包为准。')
  checkingUpdate.value = false
}

function goHome() {
  router.push('/')
}

async function openLogsDirectory() {
  if (!logsDirectory.value || logsDirectory.value.indexOf('暂未') === 0 || logsDirectory.value.indexOf('待后端') === 0) {
    ElMessage.info('当前版本暂未开放日志目录快捷入口。')
    return
  }
  try {
    const { open } = await import('@tauri-apps/api/shell')
    await open(logsDirectory.value)
  } catch (error) {
    ElMessage.info('当前版本暂未开放日志目录快捷入口。')
  }
}

function openDocs() {
  router.push('/help')
}
</script>
