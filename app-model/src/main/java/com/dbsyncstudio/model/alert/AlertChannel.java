package com.dbsyncstudio.model.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertChannel {

    private Long id;
    private String channelName;
    private AlertChannelType channelType;
    private Boolean enabled;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private String smtpToAddress;
    private String smtpFromAddress;
    private String webhookUrl;
    private String webhookToken;
    private Long createdAt;
    private Long updatedAt;
}
