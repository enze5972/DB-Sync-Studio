package com.dbsyncstudio.core.scheduler;

import com.dbsyncstudio.core.backend.DesktopBackendService;
import com.dbsyncstudio.model.sync.SyncTask;
import com.dbsyncstudio.model.sync.SyncTaskStatus;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskSchedulerService {

    private static final Logger LOGGER = Logger.getLogger(TaskSchedulerService.class.getName());

    private final DesktopBackendService backendService;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, Boolean> runningTasks;
    private volatile boolean started;

    public TaskSchedulerService(DesktopBackendService backendService) {
        this.backendService = backendService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.runningTasks = new ConcurrentHashMap<Long, Boolean>();
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                pollTasks();
            }
        }, 3L, 10L, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    public void resumeEnabledTasks() {
        pollTasks();
    }

    private void pollTasks() {
        try {
            List<SyncTask> tasks = backendService.listTasks();
            long now = System.currentTimeMillis();
            for (SyncTask task : safeList(tasks)) {
                if (!isEnabled(task)) {
                    continue;
                }
                Long nextRunAt = task.getScheduleNextRunAt();
                if (nextRunAt == null || nextRunAt.longValue() > now) {
                    continue;
                }
                trigger(task);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to poll scheduled tasks", ex);
        }
    }

    private void trigger(SyncTask task) {
        Long taskId = task.getId();
        if (taskId == null) {
            return;
        }
        if (runningTasks.putIfAbsent(taskId, Boolean.TRUE) != null) {
            appendScheduleLog(taskId.longValue(), "WARN", "Scheduled execution skipped because task is already running");
            backendService.handleScheduledSkip(taskId.longValue(), "Scheduled execution skipped because task is already running");
            return;
        }

        try {
            SyncTask current = backendService.findTaskById(taskId.longValue()).orElse(task);
            if (current.getTaskStatus() == SyncTaskStatus.RUNNING) {
                appendScheduleLog(taskId.longValue(), "WARN", "Scheduled execution skipped because task is already running");
                backendService.handleScheduledSkip(taskId.longValue(), "Scheduled execution skipped because task is already running");
                return;
            }
            long now = System.currentTimeMillis();
            current.setScheduleLastRunAt(Long.valueOf(now));
            current.setScheduleNextRunAt(Long.valueOf(TaskScheduleCalculator.computeNextRunAt(current, now)));
            current.setScheduleLastResult("RUNNING");
            current.setScheduleLastMessage("Scheduled execution triggered");
            backendService.saveTask(current);
            appendScheduleLog(taskId.longValue(), "INFO", "Scheduled execution started");
            backendService.startTask(taskId.longValue());
        } catch (SQLException ex) {
            try {
                SyncTask failed = backendService.findTaskById(taskId.longValue()).orElse(task);
                failed.setScheduleLastResult("FAILED");
                failed.setScheduleLastMessage(ex.getMessage());
                backendService.saveTask(failed);
            } catch (SQLException ignored) {
                // best effort
            }
            appendScheduleLog(taskId.longValue(), "ERROR", "Scheduled execution failed: " + ex.getMessage());
        } finally {
            runningTasks.remove(taskId);
        }
    }

    private boolean isEnabled(SyncTask task) {
        return task != null && task.getScheduleEnabled() != null && task.getScheduleEnabled().booleanValue();
    }

    private void appendScheduleLog(long taskId, String level, String message) {
        backendService.appendTaskLog(taskId, level, message);
    }

    private List<SyncTask> safeList(List<SyncTask> tasks) {
        if (tasks == null) {
            return new ArrayList<SyncTask>();
        }
        return tasks;
    }
}
