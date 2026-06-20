<template>
  <div class="dashboard-page">
    <section class="hero">
      <div class="hero__main">
        <div class="hero__badge-row">
          <el-tag type="info" effect="dark">DB Sync Studio</el-tag>
          <el-tag type="success" effect="dark">Desktop Ready</el-tag>
        </div>
        <h1 class="hero__title">把数据库同步变成桌面里的日常操作</h1>
        <p class="hero__desc">
          当前已经完成 Tauri + Vue3 桌面壳、SQLite 持久化、连接测试、元数据扫描、同步引擎和跨平台打包脚本。
          接下来继续把任务编排、日志体验和比较工具打磨得更顺手一些。
        </p>
        <div class="hero__actions">
          <el-button type="primary" round @click="goTo('/datasource')">新建数据源</el-button>
          <el-button round @click="goTo('/metadata')">表结构扫描</el-button>
          <el-button round @click="goTo('/tasks')">创建同步任务</el-button>
          <el-button round @click="goTo('/logs')">查看执行日志</el-button>
          <el-button round @click="goTo('/help')">快速开始</el-button>
        </div>
      </div>

      <div class="hero__aside glass-panel">
        <div class="mini-summary">
          <div class="mini-summary__label">数据源</div>
          <div class="mini-summary__value">{{ stats.datasourceCount }}</div>
          <div class="mini-summary__hint">已保存到本地 SQLite，可直接复用</div>
        </div>
        <div class="mini-summary">
          <div class="mini-summary__label">同步任务</div>
          <div class="mini-summary__value">{{ stats.taskCount }}</div>
          <div class="mini-summary__hint">支持全量、增量、定时和手动执行</div>
        </div>
        <div class="mini-summary">
          <div class="mini-summary__label">执行日志</div>
          <div class="mini-summary__value">{{ stats.logCount }}</div>
          <div class="mini-summary__hint">任务运行轨迹与错误原因可追踪</div>
        </div>
      </div>
    </section>

    <section class="stats-grid">
      <div v-for="item in summaryCards" :key="item.label" class="stat-card glass-panel">
        <div class="stat-card__label">{{ item.label }}</div>
        <div class="stat-card__value">{{ item.value }}</div>
        <div class="stat-card__hint">{{ item.hint }}</div>
      </div>
    </section>

    <section class="dashboard-panels">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>实施路线</h2>
          <el-tag type="success" effect="dark">Stage Driven</el-tag>
        </div>
        <div class="roadmap">
          <div v-for="phase in roadmap" :key="phase.title" class="roadmap-item">
            <div class="roadmap-item__phase">{{ phase.phase }}</div>
            <div class="roadmap-item__title">{{ phase.title }}</div>
            <p class="roadmap-item__desc">{{ phase.desc }}</p>
          </div>
        </div>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>产品能力</h2>
          <el-tag type="info" effect="dark">Core Modules</el-tag>
        </div>
        <div class="capability-list">
          <div v-for="item in capabilities" :key="item.title" class="capability-item">
            <div class="capability-item__title">{{ item.title }}</div>
            <div class="capability-item__desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="dashboard-panels dashboard-panels--compact">
      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>发布输出</h2>
          <el-tag type="warning" effect="dark">Phase 7</el-tag>
        </div>
        <ul class="release-list">
          <li>Windows：`.exe` / `.msi`</li>
          <li>macOS：`.app` / `.dmg`</li>
          <li>Linux：`.AppImage` / `.deb`</li>
          <li>统一输出目录：`release/&lt;platform&gt;/`</li>
        </ul>
      </div>

      <div class="panel-card glass-panel">
        <div class="section-title">
          <h2>当前状态</h2>
          <el-tag type="success" effect="dark">Healthy</el-tag>
        </div>
        <div class="status-stack">
          <div class="status-item">
            <span class="status-item__label">桌面壳</span>
            <span class="status-item__value">可直接打开</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">SQLite</span>
            <span class="status-item__value">已接入</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">同步引擎</span>
            <span class="status-item__value">可运行 Demo</span>
          </div>
          <div class="status-item">
            <span class="status-item__label">打包脚本</span>
            <span class="status-item__value">已落地</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchDashboardStats } from '../services/backend'

const router = useRouter()
const stats = ref({
  datasourceCount: 0,
  taskCount: 0,
  logCount: 0
})

const summaryCards = [
  {
    label: '数据源',
    value: 'MySQL / PostgreSQL / DM',
    hint: 'MySQL / PostgreSQL / DM'
  },
  {
    label: '同步任务',
    value: 'Full / Incremental',
    hint: '全量 / 增量 / 定时 / 手动'
  },
  {
    label: '字段映射',
    value: 'Ready',
    hint: '忽略 / 默认值 / 类型转换'
  },
  {
    label: '执行日志',
    value: 'Ready',
    hint: '可追踪任务状态与重试'
  }
]

onMounted(function () {
  loadStats()
})

function goTo(path) {
  router.push(path)
}

async function loadStats() {
  try {
    const response = await fetchDashboardStats()
    stats.value = response
  } catch (error) {
    ElMessage.error(error.message || '加载首页统计失败')
  }
}

const roadmap = [
  {
    phase: 'Phase 1',
    title: 'Tauri + Vue3 桌面壳',
    desc: '已完成首页壳层、导航和桌面启动入口。'
  },
  {
    phase: 'Phase 2',
    title: 'SQLite 本地存储',
    desc: '数据源配置、任务、映射和日志全部预留。'
  },
  {
    phase: 'Phase 3-5',
    title: '连接、扫描、全量同步',
    desc: '连接测试、元数据扫描和全量同步主链路已完成。'
  },
  {
    phase: 'Phase 6',
    title: '增量同步与日志',
    desc: '断点续传、失败重试和日志记录机制已接入。'
  },
  {
    phase: 'Phase 7',
    title: '跨平台打包',
    desc: '脚本、图标和发布产物目录已整理完毕。'
  },
  {
    phase: 'Next',
    title: '体验打磨',
    desc: '继续补齐交互、状态反馈和任务页面细节。'
  }
]

const capabilities = [
  {
    title: '数据源管理',
    desc: '支持 MySQL、PostgreSQL 和达梦 DM 的连接与保存。'
  },
  {
    title: '元数据扫描',
    desc: '读取 schema、table、columns 和 primary key 信息。'
  },
  {
    title: '同步编排',
    desc: '支持全量同步、增量同步、定时同步和手动执行。'
  },
  {
    title: '日志与恢复',
    desc: '记录执行日志、断点状态和失败重试过程。'
  }
]
</script>
