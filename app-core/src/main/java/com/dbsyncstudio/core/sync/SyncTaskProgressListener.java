package com.dbsyncstudio.core.sync;

public interface SyncTaskProgressListener {

    boolean isPauseRequested();

    boolean isStopRequested();

    void updateProgress(long totalRowCount, long syncedRowCount, long successRowCount, long failedRowCount,
                        double speedRowsPerSecond, Long startedAt, Long endedAt, Long durationMillis, String progressMessage);

    void saveCheckpoint(String checkpointKey, String checkpointValue);
}
