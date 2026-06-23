package com.dbsyncstudio.model.sync.dto;

import com.dbsyncstudio.model.metadata.entity.SchemaMetadataDO;
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
public class FieldMappingSuggestionRequestDTO {

    private Long taskId;
    private String sourceSchemaName;
    private String sourceTableName;
    private String targetSchemaName;
    private String targetTableName;
    @Builder.Default
    private List<SchemaMetadataDO> sourceSchemas = new ArrayList<SchemaMetadataDO>();
    @Builder.Default
    private List<SchemaMetadataDO> targetSchemas = new ArrayList<SchemaMetadataDO>();
}
