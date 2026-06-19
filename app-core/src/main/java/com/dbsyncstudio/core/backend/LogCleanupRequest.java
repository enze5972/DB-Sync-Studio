package com.dbsyncstudio.core.backend;

public class LogCleanupRequest {

    private Integer retentionDays;

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }
}
