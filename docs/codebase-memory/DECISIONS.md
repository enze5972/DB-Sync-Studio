# DECISIONS

## 已确定的技术决策

### 1. 桌面技术栈固定

- 前端：`Tauri + Vue 3 + Element Plus`
- 后端：`Java 17` 是项目目标
- 本地存储：`SQLite`
- 平台：`macOS` / `Windows` / `Linux`

### 2. 安装即用

- 用户下载安装到后直接打开
- 不要求手动安装 Java、Node、数据库客户端
- 打包时内置 JRE

### 3. 仓库形态

- 采用统一仓库、分模块结构
- 模块包括 `app-model`、`app-store`、`app-core`、`app-ui`、`app-shell`

### 4. SQLite 变更策略

- 任何 SQLite 表结构变化都必须提供迁移
- 以现有表为主，尽量复用已有任务、日志、历史类表
- 监控与告警数据也走 SQLite

### 5. 后端分层

- controller / HTTP 入口
- service / 业务编排
- repository / SQLite 持久化
- engine / 同步与比较引擎
- model / 共享模型

### 6. 调度策略

- 调度使用轻量本地实现，不引入大型分布式调度框架
- 运行中保护仍是核心行为

### 7. SQL 方言

- DDL 和修复 SQL 由方言层统一处理，不把方言规则散落在前端页面里

### 8. 自动推荐

- 字段映射自动推荐使用本地算法，不引入复杂 AI 依赖

### 9. 路径与配置

- SQLite 默认路径通过 `AppPaths` / `SqliteDatabasePaths` 统一解析
- 前端通过 Tauri `invoke` 获取后端地址
- 不硬编码环境相关路径

### 10. 监控与告警

- 监控指标保存到 SQLite
- 告警规则、告警渠道、告警历史、告警去重状态保存到 SQLite
- 告警密钥与 Webhook Token 不能明文保存
- 告警发送失败不能影响主同步流程

### 11. 验证标准

- 完成状态必须以实际验证为准
- 以后端测试、前端构建、Tauri 打包检查作为主要验证方式
