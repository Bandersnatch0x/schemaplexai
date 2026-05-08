package com.schemaplexai.agent.engine.goal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tracks goal creation, progress assessment, and milestone updates
 * for agent executions. Stores goal data in execution metadata as JSON.
 *
 * <p>v1 uses rule-based keyword matching against success criteria.
 * Future versions will use LLM-based assessment.</p>
 */
@Slf4j
@Component
public class GoalTracker {

    static final String METADATA_KEY_GOAL = "goalMetadata";
    static final String METADATA_KEY_PROGRESS = "goalProgress";
    static final double ACHIEVEMENT_THRESHOLD = 0.8;

    private final ObjectMapper objectMapper;

    public GoalTracker() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Creates a new goal and stores it in execution metadata.
     *
     * @param executionId the execution identifier (for logging)
     * @param execution   the execution entity whose metadata to update
     * @param goal        the goal metadata to persist
     */
    public void createGoal(String executionId, SfAgentExecution execution, GoalMetadata goal) {
        log.info("Creating goal {} for execution {}: {}", goal.goalId(), executionId, goal.description());

        GoalMetadata inProgressGoal = goal.withStatus(GoalStatus.IN_PROGRESS);
        storeGoal(execution, inProgressGoal);

        List<MilestoneStatus> initialMilestones = goal.milestones().stream()
                .map(MilestoneStatus::pending)
                .toList();
        GoalProgress initialProgress = GoalProgress.initial(goal.goalId(), initialMilestones);
        storeProgress(execution, initialProgress);

        log.info("Goal {} created with {} milestones", goal.goalId(), goal.milestones().size());
    }

    /**
     * Assesses progress toward the goal by checking if success criteria keywords
     * appear in the latest agent response.
     *
     * @param execution    the execution entity
     * @param lastResponse the most recent agent response to analyze
     * @return updated progress assessment
     */
    public GoalProgress assessProgress(SfAgentExecution execution, String lastResponse) {
        GoalMetadata goal = loadGoal(execution);
        GoalProgress previousProgress = loadProgress(execution);

        if (goal == null) {
            log.warn("No goal found in execution metadata, returning empty progress");
            return new GoalProgress(null, 0.0, List.of(), "No goal defined.", Instant.now());
        }

        double score = calculateCompletionScore(goal.successCriteria(), lastResponse);
        List<MilestoneStatus> updatedMilestones = assessMilestones(goal, previousProgress, lastResponse);
        String assessment = buildAssessment(score, updatedMilestones);

        GoalProgress newProgress = new GoalProgress(
                goal.goalId(),
                score,
                updatedMilestones,
                assessment,
                Instant.now()
        );

        storeProgress(execution, newProgress);

        // Update goal status if threshold crossed
        if (score >= ACHIEVEMENT_THRESHOLD) {
            storeGoal(execution, goal.withStatus(GoalStatus.ACHIEVED));
            log.info("Goal {} achieved with score {}", goal.goalId(), score);
        }

        log.debug("Progress assessed for goal {}: score={}, milestones={}/{}",
                goal.goalId(), score,
                updatedMilestones.stream().filter(MilestoneStatus::achieved).count(),
                updatedMilestones.size());

        return newProgress;
    }

    /**
     * Checks whether the goal has been achieved (score above threshold).
     *
     * @param execution the execution entity
     * @return true if completion score exceeds the achievement threshold
     */
    public boolean isGoalAchieved(SfAgentExecution execution) {
        GoalProgress progress = loadProgress(execution);
        if (progress == null) {
            return false;
        }
        return progress.completionScore() >= ACHIEVEMENT_THRESHOLD;
    }

