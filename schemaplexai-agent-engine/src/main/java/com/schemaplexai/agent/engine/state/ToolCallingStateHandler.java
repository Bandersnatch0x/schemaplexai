package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolCallingStateHandler implements AgentStateHandler {

    private final CompositeChatMemoryStore chatMemoryStore;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.TOOL_CALLING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering TOOL_CALLING state, execution {}", execution.getAgentId(), execution.getId());

        try {
            // Get the last assistant message which should contain tool calls
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

            // Parse and execute tool calls (placeholder until tool registry is fully implemented)
            List<ToolCall> toolCalls = parseToolCalls(lastMessage.getContent());
            for (ToolCall toolCall : toolCalls) {
                log.info("Executing tool {} for execution {}", toolCall.name, execution.getId());
                // Tool execution is stubbed until ToolRegistry is fully implemented
                String result = executeToolStub(toolCall);
                chatMemoryStore.saveMessage(execution.getConversationId(),
                        new LlmMessage("tool", result));
            }

            // After tool execution, go back to THINKING for next iteration
            stateMachine.transition(AgentExecutionState.THINKING, execution);
        } catch (Exception e) {
            log.error("Tool calling failed for execution {}", execution.getId(), e);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    private List<ToolCall> parseToolCalls(String content) {
        // Simple stub: check if content contains tool-like markers
        // Full implementation would parse structured tool call format
        if (content == null || content.isBlank()) {
            return List.of();
        }
        // Return empty list for now — real parsing when tool registry is ready
        return List.of();
    }

    private String executeToolStub(ToolCall toolCall) {
        return "Tool " + toolCall.name + " executed with args: " + toolCall.arguments;
    }

    private record ToolCall(String name, String arguments) {}
}
