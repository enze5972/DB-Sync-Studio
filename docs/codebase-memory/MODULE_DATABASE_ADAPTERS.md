# MODULE_DATABASE_ADAPTERS

## 模块用途

数据库连接、测试连接、元数据扫描、方言适配，覆盖 MySQL、PostgreSQL、达梦 DM。

## 关键目录

- `app-core/src/main/java/com/dbsyncstudio/core/connection`
- `app-core/src/main/java/com/dbsyncstudio/core/metadata`
- `app-core/src/main/java/com/dbsyncstudio/core/schema`
- `app-model/src/main/java/com/dbsyncstudio/model/datasource`

## 关键文件

- `app-core/src/main/java/com/dbsyncstudio/core/connection/JdbcDatasourceConnectionTester.java`
- `app-core/src/main/java/com/dbsyncstudio/core/connection/DefaultDatasourceConnectionOpener.java`
- `app-core/src/main/java/com/dbsyncstudio/core/metadata/JdbcDatabaseMetadataScanner.java`
- `app-core/src/main/java/com/dbsyncstudio/core/schema/DatabaseDialect.java`
- `app-core/src/main/java/com/dbsyncstudio/core/schema/SchemaSqlDialect.java`
- `app-core/src/main/java/com/dbsyncstudio/core/schema/SchemaComparisonEngine.java`
- `app-model/src/main/java/com/dbsyncstudio/model/datasource/DatasourceType.java`

## 核心类/组件/配置

- JDBC 连接测试
- 数据源打开器
- 元数据扫描器
- SQL 方言与差异比较
- 数据源类型枚举

## 什么时候需要读取

- 需要改数据源连接、测试连接、扫描 schema / table / column 时
- 需要改数据库方言或兼容不同数据库时
- 需要改数据源状态和连接指标时

## 什么时候不需要读取

- 只改前端页面布局时
- 只改 SQLite 监控/告警仓储时

## 已知问题

- 各数据库兼容性细节需要按具体任务验证，待确认

## 后续待办

- 继续保持 MySQL / PostgreSQL / 达梦 DM 的兼容路径清晰
- 继续保持测试连接与扫描链路稳定
