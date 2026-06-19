package com.dbsyncstudio.store.sqlite;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalSecretKeyProvider {

    private static final int KEY_LENGTH_BYTES = 32;

    @NonNull
    private final File keyFile;

    public SecretKey loadOrCreateKey() throws SQLException {
        ensureParentDirectoryExists();
        if (keyFile.exists()) {
            return readKey();
        }
        return createKey();
    }

    public File getKeyFile() {
        return keyFile;
    }

    private SecretKey readKey() throws SQLException {
        try {
            byte[] encoded = Files.readAllBytes(keyFile.toPath());
            byte[] decoded = Base64.getDecoder().decode(new String(encoded, StandardCharsets.UTF_8).trim());
            return new SecretKeySpec(decoded, "AES");
        } catch (IOException ex) {
            throw new SQLException("Failed to read local alert secret key", ex);
        }
    }

    private SecretKey createKey() throws SQLException {
        byte[] keyBytes = new byte[KEY_LENGTH_BYTES];
        new SecureRandom().nextBytes(keyBytes);
        String encoded = Base64.getEncoder().encodeToString(keyBytes);
        try {
            Files.write(keyFile.toPath(), encoded.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IOException ex) {
            throw new SQLException("Failed to create local alert secret key", ex);
        }
    }

    private void ensureParentDirectoryExists() throws SQLException {
        File parent = keyFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new SQLException("Failed to create alert secret key directory: " + parent.getAbsolutePath());
        }
    }
}
