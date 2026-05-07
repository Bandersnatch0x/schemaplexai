package com.schemaplexai.agent.engine.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.ModelResolver;
import com.schemaplexai.agent.engine.plan.SubTask;
import com.schemaplexai.agent.engine.plan.SubTaskPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Planning state handler - decomposes the user's goal into sub-tasks via LLM.
 * After creating a {@link SubTaskPlan}, stores it in execution metadata and
 * transitions to THINKING to execute the first sub-task.
 */
@Slf4j
@Component
public class PlanningStateHandler implements AgentStateHandler {

    private static final double PLANNING_TEMPERATURE = 0.4;
    private static final String METADATA_KEY_PLAN = "subTaskPlan";
    private static final Pattern SUBTASK_PATTERN = Pattern.compile(
            "^\\s*[-*]?\\s*(?:\\d+[.\\)]?\\s*)?(.*)$", Pattern.MULTILINE);

    private final AiModelRouter modelRouter;
    private final CompositeChatMemoryStore chatMemoryStore;
    private final ModelResolver modelResolver;
    private final ObjectMapper objectMapper;

    public PlanningStateHandler(AiModelRouter modelRouter,
                                 CompositeChatMemoryStore chatMemoryStore,
                                 ModelResolver modelResolver) {
        this.modelRouter = modelRouter;
        this.chatMemoryStore = chatMemoryStore;
        this.modelResolver = modelResolver;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AgentExecutionState getState() {
        return AgentExecutionState.PLANNING;
    }

    @Override
    public void handle(AgentStateMachine stateMachine, SfAgentExecution execution) {
        log.info("Agent {} entering PLANNING state, execution {}", execution.getAgentId(), execution.getId());

        try {
            // 1. Load conversation history
            List<LlmMessage> messages = chatMemoryStore.loadMessages(execution.getConversationId());

            // 2. Extract the user's goal from the last user message
            String userGoal = extractLastUserMessage(messages);
            if (userGoal == null || userGoal.isBlank()) {
                userGoal = "(no explicit goal provided)";
            }

            // 3. Build planning prompt
            String planningPrompt = buildPlanningPrompt(userGoal);

            // 4. Call LLM for task decomposition
            String modelId = resolveModelId(execution);
            String response = modelRouter.generateWithFallback(
                    planningPrompt, modelId, PLANNING_TEMPERATURE
            );

            // 5. Parse response into sub-tasks
            SubTaskPlan plan = parseSubTaskPlan(userGoal, response);

            if (plan.getSubTasks().isEmpty()) {
                log.warn("Planning produced no sub-tasks for execution {}, falling through to THINKING",
                        execution.getId());
            } else {
                log.info("Execution {} planned with {} sub-tasks",
                        execution.getId(), plan.getSubTasks().size());
            }

            // 6. Store plan in execution metadata as JSON
            String planJson = serializePlan(plan);
            execution.setMetadata(METADATA_KEY_PLAN, planJson);

            // 7. Transition to THINKING to execute the first sub-task
            stateMachine.saveExecution(execution);
            stateMachine.transition(AgentExecutionState.THINKING, execution);
        } catch (Exception e) {
            log.error("Planning failed for execution {}, transitioning to THINKING without plan",
                    execution.getId(), e);
            stateMachine.transition(AgentExecutionState.THINKING, execution);
        }
    }

    String buildPlanningPrompt(String goal) {
        return """
                You are a task decomposition assistant. Break down the following goal into clear, actionable sub-tasks.

                Goal: %s

                Instructions:
                1. List each sub-task on a separate line.
                2. Start each line with "- " or a number like "1. ".
                3. Keep sub-tasks concise (one sentence each).
                4. Order them by dependency: earlier tasks should not depend on later ones.
                5. If the goal is simple and needs no decomposition, return a single sub-task.

                Sub-tasks:
                """.formatted(goal);
    }

    SubTaskPlan parseSubTaskPlan(String goal, String llmOutput) {
        SubTaskPlan plan = new SubTaskPlan();
        plan.setGoal(goal);
        List<SubTask> subTasks = new ArrayList<>();

        if (llmOutput == null || llmOutput.isBlank()) {
            plan.setSubTasks(subTasks);
            return plan;
        }

        String[] lines = llmOutput.split("\n");
        int index = 1;
        for (String line : lines) {
            Matcher matcher = SUBTASK_PATTERN.matcher(line);
            if (matcher.find()) {
                String description = matcher.group(1).trim();
                if (!description.isEmpty() && description.length() > 3) {
                    SubTask subTask = new SubTask();
                    subTask.setId("st-" + index + "-" + UUID.randomUUID().toString().substring(0, 8));
                    subTask.setDescription(description);
                    subTask.setStatus(SubTask.STATUS_PENDING);
                    // Simple dependency: each sub-task depends on the previous one by default
                    if (index > 1 && !subTasks.isEmpty()) {
                        subTask.setDependencies(new ArrayList<>(List.of(subTasks.get(subTasks.size() - 1).getId())));
                    } else {
                        subTask.setDependencies(new ArrayList<>());
                    }
                    subTasks.add(subTask);
                    index++;
                }
            }
        }

        plan.setSubTasks(subTasks);
        return plan;
    }

    String serializePlan(SubTaskPlan plan) throws JsonProcessingException {
        return objectMapper.writeValueAsString(plan);
    }

    SubTaskPlan deserializePlan(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SubTaskPlan.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize SubTaskPlan: {}", e.getMessage());
            return null;
        }
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
        return modelResolver.resolve(execution);
    }
}
