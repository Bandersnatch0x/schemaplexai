package com.schemaplexai.agent.engine.goal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Detects whether an agent execution should stop based on goal progress.
 *
 * <p>Checks completion signals in priority order:
 * <ol>
 *   <li>All milestones achieved -- FULLY_ACHIEVED</li>
 *   <li>Some milestones achieved but max iterations reached -- PARTIALLY_ACHIEVED</li>
 *   <li>No milestones achieved and max iterations reached -- MAX_ITERATIONS</li>
 * </ol>
 * </p>
 */
@Slf4j
@Component
public class GoalCompletionDetector {

    /**
     * Detects whether the execution should complete based on current progress.
     *
     * @param progress     the latest goal progress
     * @param currentIteration the current iteration count
     * @param maxIterations the maximum allowed iterations
     * @return a completion signal indicating whether to stop
     */
    public CompletionSignal detectCompletion(GoalProgress progress,
                                             int currentIteration,
                                             int maxIterations) {
        if (progress == null) {
            return CompletionSignal.notComplete();
        }

        // Check if all milestones are achieved
        if (allMilestonesAchieved(progress)) {
            log.info("All milestones achieved for goal {}", progress.goalId());
            return CompletionSignal.fullyAchieved(
                    "All %d milestones achieved.".formatted(progress.milestones().size()));
        }

        // Check if max iterations reached
        if (currentIteration >= maxIterations) {
            long achievedCount = progress.milestones().stream()
                    .filter(MilestoneStatus::achieved)
                    .count();

            if (achievedCount > 0) {
                log.info("Max iterations reached with {}/{} milestones achieved for goal {}",
                        achievedCount, progress.milestones().size(), progress.goalId());
                return CompletionSignal.partiallyAchieved(
                        "Max iterations (%d) reached. %d/%d milestones achieved."
                                .formatted(maxIterations, achievedCount, progress.milestones().size()));
            }

            log.info("Max iterations reached with no milestones achieved for goal {}",
                    progress.goalId());
            return CompletionSignal.maxIterations(
                    "Max iterations (%d) reached. No milestones achieved."
                            .formatted(maxIterations));
        }

        return CompletionSignal.notComplete();
    }

    /**
     * Convenience overload: detects completion without iteration tracking.
     * Only checks milestone-based completion.
     *
     * @param progress the latest goal progress
     * @return a completion signal
     */
    public CompletionSignal detectCompletion(GoalProgress progress) {
        if (progress == null) {
            return CompletionSignal.notComplete();
        }

        if (allMilestonesAchieved(progress)) {
            return CompletionSignal.fullyAchieved(
                    "All %d milestones achieved.".formatted(progress.milestones().size()));
        }

        return CompletionSignal.notComplete();
    }

    private boolean allMilestonesAchieved(GoalProgress progress) {
        if (progress.milestones().isEmpty()) {
            return false;
        }
        return progress.milestones().stream().allMatch(MilestoneStatus::achieved);
    }
}
