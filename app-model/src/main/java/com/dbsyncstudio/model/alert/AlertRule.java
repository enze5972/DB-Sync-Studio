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
public class AlertRule {

    private Long id;
    private String ruleName;
    private String alertType;
    private Long taskId;
    private String tableName;
    private String alertLevel;
    private String alertContentTemplate;
    private String channelIdsJson;
    private Boolean enabled;
    private Integer cooldownSeconds;
    private Long createdAt;
    private Long updatedAt;
}
