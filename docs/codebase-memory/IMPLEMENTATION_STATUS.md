# IMPLEMENTATION STATUS

## 已完成

### P0

- 数据源管理
- 元数据扫描
- 同步任务管理
- 字段映射
- 数据预览
- SQL 编辑器
- 执行日志
- 桌面壳和启动链路

### P1

- 自动字段映射增强
- DDL 同步
- 表结构差异比较
- 增量同步增强
- 调度中心

### P2-1

- 数据校验
- 数据修复

### P2-2

- 多表任务组
- 多表并发同步
- 执行历史增强

### P2-3

- 监控表与告警表已存在于 migration 和 SQLite 初始化中
- 监控与告警相关仓储类已存在
- 后端服务已引入监控和告警编排依赖
- 前端已存在运行监控、告警设置、告警历史页面路由

### P2-4

- 运行监控页已补齐失败任务与最近指标的空态/名称展示
- 告警历史页已恢复关键字筛选透传并补充空态提示
- 执行日志导出已增加空数据提示和带时间戳的文件名
- Tauri 打包脚本已增加输入校验、cargo 真实路径调用和 bundle 安全检查
- 后端已补充 `/api/diagnostics`，SQLite 初始化会写入 `schema_migration_entry` 并设置 `PRAGMA user_version`
- 启动时会将残留的 `RUNNING` 任务恢复为 `PENDING` 并记录恢复日志
- 诊断响应已补充应用目录、数据库文件路径、任务总数、运行中任务数和最近恢复数
- 执行日志页已增加诊断导出入口
- SQL 编辑器已补充危险 SQL 确认与重复执行拦截
- 数据预览页已补充首屏提示、错误/空结果状态和路由自动预览防重复触发
- 告警设置页已补充测试/删除的二次确认与行级 loading，敏感值继续保持脱敏展示
- 告警历史页已补充加载失败提示和稳定的查询快照
- 前端构建与后端编译已再次验证通过

### P2-5-1

- 首次启动欢迎页已接入独立 `/welcome` 路由，首次进入会引导到新手流程
- 跳过/开始使用都会写入本地首启完成状态，后续启动不再自动弹出
- 首页新增快速开始入口，设置页新增重新打开新手引导和帮助文档入口
- 已补充本地帮助文档页，内容覆盖新增数据源、连接测试、表结构扫描、同步任务、字段映射、执行同步、日志、告警和常见问题
- 已新增共享空状态组件并替换数据源、表结构扫描、同步任务、字段映射、执行日志、数据校验、运行监控、执行历史、Run 详情、告警设置、告警历史和数据预览等页面的核心空态
- 欢迎页和帮助中心已做第二轮视觉收口，整体改为更克制的品牌引导和本地知识中心风格
- 前端构建与后端编译均已通过验证

### P2-5-2

- 软件设置页已从占位页升级为正式设置页，支持日志保留、监控保留、分页大小、同步批量、最大并发、危险 SQL、启动恢复、自动检查更新和新手引导开关
- About 页已新增并统一展示产品名、版本、构建时间、前端版本、Java 核心版本、Tauri 版本、SQLite schema version、平台和数据库支持信息
- 后端新增 `/api/app-settings`，设置与版本信息统一从 SQLite 本地配置和后端构建信息返回
- SQLite 已新增 `app_setting` 表，并补充迁移 `V12__app_settings_and_about_support.sql`
- 执行日志页和设置页已可导出诊断 JSON，设置页可打开日志目录和重新打开新手引导
- `mvn -pl app-core -am -DskipTests compile` 与 `npm --prefix app-ui run build` 已通过验证，保留现有 chunk size warning

### P2-5-4

- 默认数据目录已改为按平台解析：Windows 使用 `APPDATA`，macOS 使用 `~/Library/Application Support`，Linux 使用 `XDG_DATA_HOME` 或 `~/.local/share`
- 后端统一错误出口已补充更稳的用户提示，避免空消息直接回到前端
- 前端统一请求层已补充错误消息归一化，优先展示后端中文提示，再回退到可理解的请求失败信息
- Tauri 打包输入校验已补充 backend launcher、runtime image 和图标文件存在性检查
- 已完成 Java 测试、前端构建和 Tauri bundle 检查，macOS bundle 产物已生成

### P2-5-5

