package com.dbsyncstudio.core.alert;

import com.dbsyncstudio.model.alert.AlertChannel;
import com.dbsyncstudio.model.alert.AlertChannelType;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class AlertSenderService {

    private static final Logger LOGGER = Logger.getLogger(AlertSenderService.class.getName());
    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    public AlertSendResult send(AlertChannel channel, String subject, String content) {
        long start = System.currentTimeMillis();
        if (channel == null || channel.getChannelType() == null) {
            return buildResult(false, "Alert channel is missing", start);
        }
        try {
            if (channel.getChannelType() == AlertChannelType.SMTP) {
                sendSmtp(channel, subject, content);
                return buildResult(true, "SMTP sent", start);
            }
            if (channel.getChannelType() == AlertChannelType.WEBHOOK) {
                sendWebhook(channel, subject, content);
                return buildResult(true, "Webhook sent", start);
            }
            return buildResult(false, "Unsupported channel type", start);
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Alert send failed", ex);
            return buildResult(false, ex.getMessage(), start);
        }
    }

    private void sendSmtp(AlertChannel channel, String subject, String content) throws Exception {
        if (channel.getSmtpHost() == null || channel.getSmtpToAddress() == null) {
            throw new IllegalArgumentException("SMTP host or to address is missing");
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", channel.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(channel.getSmtpPort() == null ? 25 : channel.getSmtpPort().intValue()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", String.valueOf(DEFAULT_TIMEOUT_MILLIS));
        props.put("mail.smtp.timeout", String.valueOf(DEFAULT_TIMEOUT_MILLIS));
        props.put("mail.smtp.writetimeout", String.valueOf(DEFAULT_TIMEOUT_MILLIS));
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(channel.getSmtpUsername(), channel.getSmtpPassword());
            }
        });
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(channel.getSmtpFromAddress() == null ? channel.getSmtpUsername() : channel.getSmtpFromAddress()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(channel.getSmtpToAddress(), false));
        message.setSubject(subject == null ? "DB Sync Studio Alert" : subject, "UTF-8");
        message.setText(content == null ? "" : content, "UTF-8");
        Transport.send(message);
    }

    private void sendWebhook(AlertChannel channel, String subject, String content) throws Exception {
        if (channel.getWebhookUrl() == null) {
            throw new IllegalArgumentException("Webhook url is missing");
        }
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(channel.getWebhookUrl()).openConnection();
        try {
            connection.setConnectTimeout(DEFAULT_TIMEOUT_MILLIS);
            connection.setReadTimeout(DEFAULT_TIMEOUT_MILLIS);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            String body = "{\"subject\":\"" + escapeJson(subject) + "\",\"content\":\"" + escapeJson(content) + "\",\"token\":\"" + escapeJson(channel.getWebhookToken()) + "\"}";
            byte[] payload = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            connection.getOutputStream().write(payload);
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("Webhook returned status " + code);
            }
        } finally {
            connection.disconnect();
        }
    }

    private AlertSendResult buildResult(boolean success, String message, long start) {
        AlertSendResult result = new AlertSendResult();
        result.setSuccess(success);
        result.setMessage(message);
        result.setElapsedMillis(System.currentTimeMillis() - start);
        return result;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
