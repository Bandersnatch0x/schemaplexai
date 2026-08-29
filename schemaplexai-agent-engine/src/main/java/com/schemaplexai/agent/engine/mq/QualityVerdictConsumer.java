package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.PauseReason;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.redis.TenantRedisKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumes quality gate verdicts published by the quality module on
 * {@code sf.exchange} / {@code sf.quality.verdict} and applies the
 * disposition semantics of spec docs/specs/2026-04-30-v1.0-quality-gate.md §1
 * (ticket 924 / REQ-02 / REQ-18):
 *
 * <ul>
 *   <li>PASS — continue (no action; execution already COMPLETED).</li>
 *   <li>WARN — alert recorded on the quality side (sf_quality_issue),
 *       execution continues.</li>
 *   <li>BLOCK — execution set to GATE_BLOCKED (existing state reused), paused
 *       key written with PauseReason.QUALITY_GATE_BLOCKED, AGENT_GATE_BLOCKED
 *       event published with the established payload shape, awaiting manual
 *       confirmation.</li>
 *   <li>FAIL — execution terminated: state set to FAILED.</li>
 * </ul>
 *
 * <p>The state is applied by direct persistence update rather than
 * {@code AgentStateMachine.transition}: verdicts arrive after the execution
 * reached a terminal state (the wired trigger point is post-execution), and
 * the state machine guards terminal states. The existing GATE_BLOCKED event
 * payload (issue 904) is reused for downstream notification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityVerdictConsumer {

    static final String QUEUE_NAME = "sf.quality.verdict.queue";
    /** Must match QualityVerdictPublisher.RK_QUALITY_VERDICT in schemaplexai-quality. */
    static final String RK_QUALITY_VERDICT = "sf.quality.verdict";

    private static final String DISPOSITION_PASS = "PASS";
    private static final String DISPOSITION_WARN = "WARN";
    private static final String DISPOSITION_BLOCK = "BLOCK";
    private static final String DISPOSITION_FAIL = "FAIL";

    private final SfAgentExecutionMapper executionMapper;
    private final AgentExecutionEventPublisher eventPublisher;
    private final ExecutionEventBus eventBus;
    private final ExecutionEventService executionEventService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = QUEUE_NAME, durable = "true"),
                    exchange = @Exchange(value = CommonConstants.EXCHANGE_SCHEMAPLEXAI, type = "direct"),
                    key = RK_QUALITY_VERDICT
            )
    )
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            JsonNode verdict = objectMapper.readTree(body);
            Long executionId = verdict.hasNonNull("executionId") ? verdict.get("executionId").asLong() : null;
            String disposition = verdict.hasNonNull("disposition") ? verdict.get("disposition").asText() : null;
            if (executionId == null || disposition == null) {
                log.error("[QualityVerdictConsumer] verdict missing executionId/disposition, dropping: {}", body);
                channel.basicAck(deliveryTag, false);
                return;
            }
            consume(executionId, disposition, verdict);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[QualityVerdictConsumer] Failed to process verdict: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Applies one verdict. Public for unit testing.
     */
    public void consume(Long executionId, String disposition, JsonNode verdict) {
        switch (disposition.toUpperCase()) {
            case DISPOSITION_PASS -> log.info("Quality verdict PASS for execution {} — no action", executionId);
            case DISPOSITION_WARN -> log.warn("Quality verdict WARN for execution {} — issues recorded by "
                    + "quality service, execution continues", executionId);
            case DISPOSITION_BLOCK -> applyGateBlocked(executionId, verdict);
            case DISPOSITION_FAIL -> applyFailed(executionId, verdict);
            default -> log.error("Quality verdict with unknown disposition '{}' for execution {} — ignored",
                    disposition, executionId);
        }
    }

    private void applyGateBlocked(Long executionId, JsonNode verdict) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("[QualityVerdictConsumer] Execution {} not found for BLOCK verdict, dropping", executionId);
            return;
        }
        String previousState = execution.getState();
        if (AgentExecutionState.GATE_BLOCKED.name().equals(previousState)
                || AgentExecutionState.FAILED.name().equals(previousState)
                || AgentExecutionState.CANCELLED.name().equals(previousState)) {
            log.info("[QualityVerdictConsumer] Execution {} already in {}, BLOCK verdict is a no-op",
                    executionId, previousState);
            return;
        }
        if (!AgentExecutionState.COMPLETED.name().equals(previousState)) {
            // Only the post-execution trigger point is wired today (ticket 924);
            // mid-flight verdicts belong to the phased POST_TOOL/WORKFLOW_NODE points.
            log.warn("[QualityVerdictConsumer] BLOCK verdict for execution {} in non-terminal state {} — "
                    + "applying block via direct update", executionId, previousState);
        }

        execution.setState(AgentExecutionState.GATE_BLOCKED.name());
        execution.setMetadata("blockedReason", "quality_gate_blocked");
        executionMapper.updateById(execution);

        // Reuse the pause bookkeeping: BLOCK = pause awaiting manual confirmation.
        try {
            String pausedKey = TenantRedisKeyResolver.executionPaused(
                    execution.getTenantId(), String.valueOf(executionId));
            redisTemplate.opsForValue().set(pausedKey,
                    PauseReason.QUALITY_GATE_BLOCKED.name(), Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("[QualityVerdictConsumer] Failed to write pause key for execution {}: {}",
                    executionId, e.getMessage());
        }

        // Reuse the established AGENT_GATE_BLOCKED payload shape (issue 904).
        Map<String, Object> payload = new HashMap<>();
        payload.put("executionId", executionId);
        payload.put("agentId", execution.getAgentId());
        payload.put("reason", "quality_gate_blocked");
        payload.put("retryable", false);
        payload.put("disposition", DISPOSITION_BLOCK);
        payload.put("failedRules", asStringList(verdict.path("failedRules")));
        payload.put("issueCount", verdict.path("issueCount").asInt(0));
        payload.put("triggerPoint", verdict.path("triggerPoint").asText(null));
        try {
            eventPublisher.publishExecutionEvent("AGENT_GATE_BLOCKED", payload);
        } catch (Exception e) {
            log.warn("[QualityVerdictConsumer] Failed to publish AGENT_GATE_BLOCKED event for execution {}: {}",
                    executionId, e.getMessage());
        }

        emitAuditEvent(execution, "QUALITY_GATE_BLOCKED", verdict);
        eventBus.publishStateTransition(executionId,
                parseState(previousState), AgentExecutionState.GATE_BLOCKED);
        log.warn("[QualityVerdictConsumer] Execution {} blocked by quality gate ({} → GATE_BLOCKED)",
                executionId, previousState);
    }

    private void applyFailed(Long executionId, JsonNode verdict) {
        SfAgentExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            log.warn("[QualityVerdictConsumer] Execution {} not found for FAIL verdict, dropping", executionId);
            return;
        }
        String previousState = execution.getState();
        if (AgentExecutionState.FAILED.name().equals(previousState)
                || AgentExecutionState.CANCELLED.name().equals(previousState)) {
            log.info("[QualityVerdictConsumer] Execution {} already in {}, FAIL verdict is a no-op",
                    executionId, previousState);
            return;
        }

        // FAIL = terminate execution, mark failed (spec §1).
        execution.setState(AgentExecutionState.FAILED.name());
        executionMapper.updateById(execution);

        emitAuditEvent(execution, "QUALITY_GATE_FAILED", verdict);
        eventBus.publishStateTransition(executionId, parseState(previousState), AgentExecutionState.FAILED);
        log.error("[QualityVerdictConsumer] Execution {} terminated by quality gate FAIL verdict ({} → FAILED)",
                executionId, previousState);
    }

    private void emitAuditEvent(SfAgentExecution execution, String eventType, JsonNode verdict) {
        try {
            ExecutionEvent event = new ExecutionEvent();
            event.setEventId(UUID.randomUUID());
            event.setExecutionId(execution.getId());
            event.setEventType(eventType);
            event.setTenantId(execution.getTenantId() != null ? Long.valueOf(execution.getTenantId()) : null);
            event.setAgentId(execution.getAgentId());
            event.setOccurredAt(Instant.now());
            event.setSensitivity("AUDIT");
            event.setPayload(verdict != null ? verdict.toString() : "{}");

            int nextSeq = (execution.getLastEventSeq() != null ? execution.getLastEventSeq() : 0) + 1;
            event.setSeq(nextSeq);

            executionEventService.writeEvent(event);
            execution.setLastEventSeq(nextSeq);
            executionMapper.updateById(execution);
        } catch (Exception e) {
            log.error("[QualityVerdictConsumer] Failed to emit {} audit event for execution {}",
                    eventType, execution.getId(), e);
        }
    }

    private AgentExecutionState parseState(String state) {
        if (state == null) {
            return null;
        }
        try {
            return AgentExecutionState.valueOf(state);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> asStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }
}
