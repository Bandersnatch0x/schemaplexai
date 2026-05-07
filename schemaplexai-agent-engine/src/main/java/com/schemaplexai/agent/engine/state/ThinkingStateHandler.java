package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.evaluation.ValidationResult;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.memory.compaction.AutoCompactionService;
import com.schemaplexai.agent.engine.memory.compaction.CompactionResult;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.plan.SubTask;
import com.schemaplexai.agent.engine.plan.SubTaskPlan;
import com.schemaplexai.agent.engine.role.RoleOverlay;
import com.schemaplexai.agent.engine.role.RoleRegistry;
import com.schemaplexai.agent.engine.skill.SkillDefinition;
import com.schemaplexai.agent.engine.skill.SkillRegistry;
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

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final String METADATA_KEY_PLAN = "subTaskPlan";

    private final ContextInjector contextInjector;
    private final CompositeChatMemoryStore chatMemoryStore;
    private final AiModelRouter modelRouter;
    private final AgentLoopDetectionService loopDetection;
    private final com.schemaplexai.agent.engine.model.ModelResolver modelResolver;
    private final GuardrailsEngine guardrailsEngine;
    private final SkillRegistry skillRegistry;
    private final RoleRegistry roleRegistry;
    private final AutoCompactionService autoCompactionService;

    public ThinkingStateHandler(ContextInjector contextInjector,
                                 CompositeChatMemoryStore chatMemoryStore,
                                 AiModelRouter modelRouter,
                                 AgentLoopDetectionService loopDetection,
                                 com.schemaplexai.agent.engine.model.ModelResolver modelResolver,
                                 GuardrailsEngine guardrailsEngine,
                                 SkillRegistry skillRegistry,
                                 RoleRegistry roleRegistry,
                                 AutoCompactionService autoCompactionService) {
        this.contextInjector = contextInjector;
        this.chatMemoryStore = chatMemoryStore;
        this.modelRouter = modelRouter;
        this.loopDetection = loopDetection;
        this.modelResolver = modelResolver;
        this.guardrailsEngine = guardrailsEngine;
        this.skillRegistry = skillRegistry;
        this.roleRegistry = roleRegistry;
        this.autoCompactionService = autoCompactionService;
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

            // 1a. Auto-compaction: try to fit messages within token budget
            TokenBudget budget = loadBudget(execution);
            if (budget != null) {
                CompactionResult compaction = autoCompactionService.compactIfNeeded(
                        execution.getConversationId(), budget);
                if (compaction.success() && !compaction.noOp()) {
                    log.info("Compaction applied for execution {} using strategy '{}', reloading messages",
                            execution.getId(), compaction.strategyName());
                    messages = chatMemoryStore.loadMessages(execution.getConversationId());
                } else if (!compaction.success()) {
                    log.warn("Compaction failed for execution {}: {}",
                            execution.getId(), compaction.failureReason());
                    execution.setMetadata("blockedReason", "compaction_failed: " + compaction.failureReason());
                    execution.setMetadata("admissionType", "COMPACTION");
                    stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                    return;
                }
            }

            // 2. Inject context (system prompt, team context, knowledge)
            contextInjector.inject(messages, execution.getAgentId());

            // 3. Build prompt from messages with skill/role injection
            String skillName = execution.getSkillName();
            String roleName = execution.getRoleName();
            String tenantId = execution.getTenantId();
            String prompt = buildPrompt(messages, tenantId, roleName, skillName);

            // 3a. Guardrails input validation before LLM call
            ValidationResult inputGuardResult = guardrailsEngine.validateInput(prompt);
            if (!inputGuardResult.success()) {
                log.warn("Guardrails blocked input for execution {}: {}",
                        execution.getId(), inputGuardResult.errorMessage());
                execution.setMetadata("blockedReason", inputGuardResult.errorMessage());
                execution.setMetadata("admissionType", "GUARDRAILS");
                stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                return;
            }

            // 4. Estimate input tokens (rough: 1 token ~ 4 chars)
            long inputTokens = estimateTokens(prompt);

            // 5. Check token budget if present (safety gate after compaction)
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
                // Check if there's an active sub-task plan to progress
                AgentExecutionState nextState = resolveNextStateForPlan(execution);
                log.info("Execution {} completed with direct answer, transitioning to {}", execution.getId(), nextState);
                loopDetection.clearRecords(execution.getId());
                stateMachine.transition(nextState, execution);
            }
        } catch (Exception e) {
            log.error("Thinking state failed for execution {}", execution.getId(), e);
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    private String buildPrompt(List<LlmMessage> messages, String tenantId, String roleName, String skillName) {
        StringBuilder sb = new StringBuilder();

        // Add role overlay if present
        if (roleName != null && !roleName.isBlank()) {
            RoleOverlay role = roleRegistry.resolve(roleName, tenantId);
            if (role != null) {
                sb.append("# Role: ").append(role.name()).append("\n");
                sb.append(role.overlay()).append("\n\n");
            }
        }

        // Add skill instructions if present
        if (skillName != null && !skillName.isBlank()) {
            SkillDefinition skill = skillRegistry.resolve(skillName, tenantId);
            if (skill != null) {
                sb.append("# Skill: ").append(skill.name()).append("\n");
                sb.append(skill.instructions()).append("\n\n");
            }
        }

        // Append original prompt content from messages
        if (messages != null && !messages.isEmpty()) {
            for (LlmMessage msg : messages) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
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
        return modelResolver.resolve(execution);
    }

    /**
     * Checks for an active SubTaskPlan and determines the next state.
     * If a plan exists, marks the current sub-task as completed and either
     * stays in THINKING (next sub-task) or transitions to COMPLETED (all done).
     */
    private AgentExecutionState resolveNextStateForPlan(SfAgentExecution execution) {
        Object planMeta = execution.getMetadata(METADATA_KEY_PLAN);
        if (planMeta == null) {
            return AgentExecutionState.COMPLETED;
        }

        SubTaskPlan plan = deserializePlan(planMeta.toString());
        if (plan == null || plan.getSubTasks().isEmpty()) {
            return AgentExecutionState.COMPLETED;
        }

        // Mark current sub-task as completed if there is one
        if (plan.getCurrentSubTaskId() != null) {
            SubTask current = plan.getSubTaskById(plan.getCurrentSubTaskId());
            if (current != null) {
                current.setStatus(SubTask.STATUS_COMPLETED);
                log.debug("Marked sub-task {} as COMPLETED for execution {}",
                        current.getId(), execution.getId());
            }
        }

        // Find next ready sub-task
        SubTask next = plan.findNextReadySubTask();
        if (next != null) {
            next.setStatus(SubTask.STATUS_IN_PROGRESS);
            plan.setCurrentSubTaskId(next.getId());
            execution.setMetadata(METADATA_KEY_PLAN, serializePlan(plan));
            log.info("Execution {} advancing to sub-task {}: {}",
                    execution.getId(), next.getId(), next.getDescription());
            return AgentExecutionState.THINKING;
        }

        // All sub-tasks complete or no ready tasks remain
        if (plan.isAllCompleted()) {
            log.info("Execution {} all sub-tasks completed", execution.getId());
        }
        execution.setMetadata(METADATA_KEY_PLAN, serializePlan(plan));
        return AgentExecutionState.COMPLETED;
    }

    private SubTaskPlan deserializePlan(String json) {
        try {
            return new ObjectMapper().readValue(json, SubTaskPlan.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize SubTaskPlan: {}", e.getMessage());
            return null;
        }
    }

    private String serializePlan(SubTaskPlan plan) {
        try {
            return new ObjectMapper().writeValueAsString(plan);
        } catch (Exception e) {
            log.warn("Failed to serialize SubTaskPlan: {}", e.getMessage());
            return "";
        }
    }
}