- 告警历史、执行日志、执行历史和 Run 详情已完成一轮浏览器可见的 UX 收口，统一了 `list-page` 间距、自适应筛选区、状态中文标签和更自然的按钮层级
- Run 详情直达页在缺少 `taskId` / `runId` 时不再弹出警告，避免直接进入时出现打断式错误体验
- `npm --prefix app-ui run build` 已再次通过，页面改动已用真实浏览器重新核对
- 2026-06-22: 已在 Chrome 里复核 `/help`、`/about`、`/welcome` 和 `/run-monitoring`，其中独立页滚动容器与运行监控主内容滚动都正常
- 2026-06-22: `RunMonitoringView.vue` 已升级为运行监控工作台风格，补齐健康条、状态概览、趋势图、失败任务、数据源状态和最近指标的产品化收口；`npm --prefix app-ui run build` 已通过，Chrome 已复核 `/run-monitoring`
- 2026-06-22: `AlertSettingsView.vue` 已升级为 `Alert Configuration Workbench` 风格，补齐规则/渠道双栏、四项状态概览、配置建议、安全说明和脱敏展示；`npm --prefix app-ui run build` 已通过，Chrome 已复核 `/alert-settings`
- 2026-06-22: `AlertHistoryView.vue` 已升级为 `Alert History Workbench` 风格，补齐四项状态概览、紧凑筛选区、发送结果与运行批次关联展示；`npm --prefix app-ui run build` 已通过，Chrome 已复核 `/alert-history`
- 2026-06-22: `ExecutionHistoryView.vue` 已升级为 `Run History Workbench / 执行历史工作台` 风格，补齐四项状态概览、提示条、左侧 Run 列表与右侧 Run 摘要双栏结构、四种业务空态和现有 run 详情/日志跳转按钮；`npm --prefix app-ui run build` 已通过，Chrome 已复核 `/execution-history`
- 2026-06-22: `ScheduleCenterView.vue` 已升级为 `Schedule Center Workbench / 调度中心工作台` 风格，补齐四项状态概览、提示条、左侧任务列表与右侧调度详情双栏结构、任务上下文、最近执行历史和现有保存/手动执行/历史跳转行为；`npm --prefix app-ui run build` 已通过，Chrome 已复核 `/schedule-center`
- 2026-06-22: `LogsView.vue` 已升级为 `Execution Log Workbench / 执行日志工作台` 风格，补齐四项状态概览、提示条、紧凑筛选区、日志列表重点排查区和日志保留设置下沉；`npm --prefix app-ui run build` 已通过，Chrome 已复核 `/logs`
- 2026-06-22: 左侧导航已改成一级分类 + 二级子菜单，折叠态改为自定义窄栏徽标，Chrome 已复核默认首页、分组展开、二级跳转、刷新回显和长菜单滚动

### P3-1-1 / P3-1-2

- 已新增字段转换规则表、SQLite 仓储、transform engine 和后端 `/api/transform-rules` 接口
- 转换测试已支持按错误策略继续预览，成功/失败步骤都会返回结果
- 字段映射页已接入 `TransformRuleDrawer`，支持字段级转换规则的新增、编辑、排序、启停、删除、恢复未配置和测试
- 字段映射摘要会在加载、保存和删除时保持同步，删除字段映射会联动清理关联转换规则
- 前端 `build` 和后端 `mvn -pl app-core -am test` 已通过验证

### 2026-06-22 字段映射工作台轻量重构

- `FieldMappingView.vue` 已从普通表格页收口为 `Field Mapping Workbench`：顶部任务选择 + 主要操作、五项状态概览、居中的智能推荐区、下方映射规则 / 当前任务上下文双栏结构
- 保持现有字段映射、智能推荐、保存、删除、忽略和路由逻辑不变，仅补强按钮禁用原因、覆盖率、空态与任务可用性提示
- `app-ui/src/styles/app.css` 已补充字段映射页专属样式，统一了卡片边界、空态、上下文条和推荐区层级
- `npm --prefix app-ui run build` 已通过，Chrome 已复核 `/mapping` 的新结构和空态文案
- 2026-06-22: 字段映射推荐接口补了表名回退逻辑，`/api/mappings/suggest` 与 `FieldMappingView.vue` 的字段统计现在能在 demo 任务 schema 名不一致、或目标表使用 `_copy` 命名但实际库里只有基础表时继续工作；Chrome 已复核 `/mapping` 能正常展示 3 条推荐并清空 console error

### 2026-06-22 数据校验工作台轻量重构

