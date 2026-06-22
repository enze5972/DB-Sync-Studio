import { createRouter, createWebHashHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import WelcomeView from '../views/WelcomeView.vue'
import HelpView from '../views/HelpView.vue'
import DatasourceView from '../views/DatasourceView.vue'
import TaskView from '../views/TaskView.vue'
import FieldMappingView from '../views/FieldMappingView.vue'
import MetadataScanView from '../views/MetadataScanView.vue'
import DataPreviewView from '../views/DataPreviewView.vue'
import SqlEditorView from '../views/SqlEditorView.vue'
import TaskWizardView from '../views/TaskWizardView.vue'
import LogsView from '../views/LogsView.vue'
import SchemaCompareView from '../views/SchemaCompareView.vue'
import ScheduleCenterView from '../views/ScheduleCenterView.vue'
import DataValidationView from '../views/DataValidationView.vue'
import ExecutionHistoryView from '../views/ExecutionHistoryView.vue'
import TaskRunDetailView from '../views/TaskRunDetailView.vue'
import RunMonitoringView from '../views/RunMonitoringView.vue'
import AlertSettingsView from '../views/AlertSettingsView.vue'
import AlertHistoryView from '../views/AlertHistoryView.vue'
import AboutView from '../views/AboutView.vue'
import ComingSoonView from '../views/ComingSoonView.vue'
import LicenseView from '../views/LicenseView.vue'
import SettingsView from '../views/SettingsView.vue'
import MainLayout from '../layouts/MainLayout.vue'
import { readFirstLaunchCompleted, shouldRedirectToWelcome } from '../services/onboardingState'

const routes = [
  {
    path: '/',
    component: MainLayout,
    beforeEnter: async function (to, from, next) {
      const firstLaunchCompleted = await readFirstLaunchCompleted()
      if (shouldRedirectToWelcome(firstLaunchCompleted, to.path)) {
        next('/welcome')
        return
      }
      next()
    },
    children: [
      {
        path: '',
        name: 'dashboard',
        component: DashboardView
      },
      {
        path: 'datasource',
        name: 'datasource',
        component: DatasourceView
      },
      {
        path: 'tasks',
        name: 'tasks',
        component: TaskView
      },
      {
        path: 'task-wizard',
        name: 'task-wizard',
        component: TaskWizardView
      },
      {
        path: 'mapping',
        name: 'mapping',
        component: FieldMappingView
      },
      {
        path: 'metadata',
        name: 'metadata',
        component: MetadataScanView
      },
      {
        path: 'preview',
        name: 'preview',
        component: DataPreviewView
      },
      {
        path: 'sql',
        name: 'sql',
        component: SqlEditorView
      },
      {
        path: 'schema-compare',
        name: 'schema-compare',
        component: SchemaCompareView
      },
      {
        path: 'logs',
        name: 'logs',
        component: LogsView
      },
      {
        path: 'validation',
        name: 'validation',
        component: DataValidationView
      },
      {
        path: 'execution-history',
        name: 'execution-history',
        component: ExecutionHistoryView
      },
      {
        path: 'run-monitoring',
        name: 'run-monitoring',
        component: RunMonitoringView
      },
      {
        path: 'alert-settings',
        name: 'alert-settings',
        component: AlertSettingsView
      },
      {
        path: 'alert-history',
        name: 'alert-history',
        component: AlertHistoryView
      },
      {
        path: 'execution-history/detail',
        name: 'execution-history-detail',
        meta: {
          hiddenInMenu: true,
          menuActivePath: '/task-run',
          menuGroupKey: 'run-ops'
        },
        component: TaskRunDetailView
      },
      {
        path: 'task-run',
        name: 'task-run',
        meta: {
          menuActivePath: '/task-run',
          menuGroupKey: 'run-ops'
        },
        component: TaskRunDetailView
      },
      {
        path: 'schedule-center',
        name: 'schedule-center',
        component: ScheduleCenterView
      },
      {
        path: 'settings',
        name: 'settings',
        component: SettingsView
      },
      {
        path: 'license',
        name: 'license',
        component: LicenseView
      }
    ]
  },
  {
    path: '/welcome',
    name: 'welcome',
    component: WelcomeView
  },
  {
    path: '/help',
    name: 'help',
    component: HelpView
  },
  {
    path: '/about',
    name: 'about',
    component: AboutView
  },
  {
    path: '/license',
    name: 'license-page',
    component: LicenseView
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.afterEach(function () {
  requestAnimationFrame(function () {
    const content = document.querySelector('.content')
    if (content) {
      content.scrollTop = 0
    }
    const standalonePage = document.querySelector('.standalone-page')
    if (standalonePage) {
      standalonePage.scrollTop = 0
    }
    window.scrollTo(0, 0)
  })
})

export default router
