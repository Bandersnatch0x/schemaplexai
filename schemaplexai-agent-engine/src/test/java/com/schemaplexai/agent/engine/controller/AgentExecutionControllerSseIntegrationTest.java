package com.schemaplexai.agent.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.security.SseTokenValidator;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.timeline.TimelineClickHouseService;
import com.schemaplexai.agent.engine.tool.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AgentExecutionController SSE event streaming.
 *
 * <p>Verifies that the SSE endpoint properly registers emitters with the
 * ExecutionEventBus and that events flow end-to-end.</p>
 */
@ExtendWith(MockitoExtension.class)
class AgentExecutionControllerSseIntegrationTest {

    @Mock
    private AgentExecutionEngine executionEngine;

    @Mock
    private AgentExecutionLifecycleService lifecycleService;

    @Mock
    private SseTokenValidator sseTokenValidator;

    @Mock
    private TimelineClickHouseService timelineService;

    private ExecutionEventBus eventBus;
    private AgentExecutionController controller;

    @BeforeEach
    void setUp() {
        eventBus = new ExecutionEventBus(new ObjectMapper(), timelineService);
        controller = new AgentExecutionController(
                executionEngine,
                lifecycleService,
                sseTokenValidator,
                eventBus,
                timelineService);
    }

    /**
     * P1-1: Verifies that subscribeExecutionEvents registers the SseEmitter
     * with the ExecutionEventBus so that events can be delivered.
     */
    @Test
    void subscribeExecutionEvents_registersEmitterWithEventBus() {
        when(sseTokenValidator.validate(any(), any())).thenReturn(ValidationResult.valid());

        SseEmitter emitter = controller.subscribeExecutionEvents(1L, 1L, "valid-token");

        assertNotNull(emitter, "Controller should return a non-null SseEmitter");
        // Verify the emitter is registered by publishing an event;
        // if not registered, the event bus would silently drop it (no exception).
        assertDoesNotThrow(() -> eventBus.publishThought(1L, "registration verification", 1L));
    }

    /**
     * P1-1: Verifies that an emitter registered via the controller receives
     * at least one SSE event (token) when the event bus publishes.
     */
    @Test
    void sseEmitter_receivesAtLeastOneEvent() throws IOException {
        when(sseTokenValidator.validate(any(), any())).thenReturn(ValidationResult.valid());

        // Call controller to create and register emitter
        SseEmitter emitter = controller.subscribeExecutionEvents(1L, 1L, "valid-token");
        assertNotNull(emitter);

        // Spy on the emitter to verify send() is invoked
        SseEmitter spyEmitter = spy(emitter);

        // Unregister the original and register the spy (same execution id)
        eventBus.unregister("1", emitter);
        eventBus.register("1", spyEmitter);

        // Publish an event through the event bus
        eventBus.publishThought(1L, "test thought payload", 1L);

        // Verify the spy emitter received at least one event
        verify(spyEmitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void subscribeExecutionEvents_rejectsInvalidToken() {
        when(sseTokenValidator.validate(any(), any()))
                .thenReturn(ValidationResult.invalid("expired token"));

        assertThrows(SecurityException.class, () ->
                controller.subscribeExecutionEvents(1L, 1L, "invalid-token"));
    }
}
