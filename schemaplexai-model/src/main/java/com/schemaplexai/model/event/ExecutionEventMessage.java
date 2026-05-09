package com.schemaplexai.model.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Execution event published by Engine via Outbox → MQ.
 * Consumed by Core consumers (SSE, Audit, Cost projection).
 */
public record ExecutionEventMessage(
        UUID eventId,
        Long executionId,
        int seq,
        String eventType,
        String payload,
        Instant occurredAt,
        Long tenantId,
        Long agentId,
        String sensitivity
) {
}
