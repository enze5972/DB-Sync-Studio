package com.dbsyncstudio.model.schema;

public enum SchemaComparisonType {
    MISSING_COLUMN,
    EXTRA_COLUMN,
    TYPE_DIFF,
    LENGTH_DIFF,
    NULLABLE_DIFF,
    DEFAULT_DIFF,
    PRIMARY_KEY_DIFF,
    INDEX_DIFF
}
