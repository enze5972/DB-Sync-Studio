package com.dbsyncstudio.model.validation;

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
public class ValidationResult {

    private ValidationRun run;
    @Builder.Default
    private List<ValidationDifference> differences = new ArrayList<ValidationDifference>();
}
