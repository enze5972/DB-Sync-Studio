package com.dbsyncstudio.store.sqlite;

import com.dbsyncstudio.model.alert.AlertChannel;
import com.dbsyncstudio.model.alert.AlertChannelType;
import com.dbsyncstudio.model.alert.AlertDedupState;
import com.dbsyncstudio.model.alert.AlertHistoryEntry;
import com.dbsyncstudio.model.alert.AlertRule;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;

public class SqliteAlertRepositoryTest {

    @Test
    public void shouldPersistEncryptedAlertChannelsRulesHistoryAndDedupState() throws Exception {
        File tempDatabase = resetTempDatabase("db-sync-studio-alert");
        File tempConfigDirectory = createTempDirectory("db-sync-studio-alert-config");

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(tempDatabase);
        LocalSecretCryptoService cryptoService =
                new LocalSecretCryptoService(new LocalSecretKeyProvider(new File(tempConfigDirectory, "alert-secrets.key")));
        SqliteAlertRuleRepository ruleRepository = new SqliteAlertRuleRepository(connectionFactory);
        SqliteAlertChannelRepository channelRepository = new SqliteAlertChannelRepository(connectionFactory, cryptoService);
        SqliteAlertHistoryRepository historyRepository = new SqliteAlertHistoryRepository(connectionFactory);
        SqliteAlertDedupStateRepository dedupStateRepository = new SqliteAlertDedupStateRepository(connectionFactory);

        ruleRepository.initialize();
        channelRepository.initialize();
        historyRepository.initialize();
        dedupStateRepository.initialize();

        AlertRule rule = new AlertRule();
        rule.setRuleName("sync failed rule");
        rule.setAlertType("TASK_FAILURE");
        rule.setTaskId(Long.valueOf(101L));
        rule.setTableName("orders");
        rule.setAlertLevel("ERROR");
        rule.setAlertContentTemplate("Task ${taskId} failed");
        rule.setEnabled(Boolean.TRUE);
        rule.setCooldownSeconds(Integer.valueOf(600));

        long ruleId = ruleRepository.save(rule);
        Assert.assertTrue(ruleId > 0L);

        AlertChannel smtpChannel = new AlertChannel();
        smtpChannel.setChannelName("primary smtp");
        smtpChannel.setChannelType(AlertChannelType.SMTP);
        smtpChannel.setEnabled(Boolean.TRUE);
        smtpChannel.setSmtpHost("smtp.example.com");
        smtpChannel.setSmtpPort(Integer.valueOf(465));
        smtpChannel.setSmtpUsername("robot@example.com");
        smtpChannel.setSmtpPassword("smtp-secret");
        smtpChannel.setSmtpFromAddress("robot@example.com");

        long smtpChannelId = channelRepository.save(smtpChannel);
        Assert.assertTrue(smtpChannelId > 0L);

        AlertChannel webhookChannel = new AlertChannel();
        webhookChannel.setChannelName("ops webhook");
        webhookChannel.setChannelType(AlertChannelType.WEBHOOK);
        webhookChannel.setEnabled(Boolean.FALSE);
        webhookChannel.setWebhookUrl("https://hooks.example.com/notify");
        webhookChannel.setWebhookToken("webhook-token");

        long webhookChannelId = channelRepository.save(webhookChannel);
        Assert.assertTrue(webhookChannelId > 0L);

        assertEncryptedSecrets(connectionFactory, smtpChannelId, "smtp-secret", webhookChannelId, "webhook-token");

        Optional<AlertChannel> loadedSmtpChannel = channelRepository.findById(smtpChannelId);
        Assert.assertTrue(loadedSmtpChannel.isPresent());
        Assert.assertEquals(AlertChannelType.SMTP, loadedSmtpChannel.get().getChannelType());
        Assert.assertEquals("smtp-secret", loadedSmtpChannel.get().getSmtpPassword());

        Optional<AlertChannel> loadedWebhookChannel = channelRepository.findById(webhookChannelId);
        Assert.assertTrue(loadedWebhookChannel.isPresent());
        Assert.assertEquals(AlertChannelType.WEBHOOK, loadedWebhookChannel.get().getChannelType());
        Assert.assertEquals("webhook-token", loadedWebhookChannel.get().getWebhookToken());

        List<AlertChannel> enabledChannels = channelRepository.findEnabled();
        Assert.assertEquals(1, enabledChannels.size());
        Assert.assertEquals(Long.valueOf(smtpChannelId), enabledChannels.get(0).getId());

        AlertHistoryEntry historyEntry = new AlertHistoryEntry();
        historyEntry.setAlertId("alert-20260619-001");
        historyEntry.setRuleId(Long.valueOf(ruleId));
        historyEntry.setAlertType("TASK_FAILURE");
        historyEntry.setTaskId(Long.valueOf(101L));
        historyEntry.setRunId("run-20260619-001");
        historyEntry.setTableName("orders");
        historyEntry.setAlertLevel("ERROR");
        historyEntry.setAlertContent("Task 101 failed on orders");
        historyEntry.setChannelType(AlertChannelType.SMTP);
        historyEntry.setChannelId(Long.valueOf(smtpChannelId));
        historyEntry.setSendStatus("SUCCESS");
        historyEntry.setCreatedTime(Long.valueOf(1_718_888_000_000L));
        historyEntry.setSentTime(Long.valueOf(1_718_888_000_500L));

        long historyId = historyRepository.save(historyEntry);
        Assert.assertTrue(historyId > 0L);

        Optional<AlertHistoryEntry> loadedHistory = historyRepository.findById(historyId);
        Assert.assertTrue(loadedHistory.isPresent());
        Assert.assertEquals("alert-20260619-001", loadedHistory.get().getAlertId());
        Assert.assertEquals("SUCCESS", loadedHistory.get().getSendStatus());

        AlertDedupState dedupState = new AlertDedupState();
        dedupState.setDedupKey("TASK_FAILURE:101:orders:SMTP");
        dedupState.setRuleId(Long.valueOf(ruleId));
        dedupState.setAlertType("TASK_FAILURE");
        dedupState.setTaskId(Long.valueOf(101L));
        dedupState.setTableName("orders");
        dedupState.setChannelType(AlertChannelType.SMTP);
        dedupState.setChannelId(Long.valueOf(smtpChannelId));
        dedupState.setLastAlertId("alert-20260619-001");
        dedupState.setLastContentHash("hash-1");
        dedupState.setLastSentTime(Long.valueOf(1_718_888_000_500L));
        dedupState.setCooldownUntil(Long.valueOf(1_718_888_600_500L));

        long dedupId = dedupStateRepository.save(dedupState);
        Assert.assertTrue(dedupId > 0L);

        Optional<AlertDedupState> loadedDedupState = dedupStateRepository.findByDedupKey("TASK_FAILURE:101:orders:SMTP");
        Assert.assertTrue(loadedDedupState.isPresent());
        Assert.assertEquals(Long.valueOf(dedupId), loadedDedupState.get().getId());
        Assert.assertEquals(Long.valueOf(1_718_888_600_500L), loadedDedupState.get().getCooldownUntil());

        SqliteAlertDedupStateRepository restartedDedupRepository = new SqliteAlertDedupStateRepository(connectionFactory);
        Optional<AlertDedupState> restartedLoadedDedupState =
                restartedDedupRepository.findByDedupKey("TASK_FAILURE:101:orders:SMTP");
        Assert.assertTrue(restartedLoadedDedupState.isPresent());
        Assert.assertEquals("hash-1", restartedLoadedDedupState.get().getLastContentHash());

        Optional<AlertRule> loadedRule = ruleRepository.findById(ruleId);
        Assert.assertTrue(loadedRule.isPresent());
        Assert.assertEquals(Boolean.TRUE, loadedRule.get().getEnabled());
        Assert.assertEquals(Integer.valueOf(600), loadedRule.get().getCooldownSeconds());
    }

