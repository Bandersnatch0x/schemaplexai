package com.schemaplexai.quality.gate;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a single quality rule check.
 *
 * <p>Carries both the issue severity vocabulary (CRITICAL/HIGH/MEDIUM/LOW —
 * grades the issue) and the disposition (PASS/WARN/BLOCK/FAIL — the action
 * the caller must take, per spec §1). The disposition is assigned by the
 * orchestrator from the gate policy; {@link #fail(String, String)} derives a
 * fail-closed default from the severity so results created outside a gate
 * context still carry a meaningful action.
 */
@Data
@NoArgsConstructor
public class QualityCheckResult {

    private boolean passed;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String message;
    private GateDisposition disposition;

    /**
     * Name of the rule that produced this result. Assigned by the orchestrator
     * (spec §5 report content: every result must be attributable to a rule).
     */
    private String ruleName;

    /**
     * Backward-compatible constructor: the disposition is derived from the
     * severity floor (CRITICAL escalates to at least BLOCK).
     */
    public QualityCheckResult(boolean passed, String severity, String message) {
        this.passed = passed;
        this.severity = severity;
        this.message = message;
        this.disposition = passed
                ? GateDisposition.PASS
                : GateDisposition.mostSevere(GateDisposition.WARN, GateDisposition.severityFloor(severity));
    }

    public QualityCheckResult(boolean passed, String severity, String message, GateDisposition disposition) {
        this.passed = passed;
        this.severity = severity;
        this.message = message;
        this.disposition = disposition;
    }

    public static QualityCheckResult pass() {
        return new QualityCheckResult(true, null, null, GateDisposition.PASS);
    }

    /**
     * Creates a failed result whose disposition is derived from the severity
     * (WARN floor, CRITICAL escalates to BLOCK).
     */
    public static QualityCheckResult fail(String severity, String message) {
        return new QualityCheckResult(false, severity, message);
    }

    /**
     * Creates a failed result with an explicit disposition (spec §1:
     * PASS / WARN / BLOCK / FAIL action semantics).
     */
    public static QualityCheckResult fail(String severity, String message, GateDisposition disposition) {
        return new QualityCheckResult(false, severity, message, disposition);
    }
}
