package com.schemaplexai.model.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cost event published by Engine when token usage / LLM cost is recorded.
 * Consumed by Core CostSyncConsumer for PG projection (v1 short-path).
 */
public record CostRecordedEvent(
        UUID eventId,
        Long executionId,
        Long tenantId,
        Long agentId,
        String modelName,
        String provider,
        String requestType,
        Long inputTokens,
        Long outputTokens,
        Long totalTokens,
        BigDecimal costAmount,
        String currency,
        Instant occurredAt
) {
}
