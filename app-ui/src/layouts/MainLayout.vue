<template>
  <div class="page-shell">
    <div class="app-frame">
      <aside class="sidebar glass-panel">
        <div class="sidebar__brand">
          <div class="brand-mark">DB</div>
          <div>
            <div class="sidebar__title">DB Sync Studio</div>
            <div class="sidebar__subtitle">Desktop database sync studio</div>
          </div>
        </div>

        <el-menu
          router
          :default-active="activePath"
          class="sidebar-menu"
          background-color="transparent"
          text-color="#334155"
          active-text-color="#2563eb"
        >
          <el-menu-item-group title="开始">
            <el-menu-item index="/">首页</el-menu-item>
            <el-menu-item index="/welcome">快速开始</el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="数据准备">
            <el-menu-item index="/datasource">数据源管理</el-menu-item>
            <el-menu-item index="/metadata">表结构扫描</el-menu-item>
            <el-menu-item index="/tasks">同步任务</el-menu-item>
            <el-menu-item index="/task-wizard">创建向导</el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="数据工具">
            <el-menu-item index="/preview">数据预览</el-menu-item>
            <el-menu-item index="/sql">SQL 编辑器</el-menu-item>
            <el-menu-item index="/schema-compare">表结构比较</el-menu-item>
            <el-menu-item index="/mapping">字段映射</el-menu-item>
            <el-menu-item index="/validation">数据校验</el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="运行运维">
            <el-menu-item index="/run-monitoring">运行监控</el-menu-item>
            <el-menu-item index="/alert-settings">告警设置</el-menu-item>
            <el-menu-item index="/alert-history">告警历史</el-menu-item>
            <el-menu-item index="/execution-history">执行历史</el-menu-item>
            <el-menu-item index="/task-run">Run 详情</el-menu-item>
            <el-menu-item index="/schedule-center">调度中心</el-menu-item>
            <el-menu-item index="/logs">执行日志</el-menu-item>
          </el-menu-item-group>
          <el-menu-item-group title="系统">
            <el-menu-item index="/settings">软件设置</el-menu-item>
            <el-menu-item index="/license">License 授权</el-menu-item>
            <el-menu-item index="/about">关于</el-menu-item>
            <el-menu-item index="/help">帮助文档</el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </aside>

      <main class="content glass-panel">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activePath = computed(() => route.path)
</script>
