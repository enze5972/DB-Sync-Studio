<template>
  <div class="coming-soon standalone-page">
    <div class="coming-soon__box glass-panel">
      <el-tag type="warning" effect="dark">Coming Soon</el-tag>
      <h2 class="coming-soon__title">{{ title }}</h2>
      <p class="coming-soon__desc">
        这一页会在后续阶段接入真实功能。当前先保持桌面壳完整可打开，并统一保留导航和布局。
      </p>
      <div class="coming-soon__hint">
        目标：先让界面、数据和同步流程逐步接上，再继续扩展高级能力。
      </div>
      <div v-if="route.name === 'settings'" class="coming-soon__actions">
        <el-button type="primary" @click="restartGuide">重新打开新手引导</el-button>
        <el-button @click="openHelp">帮助文档</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetFirstLaunchCompleted } from '../services/onboardingState'

const route = useRoute()
const router = useRouter()
const title = computed(function () {
  switch (route.name) {
    case 'datasource':
      return '数据源管理'
    case 'tasks':
      return '同步任务'
    case 'mapping':
      return '字段映射'
    case 'metadata':
      return '表结构扫描'
    case 'logs':
      return '执行日志'
    case 'settings':
      return '软件设置'
    default:
      return '功能页'
  }
})

async function restartGuide() {
  await resetFirstLaunchCompleted()
  await router.push('/welcome')
}

function openHelp() {
  router.push('/help')
}
</script>
