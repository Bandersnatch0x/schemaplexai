package com.schemaplexai.model.event;

import java.time.Instant;
import java.util.UUID;

public record ApprovalRequestEvent(
        UUID approvalRequestId,
        Long executionId,
        Long tenantId,
        Long agentId,
        int triggeringSeq,
        String requestType,
        String riskLevel,
        String actionDescription,
        int executionVersionAtPause,
        Instant createdAt
) {
}
