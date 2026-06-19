package com.dbsyncstudio.core;

import com.dbsyncstudio.core.bootstrap.ApplicationBootstrap;
import com.dbsyncstudio.core.backend.DesktopBackendServer;
import com.dbsyncstudio.core.backend.DesktopBackendService;
import com.dbsyncstudio.core.bootstrap.AppPaths;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DbSyncStudioApplication {

    private static final Logger LOGGER = Logger.getLogger(DbSyncStudioApplication.class.getName());

    public static void main(String[] args) {
        if (hasArgument(args, "--server")) {
            startServer(args);
            return;
        }
        new ApplicationBootstrap().start(args);
    }

    private static void startServer(String[] args) {
        int port = resolvePort(args);
        File databaseFile = AppPaths.databaseFile();
        try {
            DesktopBackendService backendService = DesktopBackendService.createDefault(databaseFile);
            DesktopBackendServer server = new DesktopBackendServer(backendService);
            int actualPort = server.start(port);
            LOGGER.log(Level.INFO, "DB Sync Studio backend listening on http://127.0.0.1:{0}", Integer.valueOf(actualPort));
            server.await();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to start DB Sync Studio backend server", ex);
        }
    }

    private static boolean hasArgument(String[] args, String expected) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (expected.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static int resolvePort(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--port=")) {
                    String value = arg.substring("--port=".length());
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        // fall through to default
                    }
                }
            }
        }
        return 18444;
    }
}
