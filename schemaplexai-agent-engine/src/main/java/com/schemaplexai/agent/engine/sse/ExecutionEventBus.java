package com.schemaplexai.agent.engine.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class ExecutionEventBus {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEventBus.class);

    private final ObjectMapper objectMapper;

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(String executionId, SseEmitter emitter) {
        emitters.computeIfAbsent(executionId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> unregister(executionId, emitter));
        emitter.onTimeout(() -> unregister(executionId, emitter));
        emitter.onError(e -> unregister(executionId, emitter));
        log.debug("Registered SSE emitter for execution {}", executionId);
    }

    public void unregister(String executionId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(executionId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(executionId);
            }
        }
    }

    public void publishStateTransition(Long executionId, AgentExecutionState from, AgentExecutionState to) {
        String id = String.valueOf(executionId);
        Map<String, Object> payload = Map.of(
                "executionId", executionId,
                "fromState", from != null ? from.name() : null,
                "toState", to.name(),
                "timestamp", System.currentTimeMillis()
        );
        broadcast(id, "state-transition", payload);
    }

    public void publishExecutionCompleted(Long executionId, String finalState) {
        String id = String.valueOf(executionId);
        Map<String, Object> payload = Map.of(
                "executionId", executionId,
                "state", finalState,
                "timestamp", System.currentTimeMillis()
        );
        broadcast(id, "execution-completed", payload);
    }

    public void broadcast(String executionId, String eventName, Map<String, Object> payload) {
        List<SseEmitter> list = emitters.get(executionId);
        if (list == null || list.isEmpty()) {
            return;
        }

        String data;
        try {
            data = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE payload for execution {} event {}", executionId, eventName, e);
            return;
        }
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data);

        for (SseEmitter emitter : list) {
            try {
                emitter.send(event);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to execution {}, removing emitter", executionId);
                try {
                    emitter.completeWithError(e);
                } catch (Exception completeEx) {
                    log.debug("Error completing emitter with error for execution {}", executionId);
                }
                unregister(executionId, emitter);
            }
        }
    }

    public void complete(String executionId) {
        List<SseEmitter> list = emitters.remove(executionId);
        if (list != null) {
            for (SseEmitter emitter : list) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("Error completing emitter for execution {}", executionId);
                }
            }
        }
    }
}
