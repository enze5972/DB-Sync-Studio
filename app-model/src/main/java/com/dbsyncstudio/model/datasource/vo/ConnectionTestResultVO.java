package com.dbsyncstudio.model.datasource.vo;

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
public class ConnectionTestResultVO {

    private boolean success;
    private String message;
    private long costMillis;
}

