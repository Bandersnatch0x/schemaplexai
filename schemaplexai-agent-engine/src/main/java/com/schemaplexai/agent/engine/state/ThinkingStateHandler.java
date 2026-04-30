package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThinkingStateHandler implements AgentStateHandler {

    private final ContextInjector contextInjector;
    private final CompositeChatMemoryStore chatMemoryStore;
    private final AiModelRouter modelRouter;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.THINKING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering THINKING state, execution {}", execution.getAgentId(), execution.getId());

        try {
            // Load conversation history
            List<LlmMessage> messages = chatMemoryStore.loadMessages(execution.getConversationId());

            // Inject context (system prompt, team context, knowledge)
            contextInjector.inject(messages, execution.getAgentId());

            // Build prompt from messages
            String prompt = buildPrompt(messages);

            // Estimate input tokens (rough: 1 token ≈ 4 chars)
            long inputTokens = estimateTokens(prompt);

            // Check token budget if present
            TokenBudget budget = loadBudget(execution);
            if (budget != null && !budget.consumeInput(inputTokens)) {
                log.warn("Token budget exceeded for execution {}", execution.getId());
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // Call LLM
            String response = modelRouter.generateWithFallback(prompt, "gpt-4", 0.7);
            long outputTokens = estimateTokens(response);
            if (budget != null) {
                budget.consumeOutput(outputTokens);
                saveBudget(execution, budget);
            }

            // Save assistant response to memory
            chatMemoryStore.saveMessage(execution.getConversationId(), new LlmMessage("assistant", response));

            // Determine next state based on response
            if (containsToolCalls(response)) {
                stateMachine.transition(AgentExecutionState.TOOL_CALLING, execution);
            } else {
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            }
        } catch (Exception e) {
            log.error("Thinking state failed for execution {}", execution.getId(), e);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    private String buildPrompt(List<LlmMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (LlmMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    private long estimateTokens(String text) {
        return text.length() / 4L;
    }

    private boolean containsToolCalls(String response) {
        return response != null && (
                response.contains("<tool>") ||
                response.contains("<function>") ||
                response.contains("```tool")
        );
    }

    private TokenBudget loadBudget(SfAgentExecution execution) {
        if (execution.getTokenBudgetJson() == null || execution.getTokenBudgetJson().isBlank()) {
            return null;
        }
        try {
            String[] parts = execution.getTokenBudgetJson().split(",");
            long maxInput = Long.parseLong(parts[0]);
            long maxOutput = Long.parseLong(parts[1]);
            long consumedInput = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
            long consumedOutput = parts.length > 3 ? Long.parseLong(parts[3]) : 0;
            TokenBudget budget = new TokenBudget(maxInput, maxOutput);
            budget.consumeInput(consumedInput);
            budget.consumeOutput(consumedOutput);
            return budget;
        } catch (Exception e) {
            log.warn("Failed to parse token budget for execution {}", execution.getId());
            return null;
        }
    }

    private void saveBudget(SfAgentExecution execution, TokenBudget budget) {
        String json = budget.getMaxInputTokens() + "," + budget.getMaxOutputTokens() + "," +
                budget.getConsumedInputTokens().get() + "," + budget.getConsumedOutputTokens().get();
        execution.setTokenBudgetJson(json);
    }
}
