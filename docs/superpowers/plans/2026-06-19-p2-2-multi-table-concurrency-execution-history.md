# P2-2 Multi-Table Concurrency And Execution History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-table concurrent sync execution and richer execution history tracking to DB Sync Studio without breaking the existing single-table sync, scheduling, validation, repair, or SQL execution flows.

**Architecture:** Keep the existing `SyncTask` and single-table engines as the stable baseline. Add a task-group layer on top that introduces per-table execution records, per-run history, and per-run logs while reusing the current SQLite/JDBC persistence style. Java orchestration owns concurrency, pause/resume/stop coordination, run id propagation, and summary aggregation; Vue pages only surface the new table-level and run-level information through the existing Element Plus patterns.

**Tech Stack:** Java 17 runtime with Java 8-compatible source style, SQLite JDBC, existing `HttpServer` JSON API, Vue 3, Element Plus, Tauri 1.x.

---

## Files

- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncTaskTable.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncTaskTableRepository.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncRun.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncRunRepository.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncTableRun.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncTableRunRepository.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncRunLogEntry.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/SyncRunLogRepository.java`
- Create: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncTaskTableRepository.java`
- Create: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncRunRepository.java`
- Create: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncTableRunRepository.java`
- Create: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncRunLogRepository.java`
- Create: `app-store/src/main/resources/db/migration/V8__multi_table_sync_run_history_p2_2.sql`
- Modify: `app-store/src/main/resources/db/schema.sql`
- Modify: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java`
- Modify: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncTaskRepository.java`
- Modify: `app-store/src/main/java/com/dbsyncstudio/store/sync/SqliteExecutionLogRepository.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcFullSyncEngine.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/sync/JdbcIncrementalSyncEngine.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/scheduler/TaskSchedulerService.java`
- Modify: `app-ui/src/services/backend.js`
- Modify: `app-ui/src/router/index.js`
- Modify: `app-ui/src/layouts/MainLayout.vue`
- Modify: `app-ui/src/views/TaskView.vue`
- Create: `app-ui/src/views/ExecutionHistoryView.vue`
- Create: `app-ui/src/views/TaskRunDetailView.vue`
- Test: `app-store/src/test/java/com/dbsyncstudio/store/sqlite/SqliteSyncRunRepositoryTest.java`
- Test: `app-store/src/test/java/com/dbsyncstudio/store/sqlite/SqliteSyncTaskTableRepositoryTest.java`
- Test: `app-core/src/test/java/com/dbsyncstudio/core/backend/DesktopBackendServiceMultiTableSyncTest.java`
- Test: `app-core/src/test/java/com/dbsyncstudio/core/backend/DesktopBackendServerExecutionHistoryApiTest.java`

## Task 1: SQLite Tables And Model Layer

- [ ] Add `SyncTaskTable`, `SyncRun`, `SyncTableRun`, and `SyncRunLogEntry` model classes with the same Lombok style as existing sync models.
- [ ] Add repository interfaces for task-table configuration, run history, table-run history, and run logs.
- [ ] Extend `schema.sql` and add `V8__multi_table_sync_run_history_p2_2.sql` with explicit tables and indexes for `sync_task_table`, `sync_run`, `sync_table_run`, and `sync_run_log`.
- [ ] Update `SqliteSchemaInitializer` so older installs gain the new columns/tables without breaking existing databases.
- [ ] Add repository tests using temporary SQLite files and explicit-column round trips.

## Task 2: Backend Multi-Table Execution

- [ ] Add a small orchestration layer in `DesktopBackendService` that groups table configs under a task, generates one `run_id` per execution, and stores run/table-run state.
- [ ] Implement concurrent table execution with a fixed-size executor and per-table isolation so one table failure does not stop the others.
- [ ] Keep single-table sync paths working by reusing the existing engines where no task-table list exists.
- [ ] Propagate `run_id` into validation and repair flows so history records stay correlated across features.
- [ ] Record per-table status, counts, checkpoints, start/end time, duration, and error text.
- [ ] Make pause/resume/stop honor the existing task control flags while allowing table work to finish the current batch safely.
- [ ] Add focused backend tests for aggregation, concurrency limits, and status summarization.

## Task 3: Execution History And Logs

- [ ] Add run history APIs for listing runs, reading run detail, listing per-table runs, and querying run logs by task, run id, table, level, time range, and keyword.
- [ ] Add log retention cleanup support that skips running tasks and honors a configurable default retention period.
- [ ] Reuse the existing logs surface where possible, but add `run_id` and `table_name` so P2-2 history can be traced end to end.
- [ ] Add server tests for the new history and log APIs.

## Task 4: Frontend History And Task Detail

- [ ] Update the task detail page so table-level progress, current checkpoint, and recent errors are visible.
- [ ] Add an execution history page with run summary cards, task/run filters, and a detail drawer or route.
- [ ] Add a run detail view for table-level progress, log snippets, validation/repair links, and retry entry points.
- [ ] Wire the new pages into the router, sidebar, and backend service module.
- [ ] Keep Element Plus styling consistent with the current task/log pages.

## Task 5: Verification

- [ ] Run `mvn test` or a focused Maven test set that covers the new repositories and backend orchestration.
- [ ] Run `npm --prefix app-ui run build`.
- [ ] Run `npm --prefix app-shell run tauri:build` if the current environment supports it.
- [ ] Update the final report with changed files, SQLite changes, run/test commands, and remaining work.
