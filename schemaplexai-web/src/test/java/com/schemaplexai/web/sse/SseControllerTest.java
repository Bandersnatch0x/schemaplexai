package com.schemaplexai.web.sse;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.controller.SseController;
import com.schemaplexai.web.dto.SseEvent;
import com.schemaplexai.web.service.SseReplayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    @Mock
    private AgentSseEmitter agentSseEmitter;

    @Mock
    private SseReplayService sseReplayService;

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

    @Test
    void subscribeExecutionEvents_replaysThenReturnsEmitter() {
        SseEmitter emitter = new SseEmitter();
        SseEvent event1 = new SseEvent(1L, 3, "STATUS", "{}", "AUDIT");
        SseEvent event2 = new SseEvent(1L, 4, "STATUS", "{}", "DEBUG");

        when(agentSseEmitter.subscribeExecution(anyString(), eq(1L), eq("token"))).thenReturn(emitter);
        when(sseReplayService.replayEvents(1L, 2)).thenReturn(List.of(event1, event2));

        SseEmitter result = sseController.subscribeExecutionEvents(1L, 2, "token");

        assertThat(result).isEqualTo(emitter);
        verify(agentSseEmitter).sendEvent(anyString(), eq("STATUS"), eq(event1));
        verify(agentSseEmitter).sendEvent(anyString(), eq("STATUS"), eq(event2));
    }

    @Test
    void subscribeExecutionEvents_withDefaultLastSeq_replaysFromZero() {
        SseEmitter emitter = new SseEmitter();
        when(agentSseEmitter.subscribeExecution(anyString(), eq(2L), isNull())).thenReturn(emitter);
        when(sseReplayService.replayEvents(2L, 0)).thenReturn(List.of());

        SseEmitter result = sseController.subscribeExecutionEvents(2L, 0, null);

        assertThat(result).isEqualTo(emitter);
        verify(sseReplayService).replayEvents(2L, 0);
    }
}
