# CONTEXT COMPACT

如果你是新来的 Codex，从这里开始就够了。

## 项目目标

DB Sync Studio 是跨平台桌面数据库同步工具，下载安装到后即可直接使用，不需要手动安装 Java、Node、数据库客户端。

## 技术栈

- 前端：Tauri + Vue 3 + Element Plus
- 后端：Java 17 为项目目标，根 `pom.xml` 当前仍显示 `source/target = 1.8`，后续任务需继续以实际模块配置核对
- 本地存储：SQLite
- 平台：macOS / Windows / Linux
- 打包：内置 JRE 的桌面安装包

## 当前架构

- `app-model`：共享模型、枚举、仓储接口
- `app-store`：SQLite 初始化、迁移、仓储实现
- `app-core`：Java 核心、后端 HTTP API、同步引擎、校验/修复/调度、监控与告警编排
- `app-ui`：Vue 3 + Element Plus 页面
- `app-shell`：Tauri 桌面壳

## 已完成

- P0：数据源、元数据扫描、同步任务、字段映射、预览、SQL 编辑器、日志、桌面壳
- P1：自动字段映射增强、DDL 同步、表结构比较、增量增强、调度中心
- P2-1：数据校验、数据修复
- P2-2：多表并发同步、执行历史增强

## 当前进度

- P2-3：链路监控 + 告警系统的核心表、仓储、后端入口、前端路由和页面已在仓库中可见，后续新会话应围绕现有实现做验证和收尾
- P3：CDC 实时同步、Binlog/WAL/日志解析、数据转换脚本引擎、AI 配置助手、模板市场、团队协作权限系统，均未进入当前阶段
- 当前只做小步修复和增量开发，不做全局重构

## 关键文件

- 后端入口：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- HTTP 服务：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
- 监控响应：`app-core/src/main/java/com/dbsyncstudio/core/backend/MonitoringOverviewResponse.java`
- 监控趋势：`app-core/src/main/java/com/dbsyncstudio/core/backend/MonitoringTrendResponse.java`
- 字段推荐：`app-core/src/main/java/com/dbsyncstudio/core/mapping/FieldMappingSuggestionMatcher.java`
- 结构比较：`app-core/src/main/java/com/dbsyncstudio/core/schema/SchemaComparisonEngine.java`
- 校验：`app-core/src/main/java/com/dbsyncstudio/core/validation/DataValidationEngine.java`
- 修复：`app-core/src/main/java/com/dbsyncstudio/core/validation/DataRepairEngine.java`
- 同步：`app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcFullSyncEngine.java`
- 增量：`app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcIncrementalSyncEngine.java`
- 调度：`app-core/src/main/java/com/dbsyncstudio/core/scheduler/TaskSchedulerService.java`
- SQLite 初始化：`app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java`
- SQLite 迁移：`app-store/src/main/resources/db/migration/`
- 前端关键页：
  - `app-ui/src/views/TaskView.vue`
  - `app-ui/src/views/TaskWizardView.vue`
  - `app-ui/src/views/FieldMappingView.vue`
  - `app-ui/src/views/SchemaCompareView.vue`
  - `app-ui/src/views/DataValidationView.vue`
  - `app-ui/src/views/ExecutionHistoryView.vue`
  - `app-ui/src/views/RunMonitoringView.vue`
  - `app-ui/src/views/AlertSettingsView.vue`
  - `app-ui/src/views/AlertHistoryView.vue`
  - `app-ui/src/views/TaskRunDetailView.vue`
  - `app-ui/src/views/ScheduleCenterView.vue`
  - `app-ui/src/views/DataPreviewView.vue`

## 关键设计决策

- SQLite 结构变更必须提供迁移脚本
- 优先复用现有任务表、字段映射表、日志表
- Java 代码保持 controller / service / repository / engine / model 分层
- 不硬编码数据库账号、路径、JRE 路径、系统路径
- SQL 方言统一由后端处理，不散落在前端
- 字段映射自动推荐使用本地算法，不引入复杂 AI 依赖
- 调度使用轻量本地实现，不引入大型分布式调度框架
- 监控指标和告警数据写入 SQLite，并保留最近 30 天数据

## 已知问题

- 数据预览页的“页大小”控件之前出现过布局异常，后续要继续确认最终修复效果
- 批量运行相关测试曾出现状态收尾不稳定，后续改动要继续验证
- 告警密钥和 Webhook Token 不能明文保存，后续任务必须继续保持本地加密
- 任何未确认信息都写“待确认”，不要猜
- 增量同步执行时的 checkpoint 状态必须是单次执行局部状态，不能在引擎实例之间复用可变字段

## 下一步

- P2-3 继续按小阶段推进，优先验证监控趋势、告警触发、去重和历史查询
- 如继续修 UI，优先复用现有 Element Plus 组件
- 如继续修后端，先看 `DesktopBackendService` 和对应测试
- 每次完成一个阶段后都要更新 `docs/codebase-memory`
