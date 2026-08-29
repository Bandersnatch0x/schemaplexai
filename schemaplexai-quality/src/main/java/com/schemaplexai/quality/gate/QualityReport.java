package com.schemaplexai.quality.gate;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Quality evaluation report for one execution.
 *
 * <p>The {@code disposition} is the overall structured verdict the caller
 * acts on (spec §1): the most severe disposition among all results. An empty
 * result list evaluates to {@link GateDisposition#PASS}.
 */
@Data
@NoArgsConstructor
public class QualityReport {

    private Long executionId;
    private boolean allPassed;
    private List<QualityCheckResult> results;
    private GateDisposition disposition;

    /**
     * Backward-compatible constructor: the overall disposition is derived as
     * the most severe disposition across all results.
     */
    public QualityReport(Long executionId, boolean allPassed, List<QualityCheckResult> results) {
        this.executionId = executionId;
        this.allPassed = allPassed;
        this.results = results;
        this.disposition = deriveDisposition(results);
    }

    public QualityReport(Long executionId, boolean allPassed, List<QualityCheckResult> results,
                         GateDisposition disposition) {
        this.executionId = executionId;
        this.allPassed = allPassed;
        this.results = results;
        this.disposition = disposition;
    }

    private static GateDisposition deriveDisposition(List<QualityCheckResult> results) {
        GateDisposition overall = GateDisposition.PASS;
        if (results == null) {
            return overall;
        }
        for (QualityCheckResult result : results) {
            if (result == null) {
                continue;
            }
            GateDisposition candidate = result.isPassed()
                    ? GateDisposition.PASS
                    : result.getDisposition() != null ? result.getDisposition() : GateDisposition.WARN;
            overall = GateDisposition.mostSevere(overall, candidate);
        }
        return overall;
    }
}
