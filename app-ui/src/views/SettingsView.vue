<template>
  <div class="page-section settings-page">
    <div class="page-header">
      <div>
        <h1>软件设置</h1>
        <p>统一管理本地设置、版本信息和诊断入口。</p>
      </div>
      <el-space>
        <el-button @click="reload">刷新</el-button>
        <el-button :loading="checkingUpdate" @click="checkUpdate">检查更新</el-button>
        <el-button type="primary" :loading="saving" @click="saveSettings">保存设置</el-button>
      </el-space>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>版本与路径</h2>
          <el-tag type="success" effect="dark">{{ buildInfo.appVersion || '-' }}</el-tag>
        </div>
        <div class="status-stack">
          <div class="status-item" v-for="item in infoRows" :key="item.label">
            <span class="status-item__label">{{ item.label }}</span>
            <span class="status-item__value">{{ item.value }}</span>
          </div>
        </div>
        <div class="settings-actions">
          <el-button @click="openLogsDirectory">打开日志目录</el-button>
          <el-button @click="exportDiagnostics">导出诊断包</el-button>
          <el-button @click="reopenGuide">重新打开新手引导</el-button>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>默认行为</h2>
          <el-tag type="info" effect="dark">默认偏好</el-tag>
        </div>
        <el-form label-width="180px">
          <el-form-item label="日志保留天数">
            <el-input-number v-model="form.logRetentionDays" :min="1" :max="3650" />
          </el-form-item>
          <el-form-item label="监控指标保留天数">
            <el-input-number v-model="form.monitoringRetentionDays" :min="1" :max="3650" />
          </el-form-item>
          <el-form-item label="默认分页大小">
            <el-input-number v-model="form.defaultPageSize" :min="10" :max="1000" />
          </el-form-item>
          <el-form-item label="默认同步批量大小">
            <el-input-number v-model="form.defaultSyncBatchSize" :min="10" :max="10000" />
          </el-form-item>
          <el-form-item label="默认最大并发数">
            <el-input-number v-model="form.defaultMaxConcurrency" :min="1" :max="64" />
          </el-form-item>
          <el-form-item label="更新源地址">
            <el-input v-model="form.updateSourceUrl" placeholder="https://example.com/db-sync-studio-update.json" clearable />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>安全与启动</h2>
        <el-tag type="warning" effect="dark">敏感设置</el-tag>
      </div>
      <el-form label-width="240px">
        <el-form-item label="允许危险 SQL">
          <el-switch v-model="form.allowDangerousSql" />
          <span class="settings-page__hint">默认关闭，开启前会要求确认。</span>
        </el-form-item>
        <el-form-item label="启动时恢复调度任务">
          <el-switch v-model="form.restartScheduledTasksOnStartup" />
        </el-form-item>
        <el-form-item label="启动时自动检查更新">
          <el-switch v-model="form.autoCheckUpdatesOnStartup" />
        </el-form-item>
        <el-form-item label="License 页面">
          <el-button @click="goLicense">打开授权管理</el-button>
        </el-form-item>
        <el-form-item label="显示新手引导">
          <el-switch v-model="form.onboardingGuideEnabled" />
          <span class="settings-page__hint">关闭后仍可手动重新打开。</span>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>诊断预览</h2>
        <el-tag type="info" effect="dark">诊断信息</el-tag>
      </div>
      <div class="status-stack">
        <div class="status-item" v-for="item in diagnosticsRows" :key="item.label">
          <span class="status-item__label">{{ item.label }}</span>
          <span class="status-item__value">{{ item.value }}</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="updateDialogVisible" title="更新检查结果" width="720px">
      <div class="status-stack">
        <div class="status-item" v-for="item in updateRows" :key="item.label">
          <span class="status-item__label">{{ item.label }}</span>
          <span class="status-item__value">{{ item.value }}</span>
        </div>
      </div>
      <p class="settings-page__hint" style="margin-top: 12px;">{{ updateResult.message || '检查完成' }}</p>
      <template #footer>
        <el-space>
          <el-button @click="updateDialogVisible = false">关闭</el-button>
          <el-button v-if="updateResult.downloadUrl" type="primary" plain @click="openUpdateUrl">打开下载页</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAppSettings, saveAppSettings, getDiagnosticsStatus, checkForUpdate } from '../services/backend'
import { resetFirstLaunchCompleted } from '../services/onboardingState'

const router = useRouter()
const saving = ref(false)
const checkingUpdate = ref(false)
const updateDialogVisible = ref(false)
const updateResult = reactive({})
const buildInfo = reactive({
  appVersion: '',
  frontendVersion: '',
  javaCoreVersion: '',
  tauriVersion: '',
  sqliteSchemaVersion: '',
  buildTime: '',
  gitCommit: ''
})
const diagnostics = ref({})
const form = reactive({
  logRetentionDays: 30,
  monitoringRetentionDays: 30,
  defaultPageSize: 100,
  defaultSyncBatchSize: 500,
  defaultMaxConcurrency: 4,
  updateSourceUrl: '',
  allowDangerousSql: false,
  restartScheduledTasksOnStartup: true,
  autoCheckUpdatesOnStartup: false,
  onboardingGuideEnabled: true
})

