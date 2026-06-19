package com.dbsyncstudio.core.sync;

public class SyncTaskPausedException extends RuntimeException {

    public SyncTaskPausedException(String message) {
        super(message);
    }
}
