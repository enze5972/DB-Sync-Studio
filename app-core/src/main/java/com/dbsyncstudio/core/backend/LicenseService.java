package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.settings.vo.AppLicenseInfoVO;
import com.dbsyncstudio.model.settings.AppLicenseStatus;
import com.dbsyncstudio.model.settings.vo.AppUpdateCheckResultVO;
import com.dbsyncstudio.model.settings.vo.AppUpdateSourceVO;
import com.dbsyncstudio.store.sqlite.LocalSecretCryptoService;
import com.dbsyncstudio.store.repository.sqlite.AppSettingsRepositoryImpl;

import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URL;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class LicenseService {

    private static final String LICENSE_KEY_SETTING = "license_key_encrypted";
    private static final String LICENSE_META_SETTING = "license_meta";
    private static final String LICENSE_TRIAL_START_SETTING = "license_trial_started_at";
    private static final String UPDATE_SOURCE_SETTING = "update_source_url";
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 5000;
    private static final String DEFAULT_LICENSE_PREFIX = "DBSS-";
    private static final long TRIAL_PERIOD_MILLIS = 14L * 24L * 60L * 60L * 1000L;

    private final AppSettingsRepositoryImpl settingsRepository;
    private final LocalSecretCryptoService cryptoService;

    public LicenseService(AppSettingsRepositoryImpl settingsRepository, LocalSecretCryptoService cryptoService) {
        this.settingsRepository = settingsRepository;
        this.cryptoService = cryptoService;
    }

    public AppLicenseInfoVO loadLicenseInfo() {
        String machineCode = buildMachineCode();
        String encryptedLicenseKey = settingsRepository == null ? null : settingsRepository.findRawValue(LICENSE_KEY_SETTING);
        String licenseKey = decryptQuietly(encryptedLicenseKey);
        String meta = settingsRepository == null ? null : settingsRepository.findRawValue(LICENSE_META_SETTING);
        Long trialStart = parseLong(settingsRepository == null ? null : settingsRepository.findRawValue(LICENSE_TRIAL_START_SETTING));
        AppLicenseInfoVO.AppLicenseInfoVOBuilder builder = AppLicenseInfoVO.builder()
                .machineCode(machineCode)
                .maskedLicenseKey(maskLicenseKey(licenseKey));
        if (licenseKey == null || licenseKey.trim().length() == 0) {
            if (trialStart == null) {
                persistTrialStart();
                return builder.status(AppLicenseStatus.UNLICENSED)
                        .message("未授权，可自动启用试用期")
                        .build();
            }
            long expiresAt = trialStart.longValue() + TRIAL_PERIOD_MILLIS;
            if (expiresAt < System.currentTimeMillis()) {
                return builder.status(AppLicenseStatus.EXPIRED)
                        .issuedAt(trialStart)
                        .expiresAt(Long.valueOf(expiresAt))
                        .message("试用期已过期")
                        .build();
            }
            return builder.status(AppLicenseStatus.TRIAL)
                    .issuedAt(trialStart)
                    .expiresAt(Long.valueOf(expiresAt))
                    .message("试用版")
                    .build();
        }
        if (!isLicenseFormatValid(licenseKey)) {
            return builder.status(AppLicenseStatus.UNLICENSED)
                    .message("授权码格式无效")
                    .build();
        }
        LicenseMeta licenseMeta = parseLicenseMeta(meta);
        if (licenseMeta != null && licenseMeta.expiresAt != null && licenseMeta.expiresAt.longValue() > 0L && licenseMeta.expiresAt.longValue() < System.currentTimeMillis()) {
            return builder.status(AppLicenseStatus.EXPIRED)
                    .licensedTo(licenseMeta.licensedTo)
                    .issuedAt(licenseMeta.issuedAt)
                    .expiresAt(licenseMeta.expiresAt)
                    .message("授权已过期")
                    .build();
        }
        if (licenseMeta != null && licenseMeta.licensedTo != null) {
            return builder.status(AppLicenseStatus.LICENSED)
                    .licensedTo(licenseMeta.licensedTo)
                    .issuedAt(licenseMeta.issuedAt)
                    .expiresAt(licenseMeta.expiresAt)
                    .message("已授权")
                    .build();
        }
        return builder.status(AppLicenseStatus.TRIAL)
                .message("试用版")
                .build();
    }

    public AppLicenseInfoVO activateLicense(String licenseKey) {
        if (!isLicenseFormatValid(licenseKey)) {
            return AppLicenseInfoVO.builder()
                    .status(AppLicenseStatus.UNLICENSED)
                    .machineCode(buildMachineCode())
                    .message("授权码格式无效")
                    .build();
        }
        long now = System.currentTimeMillis();
        LicenseMeta meta = LicenseMeta.fromLicenseKey(licenseKey);
        if (settingsRepository != null) {
            settingsRepository.saveRawValue(LICENSE_KEY_SETTING, encryptQuietly(licenseKey));
            settingsRepository.saveRawValue(LICENSE_META_SETTING, meta.toStorageValue());
        }
        return AppLicenseInfoVO.builder()
                .status(meta.expiresAt != null && meta.expiresAt.longValue() > 0L && meta.expiresAt.longValue() < now ? AppLicenseStatus.EXPIRED : AppLicenseStatus.LICENSED)
                .maskedLicenseKey(maskLicenseKey(licenseKey))
                .machineCode(buildMachineCode())
                .licensedTo(meta.licensedTo)
                .issuedAt(meta.issuedAt)
                .expiresAt(meta.expiresAt)
                .message("授权已保存")
                .build();
    }

    public AppLicenseInfoVO clearLicense() {
        if (settingsRepository != null) {
            settingsRepository.saveRawValue(LICENSE_KEY_SETTING, null);
            settingsRepository.saveRawValue(LICENSE_META_SETTING, null);
        }
        return loadLicenseInfo();
    }

    public String getMachineCode() {
        return buildMachineCode();
    }

    public AppUpdateCheckResultVO checkForUpdate() {
        String updateSourceUrl = settingsRepository == null ? null : settingsRepository.findRawValue(UPDATE_SOURCE_SETTING);
        if (updateSourceUrl == null || updateSourceUrl.trim().length() == 0) {
            return AppUpdateCheckResultVO.builder()
                    .available(false)
                    .latest(false)
                    .currentVersion(RuntimeUtils.appVersion())
                    .message("未配置更新源")
                    .build();
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(updateSourceUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            int code = connection.getResponseCode();
            if (code >= 200 && code < 300) {
                String body = readBody(connection);
                AppUpdateSourceVO source = parseUpdateSource(body);
                if (source == null || source.getLatestVersion() == null || source.getLatestVersion().trim().length() == 0) {
                    return AppUpdateCheckResultVO.builder()
                            .available(false)
                            .latest(false)
                            .currentVersion(RuntimeUtils.appVersion())
                            .message("更新源响应无效")
                            .build();
                }
                boolean latest = compareVersion(RuntimeUtils.appVersion(), source.getLatestVersion()) >= 0;
                return AppUpdateCheckResultVO.builder()
                        .available(!latest)
                        .latest(latest)
                        .currentVersion(RuntimeUtils.appVersion())
                        .latestVersion(source.getLatestVersion())
                        .releasedAt(source.getReleasedAt())
                        .releaseNotes(source.getReleaseNotes())
                        .downloadUrl(source.getDownloadUrl())
                        .message(latest ? "当前版本已是最新" : "发现新版本")
                        .build();
            }
            return AppUpdateCheckResultVO.builder()
                    .available(false)
                    .latest(false)
                    .currentVersion(RuntimeUtils.appVersion())
                    .message("更新检查失败")
                    .build();
        } catch (Exception ex) {
            return AppUpdateCheckResultVO.builder()
                    .available(false)
                    .latest(false)
                    .currentVersion(RuntimeUtils.appVersion())
                    .message("更新检查超时或失败")
                    .build();
        }
    }

    private String readBody(HttpURLConnection connection) throws Exception {
        try (Scanner scanner = new Scanner(connection.getInputStream(), "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private AppUpdateSourceVO parseUpdateSource(String body) throws Exception {
        if (body == null || body.trim().length() == 0) {
            return null;
        }
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        return objectMapper.readValue(body, AppUpdateSourceVO.class);
    }

    private boolean isLicenseFormatValid(String licenseKey) {
        if (licenseKey == null) {
            return false;
        }
        String normalized = licenseKey.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith(DEFAULT_LICENSE_PREFIX) && normalized.length() >= 20;
    }

    private String maskLicenseKey(String licenseKey) {
        if (licenseKey == null || licenseKey.trim().length() == 0) {
            return "";
        }
        String value = licenseKey.trim();
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    private String decryptQuietly(String encrypted) {
        if (encrypted == null || encrypted.trim().length() == 0) {
            return null;
        }
        try {
            return cryptoService == null ? encrypted : cryptoService.decrypt(encrypted);
        } catch (Exception ex) {
            return encrypted;
        }
    }

    private String encryptQuietly(String plaintext) {
        if (plaintext == null || plaintext.trim().length() == 0) {
            return null;
        }
        try {
            return cryptoService == null ? plaintext : cryptoService.encrypt(plaintext);
        } catch (Exception ex) {
            return plaintext;
        }
    }

    private String buildMachineCode() {
        try {
            List<String> tokens = new ArrayList<String>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    byte[] hardwareAddress = networkInterface.getHardwareAddress();
                    if (hardwareAddress != null && hardwareAddress.length > 0) {
                        tokens.add(bytesToHex(hardwareAddress));
                    }
                }
            }
            if (tokens.isEmpty()) {
                tokens.add(System.getProperty("os.name", "unknown"));
                tokens.add(System.getProperty("os.arch", "unknown"));
            }
            Collections.sort(tokens);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String token : tokens) {
                digest.update(token.getBytes("UTF-8"));
            }
            byte[] hash = digest.digest();
            return formatMachineCode(hash);
        } catch (Exception ex) {
            byte[] fallback = new byte[16];
            new SecureRandom().nextBytes(fallback);
            return formatMachineCode(fallback);
        }
    }

    private void persistTrialStart() {
        if (settingsRepository == null) {
            return;
        }
        settingsRepository.saveRawValue(LICENSE_TRIAL_START_SETTING, String.valueOf(System.currentTimeMillis()));
    }

    private String formatMachineCode(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % 4 == 0) {
                builder.append('-');
            }
            builder.append(String.format(Locale.ROOT, "%02X", bytes[i]));
        }
        return builder.toString();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02X", value));
        }
        return builder.toString();
    }

    private int compareVersion(String currentVersion, String latestVersion) {
        String[] currentParts = splitVersion(currentVersion);
        String[] latestParts = splitVersion(latestVersion);
        int max = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < max; i++) {
            int current = i < currentParts.length ? parsePart(currentParts[i]) : 0;
            int latest = i < latestParts.length ? parsePart(latestParts[i]) : 0;
            if (current != latest) {
                return current - latest;
            }
        }
        return 0;
    }

    private String[] splitVersion(String version) {
        if (version == null) {
            return new String[0];
        }
        return version.replaceAll("[^0-9.]", "").split("\\.");
    }

    private int parsePart(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return 0;
        }
    }

    private LicenseMeta parseLicenseMeta(String meta) {
        if (meta == null || meta.trim().length() == 0) {
            return null;
        }
        String[] parts = meta.split("\\|", -1);
        if (parts.length < 3) {
            return null;
        }
        LicenseMeta licenseMeta = new LicenseMeta();
        licenseMeta.licensedTo = emptyToNull(parts[0]);
        licenseMeta.issuedAt = parseLong(parts[1]);
        licenseMeta.expiresAt = parseLong(parts[2]);
        return licenseMeta;
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    private Long parseLong(String value) {
        try {
            return Long.valueOf(Long.parseLong(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private static final class LicenseMeta {
        private String licensedTo;
        private Long issuedAt;
        private Long expiresAt;

        private String toStorageValue() {
            return (licensedTo == null ? "" : licensedTo) + "|" +
                    (issuedAt == null ? "" : String.valueOf(issuedAt)) + "|" +
                    (expiresAt == null ? "" : String.valueOf(expiresAt));
        }

        private static LicenseMeta fromLicenseKey(String licenseKey) {
            LicenseMeta meta = new LicenseMeta();
            meta.licensedTo = "Local Trial";
            meta.issuedAt = Long.valueOf(System.currentTimeMillis());
            meta.expiresAt = null;
            return meta;
        }
    }
}
