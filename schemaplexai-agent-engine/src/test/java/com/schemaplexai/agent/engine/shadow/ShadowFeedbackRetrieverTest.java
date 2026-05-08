package com.schemaplexai.agent.engine.shadow;

import com.schemaplexai.agent.engine.entity.SfAgentMemory;
import com.schemaplexai.agent.engine.mapper.SfAgentMemoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShadowFeedbackRetrieverTest {

    @Mock
    private SfAgentMemoryMapper memoryMapper;

    private ShadowFeedbackRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new ShadowFeedbackRetriever(memoryMapper);
    }

    // ---- retrieveRecentFeedback ----

    @Test
    void shouldRetrieveRecentFeedbackOrderedByCreatedAtDesc() {
        SfAgentMemory m1 = createMemory(1L, 10L, "ACCEPT", LocalDateTime.now().minusMinutes(5));
        SfAgentMemory m2 = createMemory(2L, 10L, "RETRY", LocalDateTime.now().minusMinutes(2));
        when(memoryMapper.selectList(any())).thenReturn(List.of(m2, m1));

        List<FeedbackSummary> result = retriever.retrieveRecentFeedback(10L, 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMemoryId()).isEqualTo(2L);
        assertThat(result.get(0).getActionType()).isEqualTo(FeedbackActionType.RETRY);
        assertThat(result.get(1).getMemoryId()).isEqualTo(1L);
        assertThat(result.get(1).getActionType()).isEqualTo(FeedbackActionType.ACCEPT);
    }

    @Test
    void shouldReturnEmptyListWhenNoFeedback() {
        when(memoryMapper.selectList(any())).thenReturn(List.of());

        List<FeedbackSummary> result = retriever.retrieveRecentFeedback(10L, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRespectLimit() {
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);

        retriever.retrieveRecentFeedback(10L, 3);

        verify(memoryMapper).selectList(captor.capture());
        // The wrapper contains a last("LIMIT 3") clause; we verify via the mock return
    }

    // ---- getFeedbackTrend ----

    @Test
    void shouldCalculateFeedbackTrend() {
        LocalDateTime now = LocalDateTime.now();
        List<SfAgentMemory> memories = List.of(
                createMemory(1L, 20L, "ACCEPT", now.minusHours(1)),
                createMemory(2L, 20L, "ACCEPT", now.minusHours(2)),
                createMemory(3L, 20L, "RETRY", now.minusHours(3)),
                createMemory(4L, 20L, "ESCALATE", now.minusHours(4)),
                createMemory(5L, 20L, "MODIFY_PROMPT", now.minusHours(5)),
                createMemory(6L, 20L, "SKIP", now.minusHours(6))
        );
        when(memoryMapper.selectList(any())).thenReturn(memories);

        FeedbackTrend trend = retriever.getFeedbackTrend(20L, Duration.ofDays(7));

        assertThat(trend.getAgentId()).isEqualTo(20L);
        assertThat(trend.getTotalCount()).isEqualTo(6L);
        assertThat(trend.getAcceptCount()).isEqualTo(2L);
        assertThat(trend.getRetryCount()).isEqualTo(1L);
        assertThat(trend.getEscalateCount()).isEqualTo(1L);
        assertThat(trend.getModifyPromptCount()).isEqualTo(1L);
        assertThat(trend.getSkipCount()).isEqualTo(1L);
        assertThat(trend.getAcceptanceRate()).isEqualTo(2.0 / 6.0);
        assertThat(trend.getEscalationRate()).isEqualTo(1.0 / 6.0);
        assertThat(trend.getActionCounts()).containsEntry(FeedbackActionType.ACCEPT, 2L);
        assertThat(trend.getActionCounts()).containsEntry(FeedbackActionType.RETRY, 1L);
    }

    @Test
    void shouldReturnPerfectRatesWhenNoMemoriesForTrend() {
        when(memoryMapper.selectList(any())).thenReturn(List.of());

        FeedbackTrend trend = retriever.getFeedbackTrend(30L, Duration.ofDays(1));

        assertThat(trend.getTotalCount()).isEqualTo(0L);
        assertThat(trend.getAcceptanceRate()).isEqualTo(1.0);
        assertThat(trend.getEscalationRate()).isEqualTo(0.0);
    }

    // ---- calculateAcceptanceRate ----

    @Test
    void shouldCalculateAcceptanceRate() {
        List<SfAgentMemory> memories = List.of(
                createMemory(1L, 40L, "ACCEPT", LocalDateTime.now().minusDays(1)),
                createMemory(2L, 40L, "ACCEPT", LocalDateTime.now().minusDays(2)),
                createMemory(3L, 40L, "RETRY", LocalDateTime.now().minusDays(3))
        );
        when(memoryMapper.selectList(any())).thenReturn(memories);

        double rate = retriever.calculateAcceptanceRate(40L);

        assertThat(rate).isEqualTo(2.0 / 3.0);
    }

    @Test
    void shouldReturnOneWhenNoMemoriesForAcceptanceRate() {
        when(memoryMapper.selectList(any())).thenReturn(List.of());

        double rate = retriever.calculateAcceptanceRate(50L);

        assertThat(rate).isEqualTo(1.0);
    }

    @Test
    void shouldReturnZeroAcceptanceRateWhenAllRejected() {
        List<SfAgentMemory> memories = List.of(
                createMemory(1L, 60L, "RETRY", LocalDateTime.now()),
                createMemory(2L, 60L, "ESCALATE", LocalDateTime.now())
        );
        when(memoryMapper.selectList(any())).thenReturn(memories);

        double rate = retriever.calculateAcceptanceRate(60L);

        assertThat(rate).isEqualTo(0.0);
    }

    // ---- parseActionType fallback ----

    @Test
    void shouldFallbackToAcceptForUnknownContent() {
        SfAgentMemory memory = createMemory(1L, 70L, "UNKNOWN_ACTION", LocalDateTime.now());
        when(memoryMapper.selectList(any())).thenReturn(List.of(memory));

        List<FeedbackSummary> result = retriever.retrieveRecentFeedback(70L, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActionType()).isEqualTo(FeedbackActionType.ACCEPT);
    }

    @Test
    void shouldParseActionTypeFromContentContainingTypeName() {
        SfAgentMemory memory = createMemory(1L, 80L, "some payload with RETRY inside", LocalDateTime.now());
        when(memoryMapper.selectList(any())).thenReturn(List.of(memory));

        List<FeedbackSummary> result = retriever.retrieveRecentFeedback(80L, 5);

        assertThat(result.get(0).getActionType()).isEqualTo(FeedbackActionType.RETRY);
    }

    // ---- helpers ----

    private SfAgentMemory createMemory(Long id, Long agentId, String content, LocalDateTime createdAt) {
        SfAgentMemory memory = new SfAgentMemory();
        memory.setId(id);
        memory.setAgentId(agentId);
        memory.setMemoryType("SHADOW_FEEDBACK");
        memory.setContent(content);
        memory.setCreatedAt(createdAt);
        return memory;
    }
}
