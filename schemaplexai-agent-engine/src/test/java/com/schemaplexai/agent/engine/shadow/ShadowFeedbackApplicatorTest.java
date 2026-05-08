package com.schemaplexai.agent.engine.shadow;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShadowFeedbackApplicatorTest {

    private ShadowFeedbackApplicator applicator;

    @BeforeEach
    void setUp() {
        applicator = new ShadowFeedbackApplicator();
    }

    // ---- applyToExecution ----

    @Test
    void shouldApplyFeedbackContextToExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);

        List<FeedbackSummary> feedback = List.of(
                FeedbackSummary.builder()
                        .memoryId(1L).agentId(10L).actionType(FeedbackActionType.ACCEPT)
                        .content("Looks good").createdAt(LocalDateTime.now()).build(),
                FeedbackSummary.builder()
                        .memoryId(2L).agentId(10L).actionType(FeedbackActionType.RETRY)
                        .content("Try again").createdAt(LocalDateTime.now()).build()
        );

        applicator.applyToExecution(execution, feedback);

        assertThat(execution.getMetadata("shadowFeedbackContext")).isNotNull();
        assertThat((String) execution.getMetadata("shadowFeedbackContext")).contains("ACCEPT");
        assertThat((String) execution.getMetadata("shadowFeedbackContext")).contains("RETRY");
        assertThat(execution.getMetadata("feedbackCount")).isEqualTo(2);
        assertThat(execution.getMetadata("historicalAcceptanceRate")).isEqualTo(0.5);
    }

    @Test
    void shouldSuggestModelOverrideForLowAcceptanceRate() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(200L);

        List<FeedbackSummary> feedback = List.of(
                FeedbackSummary.builder()
                        .memoryId(1L).agentId(20L).actionType(FeedbackActionType.RETRY).content("fail").createdAt(LocalDateTime.now()).build(),
                FeedbackSummary.builder()
                        .memoryId(2L).agentId(20L).actionType(FeedbackActionType.ESCALATE).content("esc").createdAt(LocalDateTime.now()).build(),
                FeedbackSummary.builder()
                        .memoryId(3L).agentId(20L).actionType(FeedbackActionType.RETRY).content("fail2").createdAt(LocalDateTime.now()).build()
        );

        applicator.applyToExecution(execution, feedback);

        assertThat(execution.getMetadata("suggestedModelOverride")).isEqualTo("stronger-model");
        assertThat(execution.getMetadata("historicalAcceptanceRate")).isEqualTo(0.0);
    }

    @Test
    void shouldSuggestPromptAdjustmentForRetryAndEscalation() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(300L);

        List<FeedbackSummary> feedback = List.of(
                FeedbackSummary.builder()
                        .memoryId(1L).agentId(30L).actionType(FeedbackActionType.RETRY).content(" ambiguous ").createdAt(LocalDateTime.now()).build(),
                FeedbackSummary.builder()
                        .memoryId(2L).agentId(30L).actionType(FeedbackActionType.MODIFY_PROMPT).content("reword").createdAt(LocalDateTime.now()).build(),
                FeedbackSummary.builder()
                        .memoryId(3L).agentId(30L).actionType(FeedbackActionType.ESCALATE).content("human").createdAt(LocalDateTime.now()).build()
        );

        applicator.applyToExecution(execution, feedback);

        String adjustment = (String) execution.getMetadata("promptAdjustment");
        assertThat(adjustment).isNotNull();
        assertThat(adjustment).contains("specific instructions");
        assertThat(adjustment).contains("prompt wording");
        assertThat(adjustment).contains("guardrails");
    }

    @Test
    void shouldNotSuggestPromptAdjustmentWhenNoIssues() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(400L);

        List<FeedbackSummary> feedback = List.of(
                FeedbackSummary.builder()
                        .memoryId(1L).agentId(40L).actionType(FeedbackActionType.ACCEPT).content("ok").createdAt(LocalDateTime.now()).build()
        );

        applicator.applyToExecution(execution, feedback);

        assertThat(execution.getMetadata("promptAdjustment")).isNull();
        assertThat(execution.getMetadata("suggestedModelOverride")).isNull();
    }

    @Test
    void shouldHandleNullExecutionGracefully() {
        List<FeedbackSummary> feedback = List.of(
                FeedbackSummary.builder()
                        .memoryId(1L).agentId(50L).actionType(FeedbackActionType.ACCEPT).build()
        );

        // Should not throw
        applicator.applyToExecution(null, feedback);
    }

    @Test
    void shouldHandleEmptyFeedbackGracefully() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(500L);

        applicator.applyToExecution(execution, List.of());

        assertThat(execution.getMetadata("shadowFeedbackContext")).isNull();
        assertThat(execution.getMetadata("feedbackCount")).isNull();
    }

    @Test
    void shouldHandleNullFeedbackGracefully() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(600L);

        applicator.applyToExecution(execution, null);

        assertThat(execution.getMetadata("shadowFeedbackContext")).isNull();
    }

    @Test
    void shouldSuggestPromptAdjustmentForHighEscalationRate() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(700L);

        // 4 escalations out of 10 = 0.4 escalation rate (above 0.3 threshold)
        List<FeedbackSummary> feedback = List.of(
                FeedbackSummary.builder().memoryId(1L).agentId(70L).actionType(FeedbackActionType.ESCALATE).build(),
                FeedbackSummary.builder().memoryId(2L).agentId(70L).actionType(FeedbackActionType.ESCALATE).build(),
                FeedbackSummary.builder().memoryId(3L).agentId(70L).actionType(FeedbackActionType.ESCALATE).build(),
                FeedbackSummary.builder().memoryId(4L).agentId(70L).actionType(FeedbackActionType.ESCALATE).build(),
                FeedbackSummary.builder().memoryId(5L).agentId(70L).actionType(FeedbackActionType.ACCEPT).build(),
                FeedbackSummary.builder().memoryId(6L).agentId(70L).actionType(FeedbackActionType.ACCEPT).build(),
                FeedbackSummary.builder().memoryId(7L).agentId(70L).actionType(FeedbackActionType.ACCEPT).build(),
                FeedbackSummary.builder().memoryId(8L).agentId(70L).actionType(FeedbackActionType.ACCEPT).build(),
                FeedbackSummary.builder().memoryId(9L).agentId(70L).actionType(FeedbackActionType.ACCEPT).build(),
                FeedbackSummary.builder().memoryId(10L).agentId(70L).actionType(FeedbackActionType.ACCEPT).build()
        );

        applicator.applyToExecution(execution, feedback);

        String adjustment = (String) execution.getMetadata("promptAdjustment");
        assertThat(adjustment).isNotNull();
        assertThat(adjustment).contains("High escalation rate detected");
    }

    // ---- suggestModelChange ----

    @Test
    void shouldSuggestStrongerModelForLowAcceptanceRate() {
        FeedbackTrend trend = FeedbackTrend.builder()
                .agentId(1L)
                .acceptanceRate(0.3)
                .build();

        String suggestion = applicator.suggestModelChange(trend);

        assertThat(suggestion).isEqualTo("stronger-model");
    }

    @Test
    void shouldSuggestHumanInTheLoopForHighEscalationRate() {
        FeedbackTrend trend = FeedbackTrend.builder()
                .agentId(2L)
                .acceptanceRate(0.8)
                .escalationRate(0.4)
                .build();

        String suggestion = applicator.suggestModelChange(trend);

        assertThat(suggestion).isEqualTo("human-in-the-loop");
    }

    @Test
    void shouldReturnNullWhenTrendIsHealthy() {
        FeedbackTrend trend = FeedbackTrend.builder()
                .agentId(3L)
                .acceptanceRate(0.9)
                .escalationRate(0.05)
                .build();

        String suggestion = applicator.suggestModelChange(trend);

        assertThat(suggestion).isNull();
    }

    @Test
    void shouldReturnNullForNullTrend() {
        assertThat(applicator.suggestModelChange(null)).isNull();
    }

    @Test
    void shouldPrioritizeLowAcceptanceOverHighEscalation() {
        // Both conditions met: low acceptance takes priority in current implementation
        FeedbackTrend trend = FeedbackTrend.builder()
                .agentId(4L)
                .acceptanceRate(0.2)
                .escalationRate(0.5)
                .build();

        String suggestion = applicator.suggestModelChange(trend);

        assertThat(suggestion).isEqualTo("stronger-model");
    }
}