- `DataValidationView.vue` 已从普通配置+表格页收口为 `Data Validation Workbench`：顶部任务选择+主按钮、五项状态概览、校验配置/最近校验双栏、差异明细/修复历史双栏
- 保持现有校验、差异查询、修复预览和修复执行逻辑不变，仅补强校验状态、差异数量、修复状态、按钮禁用提示和空态表达
- `app-ui/src/styles/app.css` 已补充数据校验页专属样式，统一了卡片边界、修复弹窗、空态和工作台层级
- `npm --prefix app-ui run build` 已通过；Chrome 复核在会话切换后中断，当前未完成最终浏览器复核

### 2026-06-21 数据源管理页 UX 收口

- `app-ui/src/views/DatasourceView.vue` 已完成轻量 UI 优化：顶部说明、四项状态概览、左侧连接列表/空态引导、右侧连接详情和静态连接建议
- 空状态已改为流程型引导，主按钮保留为“新建数据源”，次按钮保留为连接建议说明入口
- 数据源概览已复用前端现有连接指标接口，能够展示总数、可用连接和异常连接的真实统计
- 已用本地浏览器完成页面截图和按钮可点击验证；外部 Chrome 控制链路本次未成功连接到 extension
- `app-ui` 前端构建已通过，项目内未提供独立 lint/typecheck 脚本

### 2026-06-21 UI audit follow-up

- 右侧详情内容区已改为真正的独立滚动容器，切换左侧菜单后会在路由切换完成后回到顶部，避免停留在上一个页面的滚动位置
- `body` / `.page-shell` / `.app-frame` / `.content` 的高度和 overflow 约束已收口，帮助页和同步任务页都已重新在真实浏览器里确认可见
- `npm run build` in `app-ui` 已再次通过，当前仅保留既有 chunk size warning
- `/help` 页面已从“快速开始”语义收敛为“帮助文档”，左侧“快速开始”入口改指向 `/welcome`，首页按钮也同步指向新手引导页
- `/about` 页面已重做为产品信息中心 / 运行诊断中心 / 帮助入口，保留左侧“关于”入口并新增复制诊断、检查更新提示、日志目录入口和帮助跳转
- 2026-06-21: 独立路由滚动回归已修复，`/welcome`、`/help`、`/about`、`/license`、`ComingSoon` 统一接入 `.standalone-page` 可滚动容器；真实浏览器确认帮助页、关于页和欢迎页都能上下滚动，`/help` 与 `/about` 切换后可回到顶部

### 2026-06-21 表结构扫描页 UX 收口

- `app-ui/src/views/MetadataScanView.vue` 已完成轻量 UI 优化：顶部流程提示、四项统计卡片、左侧结构树空态引导、右侧字段详情引导和状态 chip 统一
- 页面保持现有数据源选择、结构扫描、结构比较和数据预览行为不变，仅做局部视觉收口
- 已完成构建和浏览器级确认，当前可见内容符合 Schema Explorer Workbench 的工作台语义

### 2026-06-21 创建同步任务向导轻量重构

- `app-ui/src/views/TaskWizardView.vue` 已完成产品级轻量重构：顶部标题区、紧凑 6 步步骤条、左侧当前步骤配置区、右侧实时任务摘要区和映射空态
- 保持现有 6 步流程、路由、接口、保存逻辑和字段映射生成逻辑不变，只做局部布局与状态包装
- 保存按钮在 Step 1 ~ Step 5 保持禁用并提供提示，Step 6 在配置完整时启用
- 右侧摘要区新增完成度、当前状态、配置摘要和映射空状态，表格区域改为独立滚动容器，避免底部被截断
- 已完成前端 `build` 验证；本次前端 `lint` / `typecheck` 未提供对应脚本
- 已完成浏览器级页面核验并保存 before/after 截图

### 2026-06-21 SQL 编辑器 Workbench 收口

- `app-ui/src/views/SqlEditorView.vue` 已完成产品级轻量重构：顶部 Header、4 项状态概览、左侧 SQL Console、右侧结果 / 消息 / 错误 / 历史 Tabs、底部最近执行记录
- 保持现有 SQL 执行接口、危险语句限制和日志查询逻辑不变，仅增强状态展示、结果摘要和错误摘要
- 新增执行状态拆分、数据源上下文展示、安全策略展示和被拦截态文案，未执行状态不再混淆为失败
- 右侧结果区和底部历史区已改为独立滚动容器，减少窗口下被截断的风险
- 已完成前端 `build` 验证；`app-ui` 当前没有 `lint` / `typecheck` 脚本
- 已完成外部 Chrome 页面核验并保存 `before` / `after` 截图

