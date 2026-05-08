package com.schemaplexai.agent.engine.goal;

import java.time.Instant;
import java.util.List;

/**
 * Immutable snapshot of goal progress assessed at a point in time.
 *
 * @param goalId         the goal being assessed
 * @param completionScore 0.0 to 1.0 indicating overall completion
 * @param milestones     per-milestone status
 * @param assessment     human-readable assessment summary
 * @param assessedAt     when this assessment was performed
 */
public record GoalProgress(
        String goalId,
        double completionScore,
        List<MilestoneStatus> milestones,
        String assessment,
        Instant assessedAt
) {

    public static GoalProgress initial(String goalId, List<MilestoneStatus> milestones) {
        return new GoalProgress(
                goalId,
                0.0,
                milestones != null ? List.copyOf(milestones) : List.of(),
                "Goal created, no progress assessed yet.",
                Instant.now()
        );
    }
}
