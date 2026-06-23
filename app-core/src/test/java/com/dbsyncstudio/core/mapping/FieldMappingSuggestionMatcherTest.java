package com.dbsyncstudio.core.mapping;

import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;
import com.dbsyncstudio.model.sync.vo.FieldMappingSuggestionVO;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldMappingSuggestionMatcherTest {

    @Test
    public void shouldMatchCommonAliasesWithConfidence() {
        TableMetadataDO sourceTable = tableWithColumns("user_name", "phone", "create_time", "status");
        TableMetadataDO targetTable = tableWithColumns("username", "mobile", "gmt_create", "status_flag");

        FieldMappingSuggestionMatcher matcher = new FieldMappingSuggestionMatcher();
        List<FieldMappingSuggestionVO> suggestions = matcher.match(sourceTable.getColumns(), targetTable.getColumns());

        Assert.assertEquals(4, suggestions.size());
        assertSuggestion(suggestions, "user_name", "username", 0.92d, "synonym");
        assertSuggestion(suggestions, "phone", "mobile", 0.92d, "synonym");
        assertSuggestion(suggestions, "create_time", "gmt_create", 0.92d, "synonym");
        assertSuggestion(suggestions, "status", "status_flag", 0.7d, "fuzzy");
    }

    private void assertSuggestion(List<FieldMappingSuggestionVO> suggestions, String source, String target,
                                  double minimumConfidence, String reasonFragment) {
        for (FieldMappingSuggestionVO suggestion : suggestions) {
            if (source.equalsIgnoreCase(suggestion.getSourceColumnName())) {
                Assert.assertEquals(target, suggestion.getTargetColumnName());
                Assert.assertTrue(suggestion.getConfidence() >= minimumConfidence);
                Assert.assertTrue(suggestion.getMatchReason().toLowerCase().contains(reasonFragment));
                return;
            }
        }
        Assert.fail("Missing suggestion for " + source);
    }

    private TableMetadataDO tableWithColumns(String... names) {
        TableMetadataDO tableMetadata = new TableMetadataDO();
        tableMetadata.setColumns(new ArrayList<ColumnMetadataDO>());
        for (String name : names) {
            ColumnMetadataDO columnMetadata = new ColumnMetadataDO();
            columnMetadata.setName(name);
            tableMetadata.getColumns().add(columnMetadata);
        }
        return tableMetadata;
    }
}
