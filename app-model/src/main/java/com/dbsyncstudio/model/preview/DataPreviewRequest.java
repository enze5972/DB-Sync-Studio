package com.dbsyncstudio.model.preview;

import com.dbsyncstudio.model.datasource.DatasourceConfig;

import java.util.ArrayList;
import java.util.List;

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
public class DataPreviewRequest {

    private DatasourceConfig datasource;
    private String schemaName;
    private String tableName;
    @Builder.Default
    private int pageNumber = 1;
    @Builder.Default
    private int pageSize = 100;
    private String sortColumn;
    private String sortDirection;
    @Builder.Default
    private List<DataPreviewFilter> filters = new ArrayList<DataPreviewFilter>();
}
