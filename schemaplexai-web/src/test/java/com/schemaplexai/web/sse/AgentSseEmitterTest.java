package com.schemaplexai.web.sse;

import com.schemaplexai.common.message.MessageType;
import com.schemaplexai.common.message.UnifiedMessage;
import com.schemaplexai.web.security.JwtValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentSseEmitterTest {

    private JwtValidator jwtValidator = mock(JwtValidator.class);

    @Test
    void shouldSendUnifiedMessageViaSseEmitter() throws IOException {
        when(jwtValidator.validateToken("Bearer valid-token")).thenReturn(true);
        AgentSseEmitter emitterManager = new AgentSseEmitter(jwtValidator);
        SseEmitter emitter = emitterManager.createEmitter("client-1", "Bearer valid-token");
        assertThat(emitter).isNotNull();

        UnifiedMessage message = UnifiedMessage.builder()
            .type(MessageType.AGENT_RESPONSE)
            .source("agent-engine")
            .target("client-1")
            .eventName("agent-output")
            .payload("{\"output\":\"Hello, world!\"}")
            .timestamp(System.currentTimeMillis())
            .traceId("trace-abc-123")
            .build();

        // sendUnified should not throw and should complete successfully
        emitterManager.sendUnified("client-1", message);
    }

    @Test
    void shouldSendSseEventTypeMessage() throws IOException {
        when(jwtValidator.validateToken("Bearer valid-token")).thenReturn(true);
        AgentSseEmitter emitterManager = new AgentSseEmitter(jwtValidator);
        SseEmitter emitter = emitterManager.createEmitter("client-2", "Bearer valid-token");
        assertThat(emitter).isNotNull();

        UnifiedMessage message = UnifiedMessage.builder()
            .type(MessageType.SSE_EVENT)
            .source("web")
            .target("client-2")
            .eventName("agent-step")
            .payload("{\"step\":\"thinking\"}")
            .timestamp(System.currentTimeMillis())
            .traceId("trace-def-456")
            .build();

        emitterManager.sendUnified("client-2", message);
    }

    @Test
    void shouldSendErrorTypeMessage() throws IOException {
        when(jwtValidator.validateToken("Bearer valid-token")).thenReturn(true);
        AgentSseEmitter emitterManager = new AgentSseEmitter(jwtValidator);
        SseEmitter emitter = emitterManager.createEmitter("client-3", "Bearer valid-token");
        assertThat(emitter).isNotNull();

        UnifiedMessage message = UnifiedMessage.builder()
            .type(MessageType.ERROR)
            .source("system")
            .target("client-3")
            .payload("{\"error\":\"Connection timeout\"}")
            .timestamp(System.currentTimeMillis())
            .build();

        emitterManager.sendUnified("client-3", message);
    }

    @Test
    void shouldIgnoreSendUnifiedWhenClientNotFound() {
        AgentSseEmitter emitterManager = new AgentSseEmitter(jwtValidator);

        UnifiedMessage message = UnifiedMessage.builder()
            .type(MessageType.SYSTEM)
            .source("gateway")
            .target("unknown-client")
            .payload("{\"notice\":\"Maintenance\"}")
            .timestamp(System.currentTimeMillis())
            .build();

        // Should not throw even if client does not exist
        emitterManager.sendUnified("nonexistent-client", message);
    }
}
