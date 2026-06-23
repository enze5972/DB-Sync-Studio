package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.settings.vo.AppBuildInfoVO;

public final class RuntimeUtils {

    private static final String PRODUCT_NAME = "DB Sync Studio";
    private static final String APP_VERSION = "0.1.0";
    private static final String JAVA_CORE_VERSION = "0.1.0";
    private static final String FRONTEND_VERSION = "0.1.0";
    private static final String TAURI_VERSION = "1.6.0";
    private static final String BUILD_TIME = "待构建时注入";

    private RuntimeUtils() {
    }

    public static AppBuildInfoVO buildInfo(int schemaVersion, String gitCommit) {
        return AppBuildInfoVO.builder()
                .productName(PRODUCT_NAME)
                .appVersion(APP_VERSION)
                .frontendVersion(FRONTEND_VERSION)
                .javaCoreVersion(JAVA_CORE_VERSION)
                .tauriVersion(TAURI_VERSION)
                .sqliteSchemaVersion(Integer.valueOf(schemaVersion))
                .buildTime(BUILD_TIME)
                .gitCommit(gitCommit)
                .build();
    }

    public static String appVersion() {
        return APP_VERSION;
    }
}
