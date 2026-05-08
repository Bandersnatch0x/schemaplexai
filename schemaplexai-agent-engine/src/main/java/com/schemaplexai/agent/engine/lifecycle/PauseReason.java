package com.schemaplexai.agent.engine.lifecycle;

public enum PauseReason {
    USER_REQUEST,
    TOKEN_BUDGET_WARNING,
    QUALITY_GATE_BLOCKED,
    LOOP_DETECTED,
    MANUAL_APPROVAL_REQUIRED,
    HANDOFF_TARGET_MISMATCH
}
