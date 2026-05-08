package com.schemaplexai.agent.engine.goal;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoalTrackerTest {

    private GoalTracker tracker;
    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        tracker = new GoalTracker();
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(100L);
    }

    @Test
    void createGoal_shouldStoreGoalAndInitialProgressInMetadata() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working, tests passing",
                List.of("Design schema", "Implement endpoints", "Write tests"));

        tracker.createGoal("exec-1", execution, goal);

        GoalMetadata stored = tracker.loadGoal(execution);
        assertThat(stored).isNotNull();
        assertThat(stored.goalId()).isEqualTo("goal-1");
        assertThat(stored.status()).isEqualTo(GoalStatus.IN_PROGRESS);
        assertThat(stored.description()).isEqualTo("Build REST API");
        assertThat(stored.milestones()).hasSize(3);

        GoalProgress progress = tracker.loadProgress(execution);
        assertThat(progress).isNotNull();
        assertThat(progress.completionScore()).isEqualTo(0.0);
        assertThat(progress.milestones()).hasSize(3);
        assertThat(progress.milestones()).allMatch(ms -> !ms.achieved());
    }

    @Test
    void assessProgress_shouldIncreaseScoreWhenCriteriaKeywordsAppear() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints, tests passing",
                List.of("Design schema"));

        tracker.createGoal("exec-1", execution, goal);

        // Response contains one of two criteria keywords
        GoalProgress progress = tracker.assessProgress(execution,
                "The endpoints are now implemented and working.");

        assertThat(progress.completionScore()).isEqualTo(0.5);
    }

    @Test
    void assessProgress_shouldReturnHighScoreWhenAllCriteriaMet() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working, tests passing",
                List.of("Design schema"));

        tracker.createGoal("exec-1", execution, goal);

        GoalProgress progress = tracker.assessProgress(execution,
                "All endpoints working correctly. All tests passing.");

        assertThat(progress.completionScore()).isEqualTo(1.0);
    }

    @Test
    void assessProgress_shouldReturnZeroScoreWhenNoCriteriaMet() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working, tests passing",
                List.of("Design schema"));

        tracker.createGoal("exec-1", execution, goal);

        GoalProgress progress = tracker.assessProgress(execution,
                "Still working on the initial setup.");

        assertThat(progress.completionScore()).isEqualTo(0.0);
    }

    @Test
    void assessProgress_shouldMarkGoalAchievedWhenScoreExceedsThreshold() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Simple task", "done",
                List.of());

        tracker.createGoal("exec-1", execution, goal);

        tracker.assessProgress(execution, "The task is done and complete.");

        GoalMetadata stored = tracker.loadGoal(execution);
        assertThat(stored.status()).isEqualTo(GoalStatus.ACHIEVED);
    }

    @Test
    void assessProgress_shouldHandleNullResponse() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working",
                List.of());

        tracker.createGoal("exec-1", execution, goal);

        GoalProgress progress = tracker.assessProgress(execution, null);

        assertThat(progress.completionScore()).isEqualTo(0.0);
    }

    @Test
    void assessProgress_shouldHandleNoGoalInMetadata() {
        GoalProgress progress = tracker.assessProgress(execution, "some response");

        assertThat(progress.goalId()).isNull();
        assertThat(progress.completionScore()).isEqualTo(0.0);
    }

    @Test
    void isGoalAchieved_shouldReturnTrueWhenScoreAboveThreshold() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Simple task", "done",
                List.of());

        tracker.createGoal("exec-1", execution, goal);
        tracker.assessProgress(execution, "done");

        assertThat(tracker.isGoalAchieved(execution)).isTrue();
    }

    @Test
    void isGoalAchieved_shouldReturnFalseWhenScoreBelowThreshold() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build API", "endpoints, tests passing, documentation",
                List.of());

        tracker.createGoal("exec-1", execution, goal);
        tracker.assessProgress(execution, "Only endpoints are working.");

        assertThat(tracker.isGoalAchieved(execution)).isFalse();
    }

    @Test
    void isGoalAchieved_shouldReturnFalseWhenNoGoalExists() {
        assertThat(tracker.isGoalAchieved(execution)).isFalse();
    }

    @Test
    void updateMilestone_shouldMarkMilestoneAsAchieved() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working",
                List.of("Design schema", "Implement endpoints"));

        tracker.createGoal("exec-1", execution, goal);

        tracker.updateMilestone(execution, "Design schema", true, "Schema designed with 5 tables");

        GoalProgress progress = tracker.loadProgress(execution);
        assertThat(progress.milestones().get(0).achieved()).isTrue();
        assertThat(progress.milestones().get(0).evidence()).isEqualTo("Schema designed with 5 tables");
        assertThat(progress.milestones().get(1).achieved()).isFalse();
    }

    @Test
    void updateMilestone_shouldRecalculateScoreBasedOnMilestoneCompletion() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working",
                List.of("Design schema", "Implement endpoints", "Write tests"));

        tracker.createGoal("exec-1", execution, goal);

        tracker.updateMilestone(execution, "Design schema", true, "Done");
        tracker.updateMilestone(execution, "Implement endpoints", true, "Done");

        GoalProgress progress = tracker.loadProgress(execution);
        // 2 out of 3 milestones = 0.667
        assertThat(progress.completionScore()).isCloseTo(0.667, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void updateMilestone_shouldPreserveAlreadyAchievedMilestones() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build REST API", "endpoints working",
                List.of("Design schema", "Implement endpoints"));

        tracker.createGoal("exec-1", execution, goal);

        tracker.updateMilestone(execution, "Design schema", true, "Done");
        tracker.updateMilestone(execution, "Implement endpoints", false, "Not yet");

        GoalProgress progress = tracker.loadProgress(execution);
        assertThat(progress.milestones().get(0).achieved()).isTrue();
        assertThat(progress.milestones().get(1).achieved()).isFalse();
    }

    @Test
    void updateMilestone_shouldHandleMissingProgress() {
        // No goal created -- should not throw
        tracker.updateMilestone(execution, "Some milestone", true, "evidence");

        assertThat(tracker.loadProgress(execution)).isNull();
    }

    @Test
    void calculateCompletionScore_shouldHandleEmptyCriteria() {
        assertThat(tracker.calculateCompletionScore(null, "response")).isEqualTo(0.0);
        assertThat(tracker.calculateCompletionScore("", "response")).isEqualTo(0.0);
        assertThat(tracker.calculateCompletionScore("   ", "response")).isEqualTo(0.0);
    }

    @Test
    void calculateCompletionScore_shouldHandleEmptyResponse() {
        assertThat(tracker.calculateCompletionScore("criteria", null)).isEqualTo(0.0);
        assertThat(tracker.calculateCompletionScore("criteria", "")).isEqualTo(0.0);
    }

    @Test
    void calculateCompletionScore_shouldBeCaseInsensitive() {
        double score = tracker.calculateCompletionScore("HELLO", "hello world");
        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void createGoal_shouldHandleNullMilestones() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Simple task", "done", null);

        tracker.createGoal("exec-1", execution, goal);

        GoalMetadata stored = tracker.loadGoal(execution);
        assertThat(stored.milestones()).isEmpty();
    }

    @Test
    void assessProgress_shouldAssessMilestoneKeywordsInResponse() {
        GoalMetadata goal = GoalMetadata.create(
                "goal-1", "Build API", "working",
                List.of("authentication implemented", "payment gateway integrated"));

        tracker.createGoal("exec-1", execution, goal);

        GoalProgress progress = tracker.assessProgress(execution,
                "The authentication implemented module is fully working.");

        // First milestone keyword found, second milestone keywords not in response
        assertThat(progress.milestones().get(0).achieved()).isTrue();
        assertThat(progress.milestones().get(1).achieved()).isFalse();
    }
}
