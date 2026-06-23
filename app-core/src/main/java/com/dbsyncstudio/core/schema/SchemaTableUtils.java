package com.dbsyncstudio.core.schema;

import com.dbsyncstudio.model.metadata.entity.ColumnMetadataDO;
import com.dbsyncstudio.model.metadata.entity.IndexMetadataDO;
import com.dbsyncstudio.model.metadata.entity.TableMetadataDO;

import java.util.ArrayList;
import java.util.List;

public final class SchemaTableUtils {

    private SchemaTableUtils() {
    }

    public static List<String> primaryKeyColumns(TableMetadataDO tableMetadata) {
        List<String> result = new ArrayList<String>();
        if (tableMetadata == null || tableMetadata.getColumns() == null) {
            return result;
        }
        for (ColumnMetadataDO column : tableMetadata.getColumns()) {
            if (column != null && column.isPrimaryKey() && column.getName() != null && column.getName().trim().length() > 0) {
                result.add(column.getName());
            }
        }
        return result;
    }

    public static IndexMetadataDO findIndexByColumns(TableMetadataDO tableMetadata, List<String> columns) {
        if (tableMetadata == null || tableMetadata.getIndexes() == null || columns == null || columns.isEmpty()) {
            return null;
        }
        for (IndexMetadataDO indexMetadata : tableMetadata.getIndexes()) {
            if (indexMetadata == null || indexMetadata.getColumnNames() == null) {
                continue;
            }
            if (indexMetadata.getColumnNames().size() != columns.size()) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < columns.size(); i++) {
                String left = columns.get(i);
                String right = indexMetadata.getColumnNames().get(i);
                if (left == null || right == null || !left.equalsIgnoreCase(right)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return indexMetadata;
            }
        }
        return null;
    }
}
