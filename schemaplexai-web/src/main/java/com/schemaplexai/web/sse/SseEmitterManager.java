package com.schemaplexai.web.sse;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<String, CopyOnWriteArraySet<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String tenantId, String clientId, long timeout, String token) {
        if (!tokenValid(token)) {
            throw new BaseException(ResultCode.UNAUTHORIZED);
        }
        String key = tenantId + ":" + clientId;
        SseEmitter emitter = new SseEmitter(timeout);

        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArraySet<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError(e -> removeEmitter(tenantId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            log.error("SSE send connect event failed", e);
        }

        return emitter;
    }

    public void sendToTenant(String tenantId, String eventName, Object data) {
        CopyOnWriteArraySet<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.warn("SSE send failed, removing emitter");
                removeEmitter(tenantId, emitter);
            }
        }
    }

    public void broadcast(String eventName, Object data) {
        tenantEmitters.forEach((tenantId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (IOException e) {
                    log.warn("SSE broadcast failed for tenant {}", tenantId);
                    removeEmitter(tenantId, emitter);
                }
            }
        });
    }

    private void removeEmitter(String tenantId, SseEmitter emitter) {
        CopyOnWriteArraySet<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                tenantEmitters.remove(tenantId);
            }
        }
    }

    private boolean tokenValid(String token) {
        return StringUtils.hasText(token);
    }
}
