package com.dbsyncstudio.model.monitoring;

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
public class DatasourceConnectionMetric {

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
