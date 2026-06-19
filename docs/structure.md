# DB Sync Studio 目录结构

## 推荐目录

```text
DB-Sync-Studio/
├─ README.md
├─ docs/
│  ├─ architecture.md
│  └─ structure.md
├─ app-model/
├─ app-store/
├─ app-core/
│  ├─ pom.xml
│  └─ src/
│     ├─ main/
│     │  ├─ java/
│     │  └─ resources/
│     └─ test/
├─ app-ui/
│  ├─ package.json
│  ├─ index.html
│  └─ src/
├─ app-shell/
│  ├─ package.json
│  └─ src-tauri/
├─ scripts/
│  ├─ package.sh
│  ├─ package.ps1
│  └─ package.bat
└─ release/
   └─ <platform>/
```

## 目录职责

### `app-core`

Java 同步引擎核心模块。

### `app-store`

SQLite 持久化与仓储层。

### `app-shell`

Tauri 桌面壳工程。

### `app-ui`

Vue 3 + Element Plus 前端界面。

### `scripts`

跨平台打包脚本。

### `release`

打包产物输出目录。

## 第一阶段最小可运行范围

建议先做：

- `app-core`：Java 启动 Demo
- `app-store`：预留接口和 SQLite 接入
- `app-ui`：首页占位
- `app-shell`：窗口容器
- `scripts`：跨平台打包脚本

