package com.schemaplexai.agent.engine.evaluation;

import com.schemaplexai.agent.engine.state.AgentExecutionState;

import java.time.Duration;
import java.util.List;

public record AgentExecutionTrace(
    String executionId,
    AgentExecutionState finalState,
    int iterationCount,
    int totalTokens,
    int outputTokens,
    Duration duration,
    List<ToolCallTrace> toolCalls
) {
}
