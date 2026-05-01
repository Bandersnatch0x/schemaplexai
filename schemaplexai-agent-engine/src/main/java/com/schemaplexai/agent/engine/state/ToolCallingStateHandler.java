package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionRecorder;
import com.schemaplexai.agent.engine.tool.ToolExecutionResult;
import com.schemaplexai.agent.engine.tool.ToolSafetyGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCallingStateHandler implements AgentStateHandler {

    private final CompositeChatMemoryStore chatMemoryStore;
    private final ToolSafetyGuard safetyGuard;
    private final ToolExecutionRecorder executionRecorder;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.TOOL_CALLING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering TOOL_CALLING state, execution {}", execution.getAgentId(), execution.getId());

        try {
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

            List<ToolCall> toolCalls = parseToolCalls(lastMessage.getContent());
            if (toolCalls.isEmpty()) {
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
                return;
            }

            for (ToolCall toolCall : toolCalls) {
                ToolExecutionResult result = executeToolWithGuard(toolCall);
                executionRecorder.record(execution.getId(), result);

                if (result.blocked()) {
                    log.error("Tool {} blocked for execution {}: {}",
                        toolCall.name, execution.getId(), result.errorMessage());
                    chatMemoryStore.saveMessage(execution.getConversationId(),
                        new LlmMessage("tool", "BLOCKED: " + result.errorMessage()));
                    stateMachine.transition(AgentExecutionState.FAILED, execution);
                    return;
                }

                chatMemoryStore.saveMessage(execution.getConversationId(),
                    new LlmMessage("tool", result.output()));
            }

            stateMachine.transition(AgentExecutionState.THINKING, execution);
        } catch (Exception e) {
            log.error("Tool calling failed for execution {}", execution.getId(), e);
            executionRecorder.record(execution.getId(), ToolExecutionResult.failure(
                "unknown", ToolErrorCategory.UNEXPECTED_ENVIRONMENT,
                e.getMessage(), 0, 0));
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    private ToolExecutionResult executeToolWithGuard(ToolCall toolCall) {
        ToolSafetyGuard.SafetyCheckResult safety = safetyGuard.check(toolCall.name, toolCall.arguments);
        if (safety.blocked()) {
            return ToolExecutionResult.blocked(toolCall.name, safety.errorCategory(), safety.reason());
        }

        long startTime = System.currentTimeMillis();
        try {
            String output = executeToolStub(toolCall);
            long latency = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.success(toolCall.name, output, latency, estimateTokens(output));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.failure(toolCall.name, ToolErrorCategory.UNEXPECTED_ENVIRONMENT,
                e.getMessage(), latency, 0);
        }
    }

    private List<ToolCall> parseToolCalls(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        // Stub: parse tool name from content for testing purposes
        // Look for pattern "calling <toolName>" in the message
        String trimmed = content.trim();
        if (trimmed.startsWith("calling ")) {
            String toolName = trimmed.substring(8).trim();
            return List.of(new ToolCall(toolName, trimmed));
        }
        return List.of();
    }

    private String executeToolStub(ToolCall toolCall) {
        return "Tool " + toolCall.name + " executed with args: " + toolCall.arguments;
    }

    private int estimateTokens(String text) {
        if (text == null) {
            return 0;
        }
        return text.length() / 4;
    }

    private record ToolCall(String name, String arguments) {}
}
