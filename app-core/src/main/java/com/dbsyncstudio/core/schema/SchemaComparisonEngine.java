package com.dbsyncstudio.core.schema;

import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.metadata.IndexMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;
import com.dbsyncstudio.model.schema.SchemaComparisonRequest;
import com.dbsyncstudio.model.schema.SchemaComparisonResult;
import com.dbsyncstudio.model.schema.SchemaComparisonType;
import com.dbsyncstudio.model.schema.SchemaDiffEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SchemaComparisonEngine {

    public SchemaComparisonResult compare(SchemaComparisonRequest request, TableMetadata sourceTable, TableMetadata targetTable,
                                          DatabaseDialect dialect) {
        SchemaSqlDialect sqlDialect = new SchemaSqlDialect(dialect);
        List<SchemaDiffEntry> diffEntries = new ArrayList<SchemaDiffEntry>();
        List<String> sqlList = new ArrayList<String>();

        if (sourceTable == null) {
            return SchemaComparisonResult.builder()
                    .sourceTable(null)
                    .targetTable(targetTable)
                    .diffEntries(diffEntries)
                    .suggestedSqlList(sqlList)
                    .build();
        }

        if (targetTable == null) {
            String createTableSql = sqlDialect.renderCreateTable(sourceTable);
            diffEntries.add(SchemaDiffEntry.builder()
                    .diffType(SchemaComparisonType.MISSING_COLUMN)
                    .sourceColumnName(null)
                    .targetColumnName(null)
                    .sourceColumn(null)
                    .targetColumn(null)
                    .description("目标表不存在，建议先创建目标表: " + sourceTable.getTableName())
                    .suggestedSql(createTableSql)
                    .build());
            sqlList.add(createTableSql);
            return SchemaComparisonResult.builder()
                    .sourceTable(sourceTable)
                    .targetTable(null)
                    .diffEntries(diffEntries)
                    .suggestedSqlList(sqlList)
                    .build();
        }

        Map<String, ColumnMetadata> targetColumnsByName = new LinkedHashMap<String, ColumnMetadata>();
        if (targetTable != null && targetTable.getColumns() != null) {
            for (ColumnMetadata targetColumn : targetTable.getColumns()) {
                if (targetColumn != null && targetColumn.getName() != null) {
                    targetColumnsByName.put(normalize(targetColumn.getName()), targetColumn);
                }
            }
        }

        if (sourceTable != null && sourceTable.getColumns() != null) {
            for (ColumnMetadata sourceColumn : sourceTable.getColumns()) {
                ColumnMetadata targetColumn = targetColumnsByName.remove(normalize(sourceColumn.getName()));
                if (targetColumn == null) {
                    String sql = sqlDialect.renderAddColumn(targetTable, sourceColumn);
                    diffEntries.add(SchemaDiffEntry.builder()
                            .diffType(SchemaComparisonType.MISSING_COLUMN)
                            .sourceColumnName(sourceColumn.getName())
                            .sourceColumn(sourceColumn)
                            .description("目标表缺少字段: " + sourceColumn.getName())
                            .suggestedSql(sql)
                            .build());
                    sqlList.add(sql);
                    continue;
                }

                collectColumnDiffs(sourceTable, targetTable, sourceColumn, targetColumn, sqlDialect, diffEntries, sqlList);
            }
        }

        for (ColumnMetadata extraColumn : targetColumnsByName.values()) {
            diffEntries.add(SchemaDiffEntry.builder()
                    .diffType(SchemaComparisonType.EXTRA_COLUMN)
                    .targetColumnName(extraColumn.getName())
                    .targetColumn(extraColumn)
                    .description("目标表存在额外字段: " + extraColumn.getName())
                    .build());
        }

        collectIndexDiffs(sourceTable, targetTable, sqlDialect, diffEntries, sqlList);

        return SchemaComparisonResult.builder()
                .sourceTable(sourceTable)
                .targetTable(targetTable)
                .diffEntries(diffEntries)
                .suggestedSqlList(sqlList)
                .build();
    }

    private void collectColumnDiffs(TableMetadata sourceTable, TableMetadata targetTable, ColumnMetadata sourceColumn,
                                    ColumnMetadata targetColumn, SchemaSqlDialect sqlDialect,
                                    List<SchemaDiffEntry> diffEntries, List<String> sqlList) {
        if (!safeEquals(sourceColumn.getDataType(), targetColumn.getDataType())) {
            diffEntries.add(buildColumnDiff(SchemaComparisonType.TYPE_DIFF, sourceColumn, targetColumn,
                    "字段类型不同: " + sourceColumn.getName() + " -> " + sourceColumn.getDataType() + " / " + targetColumn.getDataType(), null));
        }
        if (!safeEquals(sourceColumn.getColumnSize(), targetColumn.getColumnSize())
                || !safeEquals(sourceColumn.getDecimalDigits(), targetColumn.getDecimalDigits())) {
            diffEntries.add(buildColumnDiff(SchemaComparisonType.LENGTH_DIFF, sourceColumn, targetColumn,
                    "字段长度不同: " + sourceColumn.getName(), null));
        }
        if (sourceColumn.isNullable() != targetColumn.isNullable()) {
            diffEntries.add(buildColumnDiff(SchemaComparisonType.NULLABLE_DIFF, sourceColumn, targetColumn,
                    "是否允许为空不同: " + sourceColumn.getName(), null));
        }
        if (!safeEquals(normalizeDefault(sourceColumn.getDefaultValue()), normalizeDefault(targetColumn.getDefaultValue()))) {
            diffEntries.add(buildColumnDiff(SchemaComparisonType.DEFAULT_DIFF, sourceColumn, targetColumn,
                    "默认值不同: " + sourceColumn.getName(), null));
        }
        if (sourceColumn.isPrimaryKey() != targetColumn.isPrimaryKey()) {
            diffEntries.add(buildColumnDiff(SchemaComparisonType.PRIMARY_KEY_DIFF, sourceColumn, targetColumn,
                    "主键定义不同: " + sourceColumn.getName(), null));
        }
    }

    private void collectIndexDiffs(TableMetadata sourceTable, TableMetadata targetTable, SchemaSqlDialect sqlDialect,
                                   List<SchemaDiffEntry> diffEntries, List<String> sqlList) {
        List<IndexMetadata> sourceIndexes = sourceTable == null || sourceTable.getIndexes() == null
                ? new ArrayList<IndexMetadata>()
                : sourceTable.getIndexes();
        List<IndexMetadata> targetIndexes = targetTable == null || targetTable.getIndexes() == null
                ? new ArrayList<IndexMetadata>()
                : targetTable.getIndexes();

        for (IndexMetadata sourceIndex : sourceIndexes) {
            if (sourceIndex == null || sourceIndex.getColumnNames() == null || sourceIndex.getColumnNames().isEmpty()) {
                continue;
            }
            IndexMetadata matched = SchemaTableUtils.findIndexByColumns(targetTable, sourceIndex.getColumnNames());
            if (matched == null) {
                String sql = sqlDialect.renderCreateIndex(sourceTable, sourceIndex);
                diffEntries.add(SchemaDiffEntry.builder()
                        .diffType(SchemaComparisonType.INDEX_DIFF)
                        .description("目标表缺少索引: " + sourceIndex.getName())
                        .suggestedSql(sql)
                        .build());
                sqlList.add(sql);
            }
        }

        for (IndexMetadata targetIndex : targetIndexes) {
            if (targetIndex == null || targetIndex.getColumnNames() == null || targetIndex.getColumnNames().isEmpty()) {
                continue;
            }
            IndexMetadata matched = SchemaTableUtils.findIndexByColumns(sourceTable, targetIndex.getColumnNames());
            if (matched == null) {
                diffEntries.add(SchemaDiffEntry.builder()
                        .diffType(SchemaComparisonType.INDEX_DIFF)
                        .description("目标表存在额外索引: " + targetIndex.getName())
                        .build());
            }
        }
    }

    private SchemaDiffEntry buildColumnDiff(SchemaComparisonType diffType, ColumnMetadata sourceColumn, ColumnMetadata targetColumn,
                                            String description, String sql) {
        return SchemaDiffEntry.builder()
                .diffType(diffType)
                .sourceColumnName(sourceColumn == null ? null : sourceColumn.getName())
                .targetColumnName(targetColumn == null ? null : targetColumn.getName())
                .sourceColumn(sourceColumn)
                .targetColumn(targetColumn)
                .description(description)
                .suggestedSql(sql)
                .build();
    }

    private String normalizeDefault(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean safeEquals(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
