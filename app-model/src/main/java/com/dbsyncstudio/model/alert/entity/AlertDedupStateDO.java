package com.dbsyncstudio.model.alert.entity;
import com.dbsyncstudio.model.alert.AlertChannelType;

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
public class AlertDedupStateDO {

    private Long id;
    private String dedupKey;
    private Long ruleId;
    private String alertType;
    private Long taskId;
    private String tableName;
    private AlertChannelType channelType;
    private Long channelId;
    private String lastAlertId;
    private String lastContentHash;
    private Long lastSentTime;
    private Long cooldownUntil;
    private Long createdAt;
    private Long updatedAt;
}
