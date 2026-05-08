package com.schemaplexai.agent.engine.scheduler;

import lombok.Getter;

/**
 * Execution priority levels. Lower ordinal value = higher priority.
 */
@Getter
public enum ExecutionPriority {

    CRITICAL(1),
    HIGH(2),
    NORMAL(3),
    LOW(4),
    BACKGROUND(5);

    private final int weight;

    ExecutionPriority(int weight) {
        this.weight = weight;
    }
}
