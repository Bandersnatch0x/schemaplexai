package com.schemaplexai.agent.engine.orchestrator;

/**
 * Result of an individual sub-task execution.
 *
 * @param taskDescription the original sub-task description
 * @param agentId         the agent that executed the task (null if no agent matched)
 * @param status          execution status
 * @param errorMessage    error message (null if successful)
 */
public record SubTaskResult(
        String taskDescription,
        String agentId,
        SubTaskStatus status,
        String errorMessage
) {
    public boolean isSuccess() {
        return status == SubTaskStatus.COMPLETED;
    }
}