    @Test
    public void shouldEncryptAndDecryptSecretsWithGeneratedLocalKey() throws Exception {
        File tempConfigDirectory = createTempDirectory("db-sync-studio-alert-crypto");
        File keyFile = new File(tempConfigDirectory, "alert-secrets.key");

        LocalSecretCryptoService cryptoService = new LocalSecretCryptoService(new LocalSecretKeyProvider(keyFile));
        String encrypted = cryptoService.encrypt("sensitive-secret");

        Assert.assertNotNull(encrypted);
        Assert.assertNotEquals("sensitive-secret", encrypted);
        Assert.assertTrue(keyFile.exists());
        Assert.assertEquals("sensitive-secret", cryptoService.decrypt(encrypted));
    }

    private void assertEncryptedSecrets(SqliteConnectionFactory connectionFactory,
                                        long smtpChannelId,
                                        String smtpPlaintext,
                                        long webhookChannelId,
                                        String webhookPlaintext) throws Exception {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, smtp_password_encrypted, webhook_token_encrypted " +
                             "FROM alert_channels WHERE id IN (?, ?) ORDER BY id ASC")) {
            statement.setLong(1, smtpChannelId);
            statement.setLong(2, webhookChannelId);
            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next());
                Assert.assertEquals(Long.valueOf(smtpChannelId), Long.valueOf(resultSet.getLong("id")));
                Assert.assertNotNull(resultSet.getString("smtp_password_encrypted"));
                Assert.assertNotEquals(smtpPlaintext, resultSet.getString("smtp_password_encrypted"));

                Assert.assertTrue(resultSet.next());
                Assert.assertEquals(Long.valueOf(webhookChannelId), Long.valueOf(resultSet.getLong("id")));
                Assert.assertNotNull(resultSet.getString("webhook_token_encrypted"));
                Assert.assertNotEquals(webhookPlaintext, resultSet.getString("webhook_token_encrypted"));
            }
        }
    }

    private File resetTempDatabase(String prefix) throws Exception {
        File tempDatabase = File.createTempFile(prefix, ".sqlite");
        if (tempDatabase.exists() && !tempDatabase.delete()) {
            throw new IllegalStateException("Failed to reset temp database file");
        }
        tempDatabase.deleteOnExit();
        return tempDatabase;
    }

    private File createTempDirectory(String prefix) throws Exception {
        File tempDirectory = File.createTempFile(prefix, "");
        if (!tempDirectory.delete()) {
            throw new IllegalStateException("Failed to reset temp directory file");
        }
        if (!tempDirectory.mkdirs()) {
            throw new IllegalStateException("Failed to create temp directory");
        }
        tempDirectory.deleteOnExit();
        return tempDirectory;
    }
}
