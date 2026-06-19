# P2-1 Data Validation And Repair Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add enterprise data validation and safe target-data repair for DB Sync Studio without breaking P0/P1 sync, schema comparison, incremental sync, or scheduling.

**Architecture:** Add a focused validation/repair slice that follows the existing `app-model` / `app-store` / `app-core` / `app-ui` structure. Java engines perform database-compatible paged reads and Java-side hash normalization, SQLite repositories persist validation and repair history, and the Vue page calls local HTTP APIs through the existing backend service.

**Tech Stack:** Java 8-compatible source style compiled for Java 17 runtime, SQLite JDBC, Vue 3, Element Plus, Tauri 1.x.

---

## Files

- Create: `app-model/src/main/java/com/dbsyncstudio/model/validation/*`
- Create: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteValidationRepository.java`
- Create: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteRepairRepository.java`
- Create: `app-store/src/main/resources/db/migration/V7__validation_repair_p2_1.sql`
- Create: `app-core/src/main/java/com/dbsyncstudio/core/validation/*`
- Create: `app-ui/src/views/DataValidationView.vue`
- Modify: `app-store/src/main/resources/db/schema.sql`
- Modify: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
- Modify: `app-ui/src/services/backend.js`
- Modify: `app-ui/src/router/index.js`
- Modify: `app-ui/src/layouts/MainLayout.vue`
- Test: `app-store/src/test/java/com/dbsyncstudio/store/sqlite/SqliteValidationRepairRepositoryTest.java`
- Test: `app-core/src/test/java/com/dbsyncstudio/core/validation/DataValidationEngineTest.java`
- Test: `app-core/src/test/java/com/dbsyncstudio/core/validation/DataRepairEngineTest.java`

## Task 1: SQLite And Model Layer

- [ ] Add validation and repair model classes with Lombok builders, matching existing model style.
- [ ] Add `sync_validation_runs`, `sync_validation_differences`, `sync_repair_runs`, and `sync_repair_details` to `schema.sql`.
- [ ] Add `V7__validation_repair_p2_1.sql` with the same tables and indexes for existing installations.
- [ ] Add SQLite repositories with explicit column lists and paged history methods.
- [ ] Add repository tests using temporary SQLite files.

## Task 2: Validation Engine

- [ ] Add a Java-side SQL dialect helper for quoted identifiers and pagination.
- [ ] Implement row count validation with optional where clause text.
- [ ] Implement primary-key existence validation with single and composite primary keys, using paged reads.
- [ ] Implement random and primary-key-range sample validation.
- [ ] Implement row hash validation using Java MD5 and CRC32 normalization, without database-specific hash functions.
- [ ] Persist validation run status and differences with `task_id` and `run_id`.

## Task 3: Repair Engine

- [ ] Generate parameterized INSERT repair plans for missing target rows.
- [ ] Generate parameterized UPDATE repair plans for selected inconsistent fields only.
- [ ] Mark extra target rows and generate DELETE suggestion SQL, but do not execute by default.
- [ ] Execute confirmed INSERT / UPDATE repair details with prepared statements.
- [ ] Require explicit delete confirmation before executing DELETE details.
- [ ] Persist repair run and repair detail status.

## Task 4: Backend API

- [ ] Add API endpoints for starting validation, listing validation history, listing differences, generating repair plans, executing repairs, and listing repair history.
- [ ] Resolve task datasource and table configuration from existing `sync_task`.
- [ ] Keep dangerous operations opt-in and return clear errors when task/table metadata is incomplete.

## Task 5: Frontend

- [ ] Add `DataValidationView.vue` with task selection, validation mode selection, where/sample/hash configuration, and run button.
- [ ] Show status, source count, target count, missing count, inconsistent count, elapsed time, and differences table.
- [ ] Add repair plan preview, selectable repair details, delete-risk warning, execute action, and repair history.
- [ ] Add router, sidebar menu item, and backend service functions.

## Task 6: Verification

- [ ] Run `mvn -q -pl app-core,app-store -am test`.
- [ ] Run `npm --prefix app-ui run build`.
- [ ] Run `npm --prefix app-shell run tauri:build` if the current environment supports it.
- [ ] Update final response with changed files, SQLite changes, run/test commands, and remaining work.