const infoRows = computed(function () {
  return [
    { label: '应用版本号', value: buildInfo.appVersion || '-' },
    { label: '前端版本', value: buildInfo.frontendVersion || '-' },
    { label: 'Java 核心版本', value: buildInfo.javaCoreVersion || '-' },
    { label: 'Tauri 版本', value: buildInfo.tauriVersion || '-' },
    { label: 'SQLite 结构版本', value: buildInfo.sqliteSchemaVersion || '-' },
    { label: '构建时间', value: buildInfo.buildTime || '-' },
    { label: 'Git 提交', value: buildInfo.gitCommit || '-' },
    { label: '当前数据目录', value: diagnostics.value.applicationDirectory || '-' },
    { label: '当前日志目录', value: diagnostics.value.logsDirectory || '-' }
  ]
})

const updateRows = computed(function () {
  return [
    { label: '当前版本', value: updateResult.currentVersion || '-' },
    { label: '最新版本', value: updateResult.latestVersion || '-' },
    { label: '发布时间', value: updateResult.releasedAt || '-' },
    { label: '是否最新', value: updateResult.latest ? '是' : '否' },
    { label: '更新地址', value: updateResult.downloadUrl || '-' },
    { label: '更新说明', value: updateResult.releaseNotes || '-' }
  ]
})

const diagnosticsRows = computed(function () {
  return [
    { label: '数据库文件', value: diagnostics.value.databaseFilePath || '-' },
    { label: 'SQLite user_version', value: diagnostics.value.databaseUserVersion == null ? '-' : String(diagnostics.value.databaseUserVersion) },
    { label: '迁移记录数', value: diagnostics.value.migrationEntryCount == null ? '-' : String(diagnostics.value.migrationEntryCount) },
    { label: 'Schema version', value: diagnostics.value.schemaVersion == null ? '-' : String(diagnostics.value.schemaVersion) }
  ]
})

onMounted(function () {
  reload()
})

async function reload() {
  try {
    const response = await getAppSettings()
    const settings = response && response.settings ? response.settings : {}
    const build = response && response.buildInfo ? response.buildInfo : {}
    const diag = await getDiagnosticsStatus()
    buildInfo.appVersion = build.appVersion || ''
    buildInfo.frontendVersion = build.frontendVersion || ''
    buildInfo.javaCoreVersion = build.javaCoreVersion || ''
    buildInfo.tauriVersion = build.tauriVersion || ''
    buildInfo.sqliteSchemaVersion = build.sqliteSchemaVersion == null ? '' : String(build.sqliteSchemaVersion)
    buildInfo.buildTime = build.buildTime || ''
    buildInfo.gitCommit = build.gitCommit || ''
    diagnostics.value = diag || {}
    form.logRetentionDays = settings.logRetentionDays || 30
    form.monitoringRetentionDays = settings.monitoringRetentionDays || 30
    form.defaultPageSize = settings.defaultPageSize || 100
    form.defaultSyncBatchSize = settings.defaultSyncBatchSize || 500
    form.defaultMaxConcurrency = settings.defaultMaxConcurrency || 4
    form.updateSourceUrl = settings.updateSourceUrl || ''
    form.allowDangerousSql = !!settings.allowDangerousSql
    form.restartScheduledTasksOnStartup = settings.restartScheduledTasksOnStartup !== false
    form.autoCheckUpdatesOnStartup = !!settings.autoCheckUpdatesOnStartup
    form.onboardingGuideEnabled = settings.onboardingGuideEnabled !== false
  } catch (error) {
    ElMessage.error(error.message || '加载设置失败')
  }
}

async function saveSettings() {
  saving.value = true
  try {
    if (form.allowDangerousSql) {
      await ElMessageBox.confirm('开启危险 SQL 后，删除或结构变更语句会允许执行。请确认你了解风险。', '确认开启危险 SQL', {
        type: 'warning',
        confirmButtonText: '继续开启',
        cancelButtonText: '取消'
      })
    }
    await saveAppSettings(form)
    ElMessage.success('设置已保存')
    await reload()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '保存设置失败')
    }
  } finally {
    saving.value = false
  }
}

async function checkUpdate() {
  checkingUpdate.value = true
  try {
    const result = await checkForUpdate({
      force: true
    })
    Object.assign(updateResult, result || {})
    updateDialogVisible.value = true
    ElMessage.success((result && result.message) || '检查完成')
  } catch (error) {
    ElMessage.error(error.message || '检查更新失败')
  } finally {
    checkingUpdate.value = false
  }
}

function openUpdateUrl() {
  if (!updateResult.downloadUrl) {
    return
  }
  window.open(updateResult.downloadUrl, '_blank')
}

async function openLogsDirectory() {
  try {
    const response = await getAppSettings()
    const logsDir = response && response.logsDirectory ? response.logsDirectory : ''
    if (!logsDir) {
      ElMessage.warning('日志目录暂不可用')
      return
    }
    const { open } = await import('@tauri-apps/api/shell')
    await open(logsDir)
  } catch (error) {
    ElMessage.error(error.message || '打开日志目录失败')
  }
}

async function exportDiagnostics() {
  try {
    const response = await getDiagnosticsStatus()
    const payload = JSON.stringify(response || {}, null, 2)
    const blob = new Blob([payload], { type: 'application/json;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'db-sync-studio-diagnostics.json'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('诊断包已导出')
  } catch (error) {
    ElMessage.error(error.message || '导出诊断包失败')
  }
}

async function reopenGuide() {
  await resetFirstLaunchCompleted()
  await router.push('/welcome')
}

function goLicense() {
  router.push('/license')
}
</script>
