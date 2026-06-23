package com.dbsyncstudio.model.preview.dto;
import com.dbsyncstudio.model.preview.DataPreviewFilterOperator;

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
public class DataPreviewFilterDTO {

    private String columnName;
    private DataPreviewFilterOperator operator;
    private String value;
}
