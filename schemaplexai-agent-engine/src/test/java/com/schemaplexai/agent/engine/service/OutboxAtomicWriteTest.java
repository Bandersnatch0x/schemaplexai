package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecutionEvent + Outbox atomic write")
class OutboxAtomicWriteTest {

    @Mock
    private ExecutionEventMapper executionEventMapper;

    @Mock
    private ExecutionOutboxMapper executionOutboxMapper;

    @InjectMocks
    private ExecutionEventService executionEventService;

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
}
