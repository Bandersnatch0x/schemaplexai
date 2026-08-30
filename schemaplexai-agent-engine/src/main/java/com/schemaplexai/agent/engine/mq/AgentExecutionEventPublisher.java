package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutionEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void publishExecutionEvent(String eventType, Map<String, Object> payload) {
        // Defensive mutable copy: callers may pass immutable maps (e.g. Map.of),
        // and this method enriches the payload before publishing. Copying prevents
        // UnsupportedOperationException from degrading state-machine flows to FAILED.
        Map<String, Object> mutablePayload = payload != null ? new HashMap<>(payload) : new HashMap<>();
        mutablePayload.put("eventType", eventType);
        mutablePayload.put("timestamp", System.currentTimeMillis());
        String message = objectMapper.writeValueAsString(mutablePayload);
        rabbitTemplate.convertAndSend(CommonConstants.EXCHANGE_SCHEMAPLEXAI, CommonConstants.RK_AGENT_EXEC_EVENT, message);
        log.info("Published execution event: {}", eventType);
    }

    @SneakyThrows
    public void publishShadowConfigEvent(Long agentId, String shadowConfigJson) {
        Map<String, Object> payload = Map.of(
                "agentId", agentId,
                "shadowConfig", shadowConfigJson
        );
        String message = objectMapper.writeValueAsString(payload);
        rabbitTemplate.convertAndSend(CommonConstants.EXCHANGE_SCHEMAPLEXAI, CommonConstants.RK_AGENT_CONFIG_SHADOW, message);
        log.info("Published shadow config event for agent {}", agentId);
    }
}
