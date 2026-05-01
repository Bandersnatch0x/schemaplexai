package com.schemaplexai.web.sse;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.message.UnifiedMessage;
import com.schemaplexai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AgentSseEmitter {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String clientId, String token) {
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new BaseException(ResultCode.UNAUTHORIZED);
        }
        // Full JWT signature and expiry validation is performed by the Gateway layer (JwtAuthFilter).
        // The downstream web service receives pre-validated tokens via X-User-Id / X-Tenant-Id headers.
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout: {}", clientId);
            emitters.remove(clientId);
        });
        emitter.onError(e -> {
            log.error("SSE emitter error: {}", clientId, e);
            emitters.remove(clientId);
        });

        return emitter;
    }

    public void sendEvent(String clientId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.error("Failed to send SSE event to {}", clientId, e);
            emitters.remove(clientId);
        }
    }

    public void complete(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            emitter.complete();
        }
    }

    public void completeWithError(String clientId, Throwable ex) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            log.error("SSE emitter error for client: {}", clientId, ex);
            emitter.completeWithError(new RuntimeException("SSE connection error"));
        }
    }

    public void sendUnified(String clientId, UnifiedMessage message) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            return;
        }
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data(message);
            if (message.getEventName() != null) {
                event.name(message.getEventName());
            }
            emitter.send(event);
        } catch (IOException e) {
            log.error("Failed to send unified message to {}", clientId, e);
            emitters.remove(clientId);
        }
    }
}
