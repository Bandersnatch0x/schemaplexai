package com.schemaplexai.agent.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.util.SecretMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Single write path for ExecutionEvent payloads.
 * <p>
 * All payloads are masked via {@link SecretMasker} before being written to:
 * <ul>
 *   <li>PostgreSQL (sf_execution_event table)</li>
 *   <li>Outbox (sf_execution_outbox table)</li>
 * </ul>
 * <p>
 * Delegates MQ publishing to {@link OutboxPublisher}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionEventService {

    private final ExecutionEventMapper executionEventMapper;
    private final ExecutionOutboxMapper executionOutboxMapper;
    private final SecretMasker secretMasker;
    private final ObjectMapper objectMapper;

    /**
     * Masks the event payload in-place.
     * Applies both JSON secret masking and PII masking.
     * Idempotent: already-masked payloads are not double-masked.
     */
    void maskEventPayload(ExecutionEvent event) {
        if (event == null || event.getPayload() == null) {
            return;
        }
        String payload = event.getPayload();

        // Skip if already masked to avoid double-masking
        if (payload.contains("***MASKED***") || payload.contains("***PII***")) {
            log.debug("Payload already masked, skipping double-masking for event {}", event.getEventId());
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            JsonNode masked = secretMasker.maskJson(jsonNode);
            payload = objectMapper.writeValueAsString(masked);
        } catch (JsonProcessingException e) {
            log.debug("Payload is not valid JSON, falling back to PII-only masking for event {}",
                    event.getEventId());
        }

        // Always apply PII masking on the string representation
        payload = secretMasker.maskPii(payload);
        event.setPayload(payload);
    }

    /**
     * Writes an ExecutionEvent to PostgreSQL after masking its payload.
     */
    @Transactional
    public void writeEvent(ExecutionEvent event) {
        maskEventPayload(event);
        executionEventMapper.insert(event);
        log.debug("Wrote event {} (seq={}) to sf_execution_event", event.getEventId(), event.getSeq());
    }

    /**
     * Writes an ExecutionEvent to the Outbox after masking its payload.
     */
    @Transactional
    public void publishToOutbox(ExecutionEvent event, String topic) {
        maskEventPayload(event);

        ExecutionOutbox outbox = new ExecutionOutbox();
        outbox.setEventId(event.getEventId());
        outbox.setExecutionId(event.getExecutionId());
        outbox.setSeq(event.getSeq());
        outbox.setTopic(topic);
        outbox.setPayload(event.getPayload());
        outbox.setCreatedAt(Instant.now());
        outbox.setRetryCount(0);

        executionOutboxMapper.insert(outbox);
        log.debug("Published event {} (seq={}) to outbox with topic '{}'",
                event.getEventId(), event.getSeq(), topic);
    }

    /**
     * Atomically writes ExecutionEvent and ExecutionOutbox in the same transaction.
     * The OutboxPublisher background service will handle MQ delivery.
     * <p>
     * Both payloads are masked before persistence.
     */
    @Transactional
    public void appendEventAndOutbox(ExecutionEvent event, String topic) {
        writeEvent(event);
        publishToOutbox(event, topic);
    }
}
