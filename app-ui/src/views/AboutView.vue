<template>
  <div class="page-section about-page">
    <div class="page-header">
      <div>
        <h1>关于</h1>
        <p>统一展示产品、版本、构建和兼容性信息。</p>
      </div>
      <el-space>
        <el-button @click="goSettings">软件设置</el-button>
        <el-button @click="goLicense">License</el-button>
        <el-button type="primary" @click="reload">刷新</el-button>
      </el-space>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>{{ productName }}</h2>
          <el-tag type="success" effect="dark">{{ appVersion }}</el-tag>
        </div>
        <p class="about-page__desc">
          DB Sync Studio 是一款离线优先的桌面数据库同步工作台，面向本地同步、结构比较、任务编排和诊断回溯场景。
        </p>
        <div class="status-stack about-page__stack">
          <div class="status-item" v-for="item in versionRows" :key="item.label">
            <span class="status-item__label">{{ item.label }}</span>
            <span class="status-item__value">{{ item.value }}</span>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>支持信息</h2>
          <el-tag type="info" effect="dark">Compatibility</el-tag>
        </div>
        <div class="about-page__grid">
          <div class="about-page__card">
            <div class="about-page__label">支持平台</div>
            <div class="about-page__value">macOS / Windows / Linux</div>
          </div>
          <div class="about-page__card">
            <div class="about-page__label">支持数据库</div>
            <div class="about-page__value">MySQL / PostgreSQL / 达梦 DM</div>
          </div>
          <div class="about-page__card">
            <div class="about-page__label">官网</div>
            <div class="about-page__value">待补充</div>
          </div>
          <div class="about-page__card">
            <div class="about-page__label">更新日志</div>
            <div class="about-page__value">待补充</div>
          </div>
          <div class="about-page__card">
            <div class="about-page__label">License</div>
            <div class="about-page__value">待补充</div>
          </div>
        </div>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>说明</h2>
        <el-tag type="warning" effect="dark">Product Notes</el-tag>
      </div>
      <p class="about-page__desc">
        当前页面展示的信息都来自后端统一接口和构建配置，避免在前端多个页面里手动维护版本号。
        如果后续需要接入正式官网、发行说明或授权页，只需要替换占位入口即可。
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAppSettings } from '../services/backend'

const router = useRouter()
const productName = ref('DB Sync Studio')
const appVersion = ref('-')
const frontendVersion = ref('-')
const javaCoreVersion = ref('-')
const tauriVersion = ref('-')
const schemaVersion = ref('-')
const buildTime = ref('-')
const gitCommit = ref('-')

const versionRows = computed(function () {
  return [
    { label: '当前版本号', value: appVersion.value },
    { label: '前端版本', value: frontendVersion.value },
    { label: 'Java 核心版本', value: javaCoreVersion.value },
    { label: 'Tauri 版本', value: tauriVersion.value },
    { label: 'SQLite schema version', value: schemaVersion.value },
    { label: '构建时间', value: buildTime.value },
    { label: 'Git commit', value: gitCommit.value }
  ]
})

onMounted(function () {
  reload()
})

async function reload() {
  try {
    const response = await getAppSettings()
    const buildInfo = response && response.buildInfo ? response.buildInfo : {}
    productName.value = buildInfo.productName || productName.value
    appVersion.value = buildInfo.appVersion || '-'
    frontendVersion.value = buildInfo.frontendVersion || '-'
    javaCoreVersion.value = buildInfo.javaCoreVersion || '-'
    tauriVersion.value = buildInfo.tauriVersion || '-'
    schemaVersion.value = buildInfo.sqliteSchemaVersion == null ? '-' : String(buildInfo.sqliteSchemaVersion)
    buildTime.value = buildInfo.buildTime || '-'
    gitCommit.value = buildInfo.gitCommit || '-'
  } catch (error) {
    ElMessage.error(error.message || '加载关于信息失败')
  }
}

function goSettings() {
  router.push('/settings')
}

function goLicense() {
  router.push('/license')
}
</script>
