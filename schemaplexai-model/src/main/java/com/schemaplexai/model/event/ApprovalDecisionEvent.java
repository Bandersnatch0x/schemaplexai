package com.schemaplexai.model.event;

import java.time.Instant;
import java.util.UUID;

public record ApprovalDecisionEvent(
        UUID ticketId,
        Long executionId,
        String action,
        String approverId,
        String reason,
        Instant decidedAt,
        int decisionVersion,
        int expectedExecutionVersion
) {
}
