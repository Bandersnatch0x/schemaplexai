package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.context.AgentContext;
import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.evaluation.ValidationResult;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.memory.compaction.AutoCompactionService;
import com.schemaplexai.agent.engine.memory.compaction.CompactionResult;
import com.schemaplexai.agent.engine.extractor.FinalAnswerExtractor;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.plan.SubTask;
import com.schemaplexai.agent.engine.plan.SubTaskPlan;
import com.schemaplexai.agent.engine.reasoning.ReasoningStrategy;
import com.schemaplexai.agent.engine.reasoning.ThinkingResult;
import com.schemaplexai.agent.engine.role.RoleOverlay;
import com.schemaplexai.agent.engine.role.RoleRegistry;
import com.schemaplexai.agent.engine.skill.SkillDefinition;
import com.schemaplexai.agent.engine.skill.SkillRegistry;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolRegistry;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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
    private final ToolRegistry toolRegistry;
    private final FinalAnswerExtractor finalAnswerExtractor;
    private final List<ReasoningStrategy> reasoningStrategies;

    public ThinkingStateHandler(ContextInjector contextInjector,
                                 CompositeChatMemoryStore chatMemoryStore,
                                 AiModelRouter modelRouter,
                                 AgentLoopDetectionService loopDetection,
                                 com.schemaplexai.agent.engine.model.ModelResolver modelResolver,
                                 GuardrailsEngine guardrailsEngine,
                                 SkillRegistry skillRegistry,
                                 RoleRegistry roleRegistry,
                                 AutoCompactionService autoCompactionService,
                                 ToolRegistry toolRegistry,
                                 @Nullable List<ReasoningStrategy> reasoningStrategies) {
        this.contextInjector = contextInjector;
        this.chatMemoryStore = chatMemoryStore;
        this.modelRouter = modelRouter;
        this.loopDetection = loopDetection;
        this.modelResolver = modelResolver;
        this.guardrailsEngine = guardrailsEngine;
        this.skillRegistry = skillRegistry;
        this.roleRegistry = roleRegistry;
        this.autoCompactionService = autoCompactionService;
        this.toolRegistry = toolRegistry;
        this.finalAnswerExtractor = new FinalAnswerExtractor();
        this.reasoningStrategies = reasoningStrategies != null
                ? List.copyOf(reasoningStrategies) : Collections.emptyList();
    }

    /**
     * Backward-compatible constructor (no reasoning strategies).
     */
    public ThinkingStateHandler(ContextInjector contextInjector,
                                 CompositeChatMemoryStore chatMemoryStore,
                                 AiModelRouter modelRouter,
                                 AgentLoopDetectionService loopDetection,
                                 com.schemaplexai.agent.engine.model.ModelResolver modelResolver,
                                 GuardrailsEngine guardrailsEngine,
                                 SkillRegistry skillRegistry,
                                 RoleRegistry roleRegistry,
                                 AutoCompactionService autoCompactionService,
                                 ToolRegistry toolRegistry) {
        this(contextInjector, chatMemoryStore, modelRouter, loopDetection,
                modelResolver, guardrailsEngine, skillRegistry, roleRegistry,
                autoCompactionService, toolRegistry, null);
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.THINKING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering THINKING state, execution {}", execution.getAgentId(), execution.getId());
        stateMachine.emitTimelineEvent(execution, "thought",
                "Entering THINKING state — loading context and reasoning");

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
            String prompt = buildPrompt(messages, tenantId, roleName, skillName, execution);

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

            // 6a. Check if a ReasoningStrategy should handle the LLM call
            ReasoningStrategy strategy = resolveReasoningStrategy(execution);
            if (strategy != null) {
                AgentContext agentContext = buildAgentContext(execution);
                if (strategy.canContinue(agentContext)) {
                    log.info("Execution {} delegating to reasoning strategy '{}'",
                            execution.getId(), strategy.getName());
                    stateMachine.emitTimelineEvent(execution, "thought",
                            "Using reasoning strategy: " + strategy.getName());
                    executeWithStrategy(strategy, agentContext, execution, budget, stateMachine);
                    return;
                }
                log.warn("Strategy '{}' cannot continue for execution {}, falling back to inline",
                        strategy.getName(), execution.getId());
            }

            // 6b. Call LLM with fallback (inline reasoning — existing ReAct loop)
            String modelId = resolveModelId(execution);
            stateMachine.emitTimelineEvent(execution, "thought",
                    "Calling LLM (model=" + modelId + ", tokens≈" + inputTokens + ")");
            String response = modelRouter.generateWithFallback(prompt, modelId, DEFAULT_TEMPERATURE);
            long outputTokens = estimateTokens(response);
            stateMachine.emitTimelineEvent(execution, "output",
                    "LLM response received (" + outputTokens + " tokens)");

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
            String cleanAnswer = finalAnswerExtractor.extractFinalAnswer(response);
            String memoryContent = cleanAnswer != null ? cleanAnswer : response;
            chatMemoryStore.saveMessage(execution.getConversationId(), new LlmMessage("assistant", memoryContent));

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
                stateMachine.emitTimelineEvent(execution, "thought",
                        "Detected tool calls: " + String.join(", ", toolNames));
                execution.setMetadata("iterationToolCallCount", 0);
                stateMachine.transition(AgentExecutionState.TOOL_CALLING, execution);
            } else {
                // Check if there's an active sub-task plan to progress
                AgentExecutionState nextState = resolveNextStateForPlan(execution);
                log.info("Execution {} completed with direct answer, transitioning to {}", execution.getId(), nextState);
                stateMachine.emitTimelineEvent(execution, "output",
                        "Direct answer received, transitioning to " + nextState);
                loopDetection.clearRecords(execution.getId());
                stateMachine.transition(nextState, execution);
            }
        } catch (Exception e) {
            log.error("Thinking state failed for execution {}", execution.getId(), e);
            stateMachine.emitTimelineEvent(execution, "error",
                    "THINKING failed: " + e.getMessage());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    /**
     * Resolves a ReasoningStrategy for the given execution.
     * Checks execution metadata "reasoningStrategy" for a named strategy.
     * Returns null if no strategy is requested or available (falls back to inline).
     */
    private ReasoningStrategy resolveReasoningStrategy(SfAgentExecution execution) {
        if (reasoningStrategies.isEmpty()) {
            return null;
        }
        Object strategyMeta = execution.getMetadata("reasoningStrategy");
        if (strategyMeta == null) {
            return null;
        }
        String strategyName = strategyMeta.toString().trim();
        if (strategyName.isBlank()) {
            return null;
        }
        return reasoningStrategies.stream()
                .filter(s -> s.getName().equalsIgnoreCase(strategyName))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("ReasoningStrategy '{}' not found, available: {}",
                            strategyName,
                            reasoningStrategies.stream().map(ReasoningStrategy::getName).toList());
                    return null;
                });
    }

    /**
     * Builds an AgentContext from the execution entity for use with ReasoningStrategy.
     * projectId and userId are extracted from metadata if present, otherwise null.
     */
    private AgentContext buildAgentContext(SfAgentExecution execution) {
        String projectId = null;
        Object projectIdMeta = execution.getMetadata("projectId");
        if (projectIdMeta != null) {
            projectId = projectIdMeta.toString();
        }
        String userId = null;
        Object userIdMeta = execution.getMetadata("userId");
        if (userIdMeta != null) {
            userId = userIdMeta.toString();
        }
        return AgentContext.builder()
                .tenantId(execution.getTenantId())
                .projectId(projectId)
                .conversationId(execution.getConversationId())
                .agentId(execution.getAgentId())
                .userId(userId)
                .build();
    }

    /**
     * Delegates reasoning to a ReasoningStrategy and maps ThinkingResult to state transitions.
     * Preserves loop detection, memory persistence, and sub-task plan progression.
     */
    private void executeWithStrategy(ReasoningStrategy strategy,
                                      AgentContext agentContext,
                                      SfAgentExecution execution,
                                      TokenBudget budget,
                                      AgentStateMachine stateMachine) {
        try {
            ThinkingResult result = strategy.think(agentContext, budget);

            switch (result.type()) {
                case COMPLETED -> {
                    String answer = result.finalAnswer();
                    chatMemoryStore.saveMessage(execution.getConversationId(),
                            new LlmMessage("assistant", answer));
                    AgentExecutionState nextState = resolveNextStateForPlan(execution);
                    loopDetection.clearRecords(execution.getId());
                    stateMachine.emitTimelineEvent(execution, "output",
                            "Strategy " + strategy.getName() + " produced final answer");
                    stateMachine.transition(nextState, execution);
                }
                case TOOL_CALL -> {
                    String toolName = result.toolCall().toolName();
                    LoopDetectionResult loopResult = loopDetection.detectLoop(
                            execution.getId(), strategy.getName(), List.of(toolName));
                    if (loopResult.loopDetected()) {
                        log.warn("Loop detected via strategy {} for execution {}: {}",
                                strategy.getName(), execution.getId(), loopResult.reason());
                        execution.setMetadata("blockedReason", "agent_loop_" + loopResult.reason());
                        execution.setMetadata("admissionType", "LOOP");
                        stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                        return;
                    }
                    stateMachine.emitTimelineEvent(execution, "thought",
                            "Strategy " + strategy.getName() + " requested tool: " + toolName);
                    execution.setMetadata("iterationToolCallCount", 0);
                    stateMachine.transition(AgentExecutionState.TOOL_CALLING, execution);
                }
                case EXHAUSTED -> {
                    log.warn("Strategy {} exhausted for execution {}: {}",
                            strategy.getName(), execution.getId(), result.errorMessage());
                    execution.setMetadata("blockedReason", result.errorMessage());
                    execution.setMetadata("admissionType", "BUDGET");
                    stateMachine.transition(AgentExecutionState.GATE_BLOCKED, execution);
                }
                case ERROR -> {
                    log.error("Strategy {} error for execution {}: {}",
                            strategy.getName(), execution.getId(), result.errorMessage());
                    stateMachine.emitTimelineEvent(execution, "error",
                            "Strategy " + strategy.getName() + " failed: " + result.errorMessage());
                    stateMachine.transition(AgentExecutionState.FAILED, execution);
                }
            }
        } catch (Exception e) {
            log.error("Strategy {} threw exception for execution {}",
                    strategy.getName(), execution.getId(), e);
            stateMachine.emitTimelineEvent(execution, "error",
                    "Strategy " + strategy.getName() + " exception: " + e.getMessage());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        }
    }

    private String buildPrompt(List<LlmMessage> messages, String tenantId, String roleName, String skillName, SfAgentExecution execution) {
        StringBuilder sb = new StringBuilder();

        // Determine execution maturity tier based on current round
        int maxTier = resolveMaxTier(execution);

        // Add role overlay if present
        if (roleName != null && !roleName.isBlank()) {
            RoleOverlay role = roleRegistry.resolve(roleName, tenantId);
            if (role != null) {
                sb.append("# Role: ").append(role.name()).append("\n");
                sb.append(role.overlay()).append("\n\n");
            }
        }

        // Add skill instructions if present and within tier
        if (skillName != null && !skillName.isBlank()) {
            SkillDefinition skill = skillRegistry.resolveByTier(skillName, tenantId, maxTier);
            if (skill != null) {
                sb.append("# Skill: ").append(skill.name()).append("\n");
                sb.append(skill.instructions()).append("\n\n");
            } else {
                log.warn("Skill '{}' skipped for execution {}: tier exceeds maxTier {}", skillName, execution.getId(), maxTier);
            }
        }

        // Inject available skills up to current tier as context
        List<SkillDefinition> availableSkills = skillRegistry.resolveAvailable(tenantId, maxTier);
        if (!availableSkills.isEmpty()) {
            sb.append("# Available Skills (Tier ").append(maxTier).append(")\n");
            for (SkillDefinition s : availableSkills) {
                sb.append("- ").append(s.name()).append(": ").append(s.description()).append("\n");
            }
            sb.append("\n");
        }

        // Inject available tool descriptions
        String toolDesc = buildToolDescriptions();
        if (!toolDesc.isBlank()) {
            sb.append("# Available Tools\n").append(toolDesc).append("\n\n");
        }

        // Append original prompt content from messages
        if (messages != null && !messages.isEmpty()) {
            for (LlmMessage msg : messages) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
            }
        }

        return sb.toString();
    }

    private int resolveMaxTier(SfAgentExecution execution) {
        int currentRound = 1;
        Object roundMeta = execution.getMetadata("currentRound");
        if (roundMeta instanceof Number) {
            currentRound = ((Number) roundMeta).intValue();
        } else if (roundMeta != null) {
            try {
                currentRound = Integer.parseInt(roundMeta.toString());
            } catch (NumberFormatException e) {
                log.warn("Invalid currentRound metadata for execution {}: {}", execution.getId(), roundMeta);
            }
        }
        if (currentRound <= 2) {
            return 1;
        } else if (currentRound <= 5) {
            return 2;
        } else {
            return 3;
        }
    }

    private String buildToolDescriptions() {
        if (toolRegistry == null) {
            return "";
        }
        List<ToolDefinition> tools = toolRegistry.getAll();
        if (tools == null || tools.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ToolDefinition tool : tools) {
            sb.append("- ").append(tool.name()).append(": ").append(tool.description()).append("\n");
            if (tool.parameters() != null && !tool.parameters().isEmpty()) {
                tool.parameters().forEach(p ->
                    sb.append(String.format("  - %s (%s)%s: %s%n",
                        p.name(), p.type(), p.required() ? ", required" : "", p.description())));
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
                || response.contains("invoke_tool")
                || response.contains("Action:")
                || response.contains("Action Input:");
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
                long maxToolCalls = ((Number) map.getOrDefault("maxToolCalls", Long.MAX_VALUE)).longValue();
                long consumedInput = ((Number) map.getOrDefault("consumedInput", 0L)).longValue();
                long consumedOutput = ((Number) map.getOrDefault("consumedOutput", 0L)).longValue();
                long consumedToolCalls = ((Number) map.getOrDefault("consumedToolCalls", 0L)).longValue();
                TokenBudget budget = new TokenBudget(maxInput, maxOutput, maxToolCalls);
                budget.consumeInput(consumedInput);
                budget.consumeOutput(consumedOutput);
                for (long i = 0; i < consumedToolCalls; i++) {
                    budget.consumeToolCall();
                }
                return budget;
            }
            // Legacy fallback: comma-separated format
            String[] parts = json.split(",");
            long maxInput = Long.parseLong(parts[0]);
            long maxOutput = Long.parseLong(parts[1]);
            long maxToolCalls = parts.length > 4 ? Long.parseLong(parts[4]) : Long.MAX_VALUE;
            long consumedInput = parts.length > 2 ? Long.parseLong(parts[2]) : 0;
            long consumedOutput = parts.length > 3 ? Long.parseLong(parts[3]) : 0;
            long consumedToolCalls = parts.length > 5 ? Long.parseLong(parts[5]) : 0;
            TokenBudget budget = new TokenBudget(maxInput, maxOutput, maxToolCalls);
            budget.consumeInput(consumedInput);
            budget.consumeOutput(consumedOutput);
            for (long i = 0; i < consumedToolCalls; i++) {
                budget.consumeToolCall();
            }
            return budget;
        } catch (Exception e) {
            log.warn("Failed to parse token budget for execution {}", execution.getId());
            return null;
        }
    }

    private void saveBudget(SfAgentExecution execution, TokenBudget budget) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("maxInput", budget.getMaxInputTokens());
            map.put("maxOutput", budget.getMaxOutputTokens());
            map.put("maxToolCalls", budget.getMaxToolCalls());
            map.put("consumedInput", budget.getConsumedInputTokens().get());
            map.put("consumedOutput", budget.getConsumedOutputTokens().get());
            map.put("consumedToolCalls", budget.getConsumedToolCalls().get());
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
