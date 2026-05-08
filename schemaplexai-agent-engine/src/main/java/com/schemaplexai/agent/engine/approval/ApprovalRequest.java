package com.schemaplexai.agent.engine.approval;

import java.time.Instant;

/**
 * Represents a request for human approval before an agent execution can proceed.
 *
 * @param executionId      the execution requiring approval
 * @param agentId          the agent that triggered the approval request
 * @param tenantId         the tenant context
 * @param actionDescription human-readable description of the action requiring approval
 * @param riskLevel        risk classification (e.g. LOW, MEDIUM, HIGH, CRITICAL)
 * @param requestedAt      timestamp when approval was requested
 * @param deadline         timestamp after which the request expires if no decision is made
 */
public record ApprovalRequest(
        String executionId,
        String agentId,
        String tenantId,
        String actionDescription,
        String riskLevel,
        Instant requestedAt,
        Instant deadline
) {}
