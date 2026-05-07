package com.schemaplexai.agent.engine.tool.subagent;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tool adapter that delegates work to a sub-agent.
 *
 * <p>Tool name: {@code "task"}</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code prompt} (required) — the task description for the sub-agent</li>
 *   <li>{@code role} (optional) — role name for the sub-agent, defaults to "subagent"</li>
 *   <li>{@code agentId} (optional) — specific agent definition ID to use</li>
 * </ul>
 *
 * <p>Guardrails:</p>
 * <ul>
 *   <li>Quota checks via {@link SubAgentQuotaService}</li>
 *   <li>Max depth enforcement to prevent infinite sub-agent recursion</li>
 *   <li>Guardrails are inherited from parent and decremented for each nesting level</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskToolAdapter implements ToolAdapter {

    private static final String TOOL_NAME = "task";
    private static final String DEFAULT_ROLE = "subagent";
    private static final int DEFAULT_MAX_DEPTH = 3;

    private final SubAgentExecutionService subAgentService;
    private final SubAgentQuotaService quotaService;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolCall call, ExecutionContext ctx) {
        Map<String, Object> params = call.parameters();

        // Validate prompt
        Object promptObj = params.get("prompt");
        if (promptObj == null || promptObj.toString().isBlank()) {
            return ToolResult.error("Missing required parameter: 'prompt' must be a non-empty string");
        }
        String prompt = promptObj.toString();

        // Check max depth guardrail
        Map<String, Object> guardrails = ctx.guardrails() != null ? ctx.guardrails() : Map.of();
        int currentMaxDepth = getGuardrailInt(guardrails, "maxDepth", DEFAULT_MAX_DEPTH);
        if (currentMaxDepth <= 0) {
            return ToolResult.error("Sub-agent max depth exceeded. Cannot spawn further nested agents.");
        }

        // Check quota before execution
        Long parentExecutionId = ctx.executionId();
        String tenantId = ctx.tenantId();
        try {
            quotaService.checkAndIncrementForTenant(tenantId, parentExecutionId);
        } catch (SubAgentQuotaExceededException ex) {
            log.warn("Sub-agent quota exceeded for tenant={}, parent={}", tenantId, parentExecutionId);
            return ToolResult.error("Sub-agent quota exceeded: " + ex.getMessage());
        }

        // Build sub-agent request
        String role = params.getOrDefault("role", DEFAULT_ROLE).toString();
        SubAgentRequest request = SubAgentRequest.builder()
                .parentExecutionId(parentExecutionId)
                .prompt(prompt)
                .role(role)
                .inheritedGuardrails(guardrails)
                .maxDepth(currentMaxDepth - 1)
                .build();

        try {
            SubAgentResult result = subAgentService.execute(request);
            return ToolResult.success(result.output());
        } catch (Exception ex) {
            log.error("Sub-agent execution failed for parent={}", parentExecutionId, ex);
            // Decrement quota on failure so the parent can retry
            quotaService.decrement(parentExecutionId);
            return ToolResult.error("Sub-agent execution failed: " + ex.getMessage());
        }
    }

    private int getGuardrailInt(Map<String, Object> guardrails, String key, int defaultValue) {
        Object value = guardrails.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
