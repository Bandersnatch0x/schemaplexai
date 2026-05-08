package com.schemaplexai.agent.engine.scheduler;

import java.time.Instant;
import java.util.Optional;

/**
 * Immutable record representing a prioritized execution task.
 *
 * @param executionId          unique execution identifier
 * @param agentId              associated agent identifier
 * @param tenantId             tenant identifier for isolation
 * @param priority             execution priority level
 * @param submittedAt          timestamp when the execution was submitted
 * @param deadline             optional SLA deadline
 * @param estimatedDurationMs  estimated execution duration in milliseconds
 */
public record PrioritizedExecution(
        Long executionId,
        Long agentId,
        String tenantId,
        ExecutionPriority priority,
        Instant submittedAt,
        Optional<Instant> deadline,
        long estimatedDurationMs
) {

    public PrioritizedExecution {
        if (executionId == null) {
            throw new IllegalArgumentException("executionId must not be null");
        }
        if (agentId == null) {
            throw new IllegalArgumentException("agentId must not be null");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank");
        }
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
        if (submittedAt == null) {
            throw new IllegalArgumentException("submittedAt must not be null");
        }
        if (deadline == null) {
            deadline = Optional.empty();
        }
        if (estimatedDurationMs < 0) {
            throw new IllegalArgumentException("estimatedDurationMs must not be negative");
        }
    }

    /**
     * Returns a new PrioritizedExecution with the given priority.
     */
    public PrioritizedExecution withPriority(ExecutionPriority newPriority) {
        return new PrioritizedExecution(
                this.executionId,
                this.agentId,
                this.tenantId,
                newPriority,
                this.submittedAt,
                this.deadline,
                this.estimatedDurationMs
        );
    }
}
