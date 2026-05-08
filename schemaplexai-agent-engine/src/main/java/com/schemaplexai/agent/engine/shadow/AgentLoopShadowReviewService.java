package com.schemaplexai.agent.engine.shadow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentMemory;
import com.schemaplexai.agent.engine.mapper.SfAgentMemoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLoopShadowReviewService {

    private final ObjectMapper objectMapper;
    private final SfAgentMemoryMapper memoryMapper;
    private final ShadowFeedbackRetriever feedbackRetriever;
    private final ShadowFeedbackApplicator feedbackApplicator;

    public List<FeedbackAction> parseFeedbackActions(String feedbackActionsJson) {
        try {
            return objectMapper.readValue(feedbackActionsJson, new TypeReference<List<FeedbackAction>>() {});
        } catch (Exception e) {
            log.error("Failed to parse feedback actions JSON", e);
            return List.of();
        }
    }

    public void applyFeedbackAction(Long executionId, Long agentId, FeedbackAction action) {
        log.info("Applying feedback action {} to execution {}", action.getType(), executionId);
        switch (action.getType()) {
            case RETRY -> log.info("Retrying execution {}", executionId);
            case SKIP -> log.info("Skipping current step for execution {}", executionId);
            case MODIFY_PROMPT -> log.info("Modifying prompt for execution {}", executionId);
            case ESCALATE -> log.info("Escalating execution {}", executionId);
            case ACCEPT -> log.info("Accepting result for execution {}", executionId);
        }
        // Persist feedback as memory for future improvement
        SfAgentMemory memory = new SfAgentMemory();
        memory.setAgentId(agentId);
        memory.setMemoryType("SHADOW_FEEDBACK");
        memory.setContent(action.getPayload());
        memory.setSourceExecutionId(executionId);
        memoryMapper.insert(memory);
    }

    public void reviewLoop(Long executionId, Long agentId, String shadowConfigJson) {
        List<FeedbackAction> actions = parseFeedbackActions(shadowConfigJson);
        if (actions == null || actions.isEmpty()) {
            log.warn("No shadow feedback actions configured for agent {}", agentId);
            return;
        }
        // In shadow mode, log suggested actions without applying them
        log.info("Shadow review for execution {} suggests actions: {}", executionId, actions);
    }

    /**
     * Get recent feedback entries for an agent.
     *
     * @param agentId the agent ID
     * @param limit   maximum number of entries
     * @return list of feedback summaries
     */
    public List<FeedbackSummary> getRecentFeedback(Long agentId, int limit) {
        return feedbackRetriever.retrieveRecentFeedback(agentId, limit);
    }

    /**
     * Get the aggregated feedback trend for an agent over the default window (7 days).
     *
     * @param agentId the agent ID
     * @return feedback trend
     */
    public FeedbackTrend getFeedbackTrend(Long agentId) {
        return feedbackRetriever.getFeedbackTrend(agentId, Duration.ofDays(7));
    }
}
