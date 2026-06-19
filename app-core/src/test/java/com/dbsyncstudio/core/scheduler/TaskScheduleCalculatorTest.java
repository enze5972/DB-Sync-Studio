package com.dbsyncstudio.core.scheduler;

import com.dbsyncstudio.model.sync.SyncTask;

import org.junit.Assert;
import org.junit.Test;

public class TaskScheduleCalculatorTest {

    @Test
    public void shouldComputeNextRunForCronExpression() {
        SyncTask task = SyncTask.builder()
                .scheduleEnabled(Boolean.TRUE)
                .scheduleType("CRON")
                .scheduleCronExpression("0 9 * * *")
                .build();

        long baseMillis = 1760933700000L;
        long nextRunAt = TaskScheduleCalculator.computeNextRunAt(task, baseMillis);

        Assert.assertTrue(nextRunAt > baseMillis);
        Assert.assertEquals(0L, nextRunAt % (60L * 1000L));
    }

    @Test
    public void shouldComputeNextRunForInterval() {
        SyncTask task = SyncTask.builder()
                .scheduleEnabled(Boolean.TRUE)
                .scheduleType("INTERVAL")
                .scheduleIntervalSeconds(Integer.valueOf(60))
                .scheduleLastRunAt(Long.valueOf(1000L))
                .build();

        long nextRunAt = TaskScheduleCalculator.computeNextRunAt(task, 2000L);

        Assert.assertEquals(61000L, nextRunAt);
    }
}
