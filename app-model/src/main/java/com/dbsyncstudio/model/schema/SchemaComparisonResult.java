package com.dbsyncstudio.model.schema;

import com.dbsyncstudio.model.metadata.TableMetadata;

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
public class SchemaComparisonResult {

    private TableMetadata sourceTable;
    private TableMetadata targetTable;
    @Builder.Default
    private List<SchemaDiffEntry> diffEntries = new ArrayList<SchemaDiffEntry>();
    @Builder.Default
    private List<String> suggestedSqlList = new ArrayList<String>();
}
