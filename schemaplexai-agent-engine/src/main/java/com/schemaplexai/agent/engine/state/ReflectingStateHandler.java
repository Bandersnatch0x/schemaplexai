package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.evaluation.ReflectionResult;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reflecting state handler - implements self-correction mechanism.
 * The agent evaluates its own output via LLM self-review, with a maximum of 2 reflection rounds.
 * If the output passes review or the round limit is reached, transitions to COMPLETED.
 * Otherwise, transitions back to THINKING for revision.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReflectingStateHandler implements AgentStateHandler {

    private static final int MAX_REFLECTION_ROUNDS = 2;
    private static final String DEFAULT_MODEL = "gpt-4";
    private static final double REFLECTION_TEMPERATURE = 0.3;

    private final AiModelRouter modelRouter;
    private final GuardrailsEngine guardrailsEngine;
    private final CompositeChatMemoryStore chatMemoryStore;

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.REFLECTING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering REFLECTING state, execution {}", execution.getAgentId(), execution.getId());

        int reflectionCount = getReflectionCount(execution);

        // Check reflection round limit
        if (reflectionCount >= MAX_REFLECTION_ROUNDS) {
            log.info("Max reflection rounds ({}) reached for execution {}, accepting output",
                    MAX_REFLECTION_ROUNDS, execution.getId());
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            return;
        }

        // Load last assistant message from chat memory
        List<LlmMessage> messages = chatMemoryStore.loadMessages(execution.getConversationId());
        String assistantOutput = extractLastAssistantMessage(messages);

        // Guardrails check on the assistant output
        if (assistantOutput != null) {
            var guardResult = guardrailsEngine.validateOutput(assistantOutput);
            if (!guardResult.success()) {
                log.warn("Guardrails blocked output for execution {}: {}", execution.getId(), guardResult.errorMessage());
                stateMachine.transition(AgentExecutionState.FAILED, execution);
                return;
            }
        }

        try {
            // Build reflection prompt
            String userMessage = extractLastUserMessage(messages);
            String reflectionPrompt = buildReflectionPrompt(userMessage, assistantOutput);

            // Call LLM for self-evaluation
            String modelId = resolveModelId(execution);
            String response = modelRouter.generateWithFallback(
                    reflectionPrompt, modelId, REFLECTION_TEMPERATURE
            );

            // Parse reflection result
            ReflectionResult reflectionResult = parseReflectionResult(response);

            if (reflectionResult.needsRevision()) {
                log.info("Execution {} reflection round {} requires revision: {}",
                        execution.getId(), reflectionCount + 1, reflectionResult.suggestions());
                setReflectionCount(execution, reflectionCount + 1);
                stateMachine.saveExecution(execution);
                stateMachine.transition(AgentExecutionState.THINKING, execution);
            } else {
                log.info("Execution {} reflection round {} passed review",
                        execution.getId(), reflectionCount + 1);
                stateMachine.transition(AgentExecutionState.COMPLETED, execution);
            }
        } catch (Exception e) {
            log.error("Reflection failed for execution {}, accepting current output", execution.getId(), e);
            stateMachine.transition(AgentExecutionState.COMPLETED, execution);
        }
    }

    String buildReflectionPrompt(String userMessage, String assistantOutput) {
        String question = userMessage != null ? userMessage : "(unknown)";
        String answer = assistantOutput != null ? assistantOutput : "(no output)";

        return """
                You are a quality reviewer. Evaluate the following AI response for accuracy, completeness, and safety.

                User's original question: %s

                AI's response: %s

                Consider:
                1. Is the response factually accurate?
                2. Is it complete and helpful?
                3. Does it contain any harmful or inappropriate content?
                4. Could it be improved?

                Respond with:
                - "PASS" if the response is good enough
                - "REVISE: [specific suggestions]" if improvements are needed

                Your evaluation:
                """.formatted(question, answer);
    }

    ReflectionResult parseReflectionResult(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            return ReflectionResult.ofAccepted();
        }

        String upper = llmOutput.toUpperCase();
        if (upper.contains("PASS")) {
            return ReflectionResult.ofAccepted();
        }

        int reviseIndex = upper.indexOf("REVISE:");
        if (reviseIndex >= 0) {
            String suggestions = llmOutput.substring(reviseIndex + 7).trim();
            return ReflectionResult.ofNeedsRevision(suggestions);
        }

        // Default to accepted if format is unrecognizable
        return ReflectionResult.ofAccepted();
    }

    int getReflectionCount(SfAgentExecution execution) {
        String budget = execution.getTokenBudgetJson();
        if (budget != null && budget.contains("reflections=")) {
            try {
                String val = budget.substring(budget.indexOf("reflections=") + 12);
                int commaIdx = val.indexOf(',');
                if (commaIdx > 0) val = val.substring(0, commaIdx);
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private void setReflectionCount(SfAgentExecution execution, int count) {
        String existing = execution.getTokenBudgetJson();
        if (existing == null || existing.isBlank()) {
            execution.setTokenBudgetJson("reflections=" + count);
        } else if (existing.contains("reflections=")) {
            String updated = existing.replaceAll("reflections=\\d+", "reflections=" + count);
            execution.setTokenBudgetJson(updated);
        } else {
            execution.setTokenBudgetJson(existing + ",reflections=" + count);
        }
    }

    private String extractLastAssistantMessage(List<LlmMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("assistant".equals(messages.get(i).getRole())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }

    private String extractLastUserMessage(List<LlmMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }

    private String resolveModelId(SfAgentExecution execution) {
        return DEFAULT_MODEL;
    }
}
