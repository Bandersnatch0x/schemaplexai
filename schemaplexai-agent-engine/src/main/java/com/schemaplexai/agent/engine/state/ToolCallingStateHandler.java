package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.config.AgentEngineProperties;
import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.tool.*;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.agent.engine.tool.registry.ToolRegistry;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles TOOL_CALLING state with structured parsing, loop detection, and security checks.
 *
 * Refactored from heuristic parseToolCalls() / executeToolStub() to:
 * - ToolRegistry.parse() for structured OpenAI/Anthropic parsing
 * - ToolAdapter.execute() for real tool execution
 * - AgentLoopDetectionService for loop prevention
 * - ToolSafetyGuard for security validation
 * - SecurityPolicyLoader for tenant-aware env checks
 */
@Slf4j
@Component
public class ToolCallingStateHandler implements AgentStateHandler {

    private final CompositeChatMemoryStore chatMemoryStore;
    private final ToolSandbox sandbox;
    private final ToolRegistry toolRegistry;
    private final ToolSafetyGuard safetyGuard;
    private final AgentLoopDetectionService loopDetection;
    private final ToolExecutionRecorder executionRecorder;
    private final SecurityPolicyLoader securityPolicyLoader;
    private final AgentEngineProperties engineProperties;

    @Autowired
    public ToolCallingStateHandler(CompositeChatMemoryStore chatMemoryStore,
                                    ToolSandbox sandbox,
                                    ToolRegistry toolRegistry,
                                    ToolSafetyGuard safetyGuard,
                                    AgentLoopDetectionService loopDetection,
                                    ToolExecutionRecorder executionRecorder,
                                    SecurityPolicyLoader securityPolicyLoader,
                                    AgentEngineProperties engineProperties) {
        this.chatMemoryStore = chatMemoryStore;
        this.sandbox = sandbox;
        this.toolRegistry = toolRegistry;
        this.safetyGuard = safetyGuard;
        this.loopDetection = loopDetection;
        this.executionRecorder = executionRecorder;
        this.securityPolicyLoader = securityPolicyLoader;
        this.engineProperties = engineProperties;
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.TOOL_CALLING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering TOOL_CALLING state, execution {}", execution.getAgentId(), execution.getId());

        try {
            // 1. Load conversation history
            List<LlmMessage> messages = chatMemoryStore.loadMessages(execution.getConversationId());
            if (messages.isEmpty()) {
                log.warn("No messages found for execution {}, skipping tool calls", execution.getId());
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
                return;
            }

            LlmMessage lastMessage = messages.get(messages.size() - 1);
            if (!"assistant".equals(lastMessage.getRole())) {
                log.warn("Last message is not from assistant for execution {}", execution.getId());
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
                return;
            }

            // 2. Structured tool call parsing (replaces heuristic parseToolCalls)
            List<ToolCall> toolCalls = toolRegistry.parse(lastMessage.getContent(), null);

            // 3. Tool-call budget check
            int currentCount = getToolCallCount(execution);
            if (currentCount + toolCalls.size() > engineProperties.getMaxToolCalls()) {
                log.warn("Tool-call budget exceeded for execution {}: current={}, pending={}, max={}",
                        execution.getId(), currentCount, toolCalls.size(), engineProperties.getMaxToolCalls());
                execution.setMetadata("blockedReason", "tool_call_budget_exceeded");
                execution.setMetadata("admissionType", "BUDGET");
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // 4. Loop detection — check for repeated tool calls
            String responseHash = hashContent(lastMessage.getContent());
            List<String> toolNames = toolCalls.stream().map(ToolCall::toolName).toList();
            LoopDetectionResult loopResult = loopDetection.detectLoop(execution.getId(), responseHash, toolNames);
            if (loopResult.loopDetected()) {
                log.warn("Loop detected in execution {}: {}", execution.getId(), loopResult.reason());
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // 5. Execute each tool call with security checks
            boolean isRetry = isRetryContext(execution);
            for (ToolCall toolCall : toolCalls) {
                // If retrying, only execute the failed tool call (not all calls)
                if (isRetry && !isRetryTarget(execution, toolCall)) {
                    log.info("Skipping non-retry tool {} in retry context for execution {}",
                            toolCall.toolName(), execution.getId());
                    continue;
                }

                ToolExecutionResult result = executeToolWithGuard(execution, toolCall);
                executionRecorder.record(execution.getId(), result);

                if (!result.success() && !result.blocked()) {
                    // Record error category for retry decision
                    if (result.errorCategory() != null) {
                        execution.setMetadata("lastErrorCategory", result.errorCategory().name());
                    }
                    if (result.errorCategory() != null && result.errorCategory().isRetryable()) {
                        stateMachine.transition(AgentExecutionState.RETRYING, execution);
                        return;
                    }
                    stateMachine.transition(AgentExecutionState.FAILED, execution);
                    return;
                }

                if (result.blocked()) {
                    log.warn("Tool {} blocked for execution {}: {}",
                            toolCall.toolName(), execution.getId(), result.errorMessage());
                    stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                    return;
                }

                // Save tool result to conversation memory
                chatMemoryStore.saveMessage(execution.getConversationId(),
                        new LlmMessage("tool", result.output()));

                incrementToolCallCount(execution);
            }

            // 5. Transition back to THINKING for next LLM round
            stateMachine.transition(AgentExecutionState.THINKING, execution);
        } catch (Exception e) {
            log.error("Tool calling failed for execution {}", execution.getId(), e);
            executionRecorder.record(execution.getId(), ToolExecutionResult.failure(
                "unknown", ToolErrorCategory.UNEXPECTED_ENVIRONMENT,
                e.getMessage(), 0, 0));
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    /**
     * Execute a single tool call with safety guard and sandbox.
     * Replaces the old executeToolStub() with real tool adapter execution.
     */
    private ToolExecutionResult executeToolWithGuard(SfAgentExecution execution, ToolCall toolCall) {
        // Resolve tool adapter (whitelist check)
        ToolAdapter adapter = toolRegistry.resolve(toolCall.toolName());
        if (adapter == null) {
            return ToolExecutionResult.failure(toolCall.toolName(),
                    ToolErrorCategory.INVALID_ARGUMENT,
                    "Tool not registered: " + toolCall.toolName(), 0, 0);
        }

        // Load tenant security policy for environment-aware checks
        String environment = execution.getTenantId();
        var config = securityPolicyLoader.load(environment);
        if (config != null && config.getEnvironment() != null) {
            environment = config.getEnvironment();
        }

        // Safety guard check
        ToolSafetyGuard.SafetyCheckResult safety = safetyGuard.check(
                toolCall.toolName(),
                toolCall.parameters() != null ? toolCall.parameters().toString() : null,
                environment);
        if (safety.blocked()) {
            return ToolExecutionResult.blocked(toolCall.toolName(), safety.errorCategory(), safety.reason());
        }

        // Execute tool via adapter + sandbox
        long startTime = System.currentTimeMillis();
        try {
            ExecutionContext ctx = new ExecutionContext(
                    execution.getTenantId(),
                    execution.getId(),
                    getWorkspaceRoot(execution)
            );
            ToolResult result = adapter.execute(toolCall, ctx);
            long latency = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.success(toolCall.toolName(), result.output(), latency, estimateTokens(result.output()));
        } catch (ToolExecutionException e) {
            long latency = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.failure(toolCall.toolName(), e.getErrorCategory(),
                    e.getMessage(), latency, 0);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.failure(toolCall.toolName(),
                    ToolErrorCategory.INTERNAL_ERROR, e.getMessage(), latency, 0);
        }
    }

    private int getToolCallCount(SfAgentExecution execution) {
        Object count = execution.getMetadata("toolCallCount");
        if (count instanceof Number) {
            return ((Number) count).intValue();
        }
        return 0;
    }

    private void incrementToolCallCount(SfAgentExecution execution) {
        int current = getToolCallCount(execution);
        execution.setMetadata("toolCallCount", current + 1);
    }

    private boolean isRetryContext(SfAgentExecution execution) {
        String retryCtx = (String) execution.getMetadata("retryContext");
        return retryCtx != null && !retryCtx.isBlank();
    }

    private boolean isRetryTarget(SfAgentExecution execution, ToolCall toolCall) {
        // During retry, only execute the specific failed tool call
        String failedTool = (String) execution.getMetadata("failedToolName");
        return failedTool != null && failedTool.equals(toolCall.toolName());
    }

    private String hashContent(String content) {
        if (content == null) return "empty";
        return String.valueOf(content.hashCode());
    }

    private int estimateTokens(String text) {
        return (int) TokenEstimator.estimate(text);
    }

    private String getWorkspaceRoot(SfAgentExecution execution) {
        String workspace = (String) execution.getMetadata("workspaceRoot");
        return workspace != null ? workspace : System.getProperty("user.dir");
    }
}
