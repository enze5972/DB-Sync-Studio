package com.dbsyncstudio.model.metadata.entity;

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
public class TableMetadataDO {

    private String schemaName;
    private String tableName;
    @Builder.Default
    private List<ColumnMetadataDO> columns = new ArrayList<ColumnMetadataDO>();
    @Builder.Default
    private List<IndexMetadataDO> indexes = new ArrayList<IndexMetadataDO>();
}
