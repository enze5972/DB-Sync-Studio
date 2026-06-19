package com.dbsyncstudio.model.sync;

import com.dbsyncstudio.model.metadata.SchemaMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldMappingSuggestionRequest {

    private Long taskId;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    @Builder.Default
    private List<SchemaMetadata> sourceSchemas = new ArrayList<SchemaMetadata>();
    @Builder.Default
    private List<SchemaMetadata> targetSchemas = new ArrayList<SchemaMetadata>();
}
