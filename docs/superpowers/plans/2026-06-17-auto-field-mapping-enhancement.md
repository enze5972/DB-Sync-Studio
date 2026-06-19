# Auto Field Mapping Enhancement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add confidence-based automatic field mapping suggestions for the existing task wizard and mapping workflow without changing the current runnable or packaging paths.

**Architecture:** Keep the existing SQLite schema unchanged and reuse the current task, metadata, and field mapping flow. Add a small Java matcher engine in `app-core` that scores source-to-target column matches using normalization and common aliases, expose it through the existing JSON backend, and let the Vue wizard present the suggestions for user confirmation and editing before save.

**Tech Stack:** Java 8-compatible core, JUnit 4, existing `HttpServer` JSON API, Vue 3, Element Plus, SQLite repositories already in place.

---

### Task 1: Add a reusable Java field-matching engine and DTOs

**Files:**
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/FieldMappingSuggestion.java`
- Create: `app-model/src/main/java/com/dbsyncstudio/model/sync/FieldMappingSuggestionRequest.java`
- Create: `app-core/src/main/java/com/dbsyncstudio/core/mapping/FieldMappingSuggestionMatcher.java`
- Create: `app-core/src/test/java/com/dbsyncstudio/core/mapping/FieldMappingSuggestionMatcherTest.java`

- [ ] **Step 1: Write the failing test**

```java
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

public class FieldMappingSuggestionMatcherTest {
    @Test
    public void matchesCommonAliasesWithConfidence() {
        List<String> source = Arrays.asList("user_name", "phone", "create_time");
        List<String> target = Arrays.asList("username", "mobile", "gmt_create");
        List<FieldMappingSuggestion> suggestions = new FieldMappingSuggestionMatcher().match(source, target);
        // assert alias matches and confidence thresholds here
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl app-core -Dtest=FieldMappingSuggestionMatcherTest test`
Expected: fail because the matcher and DTOs do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Implement normalization, alias lookup, and confidence scoring for exact, alias, and fuzzy matches. Return immutable-style suggestion objects with source column, target column, confidence, and match reason.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl app-core -Dtest=FieldMappingSuggestionMatcherTest test`
Expected: pass with the alias cases covered.

- [ ] **Step 5: Commit**

```bash
git add app-model/src/main/java/com/dbsyncstudio/model/sync/FieldMappingSuggestion.java \
  app-model/src/main/java/com/dbsyncstudio/model/sync/FieldMappingSuggestionRequest.java \
  app-core/src/main/java/com/dbsyncstudio/core/mapping/FieldMappingSuggestionMatcher.java \
  app-core/src/test/java/com/dbsyncstudio/core/mapping/FieldMappingSuggestionMatcherTest.java
git commit -m "feat: add field mapping matcher"
```

### Task 2: Expose automatic mapping suggestions through the backend API

**Files:**
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java`
- Modify: `app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java`
- Modify: `app-core/src/test/java/com/dbsyncstudio/core/backend/DesktopBackendServiceTest.java` if needed for service coverage

- [ ] **Step 1: Write the failing test**

Add a service test that loads a task, scans source and target metadata, and verifies the returned suggestions include the alias matches and confidence values.

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn -pl app-core test`
Expected: fail because the suggestion API does not exist yet.

- [ ] **Step 3: Write minimal implementation**

Add a backend service method that resolves the task, scans metadata, finds the two tables, and calls the matcher. Add a new `/api/mappings/suggest` endpoint that returns the suggestion list as JSON.

- [ ] **Step 4: Run the test to verify it passes**

Run: `mvn -pl app-core test`
Expected: pass with the new endpoint and service method.

- [ ] **Step 5: Commit**

```bash
git add app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendService.java \
  app-core/src/main/java/com/dbsyncstudio/core/backend/DesktopBackendServer.java
git commit -m "feat: expose field mapping suggestions"
```

### Task 3: Update the task wizard to preview and edit suggestions inline

**Files:**
- Modify: `app-ui/src/services/backend.js`
- Modify: `app-ui/src/views/TaskWizardView.vue`

- [ ] **Step 1: Write the failing test**

Use the existing wizard view behavior as the acceptance target: the mapping table should display confidence and allow the target column and ignored flag to be edited before save.

- [ ] **Step 2: Run a build check to verify the current UI does not have the new controls**

Run: `npm --prefix app-ui run build`
Expected: build succeeds, but the new confidence/editing behavior is not present yet.

- [ ] **Step 3: Write minimal implementation**

Call the new suggestion endpoint after both tables are chosen, render confidence and match reason in the mapping table, and allow the user to modify target column / ignored state before saving.

- [ ] **Step 4: Run the build again to verify it passes**

Run: `npm --prefix app-ui run build`
Expected: pass with the new controls rendered in the wizard.

- [ ] **Step 5: Commit**

```bash
git add app-ui/src/services/backend.js app-ui/src/views/TaskWizardView.vue
git commit -m "feat: preview auto field mappings in wizard"
```

### Task 4: Verify the full project still compiles and tests cleanly

**Files:**
- No new files; validation only

- [ ] **Step 1: Run backend tests**

Run: `mvn test`
Expected: all existing Java tests plus the new matcher coverage pass.

- [ ] **Step 2: Run frontend build**

Run: `npm --prefix app-ui run build`
Expected: production bundle still builds.

- [ ] **Step 3: Summarize status**

Report which files changed, what the new suggestion flow does, and any remaining gaps left for later stages.

