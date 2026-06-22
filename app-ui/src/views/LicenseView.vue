<template>
  <div class="page-section license-page standalone-page">
    <div class="page-header">
      <div>
        <h1>License 授权</h1>
        <p>本地授权状态、机器码与授权码入口统一在这里管理。</p>
      </div>
      <el-space>
        <el-button @click="reload">刷新</el-button>
        <el-button @click="copyMachineCode">复制机器码</el-button>
        <el-button type="danger" plain :loading="clearing" @click="clearCurrentLicense">清除授权</el-button>
        <el-button type="primary" :loading="saving" @click="activateCurrentLicense">保存授权</el-button>
      </el-space>
    </div>

    <div class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>授权状态</h2>
          <el-tag :type="statusTagType" effect="dark">{{ license.status || 'UNLICENSED' }}</el-tag>
        </div>
        <div class="status-stack">
          <div class="status-item" v-for="item in licenseRows" :key="item.label">
            <span class="status-item__label">{{ item.label }}</span>
            <span class="status-item__value">{{ item.value }}</span>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>授权输入</h2>
          <el-tag type="info" effect="dark">本地模式</el-tag>
        </div>
        <el-form label-width="120px">
          <el-form-item label="授权码">
            <el-input
              v-model="licenseKeyInput"
              type="password"
              show-password
              placeholder="输入本地授权码"
              autocomplete="off"
            />
            <div class="settings-page__hint">当前版本仅本地校验，后续可接入正式授权服务。</div>
          </el-form-item>
          <el-form-item label="脱敏显示">
            <el-input :model-value="license.maskedLicenseKey || '-'" disabled />
          </el-form-item>
          <el-form-item label="机器码">
            <el-input :model-value="machineCodeDisplay" disabled />
            <div class="settings-page__hint">完整机器码可通过“复制机器码”按钮复制。</div>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="panel-card glass-panel">
      <div class="section-title">
        <h2>说明</h2>
        <el-tag type="warning" effect="dark">占位功能</el-tag>
      </div>
      <p class="about-page__desc">
        当前阶段只做本地授权状态、机器码和授权码保存框架，不依赖云端服务。后续接入正式授权服务时，只需要替换本地校验逻辑和服务端签名校验实现。
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activateLicense, clearLicense, getLicenseInfo } from '../services/backend'

const saving = ref(false)
const clearing = ref(false)
const licenseKeyInput = ref('')
const license = ref({})

const machineCodeDisplay = computed(function () {
  return formatMachineCode(license.value.machineCode)
})

const statusTagType = computed(function () {
  const status = license.value && license.value.status
  if (status === 'LICENSED') {
    return 'success'
  }
  if (status === 'EXPIRED') {
    return 'danger'
  }
  if (status === 'TRIAL') {
    return 'warning'
  }
  return 'info'
})

const licenseRows = computed(function () {
  return [
    { label: '状态', value: license.value.status || '-' },
    { label: '提示', value: license.value.message || '-' },
    { label: '授权对象', value: license.value.licensedTo || '-' },
    { label: '授权码', value: license.value.maskedLicenseKey || '-' },
    { label: '机器码', value: machineCodeDisplay.value },
    { label: '生效时间', value: formatTime(license.value.issuedAt) },
    { label: '到期时间', value: formatTime(license.value.expiresAt) }
  ]
})

onMounted(function () {
  reload()
})

async function reload() {
  try {
    license.value = await getLicenseInfo()
  } catch (error) {
    ElMessage.error(error.message || '加载授权信息失败')
  }
}

async function activateCurrentLicense() {
  if (!licenseKeyInput.value) {
    ElMessage.warning('请输入授权码')
    return
  }
  saving.value = true
  try {
    await ElMessageBox.confirm('保存授权后会覆盖当前本地授权信息，是否继续？', '确认保存授权', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消'
    })
    license.value = await activateLicense({
      licenseKey: licenseKeyInput.value
    })
    licenseKeyInput.value = ''
    ElMessage.success('授权已保存')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '保存授权失败')
    }
  } finally {
    saving.value = false
  }
}

async function clearCurrentLicense() {
  clearing.value = true
  try {
    await ElMessageBox.confirm('清除后将回到未授权或试用状态，是否继续？', '确认清除授权', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消'
    })
    license.value = await clearLicense()
    licenseKeyInput.value = ''
    ElMessage.success('授权已清除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '清除授权失败')
    }
  } finally {
    clearing.value = false
  }
}

async function copyMachineCode() {
  try {
    const code = license.value && license.value.machineCode ? license.value.machineCode : ''
    if (!code) {
      ElMessage.warning('机器码暂不可用')
      return
    }
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(code)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = code
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('机器码已复制')
  } catch (error) {
    ElMessage.error(error.message || '复制机器码失败')
  }
}

function formatTime(value) {
  if (!value) {
    return '-'
  }
  const time = Number(value)
  if (!Number.isFinite(time)) {
    return '-'
  }
  return new Date(time).toLocaleString()
}

function formatMachineCode(value) {
  const code = String(value || '').trim()
  if (!code) {
    return '-'
  }
  if (code.length <= 16) {
    return code
  }
  return code.slice(0, 8) + '…' + code.slice(-8)
}
</script>
