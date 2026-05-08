package com.schemaplexai.agent.engine.scheduler;

import java.time.Instant;
import java.util.Optional;

/**
 * Immutable record representing an SLA breach event.
 *
 * @param executionId       the execution that breached SLA
 * @param tenantId          tenant identifier
 * @param scheduledDeadline the deadline that was missed
 * @param actualStartTime   optional actual start time (empty if still queued)
 * @param breachType        type of breach
 */
public record SlaBreachEvent(
        Long executionId,
        String tenantId,
        Instant scheduledDeadline,
        Optional<Instant> actualStartTime,
        SlaBreachEvent.BreachType breachType
) {

    public enum BreachType {
        DEADLINE_MISSED,
        QUEUE_TIMEOUT
    }

    public SlaBreachEvent {
        if (executionId == null) {
            throw new IllegalArgumentException("executionId must not be null");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank");
        }
        if (scheduledDeadline == null) {
            throw new IllegalArgumentException("scheduledDeadline must not be null");
        }
        if (actualStartTime == null) {
            actualStartTime = Optional.empty();
        }
        if (breachType == null) {
            throw new IllegalArgumentException("breachType must not be null");
        }
    }
}
