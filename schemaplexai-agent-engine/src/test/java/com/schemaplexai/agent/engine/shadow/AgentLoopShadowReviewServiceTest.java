package com.schemaplexai.agent.engine.shadow;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AgentLoopShadowReviewServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SfAgentMemoryMapper memoryMapper;

    @Mock
    private ShadowFeedbackRetriever feedbackRetriever;

    @Mock
    private ShadowFeedbackApplicator feedbackApplicator;

    private AgentLoopShadowReviewService service;

    @BeforeEach
    void setUp() {
        service = new AgentLoopShadowReviewService(objectMapper, memoryMapper, feedbackRetriever, feedbackApplicator);
    }

    // ---- parseFeedbackActions ----

    @Test
    void shouldParseValidFeedbackActionsJson() throws Exception {
        String json = "[{\"type\":\"ACCEPT\",\"description\":\"Looks good\",\"payload\":\"ok\"}]";
        FeedbackAction action = FeedbackAction.builder()
                .type(FeedbackActionType.ACCEPT)
                .description("Looks good")
                .payload("ok")
                .build();
        when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(action));

        List<FeedbackAction> result = service.parseFeedbackActions(json);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(FeedbackActionType.ACCEPT);
    }

    @Test
    void shouldReturnEmptyListOnParseError() throws Exception {
        when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("bad json"));

        List<FeedbackAction> result = service.parseFeedbackActions("invalid");

        assertThat(result).isEmpty();
    }

    // ---- applyFeedbackAction ----

    @Test
    void shouldPersistFeedbackAsMemory() {
        FeedbackAction action = FeedbackAction.builder()
                .type(FeedbackActionType.RETRY)
                .description("Try again")
                .payload("retry-payload")
                .build();

        service.applyFeedbackAction(100L, 10L, action);

        ArgumentCaptor<SfAgentMemory> captor = ArgumentCaptor.forClass(SfAgentMemory.class);
        verify(memoryMapper).insert(captor.capture());

        SfAgentMemory memory = captor.getValue();
        assertThat(memory.getAgentId()).isEqualTo(10L);
        assertThat(memory.getMemoryType()).isEqualTo("SHADOW_FEEDBACK");
        assertThat(memory.getContent()).isEqualTo("retry-payload");
        assertThat(memory.getSourceExecutionId()).isEqualTo(100L);
    }

    @Test
    void shouldLogAcceptAction() {
        FeedbackAction action = FeedbackAction.builder()
                .type(FeedbackActionType.ACCEPT)
                .description("OK")
                .payload("accept-payload")
                .build();

        service.applyFeedbackAction(200L, 20L, action);

        verify(memoryMapper).insert(any(SfAgentMemory.class));
    }

    // ---- reviewLoop ----

    @Test
    void shouldLogSuggestedActionsInShadowMode() throws Exception {
        String json = "[{\"type\":\"ESCALATE\",\"description\":\"Needs review\"}]";
        FeedbackAction action = FeedbackAction.builder()
                .type(FeedbackActionType.ESCALATE)
                .description("Needs review")
                .build();
        when(objectMapper.readValue(any(String.class), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenReturn(List.of(action));

        // Should not throw and should not call memoryMapper.insert
        service.reviewLoop(300L, 30L, json);

        verify(memoryMapper, never()).insert(any());
    }

    @Test
    void shouldHandleEmptyActionsInReviewLoop() {
        service.reviewLoop(400L, 40L, "[]");

        verify(memoryMapper, never()).insert(any());
    }

    // ---- getRecentFeedback ----

    @Test
    void shouldDelegateToRetrieverForRecentFeedback() {
        List<FeedbackSummary> expected = List.of(
                FeedbackSummary.builder()
                        .memoryId(1L).agentId(50L).actionType(FeedbackActionType.ACCEPT)
                        .createdAt(LocalDateTime.now()).build()
        );
        when(feedbackRetriever.retrieveRecentFeedback(50L, 5)).thenReturn(expected);

        List<FeedbackSummary> result = service.getRecentFeedback(50L, 5);

        assertThat(result).isEqualTo(expected);
        verify(feedbackRetriever).retrieveRecentFeedback(50L, 5);
    }

    // ---- getFeedbackTrend ----

    @Test
    void shouldDelegateToRetrieverForFeedbackTrend() {
        FeedbackTrend expected = FeedbackTrend.builder()
                .agentId(60L)
                .totalCount(10L)
                .acceptanceRate(0.8)
                .build();
        when(feedbackRetriever.getFeedbackTrend(60L, Duration.ofDays(7))).thenReturn(expected);

        FeedbackTrend result = service.getFeedbackTrend(60L);

        assertThat(result).isEqualTo(expected);
        verify(feedbackRetriever).getFeedbackTrend(60L, Duration.ofDays(7));
    }

    // ---- all action types ----

    @Test
    void shouldHandleAllFeedbackActionTypes() {
        for (FeedbackActionType type : FeedbackActionType.values()) {
            FeedbackAction action = FeedbackAction.builder()
                    .type(type)
                    .description(type.name())
                    .payload(type.name().toLowerCase())
                    .build();

            service.applyFeedbackAction(500L + type.ordinal(), 50L, action);
        }

        verify(memoryMapper, times(FeedbackActionType.values().length)).insert(any(SfAgentMemory.class));
    }
}
