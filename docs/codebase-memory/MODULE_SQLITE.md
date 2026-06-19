# MODULE_SQLITE

## 模块用途

SQLite 初始化、迁移、仓储实现、本地密钥加密、监控与告警持久化。

## 关键目录

- `app-store/src/main/java/com/dbsyncstudio/store/sqlite`
- `app-store/src/main/resources/db/migration`
- `app-store/src/main/resources/db`

## 关键文件

- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java`
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteConnectionFactory.java`
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteMonitoringRepository.java`
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteAlertRuleRepository.java`
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteAlertChannelRepository.java`
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteAlertHistoryRepository.java`
- `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteAlertDedupStateRepository.java`
- `app-store/src/main/resources/db/migration/V11__monitoring_metrics_p2_3.sql`
- `app-store/src/main/resources/db/migration/V11__alert_storage_p2_3.sql`

## 核心类/组件/配置

- `SqliteSchemaInitializer`
- `SqliteMonitoringRepository`
- 告警规则、渠道、历史、去重状态仓储
- 本地加密相关类，密钥不应明文落库

## 什么时候需要读取

- 需要改 SQLite 表结构、迁移、索引、仓储 SQL 时
- 需要确认监控/告警数据如何落库时
- 需要确认本地加密和敏感信息存储时

## 什么时候不需要读取

- 只改前端页面展示时
- 只改 Tauri 启动或打包时

## 已知问题

- SQLite 结构变更必须同步迁移文件和初始化逻辑，待确认项不能猜
- 监控数据保留期、清理策略仍需持续校验

## 后续待办

- 继续验证监控表、告警表、去重状态表是否与代码一致
- 继续验证敏感信息是否始终加密存储
