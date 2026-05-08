package com.schemaplexai.agent.engine.shadow;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies historical shadow feedback to an execution by enriching its metadata
 * with feedback context and, when appropriate, suggesting model or prompt changes.
 */
@Slf4j
@Component
public class ShadowFeedbackApplicator {

    private static final String METADATA_FEEDBACK_CONTEXT = "shadowFeedbackContext";
    private static final String METADATA_SUGGESTED_MODEL = "suggestedModelOverride";
    private static final String METADATA_PROMPT_ADJUSTMENT = "promptAdjustment";
    private static final String METADATA_ACCEPTANCE_RATE = "historicalAcceptanceRate";
    private static final String METADATA_FEEDBACK_COUNT = "feedbackCount";

    private static final double LOW_ACCEPTANCE_THRESHOLD = 0.5;
    private static final double HIGH_ESCALATION_THRESHOLD = 0.3;

    /**
     * Apply historical feedback to an execution by injecting feedback metadata.
     *
     * @param execution the execution to enrich
     * @param feedback  list of recent feedback summaries
     */
    public void applyToExecution(SfAgentExecution execution, List<FeedbackSummary> feedback) {
        if (execution == null) {
            log.warn("Cannot apply feedback to null execution");
            return;
        }
        if (feedback == null || feedback.isEmpty()) {
            log.debug("No feedback to apply for execution {}", execution.getId());
            return;
        }

        log.info("Applying {} feedback entries to execution {}", feedback.size(), execution.getId());

        // Build a concise feedback context string
        String feedbackContext = buildFeedbackContext(feedback);
        execution.setMetadata(METADATA_FEEDBACK_CONTEXT, feedbackContext);
        execution.setMetadata(METADATA_FEEDBACK_COUNT, feedback.size());

        // Calculate acceptance rate from the provided feedback list
        double acceptanceRate = calculateLocalAcceptanceRate(feedback);
        execution.setMetadata(METADATA_ACCEPTANCE_RATE, acceptanceRate);

        // Suggest model override if acceptance is low
        if (acceptanceRate < LOW_ACCEPTANCE_THRESHOLD) {
            execution.setMetadata(METADATA_SUGGESTED_MODEL, "stronger-model");
            log.debug("Suggested model override for execution {} due to low acceptance rate ({}",
                    execution.getId(), acceptanceRate);
        }

        // Suggest prompt adjustment if there are many retries or escalations
        long retryCount = feedback.stream()
                .filter(f -> f.getActionType() == FeedbackActionType.RETRY)
                .count();
        long escalateCount = feedback.stream()
                .filter(f -> f.getActionType() == FeedbackActionType.ESCALATE)
                .count();
        long modifyPromptCount = feedback.stream()
                .filter(f -> f.getActionType() == FeedbackActionType.MODIFY_PROMPT)
                .count();

        double escalationRate = feedback.isEmpty() ? 0.0 : (double) escalateCount / feedback.size();

        if (retryCount > 0 || modifyPromptCount > 0 || escalationRate >= HIGH_ESCALATION_THRESHOLD) {
            StringBuilder adjustment = new StringBuilder();
            if (retryCount > 0) {
                adjustment.append("Consider adding more specific instructions to reduce ambiguity. ");
            }
            if (modifyPromptCount > 0) {
                adjustment.append("Review and refine prompt wording based on past MODIFY_PROMPT feedback. ");
            }
            if (escalationRate >= HIGH_ESCALATION_THRESHOLD) {
                adjustment.append("High escalation rate detected; consider adding guardrails or human-in-the-loop checkpoints. ");
            }
            execution.setMetadata(METADATA_PROMPT_ADJUSTMENT, adjustment.toString().trim());
        }
    }

    /**
     * Suggest a model change based on the aggregated feedback trend.
     *
     * @param trend the feedback trend for the agent
     * @return suggested model identifier, or null if no change is suggested
     */
    public String suggestModelChange(FeedbackTrend trend) {
        if (trend == null) {
            return null;
        }
        if (trend.getAcceptanceRate() < LOW_ACCEPTANCE_THRESHOLD) {
            log.info("Agent {} has low acceptance rate ({}); suggesting stronger model",
                    trend.getAgentId(), trend.getAcceptanceRate());
            return "stronger-model";
        }
        if (trend.getEscalationRate() >= HIGH_ESCALATION_THRESHOLD) {
            log.info("Agent {} has high escalation rate ({}); suggesting human-in-the-loop model",
                    trend.getAgentId(), trend.getEscalationRate());
            return "human-in-the-loop";
        }
        return null;
    }

    private String buildFeedbackContext(List<FeedbackSummary> feedback) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recent shadow feedback (").append(feedback.size()).append(" entries): ");
        for (FeedbackSummary summary : feedback) {
            sb.append("[").append(summary.getActionType()).append("]");
            if (summary.getContent() != null && !summary.getContent().isBlank()) {
                String trimmed = summary.getContent().trim();
                if (trimmed.length() > 40) {
                    trimmed = trimmed.substring(0, 40) + "...";
                }
                sb.append(" ").append(trimmed);
            }
            sb.append("; ");
        }
        return sb.toString().trim();
    }

    private double calculateLocalAcceptanceRate(List<FeedbackSummary> feedback) {
        if (feedback.isEmpty()) {
            return 1.0;
        }
        long acceptCount = feedback.stream()
                .filter(f -> f.getActionType() == FeedbackActionType.ACCEPT)
                .count();
        return (double) acceptCount / feedback.size();
    }
}
