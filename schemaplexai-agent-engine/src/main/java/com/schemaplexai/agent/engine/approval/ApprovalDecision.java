package com.schemaplexai.agent.engine.approval;

import java.time.Instant;

/**
 * Records the decision made by a human approver on an {@link ApprovalRequest}.
 *
 * @param executionId the execution this decision applies to
 * @param approverId  identity of the human approver
 * @param action      the decision action (APPROVE, REJECT, or DEFER)
 * @param reason      free-text reason provided by the approver
 * @param decidedAt   timestamp when the decision was made
 */
public record ApprovalDecision(
        String executionId,
        String approverId,
        ApprovalAction action,
        String reason,
        Instant decidedAt
) {}
