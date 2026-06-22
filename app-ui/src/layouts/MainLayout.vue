<template>
  <div class="page-shell">
    <div class="app-frame" :style="frameStyle">
      <aside class="sidebar glass-panel" :class="{ 'is-collapsed': isCollapsed }">
        <div class="sidebar__brand">
          <div class="sidebar__brand-mark" aria-hidden="true">DB</div>
          <div v-if="!isCollapsed" class="sidebar__brand-copy">
            <div class="sidebar__title">DB Sync Studio</div>
            <div class="sidebar__subtitle">Desktop database sync studio</div>
          </div>
          <el-button
            class="sidebar__collapse-toggle"
            text
            :aria-label="isCollapsed ? '展开侧边栏' : '折叠侧边栏'"
            @click="toggleSidebar"
          >
            <el-icon>
              <component :is="isCollapsed ? Expand : Fold" />
            </el-icon>
          </el-button>
        </div>

        <div class="sidebar__menu-scroll">
          <el-menu
            ref="menuRef"
            router
            unique-opened
            :collapse="isCollapsed"
            :default-active="activeMenuPath"
            class="sidebar-menu"
            background-color="transparent"
            text-color="#334155"
            active-text-color="#2563eb"
          >
            <el-sub-menu v-for="group in menuGroups" :key="group.key" :index="group.key">
              <template #title>
                <el-icon class="sidebar-menu__icon">
                  <component :is="group.icon" />
                </el-icon>
                <span class="sidebar-menu__label sidebar-menu__label--group">{{ group.title }}</span>
              </template>
              <el-menu-item v-for="item in group.children" :key="item.key" :index="item.path">
                <el-icon class="sidebar-menu__icon">
                  <component :is="item.icon" />
                </el-icon>
                <span class="sidebar-menu__label">{{ item.title }}</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </div>
      </aside>

      <main class="content glass-panel">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Expand, Fold } from '@element-plus/icons-vue'
import { menuActivePathByRoutePath, menuGroupKeyByRoutePath, menuGroups } from '../config/menu'

const STORAGE_KEY = 'db-sync-studio.sidebar-collapsed'

const route = useRoute()
const menuRef = ref(null)
const isCollapsed = ref(readCollapsedState())

const activeMenuPath = computed(function () {
  return route.meta.menuActivePath || menuActivePathByRoutePath[route.path] || route.path
})

const activeGroupKey = computed(function () {
  return route.meta.menuGroupKey || menuGroupKeyByRoutePath[route.path] || menuGroupKeyByRoutePath[activeMenuPath.value] || 'workspace'
})

const frameStyle = computed(function () {
  return {
    '--sidebar-width': isCollapsed.value ? '68px' : '240px'
  }
})

onMounted(function () {
  syncOpenedGroup()
})

watch([activeGroupKey, isCollapsed], function () {
  nextTick(syncOpenedGroup)
})

watch(isCollapsed, function (value) {
  persistCollapsedState(value)
})

function toggleSidebar() {
  isCollapsed.value = !isCollapsed.value
}

function syncOpenedGroup() {
  if (isCollapsed.value || !menuRef.value) {
    return
  }
  if (typeof menuRef.value.open === 'function') {
    menuRef.value.open(activeGroupKey.value)
  }
}

function readCollapsedState() {
  if (typeof window === 'undefined') {
    return false
  }
  try {
    return window.localStorage.getItem(STORAGE_KEY) === '1'
  } catch (error) {
    return false
  }
}

function persistCollapsedState(value) {
  if (typeof window === 'undefined') {
    return
  }
  try {
    window.localStorage.setItem(STORAGE_KEY, value ? '1' : '0')
  } catch (error) {
    // Ignore storage errors so the sidebar still works in private mode.
  }
}
</script>
