package com.schemaplexai.quality.gate;

/**
 * Disposition (处置) semantics for quality gate checks, per spec
 * {@code docs/specs/2026-04-30-v1.0-quality-gate.md} §1/§4:
 *
 * <ul>
 *   <li>{@link #PASS} — continue execution.</li>
 *   <li>{@link #WARN} — record the alert, continue execution.</li>
 *   <li>{@link #BLOCK} — pause execution, wait for manual confirmation.</li>
 *   <li>{@link #FAIL} — terminate execution, mark it failed.</li>
 * </ul>
 *
 * <p>The disposition is the <em>action</em> a caller must take; it is distinct
 * from the issue severity vocabulary (CRITICAL/HIGH/MEDIUM/LOW), which grades
 * the issue itself. {@link #severityFloor(String)} maps a CRITICAL issue
 * severity to at least BLOCK (fail-closed escalation).
 */
public enum GateDisposition {

    PASS(0),
    WARN(1),
    BLOCK(2),
    FAIL(3);

    private final int rank;

    GateDisposition(int rank) {
        this.rank = rank;
    }

    /**
     * Returns the more severe of the two dispositions (null-safe; null is PASS).
     */
    public static GateDisposition mostSevere(GateDisposition a, GateDisposition b) {
        GateDisposition left = a != null ? a : PASS;
        GateDisposition right = b != null ? b : PASS;
        return left.rank >= right.rank ? left : right;
    }

    /**
     * Fail-closed floor derived from the issue severity vocabulary: a CRITICAL
     * finding always escalates to at least BLOCK, regardless of the configured
     * gate policy. Other severities impose no floor (the gate policy decides).
     */
    public static GateDisposition severityFloor(String severity) {
        return "CRITICAL".equalsIgnoreCase(severity) ? BLOCK : PASS;
    }

    /**
     * Parses a disposition name leniently; unknown or null values yield the
     * fail-closed default BLOCK.
     */
    public static GateDisposition parseOrDefault(String value, GateDisposition fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return GateDisposition.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
