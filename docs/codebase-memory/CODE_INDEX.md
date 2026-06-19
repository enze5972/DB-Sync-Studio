# CODE_INDEX

只记录新会话需要快速定位的关键目录、模块、入口和读取时机。

## 先读顺序

1. `docs/codebase-memory/CONTEXT_COMPACT.md`
2. `docs/codebase-memory/IMPLEMENTATION_STATUS.md`
3. `docs/codebase-memory/DECISIONS.md`
4. 与当前任务相关的 `docs/codebase-memory/MODULE_*.md`
5. `docs/codebase-memory/WORKFLOW.md`
6. 当前任务相关源码

## 关键目录

- `app-core/src/main/java/com/dbsyncstudio/core/backend`
  - 读取时机：需要确认后端 API、任务执行、监控、告警、历史接口时
- `app-core/src/main/java/com/dbsyncstudio/core/sync`
  - 读取时机：需要确认同步执行流程、run_id 贯穿、失败处理、进度上报时
- `app-core/src/main/java/com/dbsyncstudio/core/validation`
  - 读取时机：需要确认校验、修复触发和告警来源时
- `app-core/src/main/java/com/dbsyncstudio/core/scheduler`
  - 读取时机：需要确认调度触发、跳过运行、运行中保护时
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite`
  - 读取时机：需要确认 SQLite 初始化、迁移兼容、仓储实现、加密、监控、告警持久化时
- `app-store/src/main/resources/db/migration`
  - 读取时机：需要确认表结构、增量迁移、索引、历史表、监控表、告警表时
- `app-ui/src/views`
  - 读取时机：需要确认页面布局、筛选、列表、图表、Element Plus 风格时
- `app-ui/src/router`
  - 读取时机：需要确认页面入口和路由挂载时
- `app-shell/src-tauri`
  - 读取时机：需要确认桌面启动、后端拉起、打包相关配置时

## 关键模块

- `DesktopBackendService`
  - 入口：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
  - 作用：后端编排入口、任务执行、监控聚合、告警触发、历史查询
- `DesktopBackendServer`
  - 入口：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
  - 作用：HTTP API 注册
- `SqliteSchemaInitializer`
  - 入口：`app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java`
  - 作用：SQLite 初始化、补列、补表、索引创建
- `SqliteMonitoringRepository`
  - 作用：监控指标读写
- `SqliteAlertRuleRepository`
  - 作用：告警规则读写
- `SqliteAlertChannelRepository`
  - 作用：告警渠道读写
- `SqliteAlertHistoryRepository`
  - 作用：告警历史读写
- `SqliteAlertDedupStateRepository`
  - 作用：告警去重状态读写

## 核心页面

- `RunMonitoringView.vue`
  - 读取时机：需要确认运行监控、趋势图、连接状态、任务统计展示时
- `AlertSettingsView.vue`
  - 读取时机：需要确认告警规则、渠道、启停、测试发送时
- `AlertHistoryView.vue`
  - 读取时机：需要确认告警历史筛选、发送结果、状态回看时
- `ExecutionHistoryView.vue`
  - 读取时机：需要确认 run_id 历史、任务详情入口时
- `TaskRunDetailView.vue`
  - 读取时机：需要确认单次执行详情、表级历史、日志链路时

## 什么时候需要补读

- 看到 `TODO`、`待确认`、构建失败、测试失败、路由缺失、迁移缺失时
- 需要确认某功能是否已在仓库中落地时
- 需要找某个页面、仓储、API、迁移表结构时
