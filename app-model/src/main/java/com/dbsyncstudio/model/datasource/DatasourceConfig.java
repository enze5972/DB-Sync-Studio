package com.dbsyncstudio.model.datasource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class DatasourceConfig {

    private Long id;
    private String name;
    private DatasourceType type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String remark;
    private Long createdAt;
    private Long updatedAt;
}

