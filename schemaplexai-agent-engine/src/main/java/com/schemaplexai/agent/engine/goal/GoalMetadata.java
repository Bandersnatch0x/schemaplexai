package com.schemaplexai.agent.engine.goal;

import java.time.Instant;
import java.util.List;

/**
 * Immutable metadata describing a goal to be achieved during an agent execution.
 *
 * @param goalId          unique goal identifier
 * @param description     human-readable description of the goal
 * @param successCriteria textual description of what constitutes success
 * @param milestones      ordered list of milestone descriptions
 * @param status          current lifecycle status
 * @param createdAt       when the goal was created
 * @param completedAt     when the goal reached a terminal status (nullable)
 */
public record GoalMetadata(
        String goalId,
        String description,
        String successCriteria,
        List<String> milestones,
        GoalStatus status,
        Instant createdAt,
        Instant completedAt
) {

    /**
     * Creates a new pending goal with the current timestamp.
     */
    public static GoalMetadata create(String goalId, String description,
                                      String successCriteria, List<String> milestones) {
        return new GoalMetadata(
                goalId,
                description,
                successCriteria,
                milestones != null ? List.copyOf(milestones) : List.of(),
                GoalStatus.PENDING,
                Instant.now(),
                null
        );
    }

    /**
     * Returns a copy with the updated status and optional completion timestamp.
     */
    public GoalMetadata withStatus(GoalStatus newStatus) {
        Instant completed = newStatus == GoalStatus.ACHIEVED
                || newStatus == GoalStatus.PARTIALLY_ACHIEVED
                || newStatus == GoalStatus.ABANDONED
                ? Instant.now()
                : this.completedAt;
        return new GoalMetadata(
                this.goalId,
                this.description,
                this.successCriteria,
                this.milestones,
                newStatus,
                this.createdAt,
                completed
        );
    }
}
