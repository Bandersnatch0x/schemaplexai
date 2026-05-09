package com.schemaplexai.web.sse;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.message.UnifiedMessage;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.web.security.JwtValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentSseEmitter {

    private final JwtValidator jwtValidator;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // executionId -> set of clientIds watching this execution
    private final Map<Long, Map<String, SseEmitter>> executionEmitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String clientId, String token) {
        if (!StringUtils.hasText(token) || !jwtValidator.validateToken(token)) {
            throw new BaseException(ResultCode.UNAUTHORIZED);
        }
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> removeEmitter(clientId));
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timeout: {}", clientId);
            removeEmitter(clientId);
        });
        emitter.onError(e -> {
            log.error("SSE emitter error: {}", clientId, e);
            removeEmitter(clientId);
        });

        return emitter;
    }

    /**
     * Subscribe a client to execution-specific events.
     */
    public SseEmitter subscribeExecution(String clientId, Long executionId, String token) {
        SseEmitter emitter = createEmitter(clientId, token);
        executionEmitters.computeIfAbsent(executionId, k -> new ConcurrentHashMap<>()).put(clientId, emitter);
        log.debug("Client {} subscribed to execution {}", clientId, executionId);
        return emitter;
    }

    /**
     * Broadcast an event to all clients watching a specific execution.
     */
    public void broadcastToExecution(Long executionId, String eventName, Object data) {
        Map<String, SseEmitter> watchers = executionEmitters.get(executionId);
        if (watchers == null || watchers.isEmpty()) {
            return;
        }
        watchers.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE to client {} for execution {}, removing", clientId, executionId);
                watchers.remove(clientId);
                emitters.remove(clientId);
            }
        });
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
        // Also remove from execution watchers
        executionEmitters.values().forEach(m -> m.remove(clientId));
    }

    public void completeWithError(String clientId, Throwable ex) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            log.error("SSE emitter error for client: {}", clientId, ex);
            emitter.completeWithError(new RuntimeException("SSE connection error"));
        }
        executionEmitters.values().forEach(m -> m.remove(clientId));
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

    private void removeEmitter(String clientId) {
        emitters.remove(clientId);
        executionEmitters.values().forEach(m -> m.remove(clientId));
    }
}
