# PROJECT OVERVIEW

DB Sync Studio 是一个跨平台数据库同步桌面工具，目标是让用户下载安装到后即可直接使用，不需要手动安装 Java、Node、数据库客户端或额外的同步中间件。

## 产品定位

- 桌面形态：`Tauri + Vue 3 + Element Plus`
- 后端/同步核心：`Java 17`
- 本地存储：`SQLite`
- 支持平台：`macOS` / `Windows` / `Linux`
- 打包目标：内置 JRE 的独立安装包

## 仓库目标

这个仓库的核心任务是把数据库同步工作台做成一个可安装、可运行、可打包的本地桌面应用，覆盖：

- 数据源管理
- 表结构扫描
- 同步任务管理
- 字段映射
- 数据预览
- SQL 执行
- 表结构比较和 DDL 建议
- 增量同步
- 调度中心
- 执行日志和断点恢复

## 当前状态

- P0 已完成
- P1 已完成
- 下一阶段准备进入 P2

## 代码结构总览

```text
DB-Sync-Studio/
├── app-model/   # 共享模型、枚举、仓储接口
├── app-store/   # SQLite 初始化与仓储实现
├── app-core/    # Java 核心、后端 HTTP API、同步引擎
├── app-ui/      # Vue 3 + Element Plus 前端
├── app-shell/   # Tauri 桌面壳
├── scripts/     # 构建与打包脚本
└── docs/        # 项目文档与上下文压缩文档
```

## 新会话优先看什么

1. `docs/codebase-memory/CONTEXT_COMPACT.md`
2. `docs/codebase-memory/PROJECT_OVERVIEW.md`
3. `docs/codebase-memory/IMPLEMENTATION_STATUS.md`

