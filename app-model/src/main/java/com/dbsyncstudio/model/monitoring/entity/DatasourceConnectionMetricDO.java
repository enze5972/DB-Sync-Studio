package com.dbsyncstudio.model.monitoring.entity;

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
public class DatasourceConnectionMetricDO {

    private Long id;
    private Long datasourceId;
    private String connectionStatus;
    private Long lastSuccessTime;
    private Long lastFailureTime;
    private String failureReason;
    private Double averageTestConnectionMillis;
    private Long lastTestConnectionMillis;
    private Long metricTime;
}
