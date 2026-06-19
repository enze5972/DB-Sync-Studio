package com.dbsyncstudio.core.schema;

import com.dbsyncstudio.model.metadata.ColumnMetadata;
import com.dbsyncstudio.model.metadata.IndexMetadata;
import com.dbsyncstudio.model.metadata.TableMetadata;

import java.util.List;

public class SchemaSqlDialect {

    private final DatabaseDialect dialect;

    public SchemaSqlDialect(DatabaseDialect dialect) {
        this.dialect = dialect == null ? DatabaseDialect.MYSQL : dialect;
    }

    public String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.trim().length() == 0) {
            return identifier;
        }
        if (dialect == DatabaseDialect.POSTGRESQL || dialect == DatabaseDialect.DM) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    public String qualifiedTableName(String schemaName, String tableName) {
        if (schemaName == null || schemaName.trim().length() == 0 || "default".equalsIgnoreCase(schemaName)) {
            return quoteIdentifier(tableName);
        }
        return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
    }

    public String renderColumnDefinition(ColumnMetadata column) {
        StringBuilder builder = new StringBuilder();
        builder.append(quoteIdentifier(column.getName())).append(" ").append(renderType(column));
        if (!column.isNullable()) {
            builder.append(" NOT NULL");
        }
        if (column.getDefaultValue() != null && column.getDefaultValue().trim().length() > 0) {
            builder.append(" DEFAULT ").append(column.getDefaultValue());
        }
        if (column.isAutoIncrement()) {
            builder.append(renderAutoIncrementClause(column));
        }
        return builder.toString();
    }

    public String renderCreateTable(TableMetadata tableMetadata) {
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE ").append(qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName())).append(" (");
        boolean first = true;
        for (ColumnMetadata column : tableMetadata.getColumns()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(renderColumnDefinition(column));
            first = false;
        }
        List<String> primaryKeyColumns = SchemaTableUtils.primaryKeyColumns(tableMetadata);
        if (!primaryKeyColumns.isEmpty()) {
            builder.append(", PRIMARY KEY (").append(joinQuotedColumns(primaryKeyColumns)).append(")");
        }
        builder.append(")");
        return builder.toString();
    }

    public String renderCreateTableIfNotExists(TableMetadata tableMetadata) {
        String sql = renderCreateTable(tableMetadata);
        return sql.replaceFirst("CREATE TABLE ", "CREATE TABLE IF NOT EXISTS ");
    }

    public String renderAddColumn(TableMetadata tableMetadata, ColumnMetadata column) {
        return "ALTER TABLE " + qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName())
                + " ADD COLUMN " + renderColumnDefinition(column);
    }

    public String renderCreateIndex(TableMetadata tableMetadata, IndexMetadata indexMetadata) {
        StringBuilder builder = new StringBuilder();
        if (indexMetadata.isUnique()) {
            builder.append("CREATE UNIQUE INDEX ");
        } else {
            builder.append("CREATE INDEX ");
        }
        builder.append(quoteIdentifier(indexMetadata.getName()))
                .append(" ON ")
                .append(qualifiedTableName(tableMetadata.getSchemaName(), tableMetadata.getTableName()))
                .append(" (")
                .append(joinQuotedColumns(indexMetadata.getColumnNames()))
                .append(")");
        return builder.toString();
    }

    public String renderType(ColumnMetadata column) {
        String dataType = column.getDataType() == null ? "TEXT" : column.getDataType().toUpperCase();
        Integer size = column.getColumnSize();
        Integer decimalDigits = column.getDecimalDigits();
        if (dataType.contains("CHAR") || dataType.contains("CLOB") || dataType.contains("TEXT")) {
            if (size != null && size.intValue() > 0 && !dataType.contains("TEXT")) {
                return dataType + "(" + size + ")";
            }
            return dataType;
        }
        if ((dataType.contains("DECIMAL") || dataType.contains("NUMERIC") || dataType.contains("NUMBER"))
                && size != null && size.intValue() > 0) {
            if (decimalDigits != null && decimalDigits.intValue() > 0) {
                return dataType + "(" + size + "," + decimalDigits + ")";
            }
            return dataType + "(" + size + ")";
        }
        if ((dataType.contains("INT") || dataType.contains("NUMBER") || dataType.contains("INTEGER")) && size != null && size.intValue() > 0) {
            return dataType + "(" + size + ")";
        }
        return dataType;
    }

    private String renderAutoIncrementClause(ColumnMetadata column) {
        if (dialect == DatabaseDialect.POSTGRESQL) {
            return " GENERATED BY DEFAULT AS IDENTITY";
        }
        if (dialect == DatabaseDialect.DM) {
            return " IDENTITY";
        }
        return " AUTO_INCREMENT";
    }

    private String joinQuotedColumns(List<String> columnNames) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(quoteIdentifier(columnNames.get(i)));
        }
        return builder.toString();
    }
}
