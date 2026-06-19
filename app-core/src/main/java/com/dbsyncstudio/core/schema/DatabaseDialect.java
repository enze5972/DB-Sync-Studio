package com.dbsyncstudio.core.schema;

import com.dbsyncstudio.model.datasource.DatasourceType;

public enum DatabaseDialect {
    MYSQL,
    POSTGRESQL,
    DM;

    public static DatabaseDialect from(DatasourceType type) {
        if (type == null) {
            return MYSQL;
        }
        if (type == DatasourceType.POSTGRESQL) {
            return POSTGRESQL;
        }
        if (type == DatasourceType.DM) {
            return DM;
        }
        return MYSQL;
    }
}
