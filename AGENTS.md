# Codex Work Rules

This file only defines how Codex should work in this repository.

## Context Policy

- Do not default to scanning the whole repository.
- Start every task by reading `docs/codebase-memory` context files first.
- Prefer this order:
  1. `docs/codebase-memory/CONTEXT_COMPACT.md`
  2. `docs/codebase-memory/NEXT_TASKS.md`
  3. `docs/codebase-memory/CODE_INDEX.md`
  4. `docs/codebase-memory/DECISIONS.md`
- Read only source files directly related to the current task.
- Initial source reads must stay within 8 files.
- If more context is needed, stop first and say which files still need to be read and why.

## Token Saving Rules

- Do not output full file contents.
- Do not write long explanations unless the user explicitly asks for them.
- Keep exploration narrow and incremental.
- Prefer existing code paths, existing docs, and existing conventions.
- Avoid repeated broad searches across the repo.

## Development Rules

- Make the smallest correct change.
- Do not refactor unrelated code.
- Do not change business code unless the task requires it.
- Verify assumptions before editing.
- Keep changes focused on the current task slice only.
- After each completed phase, update the relevant `docs/codebase-memory` files.

## Codebase Memory

- Treat `docs/codebase-memory` as the shared project memory.
- Read memory before opening unrelated source files.
- Keep memory updates short, factual, and task-oriented.
- If a memory document is missing, note that clearly instead of guessing.
- Use memory to avoid redoing prior discovery work.

## Skill Usage Policy

- Default to the minimum skill set needed for the task.
- For normal development tasks, prefer these skills when relevant:
  - `codebase-memory-mcp-intelligence`
  - `executing-plans`
  - `verification-before-completion`
  - `systematic-debugging`
- Use these skills only for larger or clearly parallelizable tasks:
  - `subagent-driven-development`
  - `dispatching-parallel-agents`
  - `taskmaster`
  - `using-git-worktrees`
- Do not enable these skills by default for ordinary code tasks:
  - `browser` / `chrome` / `playwright`
  - `pdf` / `documents` / `spreadsheets` / `presentations`
  - `imagegen`
  - `plugin-creator`
  - `skill-creator`
  - `skill-installer`
- If a task needs an extra skill, say why before using it.

## Task Output Format

- Keep task updates short.
- Report only:
  - what changed
  - which files changed
  - test or build status
  - whether codebase-memory was updated
  - next-step suggestion
- Do not include full diffs or full file contents.
- Do not write long retrospectives.
