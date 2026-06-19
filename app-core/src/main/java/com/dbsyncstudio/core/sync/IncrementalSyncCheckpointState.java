package com.dbsyncstudio.core.sync;

public class IncrementalSyncCheckpointState {

    private String watermarkValue;
    private String tieBreakerValue;
    private String compositeValue;

    public String getWatermarkValue() {
        return watermarkValue;
    }

    public void setWatermarkValue(String watermarkValue) {
        this.watermarkValue = watermarkValue;
    }

    public String getTieBreakerValue() {
        return tieBreakerValue;
    }

    public void setTieBreakerValue(String tieBreakerValue) {
        this.tieBreakerValue = tieBreakerValue;
    }

    public String getCompositeValue() {
        return compositeValue;
    }

    public void setCompositeValue(String compositeValue) {
        this.compositeValue = compositeValue;
    }
}
