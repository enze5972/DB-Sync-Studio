# MODULE_PACKAGING

## 模块用途

统一构建、打包、分发入口，覆盖前端构建、Tauri 构建和平台安装包生成。

## 关键目录

- `scripts`
- `app-shell`
- `release`

## 关键文件

- `scripts/package.js`
- `scripts/package.sh`
- `scripts/package.ps1`
- `scripts/package.bat`
- `scripts/generate-tauri-icons.js`
- `app-shell/package.json`
- `app-shell/src-tauri/tauri.conf.json`

## 核心类/组件/配置

- 平台化打包脚本
- 前端 build 前置命令
- Tauri bundle 配置
- 图标生成脚本

## 什么时候需要读取

- 需要改构建、打包、发布产物路径时
- 需要确认 macOS / Windows / Linux 包产物时
- 需要确认前端 build 和 Tauri build 的顺序时

## 什么时候不需要读取

- 只改业务页面或 SQLite 表结构时
- 只改同步引擎或调度逻辑时

## 已知问题

- 各平台打包脚本需要与本机环境匹配，待确认

## 后续待办

- 继续保持 build / package / bundle 流程稳定
- 继续保持图标和安装包配置跨平台可用
