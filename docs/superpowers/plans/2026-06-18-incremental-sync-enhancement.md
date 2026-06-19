# 增量同步增强接线 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将任务表、任务向导、SQLite 持久化和 Java 增量同步引擎真正接通，让增量模式、增量字段和断点恢复能够从任务配置中读取并稳定执行。

**Architecture:** 保持现有 `SyncTask` 作为任务配置的唯一入口，不新增调度或执行层。`app-store` 负责把增量字段持久化到 SQLite，`app-core` 负责把任务配置转换成 `IncrementalSyncRequest` 并按模式构造增量查询，`app-ui` 只补充表单和预览控件。断点仍写入现有的 `incremental_sync_checkpoint` 表，避免碰 P0 旧断点逻辑。

**Tech Stack:** Java 8-compatible core, JUnit 4, existing `HttpServer` JSON API, Vue 3, Element Plus, SQLite repositories already in place.

---

### Task 1: Make incremental task fields persist and load from SQLite

**Files:**
- Modify: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncTaskRepository.java`
- Modify: `app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java`
- Modify: `app-store/src/test/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializerTest.java`
- Modify: `app-store/src/test/java/com/dbsyncstudio/store/sync/SqliteSyncRepositoriesTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
public void shouldPersistIncrementalTaskFields() throws Exception {
    // create task with incrementalMode, incrementalColumnName, tieBreaker and composite values
    // save it, load it back, and assert all incremental fields survive round-trip
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl app-store -Dtest=SqliteSyncRepositoriesTest test`
Expected: fail because the repository still drops the incremental columns on save/load.

- [ ] **Step 3: Write minimal implementation**

Extend the INSERT/UPDATE/SELECT column lists and row mapper to include `incremental_mode`, `incremental_column_name`, `incremental_tie_breaker_column_name`, and `incremental_composite_column_name`. Keep the existing schema initializer migration-safe by adding missing columns if older databases are opened.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl app-store -Dtest=SqliteSyncRepositoriesTest test`
Expected: pass with the task fields round-tripping.

- [ ] **Step 5: Commit**

```bash
git add app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSyncTaskRepository.java \
  app-store/src/main/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializer.java \
  app-store/src/test/java/com/dbsyncstudio/store/sqlite/SqliteSchemaInitializerTest.java \
  app-store/src/test/java/com/dbsyncstudio/store/sync/SqliteSyncRepositoriesTest.java
git commit -m "feat: persist incremental task configuration"
```

### Task 2: Read task-level incremental settings in the backend service

**Files:**
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- Modify: `app-core/src/test/java/com/dbsyncstudio/core/sync/JdbcIncrementalSyncEngineTest.java`
- Modify: `app-core/src/test/java/com/dbsyncstudio/core/backend/DesktopBackendServiceTest.java` if a focused service test already exists or needs to be added

- [ ] **Step 1: Write the failing test**

Add a test that builds a `SyncTask` with `incrementalMode = AUTO_INCREMENT_ID`, `incrementalColumnName = "id"`, and verifies the service forwards those values into the generated `IncrementalSyncRequest` instead of hardcoding `updated_at`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl app-core -Dtest=JdbcIncrementalSyncEngineTest test`
Expected: fail because the current service still hardcodes the watermark column.

- [ ] **Step 3: Write minimal implementation**

Change the incremental sync branch in `DesktopBackendService` to read `SyncTask.incrementalMode`, `incrementalColumnName`, `incrementalTieBreakerColumnName`, and `incrementalCompositeColumnName`, falling back to sensible defaults only when a field is blank.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl app-core -Dtest=JdbcIncrementalSyncEngineTest test`
Expected: pass with the service forwarding the task settings.

- [ ] **Step 5: Commit**

```bash
git add app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java \
  app-core/src/test/java/com/dbsyncstudio/core/sync/JdbcIncrementalSyncEngineTest.java
git commit -m "feat: wire incremental settings into backend"
```

### Task 3: Add incremental mode controls to the task UI and wizard

**Files:**
- Modify: `app-ui/src/views/TaskView.vue`
- Modify: `app-ui/src/views/TaskWizardView.vue`
- Modify: `app-ui/src/services/backend.js` only if request/response helpers need a small shape adjustment

- [ ] **Step 1: Write the failing test**

Use the existing build as the check: the task editor should render incremental mode selection and related field inputs, and the wizard should include the same controls before save.

- [ ] **Step 2: Run a build to verify the current UI lacks the controls**

Run: `npm --prefix app-ui run build`
Expected: build succeeds, but the new controls are not present yet.

- [ ] **Step 3: Write minimal implementation**

Add Element Plus form controls for `none`, `timestamp`, `auto_increment_id`, and `composite`, plus inputs for the incremental field names. Keep the current style and layout, and only show the field inputs when the selected mode needs them.

- [ ] **Step 4: Run the build again to verify it passes**

Run: `npm --prefix app-ui run build`
Expected: pass with the new controls rendered.

- [ ] **Step 5: Commit**

```bash
git add app-ui/src/views/TaskView.vue app-ui/src/views/TaskWizardView.vue
git commit -m "feat: add incremental sync controls"
```

### Task 4: Verify backend tests, frontend build, and SQLite migration coverage

**Files:**
- No new files; validation only

- [ ] **Step 1: Run backend compilation and tests**

Run: `mvn -q -pl app-core -am -DskipTests test-compile && mvn -q -pl app-store -Dtest=SqliteSyncRepositoriesTest test && mvn -q -pl app-core -Dtest=JdbcIncrementalSyncEngineTest test`
Expected: Java compilation and the focused incremental tests pass.

- [ ] **Step 2: Run frontend build**

Run: `npm --prefix app-ui run build`
Expected: production bundle still builds.

- [ ] **Step 3: Report validation status**

Summarize the changed files, the SQLite migration impact, how to run the flow, and what remains for the next P1 sub-step.