    /**
     * Manually updates a milestone's status.
     *
     * @param execution the execution entity
     * @param milestone the milestone description to update
     * @param achieved  whether the milestone is now achieved
     * @param evidence  supporting evidence
     */
    public void updateMilestone(SfAgentExecution execution, String milestone,
                                boolean achieved, String evidence) {
        GoalProgress previousProgress = loadProgress(execution);
        if (previousProgress == null) {
            log.warn("No progress found, cannot update milestone '{}'", milestone);
            return;
        }

        List<MilestoneStatus> updated = previousProgress.milestones().stream()
                .map(ms -> ms.milestone().equals(milestone)
                        ? (achieved
                        ? MilestoneStatus.achieved(milestone, evidence)
                        : MilestoneStatus.notAchieved(milestone, evidence))
                        : ms)
                .toList();

        // Recalculate score based on milestone completion
        long achievedCount = updated.stream().filter(MilestoneStatus::achieved).count();
        double score = updated.isEmpty() ? previousProgress.completionScore()
                : (double) achievedCount / updated.size();

        GoalProgress newProgress = new GoalProgress(
                previousProgress.goalId(),
                score,
                updated,
                previousProgress.assessment(),
                Instant.now()
        );

        storeProgress(execution, newProgress);
        log.info("Milestone '{}' updated to achieved={} for goal {}",
                milestone, achieved, previousProgress.goalId());
    }

    /**
     * Loads the current goal from execution metadata.
     */
    public GoalMetadata loadGoal(SfAgentExecution execution) {
        Object raw = execution.getMetadata(METADATA_KEY_GOAL);
        if (raw == null) return null;
        if (raw instanceof GoalMetadata gm) return gm;
        return deserialize(raw.toString(), GoalMetadata.class);
    }

    /**
     * Loads the current progress from execution metadata.
     */
    public GoalProgress loadProgress(SfAgentExecution execution) {
        Object raw = execution.getMetadata(METADATA_KEY_PROGRESS);
        if (raw == null) return null;
        if (raw instanceof GoalProgress gp) return gp;
        return deserialize(raw.toString(), GoalProgress.class);
    }

    // ---- internal helpers ----

    private void storeGoal(SfAgentExecution execution, GoalMetadata goal) {
        execution.setMetadata(METADATA_KEY_GOAL, goal);
    }

    private void storeProgress(SfAgentExecution execution, GoalProgress progress) {
        execution.setMetadata(METADATA_KEY_PROGRESS, progress);
    }

    /**
     * Rule-based (v1) completion score: fraction of success-criteria keywords
     * found (case-insensitive) in the response.
     */
    double calculateCompletionScore(String successCriteria, String response) {
        if (successCriteria == null || successCriteria.isBlank()) return 0.0;
        if (response == null || response.isBlank()) return 0.0;

        String[] keywords = successCriteria.split("[,;]");
        if (keywords.length == 0) return 0.0;

        String lowerResponse = response.toLowerCase(Locale.ROOT);
        long matched = 0;
        for (String keyword : keywords) {
            String trimmed = keyword.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty() && lowerResponse.contains(trimmed)) {
                matched++;
            }
        }
        return (double) matched / keywords.length;
    }

    /**
     * Assesses each milestone by checking if its keywords appear in the response.
     */
    private List<MilestoneStatus> assessMilestones(GoalMetadata goal,
                                                   GoalProgress previousProgress,
                                                   String response) {
        List<MilestoneStatus> result = new ArrayList<>();
        List<MilestoneStatus> previousMilestones = previousProgress != null
                ? previousProgress.milestones()
                : List.of();

        for (int i = 0; i < goal.milestones().size(); i++) {
            String milestoneDesc = goal.milestones().get(i);

            // Preserve already-achieved milestones
            if (i < previousMilestones.size() && previousMilestones.get(i).achieved()) {
                result.add(previousMilestones.get(i));
                continue;
            }

            // Check if milestone keywords appear in response
            boolean found = response != null
                    && containsAnyKeyword(response, milestoneDesc);
            if (found) {
                result.add(MilestoneStatus.achieved(milestoneDesc,
                        "Keywords from milestone found in response"));
            } else {
                result.add(MilestoneStatus.pending(milestoneDesc));
            }
        }
        return List.copyOf(result);
    }

    private boolean containsAnyKeyword(String text, String keywords) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String kw : keywords.split("[,;\\s]+")) {
            String trimmed = kw.trim().toLowerCase(Locale.ROOT);
            if (trimmed.length() >= 3 && lower.contains(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private String buildAssessment(double score, List<MilestoneStatus> milestones) {
        long achieved = milestones.stream().filter(MilestoneStatus::achieved).count();
        return String.format("Completion: %.0f%%. Milestones: %d/%d achieved.",
                score * 100, achieved, milestones.size());
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize {}: {}", type.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
