package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThinkingStateHandler implements AgentStateHandler {

    private static final String DEFAULT_MODEL = "gpt-4";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int TOOL_DETECTION_THRESHOLD = 3;

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
            // 1. Load conversation history
            List<LlmMessage> messages = chatMemoryStore.loadMessages(execution.getConversationId());

            // 2. Inject context (system prompt, team context, knowledge)
            contextInjector.inject(messages, execution.getAgentId());

            // 3. Build prompt from messages
            String prompt = buildPrompt(messages);

            // 4. Estimate input tokens (rough: 1 token ~ 4 chars)
            long inputTokens = estimateTokens(prompt);

            // 5. Check token budget if present
            TokenBudget budget = loadBudget(execution);
            if (budget != null && !budget.consumeInput(inputTokens)) {
                log.warn("Token budget exceeded for execution {} (input: {}, remaining: {})",
                        execution.getId(), inputTokens, budget.remainingInput());
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // 6. Call LLM with fallback
            String modelId = resolveModelId(execution);
            String response = modelRouter.generateWithFallback(prompt, modelId, DEFAULT_TEMPERATURE);
            long outputTokens = estimateTokens(response);

            if (budget != null) {
                if (!budget.consumeOutput(outputTokens)) {
                    log.warn("Output token budget exceeded for execution {} (output: {}, remaining: {})",
                            execution.getId(), outputTokens, budget.remainingOutput());
                    stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                    return;
                }
                saveBudget(execution, budget);
            }

            // 7. Save assistant response to memory
            chatMemoryStore.saveMessage(execution.getConversationId(), new LlmMessage("assistant", response));

            // 8. Determine next state based on response
            if (containsToolCalls(response)) {
                log.info("Execution {} detected tool calls, transitioning to TOOL_CALLING", execution.getId());
                stateMachine.transition(AgentExecutionState.TOOL_CALLING, execution);
            } else {
                log.info("Execution {} completed with direct answer, transitioning to COMPLETED", execution.getId());
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            }
        } catch (Exception e) {
            log.error("Thinking state failed for execution {}", execution.getId(), e);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    private String buildPrompt(List<LlmMessage> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (LlmMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }

    private long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 4L);
    }

    private boolean containsToolCalls(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        int toolIndicatorCount = 0;
        if (response.contains("<tool>")) toolIndicatorCount++;
        if (response.contains("<function>")) toolIndicatorCount++;
        if (response.contains("```tool")) toolIndicatorCount++;
        if (response.contains("TOOL_CALL")) toolIndicatorCount++;
        if (response.contains("invoke_tool")) toolIndicatorCount++;
        return toolIndicatorCount >= 1;
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

    private String resolveModelId(SfAgentExecution execution) {
        // TODO: Read from execution model config when available
        return DEFAULT_MODEL;
    }
}
