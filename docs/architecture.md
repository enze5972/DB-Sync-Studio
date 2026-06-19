# DB Sync Studio 架构设计

## 1. 目标

DB Sync Studio 是一个面向本地桌面的跨平台数据库同步工具，目标是让用户下载安装后即可使用，无需额外安装 Java、Node、MySQL 客户端。

## 2. 技术选型

- UI：Vue 3 + Element Plus
- Desktop Shell：Tauri
- Core Engine：Java 17 优先
- Local Store：SQLite
- Packaging：内置 JRE
- Sync Execution：Java 进程内执行

## 3. 总体分层

### 3.1 前端层

负责桌面交互，不直接访问数据库。

职责：

- 首页仪表盘
- 数据源管理
- 同步任务管理
- 字段映射编辑
- 执行日志查看
- 软件设置

### 3.2 桌面壳层

Tauri 负责：

- 窗口创建
- 系统托盘
- 本地文件访问
- 调用后端 Java 进程
- 打包与分发

### 3.3 后端核心层

Java 负责所有业务逻辑和同步执行。

职责：

- 数据源连接测试
- Schema 扫描
- 同步任务编排
- 字段映射处理
- 分页读取与批量写入
- 断点续传与重试
- 执行日志记录

### 3.4 本地存储层

SQLite 存储：

- 数据源配置
- 同步任务定义
- 字段映射关系
- 执行日志
- 任务状态与断点

## 4. 核心模块

### 4.1 数据源管理

支持：

- MySQL
- PostgreSQL
- 达梦 DM

能力：

- 保存连接配置
- 测试连接
- 加密保存敏感信息

### 4.2 元数据扫描

扫描内容：

- schema
- table
- columns
- primary key

### 4.3 同步任务

支持的同步方向：

- MySQL -> PostgreSQL
- PostgreSQL -> MySQL
- MySQL -> DM
- DM -> MySQL

执行方式：

- 全量同步
- 增量同步
- 定时同步
- 手动执行

### 4.4 字段映射

支持：

- 源字段到目标字段映射
- 忽略字段
- 默认值
- 类型转换

### 4.5 同步引擎

关键能力：

- 分页读取
- 批量写入
- 断点续传
- 失败重试
- 执行日志

### 4.6 日志与审计

记录：

- 任务开始与结束
- 同步进度
- 失败原因
- 重试记录

## 5. 推荐工程形态

建议采用前后端分离但仓库统一管理的 monorepo 结构：

- `app-model`：共享数据模型与仓储接口
- `app-ui`：Vue 3 前端
- `app-shell`：Tauri 壳工程
- `app-core`：Java 核心同步引擎
- `app-store`：SQLite 持久化访问层

## 6. 进程模型

推荐方案：

1. Tauri 启动桌面窗口
2. 前端通过 Tauri command 调用本地后端能力
3. Java 核心以本地进程或嵌入式服务方式执行
4. 所有任务状态持久化到 SQLite

## 7. 数据流

典型同步流程：

1. 用户选择源库和目标库
2. 扫描元数据
3. 配置字段映射
4. 创建同步任务
5. 执行全量或增量同步
6. 写入执行日志
7. 保存断点与任务状态

## 8. 打包策略

### 8.1 Windows

- `.exe`
- `.msi`

### 8.2 macOS

- `.app`
- `.dmg`

### 8.3 Linux

- `.AppImage`
- `.deb`

### 8.4 运行时

打包时内置 JRE，避免用户额外安装 Java。

### 8.5 产物组织

推荐由 `scripts/package.js` 统一编排：

- 先构建 `app-core` 后端
- 再构建 `app-ui` 前端
- 最后生成 `release/<platform>/` 目录

`release/<platform>/` 中建议包含：

- `backend/`：后端 jar、依赖和启动脚本
- `ui/dist/`：前端静态资源
- `runtime/`：可选的精简 JRE
- `tauri-bundle/`：Tauri 原生安装包产物
- `tauri-bundle/manifest.json`：bundle 文件清单
- `manifest.json`：打包清单

## 9. 逐步落地顺序

建议实现顺序：

1. 工程骨架与启动页
2. SQLite 持久化
3. 数据源管理
4. 元数据扫描
5. 同步任务管理
6. 字段映射
7. 同步引擎
8. 日志与定时任务
9. 打包发布
