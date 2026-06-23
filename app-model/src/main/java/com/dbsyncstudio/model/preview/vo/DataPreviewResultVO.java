package com.dbsyncstudio.model.preview.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class DataPreviewResultVO {

    @Builder.Default
    private List<String> columns = new ArrayList<String>();
    @Builder.Default
    private List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
    private long totalRowCount;
    private int pageNumber;
    private int pageSize;
}
