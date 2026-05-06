package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Handles THINKING state — LLM reasoning with loop detection and token budget gating.
 *
 * Integrated with AgentLoopDetectionService: checks for agent loops before transitioning
 * to TOOL_CALLING, preventing infinite tool calling cycles.
 */
@Slf4j
@Component
public class ThinkingStateHandler implements AgentStateHandler {

    private static final String DEFAULT_MODEL = "gpt-4";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final ContextInjector contextInjector;
    private final CompositeChatMemoryStore chatMemoryStore;
    private final AiModelRouter modelRouter;
    private final AgentLoopDetectionService loopDetection;

    public ThinkingStateHandler(ContextInjector contextInjector,
                                 CompositeChatMemoryStore chatMemoryStore,
                                 AiModelRouter modelRouter,
                                 AgentLoopDetectionService loopDetection) {
        this.contextInjector = contextInjector;
        this.chatMemoryStore = chatMemoryStore;
        this.modelRouter = modelRouter;
        this.loopDetection = loopDetection;
    }

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
                execution.setMetadata("blockedReason", "token_budget_exceeded");
                execution.setMetadata("admissionType", "BUDGET");
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
                    execution.setMetadata("blockedReason", "output_token_budget_exceeded");
                    execution.setMetadata("admissionType", "BUDGET");
                    stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                    return;
                }
                saveBudget(execution, budget);
            }

            // 7. Save assistant response to memory
            chatMemoryStore.saveMessage(execution.getConversationId(), new LlmMessage("assistant", response));

            // 8. Loop detection before transitioning to TOOL_CALLING
            if (containsToolCalls(response)) {
                List<String> toolNames = extractToolNames(response);
                String responseHash = String.valueOf(response.hashCode());
                LoopDetectionResult loopResult = loopDetection.detectLoop(
                        execution.getId(), responseHash, toolNames);

                if (loopResult.loopDetected()) {
                    log.warn("Loop detected in THINKING for execution {}: {}",
                            execution.getId(), loopResult.reason());
                    execution.setMetadata("blockedReason", "agent_loop_" + loopResult.reason());
                    execution.setMetadata("admissionType", "LOOP");
                    stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                    return;
                }

                log.info("Execution {} detected tool calls, transitioning to TOOL_CALLING", execution.getId());
                stateMachine.transition(AgentExecutionState.TOOL_CALLING, execution);
            } else {
                log.info("Execution {} completed with direct answer, transitioning to COMPLETED", execution.getId());
                loopDetection.clearRecords(execution.getId());
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
        return TokenEstimator.estimate(text);
    }

    private boolean containsToolCalls(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }
        return response.contains("<tool_use>")
                || response.contains("tool_calls")
                || response.contains("<function>")
                || response.contains("<tool>")
                || response.contains("```tool")
                || response.contains("invoke_tool");
    }

    private List<String> extractToolNames(String response) {
        if (response == null || response.isBlank()) {
            return Collections.emptyList();
        }
        // Simple extraction: look for tool_use name tags or function.name patterns
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "<name>([^<]+)</name>|\"name\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(response);
        while (m.find()) {
            String name = m.group(1) != null ? m.group(1) : m.group(2);
            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }
        return names;
    }

    private TokenBudget loadBudget(SfAgentExecution execution) {
        if (execution.getTokenBudgetJson() == null || execution.getTokenBudgetJson().isBlank()) {
            return null;
        }
        try {
            String json = execution.getTokenBudgetJson().trim();
            if (json.startsWith("{")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> map = mapper.readValue(json, java.util.Map.class);
                long maxInput = ((Number) map.getOrDefault("maxInput", 0L)).longValue();
                long maxOutput = ((Number) map.getOrDefault("maxOutput", 0L)).longValue();
                long consumedInput = ((Number) map.getOrDefault("consumedInput", 0L)).longValue();
                long consumedOutput = ((Number) map.getOrDefault("consumedOutput", 0L)).longValue();
                TokenBudget budget = new TokenBudget(maxInput, maxOutput);
                budget.consumeInput(consumedInput);
                budget.consumeOutput(consumedOutput);
                return budget;
            }
            // Legacy fallback: comma-separated format
            String[] parts = json.split(",");
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
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Long> map = java.util.Map.of(
                "maxInput", budget.getMaxInputTokens(),
                "maxOutput", budget.getMaxOutputTokens(),
                "consumedInput", budget.getConsumedInputTokens().get(),
                "consumedOutput", budget.getConsumedOutputTokens().get()
            );
            execution.setTokenBudgetJson(mapper.writeValueAsString(map));
        } catch (Exception e) {
            log.warn("Failed to serialize token budget for execution {}", execution.getId());
        }
    }

    private String resolveModelId(SfAgentExecution execution) {
        // TODO: Read from execution model config when available
        return DEFAULT_MODEL;
    }
}
