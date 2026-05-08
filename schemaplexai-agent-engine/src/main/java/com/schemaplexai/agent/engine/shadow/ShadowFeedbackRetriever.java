package com.schemaplexai.agent.engine.shadow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentMemory;
import com.schemaplexai.agent.engine.mapper.SfAgentMemoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Retrieves historical shadow feedback from {@link SfAgentMemory} for a given agent.
 * Provides recent feedback summaries, trend analysis, and acceptance rate calculation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShadowFeedbackRetriever {

    private static final String SHADOW_FEEDBACK_MEMORY_TYPE = "SHADOW_FEEDBACK";

    private final SfAgentMemoryMapper memoryMapper;

    /**
     * Retrieve the most recent feedback entries for an agent.
     *
     * @param agentId the agent ID
     * @param limit   maximum number of entries to return
     * @return list of feedback summaries, ordered by most recent first
     */
    public List<FeedbackSummary> retrieveRecentFeedback(Long agentId, int limit) {
        log.debug("Retrieving recent feedback for agent {} limit {}", agentId, limit);

        List<SfAgentMemory> memories = memoryMapper.selectList(
                new LambdaQueryWrapper<SfAgentMemory>()
                        .eq(SfAgentMemory::getAgentId, agentId)
                        .eq(SfAgentMemory::getMemoryType, SHADOW_FEEDBACK_MEMORY_TYPE)
                        .orderByDesc(SfAgentMemory::getCreatedAt)
                        .last("LIMIT " + limit)
        );

        return memories.stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * Calculate the aggregated feedback trend for an agent over a given time window.
     *
     * @param agentId the agent ID
     * @param window  the time window to look back from now
     * @return feedback trend with counts and rates
     */
    public FeedbackTrend getFeedbackTrend(Long agentId, Duration window) {
        log.debug("Calculating feedback trend for agent {} over window {}", agentId, window);

        LocalDateTime since = LocalDateTime.now().minus(window);

        List<SfAgentMemory> memories = memoryMapper.selectList(
                new LambdaQueryWrapper<SfAgentMemory>()
                        .eq(SfAgentMemory::getAgentId, agentId)
                        .eq(SfAgentMemory::getMemoryType, SHADOW_FEEDBACK_MEMORY_TYPE)
                        .ge(SfAgentMemory::getCreatedAt, since)
        );

        return buildTrend(agentId, memories);
    }

    /**
     * Calculate the acceptance rate (fraction of ACCEPT actions) for an agent.
     *
     * @param agentId the agent ID
     * @return acceptance rate between 0.0 and 1.0; returns 1.0 if no feedback exists
     */
    public double calculateAcceptanceRate(Long agentId) {
        log.debug("Calculating acceptance rate for agent {}", agentId);

        List<SfAgentMemory> memories = memoryMapper.selectList(
                new LambdaQueryWrapper<SfAgentMemory>()
                        .eq(SfAgentMemory::getAgentId, agentId)
                        .eq(SfAgentMemory::getMemoryType, SHADOW_FEEDBACK_MEMORY_TYPE)
        );

        if (memories.isEmpty()) {
            return 1.0;
        }

        long acceptCount = memories.stream()
                .filter(m -> parseActionType(m.getContent()) == FeedbackActionType.ACCEPT)
                .count();

        return (double) acceptCount / memories.size();
    }

    private FeedbackSummary toSummary(SfAgentMemory memory) {
        return FeedbackSummary.builder()
                .memoryId(memory.getId())
                .agentId(memory.getAgentId())
                .sourceExecutionId(memory.getSourceExecutionId())
                .actionType(parseActionType(memory.getContent()))
                .content(memory.getContent())
                .createdAt(memory.getCreatedAt())
                .build();
    }

    private FeedbackTrend buildTrend(Long agentId, List<SfAgentMemory> memories) {
        Map<FeedbackActionType, Long> counts = new EnumMap<>(FeedbackActionType.class);
        for (FeedbackActionType type : FeedbackActionType.values()) {
            counts.put(type, 0L);
        }

        for (SfAgentMemory memory : memories) {
            FeedbackActionType type = parseActionType(memory.getContent());
            counts.merge(type, 1L, Long::sum);
        }

        long total = memories.size();
        long accept = counts.getOrDefault(FeedbackActionType.ACCEPT, 0L);
        long retry = counts.getOrDefault(FeedbackActionType.RETRY, 0L);
        long escalate = counts.getOrDefault(FeedbackActionType.ESCALATE, 0L);
        long modifyPrompt = counts.getOrDefault(FeedbackActionType.MODIFY_PROMPT, 0L);
        long skip = counts.getOrDefault(FeedbackActionType.SKIP, 0L);

        double acceptanceRate = total == 0 ? 1.0 : (double) accept / total;
        double escalationRate = total == 0 ? 0.0 : (double) escalate / total;

        return FeedbackTrend.builder()
                .agentId(agentId)
                .totalCount(total)
                .acceptCount(accept)
                .retryCount(retry)
                .escalateCount(escalate)
                .modifyPromptCount(modifyPrompt)
                .skipCount(skip)
                .acceptanceRate(acceptanceRate)
                .escalationRate(escalationRate)
                .actionCounts(counts)
                .build();
    }

    /**
     * Parse the action type from the memory content string.
     * The content is the raw payload stored by {@link AgentLoopShadowReviewService#applyFeedbackAction}.
     * We attempt to match known action type names as a best-effort heuristic.
     */
    private FeedbackActionType parseActionType(String content) {
        if (content == null || content.isBlank()) {
            return FeedbackActionType.ACCEPT;
        }
        String upper = content.toUpperCase();
        for (FeedbackActionType type : FeedbackActionType.values()) {
            if (upper.contains(type.name())) {
                return type;
            }
        }
        return FeedbackActionType.ACCEPT;
    }
}
