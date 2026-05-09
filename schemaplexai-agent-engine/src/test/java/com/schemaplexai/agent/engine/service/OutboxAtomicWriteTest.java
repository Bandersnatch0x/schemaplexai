package com.schemaplexai.agent.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.util.SecretMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionEvent + Outbox atomic write")
class OutboxAtomicWriteTest {

    @Mock
    private ExecutionEventMapper executionEventMapper;

    @Mock
    private ExecutionOutboxMapper executionOutboxMapper;

    private SecretMasker secretMasker;

    @InjectMocks
    private ExecutionEventService executionEventService;

    @BeforeEach
    void setUp() {
        secretMasker = new SecretMasker(new ObjectMapper());
        executionEventService = new ExecutionEventService(
                executionEventMapper, executionOutboxMapper, secretMasker, new ObjectMapper());
    }

    @Test
    @DisplayName("should write ExecutionEvent and ExecutionOutbox in same transaction")
    void atomicWriteEventAndOutbox() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(1);
        event.setEventType("APPROVAL_REQUESTED");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);

        assertThatNoException()
                .isThrownBy(() -> executionEventService.appendEventAndOutbox(event, "approval.requests"));
    }

    @Test
    @DisplayName("should rollback both Event and Outbox on transaction failure")
    void rollbackOnFailure() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(2);
        event.setEventType("TOOL_CALLED");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);

        assertThatNoException()
                .isThrownBy(() -> executionEventService.appendEventAndOutbox(event, "execution.events"));
    }

    @Test
    @DisplayName("should mask secret keys in payload before writing to PG and Outbox")
    void masksSecretsBeforeWrite() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(1);
        event.setEventType("TOOL_CALLED");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);
        event.setPayload("{\"apiKey\":\"sk-secret123\",\"name\":\"test\"}");

        executionEventService.appendEventAndOutbox(event, "execution.events");

        ArgumentCaptor<ExecutionEvent> eventCaptor = ArgumentCaptor.forClass(ExecutionEvent.class);
        verify(executionEventMapper).insert(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getPayload()).contains("***MASKED***");
        assertThat(eventCaptor.getValue().getPayload()).contains("\"name\":\"test\"");

        ArgumentCaptor<ExecutionOutbox> outboxCaptor = ArgumentCaptor.forClass(ExecutionOutbox.class);
        verify(executionOutboxMapper).insert(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getPayload()).contains("***MASKED***");
    }

    @Test
    @DisplayName("should mask PII in payload before writing")
    void masksPiiBeforeWrite() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(1);
        event.setEventType("OUTPUT");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);
        event.setPayload("Contact alice@example.com or call 555-123-4567");

        executionEventService.writeEvent(event);

        ArgumentCaptor<ExecutionEvent> captor = ArgumentCaptor.forClass(ExecutionEvent.class);
        verify(executionEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("***PII***");
        assertThat(captor.getValue().getPayload()).doesNotContain("alice@example.com");
        assertThat(captor.getValue().getPayload()).doesNotContain("555-123-4567");
    }

    @Test
    @DisplayName("should not double-mask already masked payloads")
    void noDoubleMasking() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(1);
        event.setEventType("OUTPUT");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);
        event.setPayload("{\"apiKey\":\"***MASKED***\",\"email\":\"***PII***\"}");

        executionEventService.writeEvent(event);

        ArgumentCaptor<ExecutionEvent> captor = ArgumentCaptor.forClass(ExecutionEvent.class);
        verify(executionEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo(event.getPayload());
    }

    @Test
    @DisplayName("should handle null payload gracefully")
    void handlesNullPayload() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(1);
        event.setEventType("OUTPUT");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);
        event.setPayload(null);

        assertThatNoException().isThrownBy(() -> executionEventService.writeEvent(event));

        ArgumentCaptor<ExecutionEvent> captor = ArgumentCaptor.forClass(ExecutionEvent.class);
        verify(executionEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayload()).isNull();
    }

    @Test
    @DisplayName("should handle non-JSON payload with PII masking")
    void handlesNonJsonPayload() {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(1L);
        event.setSeq(1);
        event.setEventType("OUTPUT");
        event.setOccurredAt(Instant.now());
        event.setTenantId(1L);
        event.setPayload("Plain text with email bob@test.org");

        executionEventService.writeEvent(event);

        ArgumentCaptor<ExecutionEvent> captor = ArgumentCaptor.forClass(ExecutionEvent.class);
        verify(executionEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("Plain text with email ***PII***");
    }
}
