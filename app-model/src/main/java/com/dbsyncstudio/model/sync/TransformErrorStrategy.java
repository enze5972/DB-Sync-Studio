package com.dbsyncstudio.model.sync;

public enum TransformErrorStrategy {

    FAIL,
    USE_ORIGINAL,
    USE_DEFAULT,
    SET_NULL;

    public static TransformErrorStrategy fromValue(String value) {
        if (value == null || value.trim().length() == 0) {
            return FAIL;
        }
        for (TransformErrorStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(value.trim())) {
                return strategy;
            }
        }
        return FAIL;
    }
}
