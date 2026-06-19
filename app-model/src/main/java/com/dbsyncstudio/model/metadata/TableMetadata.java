package com.dbsyncstudio.model.metadata;

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
public class TableMetadata {

    private String schemaName;
    private String tableName;
    @Builder.Default
    private List<ColumnMetadata> columns = new ArrayList<ColumnMetadata>();
    @Builder.Default
    private List<IndexMetadata> indexes = new ArrayList<IndexMetadata>();
}
