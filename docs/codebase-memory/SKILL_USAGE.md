# SKILL_USAGE

## 默认规则

- 普通开发任务只使用必要 skill。
- 优先考虑：
  - `codebase-memory-mcp-intelligence`
  - `executing-plans`
  - `verification-before-completion`
  - `systematic-debugging`

## 大任务可按需使用

- `subagent-driven-development`
- `dispatching-parallel-agents`
- `taskmaster`
- `using-git-worktrees`

## 普通代码任务中不默认启用

- `browser` / `chrome` / `playwright`
- `pdf` / `documents` / `spreadsheets` / `presentations`
- `imagegen`
- `plugin-creator`
- `skill-creator`
- `skill-installer`

## 使用要求

- 任务需要额外 skill 时，先说明原因，再启用。
- 只使用和当前任务直接相关的 skill。
- 不为了“保险”而同时启用一堆无关 skill。
