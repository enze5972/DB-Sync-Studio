package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.validation.RepairDetail;
import com.dbsyncstudio.model.validation.RepairRun;
import com.dbsyncstudio.model.validation.ValidationDifference;
import com.dbsyncstudio.model.validation.ValidationRun;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class SqliteValidationRepairRepositoryTest {

    @Test
    public void shouldPersistValidationRunAndDifferences() throws Exception {
        File tempDatabase = resetTempDatabase("db-sync-studio-validation");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteValidationRepository repository = new SqliteValidationRepository(connectionFactory);
        repository.initialize();

        ValidationRun run = ValidationRun.builder()
                .taskId(Long.valueOf(12L))
                .runId("validation-run-1")
                .validationMethod("PRIMARY_KEY_EXISTS")
                .sourceTableName("users")
                .targetTableName("users_copy")
                .status("SUCCESS")
                .sourceRowCount(Long.valueOf(2L))
                .targetRowCount(Long.valueOf(1L))
                .missingCount(Long.valueOf(1L))
                .inconsistentCount(Long.valueOf(0L))
                .startedAt(Long.valueOf(100L))
                .endedAt(Long.valueOf(140L))
                .elapsedMillis(Long.valueOf(40L))
                .build();

        long runId = repository.saveRun(run);
        Assert.assertTrue(runId > 0L);

        ValidationDifference difference = ValidationDifference.builder()
                .validationRunId(Long.valueOf(runId))
                .taskId(Long.valueOf(12L))
                .runId("validation-run-1")
                .differenceType("MISSING_TARGET")
                .primaryKeyJson("{\"id\":2}")
                .sourceRowJson("{\"id\":2,\"name\":\"Ann\"}")
                .createdAt(Long.valueOf(150L))
                .build();
        long differenceId = repository.saveDifference(difference);
        Assert.assertTrue(differenceId > 0L);

        List<ValidationRun> recent = repository.findRecentRuns(10);
        Assert.assertEquals(1, recent.size());
        Assert.assertEquals(Long.valueOf(runId), recent.get(0).getId());
        Assert.assertEquals("validation-run-1", recent.get(0).getRunId());
        Assert.assertEquals(Long.valueOf(1L), recent.get(0).getMissingCount());

        List<ValidationDifference> differences = repository.findDifferencesByRunId(runId);
        Assert.assertEquals(1, differences.size());
        Assert.assertEquals(Long.valueOf(differenceId), differences.get(0).getId());
        Assert.assertEquals("MISSING_TARGET", differences.get(0).getDifferenceType());
        Assert.assertEquals("{\"id\":2}", differences.get(0).getPrimaryKeyJson());
    }

    @Test
    public void shouldPersistRepairRunAndDetails() throws Exception {
        File tempDatabase = resetTempDatabase("db-sync-studio-repair");
        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        SqliteRepairRepository repository = new SqliteRepairRepository(connectionFactory);
        repository.initialize();

        RepairRun run = RepairRun.builder()
                .validationRunId(Long.valueOf(7L))
                .taskId(Long.valueOf(12L))
                .runId("repair-run-1")
                .tableName("users_copy")
                .repairType("INSERT_MISSING")
                .status("PREVIEWED")
                .repairCount(Long.valueOf(2L))
                .successCount(Long.valueOf(0L))
                .failedCount(Long.valueOf(0L))
                .startTime(Long.valueOf(200L))
                .build();

        long runId = repository.saveRun(run);
        Assert.assertTrue(runId > 0L);

        RepairDetail detail = RepairDetail.builder()
                .repairRunId(Long.valueOf(runId))
                .validationDifferenceId(Long.valueOf(3L))
                .taskId(Long.valueOf(12L))
                .repairType("INSERT_MISSING")
                .primaryKeyJson("{\"id\":2}")
                .sqlPreview("INSERT INTO `users_copy` (`id`) VALUES (?)")
                .parameterJson("[2]")
                .status("PENDING")
                .createdAt(Long.valueOf(210L))
                .updatedAt(Long.valueOf(210L))
                .build();
        long detailId = repository.saveDetail(detail);
        Assert.assertTrue(detailId > 0L);

        List<RepairRun> recent = repository.findRecentRuns(10);
        Assert.assertEquals(1, recent.size());
        Assert.assertEquals(Long.valueOf(runId), recent.get(0).getId());
        Assert.assertEquals("PREVIEWED", recent.get(0).getStatus());

        List<RepairDetail> details = repository.findDetailsByRunId(runId);
        Assert.assertEquals(1, details.size());
        Assert.assertEquals(Long.valueOf(detailId), details.get(0).getId());
        Assert.assertEquals("INSERT_MISSING", details.get(0).getRepairType());
        Assert.assertEquals("[2]", details.get(0).getParameterJson());
    }

    private File resetTempDatabase(String prefix) throws Exception {
        File tempDatabase = File.createTempFile(prefix, ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();
        return tempDatabase;
    }
}
