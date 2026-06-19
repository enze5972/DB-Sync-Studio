package com.dbsyncstudio.core.sync;

public class SyncTaskStoppedException extends RuntimeException {

    public SyncTaskStoppedException(String message) {
        super(message);
    }
}
