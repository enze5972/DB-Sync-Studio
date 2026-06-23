package com.dbsyncstudio.model.schema.vo;
import com.dbsyncstudio.model.schema.entity.SchemaDiffEntryDO;

import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;

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
public class SchemaComparisonResultVO {

    private TableMetadataDO sourceTable;
    private TableMetadataDO targetTable;
    @Builder.Default
    private List<SchemaDiffEntryDO> diffEntries = new ArrayList<SchemaDiffEntryDO>();
    @Builder.Default
    private List<String> suggestedSqlList = new ArrayList<String>();
}
