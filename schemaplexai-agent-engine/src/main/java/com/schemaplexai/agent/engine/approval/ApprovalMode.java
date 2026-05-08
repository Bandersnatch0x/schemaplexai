package com.schemaplexai.agent.engine.approval;

/**
 * Controls how tool execution approval is handled in the agent FSM.
 *
 * <p>When a tool is classified as requiring approval (via {@link ToolRiskClassifier}),
 * this mode determines the behavior:
 */
public enum ApprovalMode {

    /**
     * All tools execute automatically without human approval.
     * This is the default mode for low-risk environments.
     */
    AUTO,

    /**
     * Tools flagged as high-risk require explicit human approval before execution.
     * The execution is paused (PAUSED state) and an approval request is submitted.
     * The tool executes only after an approver approves the request.
     */
    MANUAL,

    /**
     * All tools execute automatically. Approval requests are logged for audit
     * but do not block execution. Useful for monitoring without enforcement.
     */
    AUDIT_ONLY
}
