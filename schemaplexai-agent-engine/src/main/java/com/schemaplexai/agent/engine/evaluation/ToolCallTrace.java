package com.schemaplexai.agent.engine.evaluation;

import com.schemaplexai.agent.engine.tool.ToolExecutionResult;

public record ToolCallTrace(
    String toolName,
    ToolExecutionResult result
) {
}
