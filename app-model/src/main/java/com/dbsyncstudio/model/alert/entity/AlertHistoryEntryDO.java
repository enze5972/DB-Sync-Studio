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
public class AlertHistoryEntryDO {

    private Long id;
    private String alertId;
    private Long ruleId;
    private String alertType;
    private Long taskId;
    private String runId;
    private String tableName;
    private String alertLevel;
    private String alertContent;
    private AlertChannelType channelType;
    private Long channelId;
    private String sendStatus;
    private String errorMessage;
    private Long createdTime;
    private Long sentTime;
}
