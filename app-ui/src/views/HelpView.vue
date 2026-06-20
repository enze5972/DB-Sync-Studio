<template>
  <div class="help-center">
    <div class="help-center__hero glass-panel">
      <div class="help-center__hero-main">
        <div class="help-center__eyebrow">Offline Help Center</div>
        <h1>快速开始</h1>
        <p>
          从数据源到同步任务，把最核心的操作路径整理成一张清晰的地图。
          不依赖外部网络，随时都可以打开。
        </p>
        <div class="help-center__actions">
          <el-button type="primary" round @click="openDatasource">新增数据源</el-button>
          <el-button round @click="goHome">返回首页</el-button>
        </div>
      </div>

      <div class="help-center__hero-side">
        <div class="help-metric">
          <div class="help-metric__value">9</div>
          <div class="help-metric__label">个常用操作主题</div>
        </div>
        <div class="help-metric">
          <div class="help-metric__value">100%</div>
          <div class="help-metric__label">本地可用</div>
        </div>
      </div>
    </div>

    <div class="help-center__grid">
      <div class="help-card glass-panel help-card--primary">
        <div class="help-card__header">
          <div>
            <div class="help-card__eyebrow">Start here</div>
            <h2>快速开始</h2>
          </div>
          <el-tag type="success" effect="dark">入门</el-tag>
        </div>
        <ol class="help-list help-list--compact help-list--numbered">
          <li v-for="item in quickStart" :key="item">{{ item }}</li>
        </ol>
        <div class="help-card__footer">
          目标很明确: 先连通数据库，再完成扫描、映射和同步。
        </div>
      </div>

      <div class="help-card glass-panel">
        <div class="help-card__header">
          <div>
            <div class="help-card__eyebrow">Mapping</div>
            <h2>字段映射</h2>
          </div>
          <el-tag type="warning" effect="dark">进阶</el-tag>
        </div>
        <ol class="help-list help-list--compact help-list--numbered">
          <li v-for="item in mappingHelp" :key="item">{{ item }}</li>
        </ol>
        <div class="help-card__footer">
          推荐先用自动推荐结果，再做少量人工修正。
        </div>
      </div>

      <div class="help-card glass-panel">
        <div class="help-card__header">
          <div>
            <div class="help-card__eyebrow">Alerts</div>
            <h2>告警配置</h2>
          </div>
          <el-tag type="info" effect="dark">监控</el-tag>
        </div>
        <ol class="help-list help-list--compact help-list--numbered">
          <li v-for="item in alertHelp" :key="item">{{ item }}</li>
        </ol>
        <div class="help-card__footer">
          告警和同步主流程解耦，适合先低成本接入。
        </div>
      </div>
    </div>

    <div class="help-center__faq glass-panel">
      <div class="help-card__header">
        <div>
          <div class="help-card__eyebrow">FAQ</div>
          <h2>常见问题</h2>
        </div>
        <el-tag type="info" effect="dark">本地文档</el-tag>
      </div>
      <div class="help-faq help-faq--grid">
        <div v-for="item in faq" :key="item.q" class="help-faq__item">
          <div class="help-faq__q">{{ item.q }}</div>
          <div class="help-faq__a">{{ item.a }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const quickStart = [
  '先去“数据源管理”新建一个连接。',
  '点“测试连接”确认数据库可访问。',
  '进入“表结构扫描”读取源表和目标表。',
  '在“同步任务”里创建任务并绑定表。',
  '执行同步后去“执行日志”和“执行历史”回看结果。'
]

const mappingHelp = [
  '先选择同步任务。',
  '查看智能推荐。',
  '必要时手动修改目标字段或忽略字段。',
  '保存后同步引擎会按映射执行。'
]

const alertHelp = [
  '在“告警设置”里创建规则和渠道。',
  '支持 SMTP 和 Webhook。',
  '告警历史和运行监控页可以回看通知结果。'
]

const faq = [
  {
    q: '为什么第一次打开会先看到欢迎页？',
    a: '这是首启引导，用来告诉你这套桌面工作台的基本流程。'
  },
  {
    q: '跳过引导后还能再看吗？',
    a: '可以，去“软件设置”重新打开新手引导。'
  },
  {
    q: '帮助内容需要联网吗？',
    a: '不需要，当前全部是本地页面。'
  }
]

function goHome() {
  router.push('/')
}

function openDatasource() {
  router.push('/datasource')
}
</script>
