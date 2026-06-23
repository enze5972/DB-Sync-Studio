package com.dbsyncstudio.core.scheduler;

import com.dbsyncstudio.model.sync.entity.SyncTaskDO;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class TaskScheduleCalculator {

    private TaskScheduleCalculator() {
    }

    public static long computeNextRunAt(SyncTaskDO task, long nowMillis) {
        if (task == null || task.getScheduleEnabled() == null || !task.getScheduleEnabled().booleanValue()) {
            return 0L;
        }

        String scheduleType = task.getScheduleType() == null ? "" : task.getScheduleType().trim().toUpperCase();
        if ("INTERVAL".equals(scheduleType)) {
            Integer intervalSeconds = task.getScheduleIntervalSeconds();
            if (intervalSeconds == null || intervalSeconds.intValue() <= 0) {
                return 0L;
            }
            Long lastRunAt = task.getScheduleLastRunAt();
            long base = lastRunAt == null ? nowMillis : lastRunAt.longValue();
            return base + (intervalSeconds.longValue() * 1000L);
        }

        if ("CRON".equals(scheduleType)) {
            String expression = task.getScheduleCronExpression();
            if (expression == null || expression.trim().length() == 0) {
                return 0L;
            }
            return computeNextCronAt(expression, nowMillis);
        }

        return 0L;
    }

    private static long computeNextCronAt(String expression, long nowMillis) {
        String[] parts = expression.trim().split("\\s+");
        if (parts.length != 5) {
            return 0L;
        }
        LocalDateTime start = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
                .toLocalDateTime()
                .withSecond(0)
                .withNano(0)
                .plusMinutes(1L);
        for (int i = 0; i < 366 * 24 * 60; i++) {
            LocalDateTime candidate = start.plusMinutes(i);
            if (matches(parts, candidate)) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
        }
        return 0L;
    }

    private static boolean matches(String[] parts, LocalDateTime candidate) {
        return matchesPart(parts[0], candidate.getMinute())
                && matchesPart(parts[1], candidate.getHour())
                && matchesPart(parts[2], candidate.getDayOfMonth())
                && matchesPart(parts[3], candidate.getMonthValue())
                && matchesDayOfWeek(parts[4], candidate.getDayOfWeek().getValue() % 7);
    }

    private static boolean matchesPart(String part, int value) {
        if ("*".equals(part)) {
            return true;
        }
        return parseInt(part, Integer.MIN_VALUE) == value;
    }

    private static boolean matchesDayOfWeek(String part, int value) {
        if ("*".equals(part)) {
            return true;
        }
        int parsed = parseInt(part, Integer.MIN_VALUE);
        if (parsed == 7) {
            parsed = 0;
        }
        return parsed == value;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
