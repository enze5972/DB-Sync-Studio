package com.dbsyncstudio.model.metadata;

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
public class ColumnMetadata {

    private String name;
    private String dataType;
    private Integer columnSize;
    private Integer decimalDigits;
    private boolean nullable;
    private boolean primaryKey;
    private boolean autoIncrement;
    private String defaultValue;
}
