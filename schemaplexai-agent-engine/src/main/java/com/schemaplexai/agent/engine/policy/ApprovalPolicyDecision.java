package com.schemaplexai.agent.engine.policy;

/**
 * Decision produced by {@link ApprovalPolicyEngine} for a tool call.
 */
public enum ApprovalPolicyDecision {
    /**
     * Tool call is safe — proceed without approval.
     */
    AUTO_APPROVE,

    /**
     * Fast single-step approval: pause execution, send ApprovalRequestEvent,
     * wait for a single approver decision.
     */
    FAST_APPROVAL,

    /**
     * BPMN workflow approval: pause execution, send ApprovalRequestEvent with
     * requestType=BPMN, Core starts multi-step approval workflow.
     */
    BPMN_APPROVAL
}
