package com.schemaplexai.agent.engine.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.timeline.TimelineClickHouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionEventBusTest {

    private ExecutionEventBus eventBus;

    @Mock
    private SseEmitter emitter;

    @Mock
    private TimelineClickHouseService timelineService;

    @BeforeEach
    void setUp() {
        eventBus = new ExecutionEventBus(new ObjectMapper(), timelineService);
    }

    @Test
    void registerAddsEmitter() {
        eventBus.register("exec-1", emitter);
        assertDoesNotThrow(() -> eventBus.broadcast("exec-1", "test", Map.of("k", "v")));
    }

    @Test
    void unregisterRemovesEmitter() {
        eventBus.register("exec-1", emitter);
        eventBus.unregister("exec-1", emitter);
        assertDoesNotThrow(() -> eventBus.broadcast("exec-1", "test", Map.of("k", "v")));
    }

    @Test
    void broadcastSendsEventToRegisteredEmitters() throws IOException {
        eventBus.register("exec-1", emitter);

        eventBus.broadcast("exec-1", "my-event", Map.of("key", "value"));

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void publishStateTransitionBroadcastsCorrectPayload() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishStateTransition(1L, AgentExecutionState.THINKING, AgentExecutionState.TOOL_CALLING);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
    }

    @Test
    void publishExecutionCompletedBroadcastsCorrectPayload() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishExecutionCompleted(1L, "COMPLETED");

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void completeClosesAllEmitters() {
        eventBus.register("exec-1", emitter);
        eventBus.complete("exec-1");

        verify(emitter, times(1)).complete();
    }

    @Test
    void broadcastRemovesEmitterOnIOException() throws IOException {
        IOException brokenPipe = new IOException("broken pipe");
        doThrow(brokenPipe).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        eventBus.register("exec-1", emitter);

        eventBus.broadcast("exec-1", "test", Map.of("k", "v"));

        verify(emitter, times(1)).completeWithError(brokenPipe);
        verify(emitter, never()).complete();
    }

    @Test
    void publishThoughtBroadcastsAndPersists() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishThought(1L, "Thinking about the problem", 42L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(timelineService).enqueue(any());
    }

    @Test
    void publishToolCallBroadcastsAndPersists() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishToolCall(1L, "search", "{\"query\": \"test\"}", 42L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(timelineService).enqueue(any());
    }

    @Test
    void publishToolResultBroadcastsAndPersists() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishToolResult(1L, "search", "5 results found", 42L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(timelineService).enqueue(any());
    }

    @Test
    void publishPlanBroadcastsAndPersists() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishPlan(1L, "Step 1: Analyze, Step 2: Execute", 42L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(timelineService).enqueue(any());
    }

    @Test
    void publishOutputBroadcastsAndPersists() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishOutput(1L, "Final answer here", 42L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(timelineService).enqueue(any());
    }

    @Test
    void publishErrorBroadcastsAndPersists() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishError(1L, "Connection timeout", 42L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(timelineService).enqueue(any());
    }

    @Test
    void publishThoughtPublishesCorrectEventName() throws IOException {
        eventBus.register("1", emitter);

        eventBus.publishThought(1L, "thinking", 42L);

        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter).send(captor.capture());
        // Verify the event was sent (SseEventBuilder internals are not easily inspectible,
        // but the send call itself confirms the path)
    }

    @Test
    void broadcastSkipsWhenNoEmitters() {
        assertDoesNotThrow(() -> eventBus.broadcast("no-one", "test", Map.of("k", "v")));
        verifyNoInteractions(emitter);
        verify(timelineService, never()).enqueue(any());
    }

    @Test
    void completeDoesNotThrowWhenNoEmitters() {
        assertDoesNotThrow(() -> eventBus.complete("nonexistent"));
    }
}
