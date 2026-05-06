package com.schemaplexai.agent.engine.tool.adapter;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;

/**
 * Unified interface for tool adapters.
 * All concrete tool implementations (FileRead, HttpCall, etc.) must implement this interface.
 * Each adapter is auto-discovered by ToolRegistry via Spring dependency injection.
 */
public interface ToolAdapter {

    /**
     * Execute a tool call within the given execution context.
     *
     * @param call the tool call request containing name and parameters
     * @param ctx  runtime execution context (tenant, workspace, etc.)
     * @return the result of tool execution
     * @throws ToolExecutionException if execution fails with a known error category
     */
    ToolResult execute(ToolCall call, ExecutionContext ctx) throws ToolExecutionException;

    /**
     * Return the unique tool name this adapter handles.
     * Used by ToolRegistry for tool discovery via resolve().
     */
    String getToolName();
}
