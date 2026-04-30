package com.schemaplexai.quality.gate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityReport {

    private Long executionId;
    private boolean allPassed;
    private List<QualityCheckResult> results;
}
