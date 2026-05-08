package com.schemaplexai.agent.engine.approval;

/**
 * Possible actions a human approver can take on an approval request.
 */
public enum ApprovalAction {
    /** Approve the action — execution resumes. */
    APPROVE,
    /** Reject the action — execution transitions to FAILED. */
    REJECT,
    /** Defer the decision — execution stays PAUSED with an extended deadline. */
    DEFER
}
