package com.dbsyncstudio.model.metadata.entity;

import java.util.ArrayList;
import java.util.List;

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
public class IndexMetadataDO {

    private String name;
    private boolean unique;
    @Builder.Default
    private List<String> columnNames = new ArrayList<String>();
}
