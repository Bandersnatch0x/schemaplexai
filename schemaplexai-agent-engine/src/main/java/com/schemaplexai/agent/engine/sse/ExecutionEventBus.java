package com.schemaplexai.agent.engine.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.timeline.AgentTimelineEvent;
import com.schemaplexai.agent.engine.timeline.TimelineClickHouseService;
import com.schemaplexai.agent.engine.timeline.TimelineEventType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class ExecutionEventBus {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEventBus.class);

    private final ObjectMapper objectMapper;
    private final TimelineClickHouseService timelineService;

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
        persistEvent(executionId, TimelineEventType.STATE_TRANSITION,
                "State: " + from + " → " + to, payload, null);
    }

    public void publishExecutionCompleted(Long executionId, String finalState) {
        String id = String.valueOf(executionId);
        Map<String, Object> payload = Map.of(
                "executionId", executionId,
                "state", finalState,
                "timestamp", System.currentTimeMillis()
        );
        broadcast(id, "execution-completed", payload);
        persistEvent(executionId, TimelineEventType.COMPLETED,
                "Execution completed with state: " + finalState, payload, null);
    }

    public void publishThought(Long executionId, String thought, Long tenantId) {
        Map<String, Object> payload = buildBasePayload(executionId, thought);
        broadcast(String.valueOf(executionId), "thought", payload);
        persistEvent(executionId, TimelineEventType.THOUGHT, thought, payload, tenantId);
    }

    public void publishToolCall(Long executionId, String toolName, String params, Long tenantId) {
        Map<String, Object> payload = buildBasePayload(executionId, toolName);
        payload.put("toolName", toolName);
        payload.put("parameters", params);
        broadcast(String.valueOf(executionId), "tool-call", payload);
        persistEvent(executionId, TimelineEventType.TOOL_CALL,
                "Tool: " + toolName, payload, tenantId);
    }

    public void publishToolResult(Long executionId, String toolName, String result, Long tenantId) {
        Map<String, Object> payload = buildBasePayload(executionId, result);
        payload.put("toolName", toolName);
        broadcast(String.valueOf(executionId), "tool-result", payload);
        persistEvent(executionId, TimelineEventType.TOOL_RESULT,
                "Result from " + toolName, payload, tenantId);
    }

    public void publishPlan(Long executionId, String planDescription, Long tenantId) {
        Map<String, Object> payload = buildBasePayload(executionId, planDescription);
        broadcast(String.valueOf(executionId), "plan", payload);
        persistEvent(executionId, TimelineEventType.PLAN, planDescription, payload, tenantId);
    }

    public void publishOutput(Long executionId, String output, Long tenantId) {
        Map<String, Object> payload = buildBasePayload(executionId, output);
        broadcast(String.valueOf(executionId), "output", payload);
        persistEvent(executionId, TimelineEventType.OUTPUT, output, payload, tenantId);
    }

    public void publishError(Long executionId, String error, Long tenantId) {
        Map<String, Object> payload = buildBasePayload(executionId, error);
        broadcast(String.valueOf(executionId), "error", payload);
        persistEvent(executionId, TimelineEventType.ERROR, error, payload, tenantId);
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

    private Map<String, Object> buildBasePayload(Long executionId, String content) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("executionId", executionId);
        payload.put("content", content);
        payload.put("timestamp", System.currentTimeMillis());
        return payload;
    }

    private void persistEvent(Long executionId, TimelineEventType type, String content,
                              Map<String, Object> payload, Long tenantId) {
        try {
            String metaJson = objectMapper.writeValueAsString(payload);
            AgentTimelineEvent event = AgentTimelineEvent.of(
                    executionId, type.value(), content, metaJson, tenantId);
            timelineService.enqueue(event);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize timeline payload for execution {}", executionId, e);
        }
    }
}