## 部分完成

- P2-3 的最终收尾校验仍需要继续验证
- 需要确认监控图表、告警触发、去重、过滤、发送结果展示是否都达到最终预期
- 需要继续跑后端测试、前端构建、必要时做 Tauri 构建检查
- P2-4 的交付性/observability/UX 收尾已做最小修补，但仍建议在目标平台做一次完整打包回归
- P2-4 稳定性收尾已补充 datasource 连接测试空值防护、元数据扫描空值防护，以及数据预览 / SQL 编辑器 / 表结构比较的错误提示与确认拦截
- P2-5-1 的首启状态目前使用前端本地 `localStorage`，未新增 SQLite 表结构或迁移；如果后续要把首启/帮助偏好统一纳入本地配置或 SQLite，需要再单独评估迁移方案

## 未完成

- P3：CDC 实时同步、MySQL Binlog、PostgreSQL WAL、达梦日志解析、数据转换脚本引擎、AI 同步配置助手、模板市场、团队协作权限系统

## 当前已知实现位置

- 后端入口：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- HTTP 服务：`app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
- 运行监控：`app-model/src/main/java/com/dbsyncstudio/model/monitoring/vo/MonitoringOverviewVO.java`
- 监控趋势：`app-model/src/main/java/com/dbsyncstudio/model/monitoring/vo/MonitoringTrendVO.java`
- SQLite 初始化：`app-store/src/main/java/com/dbsyncstudio/store/sqlite/DatabaseSchemaInitializer.java`
- 监控迁移：`app-store/src/main/resources/db/migration/V11__monitoring_metrics_p2_3.sql`
- 告警迁移：`app-store/src/main/resources/db/migration/V11__alert_storage_p2_3.sql`
- 前端运行监控页：`app-ui/src/views/RunMonitoringView.vue`
- 前端告警设置页：`app-ui/src/views/AlertSettingsView.vue`
- 前端告警历史页：`app-ui/src/views/AlertHistoryView.vue`

## 验证状态

- 需要以后端测试、前端构建、Tauri 构建检查作为最终验证
- 当前文档只记录状态，不替代实际验证结果
- 2026-06-19: `mvn -pl app-core -am -DskipTests compile` 成功
- 2026-06-19: `npm run build` in `app-ui` 成功，存在现有 chunk size warning
- 2026-06-19: `mvn -pl app-core -am -DfailIfNoTests=false -Dtest=DesktopBackendServiceScheduleTest test` 成功
- 2026-06-22: 左侧导航继续补齐了 `connection-test`、`task-create`、`sync-progress`、`logs-directory`、`version-info`、`sqlite-status` 等别名路由，前端 `npm --prefix app-ui run build` 已通过，Chrome 已复核 `连接测试`、`创建同步任务`、`刷新后自动展开` 与侧栏滚动
- 2026-06-23: Maven 编译基线已切到 Java 17；使用 JDK 17 运行 `JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin:$PATH mvn -pl app-core -am -DskipTests compile` 已通过
- 2026-06-23: `scripts/package.js` 已改为自动选择 JDK 17 并默认跳过后端测试，`scripts/package.sh` 已成功打出 macOS release 到 `release/macos`
- 2026-06-23: `ScriptJsTransformerTest` 的只读输入写入断言已按预期通过；默认 `node scripts/package.js macos` 已成功产出 `release/macos`，带测试模式仍会受真实 MySQL 集成测试环境影响
- 2026-06-23: GitHub release workflow 的 Windows job 需要显式安装 Maven 才能跑 `scripts/package.js windows`
- 2026-06-23: Windows 打包脚本在重建 Java 环境时需要同时保留 `Path` 和 `PATH`，否则会把 Maven 路径冲掉并导致 `mvn` 不可用
- 2026-06-23: `scripts/package-env.test.js` 已补上并通过，锁定 Windows `Path` / `PATH` 回归
- 2026-06-23: Windows desktop shell 启动日志已改为写入 `~/.db-sync-studio/logs/startup.log`（Windows 实际路径为 `APPDATA\.db-sync-studio\logs\startup.log`），后端 stdout/stderr 也会一起写入
