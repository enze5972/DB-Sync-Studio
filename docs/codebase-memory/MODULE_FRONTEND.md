# MODULE_FRONTEND

## 模块用途

前端桌面页面，基于 Vue 3 + Element Plus 展示数据源、任务、执行历史、运行监控、告警和日志。

## 关键目录

- `app-ui/src/views`
- `app-ui/src/router`
- `app-ui/src/services`
- `app-ui/src/components`

## 关键文件

- `app-ui/src/router/index.js`
- `app-ui/src/services/backend.js`
- `app-ui/src/views/DashboardView.vue`
- `app-ui/src/views/TaskView.vue`
- `app-ui/src/views/ExecutionHistoryView.vue`
- `app-ui/src/views/RunMonitoringView.vue`
- `app-ui/src/views/AlertSettingsView.vue`
- `app-ui/src/views/AlertHistoryView.vue`
- `app-ui/src/views/TaskRunDetailView.vue`

## 核心类/组件/配置

- Vue 路由配置
- Element Plus 表格、表单、弹窗、抽屉、分页、描述列表
- 运行监控页、告警设置页、告警历史页
- `backend.js` 的后端 API 调用封装

## 什么时候需要读取

- 需要改页面布局、筛选、表格、弹窗、路由时
- 需要确认前端 API 调用、页面入口、页面跳转时
- 需要确认 Element Plus 风格是否统一时

## 什么时候不需要读取

- 只改后端同步引擎、SQLite 迁移、调度逻辑时
- 只改桌面打包或 Tauri 启动链路时

## 已知问题

- 页面功能较多，任务前要先定位到具体路由和页面文件
- 图表依赖和趋势展示方式需要以当前页面实现为准，待确认

## 后续待办

- 继续完善运行监控页面展示与筛选
- 继续完善告警规则、告警渠道、历史查询
- 保持 Element Plus 风格一致
