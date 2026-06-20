<template>
  <div class="welcome-page">
    <div class="welcome-page__backdrop welcome-page__backdrop--one"></div>
    <div class="welcome-page__backdrop welcome-page__backdrop--two"></div>

    <div class="welcome-shell">
      <div class="welcome-shell__hero glass-panel">
        <div class="welcome-hero__topline">
          <div class="welcome-brand">
            <div class="welcome-brand__mark">DB</div>
            <div>
              <div class="welcome-brand__name">DB Sync Studio</div>
              <div class="welcome-brand__subtitle">跨平台本地数据库同步工作台</div>
            </div>
          </div>
          <el-tag type="success" effect="dark">首次启动</el-tag>
        </div>

        <div class="welcome-hero__eyebrow">WELCOME / FIRST RUN</div>
        <h1 class="welcome-hero__title">先带你走一遍最重要的 6 个步骤</h1>
        <p class="welcome-hero__desc">
          这不是一套复杂的向导，而是一个轻量的起点。
          你只要按顺序完成下面的动作，就能把本地数据库同步流程真正跑起来。
        </p>

        <div class="welcome-hero__actions">
          <el-button type="primary" round size="large" @click="startUsing">开始使用</el-button>
          <el-button round size="large" @click="skipGuide">跳过引导</el-button>
          <el-button text @click="openHelp">先看看快速开始</el-button>
        </div>

        <div class="welcome-hero__stats">
          <div class="welcome-stat">
            <div class="welcome-stat__value">3</div>
            <div class="welcome-stat__label">种数据库类型</div>
          </div>
          <div class="welcome-stat">
            <div class="welcome-stat__value">1</div>
            <div class="welcome-stat__label">套本地工作台</div>
          </div>
          <div class="welcome-stat">
            <div class="welcome-stat__value">0</div>
            <div class="welcome-stat__label">额外安装负担</div>
          </div>
        </div>

        <div class="welcome-hero__note">
          如果你只是想先看看界面，也可以直接跳过。后面在设置里还能随时把引导重新打开。
        </div>
      </div>

      <div class="welcome-shell__side">
        <div class="welcome-card glass-panel">
          <div class="welcome-card__title">你接下来会做什么</div>
          <div class="welcome-card__subtitle">跟着这条路径走，第一次上手会更顺滑。</div>
          <div class="welcome-flow">
            <div v-for="item in steps" :key="item.title" class="welcome-flow__item">
              <div class="welcome-flow__rail">
                <div class="welcome-flow__dot">{{ item.index }}</div>
                <div v-if="item.index !== '6'" class="welcome-flow__line"></div>
              </div>
              <div class="welcome-flow__body">
                <div class="welcome-flow__name">{{ item.title }}</div>
                <div class="welcome-flow__hint">{{ item.hint }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="welcome-card welcome-card--quiet glass-panel">
          <div class="welcome-card__title">小提示</div>
          <div class="welcome-card__subtitle">这个产品的思路很简单：先连通，再扫描，再同步。</div>
          <ul class="welcome-tips">
            <li>先新建数据源，再做后面的扫描和任务。</li>
            <li>欢迎页只出现一次，后续可以在设置里重新打开。</li>
            <li>如果你现在只是想先看看界面，可以直接跳过。</li>
          </ul>
          <div class="welcome-hero__chips">
            <el-tag effect="plain" round>本地可用</el-tag>
            <el-tag effect="plain" round>无需联网</el-tag>
            <el-tag effect="plain" round>支持恢复入口</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { markFirstLaunchCompleted } from '../services/onboardingState'

const router = useRouter()

const steps = [
  { index: '1', title: '新建数据源', hint: '录入 MySQL、PostgreSQL 或达梦连接信息。' },
  { index: '2', title: '测试连接', hint: '先确认连通性，再保存到本地。' },
  { index: '3', title: '扫描表结构', hint: '读取 schema、table、columns 和主键信息。' },
  { index: '4', title: '创建同步任务', hint: '配置源表、目标表和同步模式。' },
  { index: '5', title: '执行同步', hint: '支持全量、增量、定时和手动执行。' },
  { index: '6', title: '查看日志', hint: '回看 run、表级进度、失败原因和重试过程。' }
]

async function startUsing() {
  await markFirstLaunchCompleted()
  await router.replace('/')
}

async function skipGuide() {
  await markFirstLaunchCompleted()
  await router.replace('/')
}

function openHelp() {
  router.push('/help')
}
</script>
