package com.schemaplexai.agent.engine.cost;

import com.schemaplexai.agent.engine.mq.CostRecordedEventPublisher;
import com.schemaplexai.model.event.CostRecordedEvent;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the cost-collection recorder (ticket 919): every completed LLM call
 * inside a bound execution context produces a CostRecordedEvent.
 */
@ExtendWith(MockitoExtension.class)
class TokenUsageRecorderTest {

    @Mock
    private CostRecordedEventPublisher publisher;

    private TokenUsageRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new TokenUsageRecorder(publisher);
    }

    @AfterEach
    void tearDown() {
        LlmCallContext.clear();
        LlmUsageDispatch.setListener(null);
    }

    @Test
    void onTokenUsage_boundContext_publishesFullEvent() {
        LlmCallContext.set(new LlmCallContext(1001L, "10", 42L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(1000, 500, 1500));

        ArgumentCaptor<CostRecordedEvent> captor = ArgumentCaptor.forClass(CostRecordedEvent.class);
        verify(publisher).publishCostRecorded(captor.capture());

        CostRecordedEvent event = captor.getValue();
        assertNotNull(event.eventId());
        assertEquals(1001L, event.executionId());
        assertEquals(10L, event.tenantId());
        assertEquals(42L, event.agentId());
        assertEquals("gpt-4o", event.modelName());
        assertEquals("OPENAI", event.provider());
        assertEquals(TokenUsageRecorder.REQUEST_TYPE_CHAT, event.requestType());
        assertEquals(1000L, event.inputTokens());
        assertEquals(500L, event.outputTokens());
        assertEquals(1500L, event.totalTokens());
        assertNull(event.costAmount(), "pricing belongs to the consuming CostService");
        assertEquals(TokenUsageRecorder.DEFAULT_CURRENCY, event.currency());
        assertNotNull(event.occurredAt());
    }

    @Test
    void onTokenUsage_totalTokensDerivedWhenAbsent() {
        LlmCallContext.set(new LlmCallContext(1L, "1", 1L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(120, 30));

        ArgumentCaptor<CostRecordedEvent> captor = ArgumentCaptor.forClass(CostRecordedEvent.class);
        verify(publisher).publishCostRecorded(captor.capture());
        assertEquals(150L, captor.getValue().totalTokens());
    }

    @Test
    void onTokenUsage_partialCounts_defaultToZero() {
        LlmCallContext.set(new LlmCallContext(1L, "1", 1L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(120, null, 120));

        ArgumentCaptor<CostRecordedEvent> captor = ArgumentCaptor.forClass(CostRecordedEvent.class);
        verify(publisher).publishCostRecorded(captor.capture());
        assertEquals(120L, captor.getValue().inputTokens());
        assertEquals(0L, captor.getValue().outputTokens());
    }

    @Test
    void onTokenUsage_noBoundContext_skipsPublish() {
        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(1000, 500, 1500));

        verifyNoInteractions(publisher);
    }

    @Test
    void onTokenUsage_nullExecutionId_skipsPublish() {
        LlmCallContext.set(new LlmCallContext(null, "10", 42L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(1000, 500, 1500));

        verifyNoInteractions(publisher);
    }

    @Test
    void onTokenUsage_nonNumericTenant_skipsPublish() {
        LlmCallContext.set(new LlmCallContext(1L, "tenant-alpha", 42L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(1000, 500, 1500));

        verifyNoInteractions(publisher);
    }

    @Test
    void onTokenUsage_nullTenant_skipsPublish() {
        LlmCallContext.set(new LlmCallContext(1L, null, 42L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(1000, 500, 1500));

        verifyNoInteractions(publisher);
    }

    @Test
    void onTokenUsage_nullUsage_skipsPublish() {
        LlmCallContext.set(new LlmCallContext(1L, "10", 42L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", null);

        verifyNoInteractions(publisher);
    }

    @Test
    void onTokenUsage_emptyUsage_skipsPublish() {
        LlmCallContext.set(new LlmCallContext(1L, "10", 42L));

        recorder.onTokenUsage("OPENAI", "gpt-4o", new TokenUsage(null, null, null));

        verifyNoInteractions(publisher);
    }

    @Test
    void register_wiresDispatchToRecorder() {
        recorder.register();
        try {
            LlmCallContext.set(new LlmCallContext(7L, "3", 9L));

            LlmUsageDispatch.report("ANTHROPIC", "claude-3-haiku", new TokenUsage(10, 20, 30));

            ArgumentCaptor<CostRecordedEvent> captor = ArgumentCaptor.forClass(CostRecordedEvent.class);
            verify(publisher).publishCostRecorded(captor.capture());
            assertEquals(7L, captor.getValue().executionId());
            assertEquals("ANTHROPIC", captor.getValue().provider());
        } finally {
            recorder.unregister();
        }
    }

    @Test
    void dispatch_withoutListener_isNoOp() {
        LlmUsageDispatch.setListener(null);

        assertDoesNotThrow(() ->
                LlmUsageDispatch.report("OPENAI", "gpt-4o", new TokenUsage(1, 1, 2)));
    }

    @Test
    void dispatch_listenerFailure_isSwallowed() {
        LlmUsageDispatch.setListener((provider, model, usage) -> {
            throw new IllegalStateException("listener boom");
        });

        assertDoesNotThrow(() ->
                LlmUsageDispatch.report("OPENAI", "gpt-4o", new TokenUsage(1, 1, 2)));
    }
}
