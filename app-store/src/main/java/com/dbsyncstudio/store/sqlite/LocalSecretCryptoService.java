package com.dbsyncstudio.store.sqlite;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalSecretCryptoService {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    @NonNull
    private final LocalSecretKeyProvider keyProvider;

    public String encrypt(String plaintext) throws SQLException {
        if (plaintext == null || plaintext.trim().isEmpty()) {
            return null;
        }
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keyProvider.loadOrCreateKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new SQLException("Failed to encrypt alert secret", ex);
        }
    }

    public String decrypt(String ciphertext) throws SQLException {
        if (ciphertext == null || ciphertext.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] encrypted = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);

            SecretKey key = keyProvider.loadOrCreateKey();
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new SQLException("Failed to decrypt alert secret", ex);
        }
    }
}
