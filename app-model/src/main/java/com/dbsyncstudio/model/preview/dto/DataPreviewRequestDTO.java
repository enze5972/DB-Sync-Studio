package com.dbsyncstudio.model.preview.dto;

import com.dbsyncstudio.model.datasource.entity.DatasourceConfigDO;

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
public class DataPreviewRequestDTO {

    private DatasourceConfigDO datasource;
    private String schemaName;
    private String tableName;
    @Builder.Default
    private int pageNumber = 1;
    @Builder.Default
    private int pageSize = 100;
    private String sortColumn;
    private String sortDirection;
    @Builder.Default
    private List<DataPreviewFilterDTO> filters = new ArrayList<DataPreviewFilterDTO>();
}
