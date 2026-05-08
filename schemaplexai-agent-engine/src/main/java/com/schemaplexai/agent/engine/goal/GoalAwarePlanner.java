package com.schemaplexai.agent.engine.goal;

import com.schemaplexai.agent.engine.plan.SubTask;
import com.schemaplexai.agent.engine.plan.SubTaskPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Integrates goal tracking with the sub-task planning system.
 *
 * <p>Aligns plans with goals by injecting goal-check sub-tasks, and determines
 * whether execution should continue based on goal progress.</p>
 */
@Slf4j
@Component
public class GoalAwarePlanner {

    private static final String GOAL_CHECK_PREFIX = "goal-check-";
    private static final String GOAL_CHECK_DESCRIPTION_TEMPLATE =
            "Verify goal achievement: assess whether success criteria '%s' have been met";

    /**
     * Aligns a sub-task plan with a goal by appending goal-check sub-tasks.
     *
     * <p>If the plan already contains goal-check sub-tasks, it is returned unchanged.</p>
     *
     * @param plan the existing sub-task plan
     * @param goal the goal metadata to align with
     * @return a new plan with goal-check sub-tasks appended (or the original if already aligned)
     */
    public SubTaskPlan alignPlanWithGoal(SubTaskPlan plan, GoalMetadata goal) {
        if (plan == null || goal == null) {
            return plan;
        }

        // Check if plan already has goal-check sub-tasks
        boolean hasGoalCheck = plan.getSubTasks().stream()
                .anyMatch(st -> st.getId() != null && st.getId().startsWith(GOAL_CHECK_PREFIX));

        if (hasGoalCheck) {
            log.debug("Plan already contains goal-check sub-tasks, skipping alignment");
            return plan;
        }

        // Create a goal-check sub-task for each milestone
        List<SubTask> newSubTasks = new ArrayList<>(plan.getSubTasks());
        String lastSubTaskId = newSubTasks.isEmpty() ? null
                : newSubTasks.get(newSubTasks.size() - 1).getId();

        for (int i = 0; i < goal.milestones().size(); i++) {
            String milestone = goal.milestones().get(i);
            SubTask goalCheck = SubTask.builder()
                    .id(GOAL_CHECK_PREFIX + (i + 1) + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .description(GOAL_CHECK_DESCRIPTION_TEMPLATE.formatted(milestone))
                    .status(SubTask.STATUS_PENDING)
                    .dependencies(new ArrayList<>(
                            i == 0 && lastSubTaskId != null
                                    ? List.of(lastSubTaskId)
                                    : i > 0
                                    ? List.of(GOAL_CHECK_PREFIX + i + "-" + extractSuffix(newSubTasks, i))
                                    : List.of()))
                    .build();
            newSubTasks.add(goalCheck);
        }

        // Also add a final goal-evaluation sub-task
        SubTask evaluation = SubTask.builder()
                .id(GOAL_CHECK_PREFIX + "eval-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Final goal evaluation: determine if goal '" + goal.description()
                        + "' has been achieved based on all milestone results")
                .status(SubTask.STATUS_PENDING)
                .dependencies(newSubTasks.isEmpty() ? List.of()
                        : List.of(newSubTasks.get(newSubTasks.size() - 1).getId()))
                .build();
        newSubTasks.add(evaluation);

        SubTaskPlan aligned = SubTaskPlan.builder()
                .goal(plan.getGoal())
                .subTasks(newSubTasks)
                .currentSubTaskId(plan.getCurrentSubTaskId())
                .build();

        log.info("Aligned plan with goal '{}': added {} goal-check sub-tasks",
                goal.description(), goal.milestones().size() + 1);

        return aligned;
    }

    /**
     * Determines whether execution should continue based on plan state and goal progress.
     *
     * @param plan     the current sub-task plan
     * @param progress the latest goal progress
     * @return true if execution should continue, false if it should stop
     */
    public boolean shouldContinue(SubTaskPlan plan, GoalProgress progress) {
        if (progress == null) {
            // No progress data -- continue if plan has remaining work
            return plan != null && !plan.getSubTasks().isEmpty() && !plan.isAllCompleted();
        }

        // Stop if goal is fully achieved
        if (progress.completionScore() >= GoalTracker.ACHIEVEMENT_THRESHOLD) {
            log.info("Goal {} achieved (score={}), execution should stop",
                    progress.goalId(), progress.completionScore());
            return false;
        }

        // Stop if all milestones are achieved
        if (!progress.milestones().isEmpty()
                && progress.milestones().stream().allMatch(MilestoneStatus::achieved)) {
            log.info("All milestones achieved for goal {}, execution should stop",
                    progress.goalId());
            return false;
        }

        // Continue if plan has remaining sub-tasks
        if (plan != null && !plan.getSubTasks().isEmpty() && !plan.isAllCompleted()) {
            return true;
        }

        // Plan is complete but goal not achieved -- could continue with re-planning
        log.info("Plan completed but goal {} not fully achieved (score={}), stopping",
                progress.goalId(), progress.completionScore());
        return false;
    }

    private String extractSuffix(List<SubTask> subTasks, int milestoneIndex) {
        // Find the goal-check sub-task at the given index to extract its UUID suffix
        int goalCheckCount = 0;
        for (SubTask st : subTasks) {
            if (st.getId() != null && st.getId().startsWith(GOAL_CHECK_PREFIX)) {
                goalCheckCount++;
                if (goalCheckCount == milestoneIndex) {
                    String id = st.getId();
                    int lastDash = id.lastIndexOf('-');
                    return lastDash >= 0 ? id.substring(lastDash + 1) : "";
                }
            }
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
