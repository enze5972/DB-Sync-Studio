package com.dbsyncstudio.model.sync.dto;

public class TaskBatchTableDTO {

    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    private String syncMode;
    private String incrementalMode;
    private String incrementalColumnName;
    private String incrementalTieBreakerColumnName;
    private String incrementalCompositeColumnName;
    private Integer batchSize;

    public String getSourceSchemaName() {
        return sourceSchemaName;
    }

    public void setSourceSchemaName(String sourceSchemaName) {
        this.sourceSchemaName = sourceSchemaName;
    }

    public String getSourceTableName() {
        return sourceTableName;
    }

    public void setSourceTableName(String sourceTableName) {
        this.sourceTableName = sourceTableName;
    }

    public String getTargetSchemaName() {
        return targetSchemaName;
    }

    public void setTargetSchemaName(String targetSchemaName) {
        this.targetSchemaName = targetSchemaName;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public String getIncrementalMode() {
        return incrementalMode;
    }

    public void setIncrementalMode(String incrementalMode) {
        this.incrementalMode = incrementalMode;
    }

    public String getIncrementalColumnName() {
        return incrementalColumnName;
    }

    public void setIncrementalColumnName(String incrementalColumnName) {
        this.incrementalColumnName = incrementalColumnName;
    }

    public String getIncrementalTieBreakerColumnName() {
        return incrementalTieBreakerColumnName;
    }

    public void setIncrementalTieBreakerColumnName(String incrementalTieBreakerColumnName) {
        this.incrementalTieBreakerColumnName = incrementalTieBreakerColumnName;
    }

    public String getIncrementalCompositeColumnName() {
        return incrementalCompositeColumnName;
    }

    public void setIncrementalCompositeColumnName(String incrementalCompositeColumnName) {
        this.incrementalCompositeColumnName = incrementalCompositeColumnName;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }
}
