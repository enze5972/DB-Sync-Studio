# ARCHITECTURE

## 分层

### 前端层

- 目录：`app-ui`
- 技术：Vue 3 + Element Plus + Vite
- 职责：
  - 页面展示
  - 表单和表格交互
  - 调用本地后端 HTTP API

### 桌面壳层

- 目录：`app-shell`
- 技术：Tauri 1.x
- 职责：
  - 启动前端
  - 拉起本地 Java 后端
  - 提供打包和安装包产物

### 后端核心层

- 目录：`app-core`
- 技术：Java 17 运行时，源码风格尽量保持简单兼容
- 职责：
  - 数据源连接测试
  - 元数据扫描
  - 同步任务执行
  - 字段映射推荐
  - DDL 同步与表结构比较
  - 增量同步
  - 调度中心
  - SQL 执行与日志

### 本地存储层

- 目录：`app-store`
- 技术：SQLite + JDBC
- 职责：
  - 数据源配置
  - 同步任务
  - 字段映射
  - 执行日志
  - SQL 执行日志
  - 增量断点
  - 表结构比较历史

## 运行模型

当前实现是本地桌面应用形态：

1. Tauri 打开桌面窗口
2. 前端通过 `invoke` 或本地 HTTP 调用后端
3. Java 后端以本地 HTTP 服务方式提供接口
4. SQLite 存在用户家目录下的本地应用目录

关键路径：

- `app-shell/src-tauri/tauri.conf.json` 配置前后端联动
- `app-ui/src/services/backend.js` 负责获取后端 base URL
- `app-core/src/main/java/com/dbsyncstudio/core/DbSyncStudioApplication.java` 负责启动后端
- `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java` 提供 HTTP API

## 路径与存储

- SQLite 默认文件：`~/.db-sync-studio/db-sync-studio.sqlite`
- 入口封装：
  - `app-core/src/main/java/com/dbsyncstudio/core/bootstrap/AppPaths.java`
  - `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteDatabasePaths.java`

## 关键业务模块

### 字段映射

- 推荐引擎：`app-core/src/main/java/com/dbsyncstudio/core/mapping/FieldMappingSuggestionMatcher.java`
- 前端：`app-ui/src/views/FieldMappingView.vue`

### 表结构比较与 DDL

- 比较引擎：`app-core/src/main/java/com/dbsyncstudio/core/schema/SchemaComparisonEngine.java`
- 方言渲染：`app-core/src/main/java/com/dbsyncstudio/core/schema/SchemaSqlDialect.java`
- 前端：`app-ui/src/views/SchemaCompareView.vue`

### 增量同步

- 后端：`app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcIncrementalSyncEngine.java`
- 任务接线：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- 前端：`app-ui/src/views/TaskView.vue`、`app-ui/src/views/TaskWizardView.vue`

### 调度中心

- 调度服务：`app-core/src/main/java/com/dbsyncstudio/core/scheduler/TaskSchedulerService.java`
- 调度计算：`app-core/src/main/java/com/dbsyncstudio/core/scheduler/TaskScheduleCalculator.java`
- 前端：`app-ui/src/views/ScheduleCenterView.vue`

## 数据库

SQLite 初始化与迁移都已经落在仓库里：

- `app-store/src/main/resources/db/schema.sql`
- `app-store/src/main/resources/db/migration/V2__p0.sql`
- `app-store/src/main/resources/db/migration/V3__schema_diff_p1.sql`
- `app-store/src/main/resources/db/migration/V4__incremental_mode_p1.sql`
- `app-store/src/main/resources/db/migration/V5__sync_task_checkpoint_p1.sql`
- `app-store/src/main/resources/db/migration/V6__sync_task_table_name_p1.sql`

原则：

- SQLite 表结构变更必须配迁移
- 尽量复用现有任务表、字段映射表、日志表
- 避免新建平行表替代已有表

