package com.dbsyncstudio.core.backend;

import com.dbsyncstudio.model.schema.vo.SchemaComparisonResultVO;
import com.dbsyncstudio.model.schema.SchemaComparisonType;
import com.dbsyncstudio.model.schema.entity.SchemaDiffEntryDO;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DesktopBackendServiceSchemaComparisonHistoryTest {

    @Test
    public void shouldBuildReadableSchemaComparisonSummary() {
        List<SchemaDiffEntryDO> diffEntries = new ArrayList<SchemaDiffEntryDO>();
        diffEntries.add(SchemaDiffEntryDO.builder().diffType(SchemaComparisonType.MISSING_COLUMN).build());
        diffEntries.add(SchemaDiffEntryDO.builder().diffType(SchemaComparisonType.INDEX_DIFF).build());

        SchemaComparisonResultVO result = SchemaComparisonResultVO.builder()
                .diffEntries(diffEntries)
                .suggestedSqlList(new ArrayList<String>() {{
                    add("ALTER TABLE t ADD COLUMN c1 VARCHAR(32)");
                    add("CREATE INDEX idx_t_c1 ON t(c1)");
                }})
                .build();

        String summary = DesktopBackendService.buildSchemaComparisonHistorySummary(result);

        Assert.assertTrue(summary.contains("差异 2 项"));
        Assert.assertTrue(summary.contains("SQL 2 条"));
        Assert.assertTrue(summary.contains("MISSING_COLUMN=1"));
        Assert.assertTrue(summary.contains("INDEX_DIFF=1"));
    }
}
