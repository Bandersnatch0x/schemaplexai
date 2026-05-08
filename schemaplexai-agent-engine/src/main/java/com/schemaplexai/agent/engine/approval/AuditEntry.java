package com.schemaplexai.agent.engine.approval;

import java.time.Instant;

/**
 * Immutable entry in the audit trail, recording a single action taken
 * during the HITL approval lifecycle.
 *
 * @param executionId the execution this entry relates to
 * @param actor       the entity that performed the action (system, user, or approver id)
 * @param action      the action performed (e.g. APPROVAL_REQUESTED, APPROVED, REJECTED, DEFERRED)
 * @param detail      human-readable detail about the action
 * @param timestamp   when the action occurred
 */
public record AuditEntry(
        String executionId,
        String actor,
        String action,
        String detail,
        Instant timestamp
) {}
