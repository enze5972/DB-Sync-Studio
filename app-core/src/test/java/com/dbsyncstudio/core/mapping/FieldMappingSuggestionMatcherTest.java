package com.dbsyncstudio.core.mapping;

import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;
import com.dbsyncstudio.model.sync.FieldMappingSuggestion;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FieldMappingSuggestionMatcherTest {

    @Test
    public void shouldMatchCommonAliasesWithConfidence() {
        TableMetadata sourceTable = tableWithColumns("user_name", "phone", "create_time", "status");
        TableMetadata targetTable = tableWithColumns("username", "mobile", "gmt_create", "status_flag");

        FieldMappingSuggestionMatcher matcher = new FieldMappingSuggestionMatcher();
        List<FieldMappingSuggestion> suggestions = matcher.match(sourceTable.getColumns(), targetTable.getColumns());

        Assert.assertEquals(4, suggestions.size());
        assertSuggestion(suggestions, "user_name", "username", 0.92d, "synonym");
        assertSuggestion(suggestions, "phone", "mobile", 0.92d, "synonym");
        assertSuggestion(suggestions, "create_time", "gmt_create", 0.92d, "synonym");
        assertSuggestion(suggestions, "status", "status_flag", 0.7d, "fuzzy");
    }

    private void assertSuggestion(List<FieldMappingSuggestion> suggestions, String source, String target,
                                  double minimumConfidence, String reasonFragment) {
        for (FieldMappingSuggestion suggestion : suggestions) {
            if (source.equalsIgnoreCase(suggestion.getSourceColumnName())) {
                Assert.assertEquals(target, suggestion.getTargetColumnName());
                Assert.assertTrue(suggestion.getConfidence() >= minimumConfidence);
                Assert.assertTrue(suggestion.getMatchReason().toLowerCase().contains(reasonFragment));
                return;
            }
        }
        Assert.fail("Missing suggestion for " + source);
    }

    private TableMetadata tableWithColumns(String... names) {
        TableMetadata tableMetadata = new TableMetadata();
        tableMetadata.setColumns(new ArrayList<ColumnMetadata>());
        for (String name : names) {
            ColumnMetadata columnMetadata = new ColumnMetadata();
            columnMetadata.setName(name);
            tableMetadata.getColumns().add(columnMetadata);
        }
        return tableMetadata;
    }
}
