import {
  Bell,
  CircleCheck,
  Clock,
  Connection,
  Document,
  EditPen,
  Files,
  Guide,
  HomeFilled,
  House,
  InfoFilled,
  Key,
  Monitor,
  Operation,
  QuestionFilled,
  Setting,
  Share,
  Timer,
  View
} from '@element-plus/icons-vue'

export const menuGroups = [
  {
    key: 'workspace',
    title: '工作台',
    icon: HomeFilled,
    children: [
      { key: 'dashboard', title: '首页', path: '/', icon: House },
      { key: 'welcome', title: '快速开始', path: '/welcome', icon: Guide }
    ]
  },
  {
    key: 'data-prepare',
    title: '数据准备',
    icon: Connection,
    children: [
      { key: 'datasource', title: '数据源管理', path: '/datasource', icon: Connection },
      { key: 'metadata', title: '表结构扫描', path: '/metadata', icon: Files }
    ]
  },
  {
    key: 'sync-task',
    title: '同步任务',
    icon: Operation,
    children: [
      { key: 'tasks', title: '同步任务', path: '/tasks', icon: Operation },
      { key: 'task-wizard', title: '创建向导', path: '/task-wizard', icon: Guide },
      { key: 'mapping', title: '字段映射', path: '/mapping', icon: Share },
      { key: 'validation', title: '数据校验', path: '/validation', icon: CircleCheck }
    ]
  },
  {
    key: 'data-tools',
    title: '数据工具',
    icon: Operation,
    children: [
      { key: 'preview', title: '数据预览', path: '/preview', icon: View },
      { key: 'sql', title: 'SQL 编辑器', path: '/sql', icon: EditPen },
      { key: 'schema-compare', title: '表结构比较', path: '/schema-compare', icon: Operation }
    ]
  },
  {
    key: 'run-ops',
    title: '运行运维',
    icon: Monitor,
    children: [
      { key: 'run-monitoring', title: '运行监控', path: '/run-monitoring', icon: Monitor },
      { key: 'schedule-center', title: '调度中心', path: '/schedule-center', icon: Timer },
      { key: 'execution-history', title: '执行历史', path: '/execution-history', icon: Clock },
      { key: 'task-run', title: 'Run 详情', path: '/task-run', icon: Document },
      { key: 'logs', title: '执行日志', path: '/logs', icon: Document }
    ]
  },
  {
    key: 'alert-center',
    title: '告警中心',
    icon: Bell,
    children: [
      { key: 'alert-settings', title: '告警设置', path: '/alert-settings', icon: Setting },
      { key: 'alert-history', title: '告警历史', path: '/alert-history', icon: Bell }
    ]
  },
  {
    key: 'system',
    title: '系统管理',
    icon: Setting,
    children: [
      { key: 'settings', title: '软件设置', path: '/settings', icon: Setting },
      { key: 'license', title: 'License 授权', path: '/license', icon: Key },
      { key: 'help', title: '帮助文档', path: '/help', icon: QuestionFilled },
      { key: 'about', title: '关于', path: '/about', icon: InfoFilled }
    ]
  }
]

export const menuActivePathByRoutePath = buildMenuActivePathByRoutePath(menuGroups)
export const menuGroupKeyByRoutePath = buildMenuGroupKeyByRoutePath(menuGroups)

menuActivePathByRoutePath['/execution-history/detail'] = '/execution-history'
menuActivePathByRoutePath['/task-run'] = '/task-run'
menuGroupKeyByRoutePath['/execution-history/detail'] = 'run-ops'
menuGroupKeyByRoutePath['/task-run'] = 'run-ops'

function buildMenuActivePathByRoutePath(groups) {
  const result = {}
  groups.forEach(function (group) {
    group.children.forEach(function (item) {
      result[item.path] = item.path
    })
  })
  return result
}

function buildMenuGroupKeyByRoutePath(groups) {
  const result = {}
  groups.forEach(function (group) {
    group.children.forEach(function (item) {
      result[item.path] = group.key
    })
  })
  return result
}
