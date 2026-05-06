package com.schemaplexai.agent.engine.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
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

    @BeforeEach
    void setUp() {
        eventBus = new ExecutionEventBus(new ObjectMapper());
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
}
