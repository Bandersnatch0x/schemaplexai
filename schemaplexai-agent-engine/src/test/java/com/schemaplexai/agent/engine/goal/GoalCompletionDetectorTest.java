package com.schemaplexai.agent.engine.goal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoalCompletionDetectorTest {

    private GoalCompletionDetector detector;

    @BeforeEach
    void setUp() {
        detector = new GoalCompletionDetector();
    }

    @Test
    void shouldDetectFullyAchievedWhenAllMilestonesComplete() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                1.0,
                List.of(
                        MilestoneStatus.achieved("M1", "done"),
                        MilestoneStatus.achieved("M2", "done")
                ),
                "All done",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress);

        assertThat(signal.complete()).isTrue();
        assertThat(signal.type()).isEqualTo(CompletionType.FULLY_ACHIEVED);
        assertThat(signal.reason()).contains("All 2 milestones achieved");
    }

    @Test
    void shouldNotCompleteWhenSomeMilestonesPending() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                0.5,
                List.of(
                        MilestoneStatus.achieved("M1", "done"),
                        MilestoneStatus.pending("M2")
                ),
                "Half done",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress);

        assertThat(signal.complete()).isFalse();
    }

    @Test
    void shouldNotCompleteWhenNoMilestonesExist() {
        GoalProgress progress = new GoalProgress(
                "goal-1", 0.0, List.of(), "No milestones", Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress);

        assertThat(signal.complete()).isFalse();
    }

    @Test
    void shouldReturnNotCompleteWhenProgressIsNull() {
        CompletionSignal signal = detector.detectCompletion(null);

        assertThat(signal.complete()).isFalse();
        assertThat(signal.reason()).isNull();
    }

    @Test
    void shouldDetectMaxIterationsWithSomeAchieved() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                0.5,
                List.of(
                        MilestoneStatus.achieved("M1", "done"),
                        MilestoneStatus.pending("M2")
                ),
                "Partial",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress, 10, 10);

        assertThat(signal.complete()).isTrue();
        assertThat(signal.type()).isEqualTo(CompletionType.PARTIALLY_ACHIEVED);
        assertThat(signal.reason()).contains("Max iterations");
        assertThat(signal.reason()).contains("1/2 milestones achieved");
    }

    @Test
    void shouldDetectMaxIterationsWithNoneAchieved() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                0.0,
                List.of(
                        MilestoneStatus.pending("M1"),
                        MilestoneStatus.pending("M2")
                ),
                "No progress",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress, 10, 10);

        assertThat(signal.complete()).isTrue();
        assertThat(signal.type()).isEqualTo(CompletionType.MAX_ITERATIONS);
        assertThat(signal.reason()).contains("No milestones achieved");
    }

    @Test
    void shouldNotCompleteWhenBelowMaxIterations() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                0.3,
                List.of(MilestoneStatus.pending("M1")),
                "In progress",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress, 5, 10);

        assertThat(signal.complete()).isFalse();
    }

    @Test
    void shouldDetectFullyAchievedEvenAtMaxIterations() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                1.0,
                List.of(MilestoneStatus.achieved("M1", "done")),
                "All done",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress, 10, 10);

        // Fully achieved takes priority over max iterations
        assertThat(signal.complete()).isTrue();
        assertThat(signal.type()).isEqualTo(CompletionType.FULLY_ACHIEVED);
    }

    @Test
    void shouldHandleNullProgressWithIterationOverload() {
        CompletionSignal signal = detector.detectCompletion(null, 10, 10);

        assertThat(signal.complete()).isFalse();
    }

    @Test
    void shouldDetectFullyAchievedWithSingleMilestone() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                1.0,
                List.of(MilestoneStatus.achieved("M1", "done")),
                "Done",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress);

        assertThat(signal.complete()).isTrue();
        assertThat(signal.type()).isEqualTo(CompletionType.FULLY_ACHIEVED);
        assertThat(signal.reason()).contains("All 1 milestones achieved");
    }

    @Test
    void shouldHandleIterationExactlyAtMax() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                0.3,
                List.of(MilestoneStatus.pending("M1")),
                "Working",
                Instant.now()
        );

        // currentIteration == maxIterations should trigger
        CompletionSignal signal = detector.detectCompletion(progress, 10, 10);

        assertThat(signal.complete()).isTrue();
        assertThat(signal.type()).isEqualTo(CompletionType.MAX_ITERATIONS);
    }

    @Test
    void shouldHandleIterationJustBelowMax() {
        GoalProgress progress = new GoalProgress(
                "goal-1",
                0.3,
                List.of(MilestoneStatus.pending("M1")),
                "Working",
                Instant.now()
        );

        CompletionSignal signal = detector.detectCompletion(progress, 9, 10);

        assertThat(signal.complete()).isFalse();
    }

    @Test
    void completionSignalFactoryMethods_shouldReturnCorrectTypes() {
        CompletionSignal notComplete = CompletionSignal.notComplete();
        assertThat(notComplete.complete()).isFalse();

        CompletionSignal full = CompletionSignal.fullyAchieved("reason");
        assertThat(full.complete()).isTrue();
        assertThat(full.type()).isEqualTo(CompletionType.FULLY_ACHIEVED);

        CompletionSignal partial = CompletionSignal.partiallyAchieved("reason");
        assertThat(partial.complete()).isTrue();
        assertThat(partial.type()).isEqualTo(CompletionType.PARTIALLY_ACHIEVED);

        CompletionSignal maxIter = CompletionSignal.maxIterations("reason");
        assertThat(maxIter.complete()).isTrue();
        assertThat(maxIter.type()).isEqualTo(CompletionType.MAX_ITERATIONS);

        CompletionSignal timeout = CompletionSignal.timeout("reason");
        assertThat(timeout.complete()).isTrue();
        assertThat(timeout.type()).isEqualTo(CompletionType.TIMEOUT);
    }
}
