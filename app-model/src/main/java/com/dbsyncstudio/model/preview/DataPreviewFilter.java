package com.dbsyncstudio.model.preview;

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
public class DataPreviewFilter {

    private String columnName;
    private DataPreviewFilterOperator operator;
    private String value;
}
