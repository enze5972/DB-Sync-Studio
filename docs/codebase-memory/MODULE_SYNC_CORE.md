# MODULE_SYNC_CORE

## 模块用途

同步核心，负责任务执行、全量同步、增量同步、校验、修复、执行历史和监控/告警编排。

## 关键目录

- `app-core/src/main/java/com/dbsyncstudio/core/backend`
- `app-core/src/main/java/com/dbsyncstudio/core/sync`
- `app-core/src/main/java/com/dbsyncstudio/core/validation`
- `app-core/src/main/java/com/dbsyncstudio/core/alert`
- `app-core/src/main/java/com/dbsyncstudio/core/metadata`
- `app-core/src/main/java/com/dbsyncstudio/core/schema`

## 关键文件

- `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
- `app-core/src/main/java/com/dbsyncstudio/core/backend/MonitoringOverviewResponse.java`
- `app-core/src/main/java/com/dbsyncstudio/core/backend/MonitoringTrendResponse.java`
- `app-core/src/main/java/com/dbsyncstudio/core/backend/SyncRunDetailResponse.java`
- `app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcFullSyncEngine.java`
- `app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcIncrementalSyncEngine.java`
- `app-core/src/main/java/com/dbsyncstudio/core/validation/DataValidationEngine.java`
- `app-core/src/main/java/com/dbsyncstudio/core/validation/DataRepairEngine.java`
- `app-core/src/main/java/com/dbsyncstudio/core/alert/AlertSenderService.java`

## 核心类/组件/配置

- `DesktopBackendService`：后端编排入口
- `DesktopBackendServer`：HTTP API 注册
- `TaskSchedulerService`：调度触发
- `JdbcFullSyncEngine` / `JdbcIncrementalSyncEngine`：同步执行
- `DataValidationEngine` / `DataRepairEngine`：校验与修复
- `AlertSenderService`：告警发送
- `run_id` 贯穿同步、历史、监控、告警

## 什么时候需要读取

- 需要改任务执行流程、监控采集、告警触发、执行历史时
- 需要确认 run_id、table_task_id、task_id 的链路时
- 需要改后端 API 或状态流转时

## 什么时候不需要读取

- 只改前端样式、路由或 Tauri 配置时
- 只改 SQLite 表结构初始化时

## 已知问题

- 任务执行链路较长，任务前应先定位到具体 API 或 engine
- 监控/告警属于 P2-3 收尾范围，细节仍需按实现校验，待确认

## 后续待办

- 继续验证监控趋势和告警触发是否完整
- 继续验证执行历史与监控、告警的关联是否完整
