# MODULE_TAURI

## 模块用途

Tauri 桌面壳，负责启动前端、拉起后端、暴露桌面命令和打包入口。

## 关键目录

- `app-shell/src-tauri`
- `app-shell/package.json`
- `app-shell/src-tauri/icons`

## 关键文件

- `app-shell/src-tauri/src/main.rs`
- `app-shell/src-tauri/src/backend.rs`
- `app-shell/src-tauri/tauri.conf.json`
- `app-shell/package.json`
- `app-shell/src-tauri/Cargo.toml`

## 核心类/组件/配置

- Tauri command 定义
- 后端地址获取逻辑
- `beforeDevCommand` / `beforeBuildCommand`
- bundle 配置、窗口尺寸、图标、跨平台打包配置

## 什么时候需要读取

- 需要改桌面启动、窗口、后端拉起、打包配置时
- 需要确认前端 dev/build 和 Tauri 联动时
- 需要确认 macOS / Windows / Linux 打包参数时

## 什么时候不需要读取

- 只改前端页面内容时
- 只改后端同步、SQLite、调度时

## 已知问题

- 打包配置和命令链路需要保持与前端构建一致，待确认

## 后续待办

- 继续保持跨平台 bundle 配置可用
- 继续保持后端启动链路稳定
