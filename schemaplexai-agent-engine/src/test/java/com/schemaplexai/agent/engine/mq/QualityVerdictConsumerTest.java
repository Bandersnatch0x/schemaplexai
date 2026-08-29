package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.PauseReason;
import com.schemaplexai.agent.engine.mapper.SfAgentExecutionMapper;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ticket 924 / REQ-02 / REQ-18: disposition semantics applied to verdicts.
 * BLOCK sets the execution to GATE_BLOCKED (existing state + payload reused);
 * FAIL terminates; PASS/WARN leave the completed execution untouched.
 */
@ExtendWith(MockitoExtension.class)
class QualityVerdictConsumerTest {

    @Mock
    private SfAgentExecutionMapper executionMapper;
    @Mock
    private AgentExecutionEventPublisher eventPublisher;
    @Mock
    private ExecutionEventBus eventBus;
    @Mock
    private ExecutionEventService executionEventService;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private QualityVerdictConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new QualityVerdictConsumer(executionMapper, eventPublisher, eventBus,
                executionEventService, redisTemplate, objectMapper);
    }

    private SfAgentExecution execution(Long id, String state) {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(id);
        execution.setAgentId(42L);
        execution.setTenantId("3");
        execution.setState(state);
        return execution;
    }

    private JsonNode verdict(String disposition) throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("executionId", 100L);
        node.put("disposition", disposition);
        node.put("triggerPoint", "POST_EXECUTION");
        node.put("issueCount", 1);
        node.putArray("failedRules").add("SECURITY_SCAN");
        return node;
    }

    @Test
    void blockVerdictSetsGateBlockedAndPublishesEstablishedPayload() throws Exception {
        when(executionMapper.selectById(100L)).thenReturn(execution(100L, AgentExecutionState.COMPLETED.name()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        consumer.consume(100L, "BLOCK", verdict("BLOCK"));

        ArgumentCaptor<SfAgentExecution> updated = ArgumentCaptor.forClass(SfAgentExecution.class);
        verify(executionMapper, org.mockito.Mockito.atLeastOnce()).updateById(updated.capture());
        assertEquals(AgentExecutionState.GATE_BLOCKED.name(),
                updated.getAllValues().get(0).getState());

        // Reused pause bookkeeping (PauseReason.QUALITY_GATE_BLOCKED, previously dead)
        verify(valueOperations).set(contains("paused"), eq(PauseReason.QUALITY_GATE_BLOCKED.name()),
                eq(Duration.ofHours(24)));

        // Reused AGENT_GATE_BLOCKED payload shape
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publishExecutionEvent(eq("AGENT_GATE_BLOCKED"), payload.capture());
        Map<String, Object> sent = payload.getValue();
        assertEquals(100L, sent.get("executionId"));
        assertEquals("quality_gate_blocked", sent.get("reason"));
        assertEquals(false, sent.get("retryable"));
        assertEquals("BLOCK", sent.get("disposition"));

        // Audit event + SSE broadcast
        ArgumentCaptor<ExecutionEvent> audit = ArgumentCaptor.forClass(ExecutionEvent.class);
        verify(executionEventService).writeEvent(audit.capture());
        assertEquals("QUALITY_GATE_BLOCKED", audit.getValue().getEventType());
        verify(eventBus).publishStateTransition(100L,
                AgentExecutionState.COMPLETED, AgentExecutionState.GATE_BLOCKED);
    }

    @Test
    void failVerdictTerminatesExecution() throws Exception {
        when(executionMapper.selectById(101L)).thenReturn(execution(101L, AgentExecutionState.COMPLETED.name()));

        consumer.consume(101L, "FAIL", verdict("FAIL"));

        ArgumentCaptor<SfAgentExecution> updated = ArgumentCaptor.forClass(SfAgentExecution.class);
        verify(executionMapper, org.mockito.Mockito.atLeastOnce()).updateById(updated.capture());
        assertEquals(AgentExecutionState.FAILED.name(), updated.getAllValues().get(0).getState());
        verify(eventBus).publishStateTransition(101L,
                AgentExecutionState.COMPLETED, AgentExecutionState.FAILED);
        // FAIL terminates — no GATE_BLOCKED pause machinery involved
        verify(eventPublisher, never()).publishExecutionEvent(anyString(), any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void passVerdictLeavesExecutionUntouched() throws Exception {
        consumer.consume(102L, "PASS", verdict("PASS"));

        verify(executionMapper, never()).selectById(anyLong());
        verify(executionMapper, never()).updateById(any());
        verify(eventPublisher, never()).publishExecutionEvent(anyString(), any());
    }

    @Test
    void warnVerdictLeavesExecutionUntouched() throws Exception {
        consumer.consume(103L, "WARN", verdict("WARN"));

        verify(executionMapper, never()).selectById(anyLong());
        verify(executionMapper, never()).updateById(any());
        verify(eventPublisher, never()).publishExecutionEvent(anyString(), any());
    }

    @Test
    void blockVerdictForMissingExecutionIsDropped() throws Exception {
        when(executionMapper.selectById(404L)).thenReturn(null);

        consumer.consume(404L, "BLOCK", verdict("BLOCK"));

        verify(executionMapper, never()).updateById(any());
        verify(eventPublisher, never()).publishExecutionEvent(anyString(), any());
    }

    @Test
    void blockVerdictIsIdempotentWhenAlreadyGateBlocked() throws Exception {
        when(executionMapper.selectById(104L)).thenReturn(execution(104L, AgentExecutionState.GATE_BLOCKED.name()));

        consumer.consume(104L, "BLOCK", verdict("BLOCK"));

        verify(executionMapper, never()).updateById(any());
        verify(eventPublisher, never()).publishExecutionEvent(anyString(), any());
    }

    @Test
    void unknownDispositionIsIgnored() throws Exception {
        consumer.consume(105L, "EXPLODE", verdict("EXPLODE"));

        verify(executionMapper, never()).selectById(anyLong());
        verify(executionMapper, never()).updateById(any());
    }

    @Test
    void onMessageDropsMalformedPayloadWithoutRequeue() throws Exception {
        com.rabbitmq.client.Channel channel = mock(com.rabbitmq.client.Channel.class);
        org.springframework.amqp.core.Message message = new org.springframework.amqp.core.Message(
                "{\"disposition\":\"BLOCK\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        consumer.onMessage(message, channel);

        // executionId missing -> ack-and-drop, no state change
        verify(channel).basicAck(anyLong(), eq(false));
        verify(executionMapper, never()).updateById(any());
    }
}
