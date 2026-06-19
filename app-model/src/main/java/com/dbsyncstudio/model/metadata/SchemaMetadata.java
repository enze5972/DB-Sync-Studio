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
public class SchemaMetadata {

    private String schemaName;
    @Builder.Default
    private List<TableMetadata> tables = new ArrayList<TableMetadata>();
}

