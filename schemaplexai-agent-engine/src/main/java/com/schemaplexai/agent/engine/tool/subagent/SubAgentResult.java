package com.schemaplexai.agent.engine.tool.subagent;

/**
 * Result of a sub-agent execution.
 *
 * @param output      the textual output produced by the sub-agent
 * @param executionId the execution ID of the child agent run
 */
public record SubAgentResult(String output, Long executionId) {
}
