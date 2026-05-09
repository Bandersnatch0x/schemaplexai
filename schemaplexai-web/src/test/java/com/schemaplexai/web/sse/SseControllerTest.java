package com.schemaplexai.web.sse;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.controller.SseController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    @Mock
    private AgentSseEmitter agentSseEmitter;

    @InjectMocks
    private SseController sseController;

    @Test
    void subscribe_returnsEmitter() {
        SseEmitter emitter = new SseEmitter();
        when(agentSseEmitter.createEmitter("client1", "token")).thenReturn(emitter);

        SseEmitter result = sseController.subscribe("client1", "token");

        assertThat(result).isEqualTo(emitter);
    }

    @Test
    void sendEvent_returnsSuccess() {
        Result<Void> result = sseController.sendEvent("client1", "event", "data");

        assertThat(result.getCode()).isEqualTo(200);
        verify(agentSseEmitter).sendEvent("client1", "event", "data");
    }
}
