package com.schemaplexai.quality.service;

import com.schemaplexai.model.event.ApprovalRequestEvent;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Approval request MQ consumer idempotency")
class ApprovalRequestIdempotencyTest {

    @Mock
    private ApprovalTicketMapper approvalTicketMapper;

    @InjectMocks
    private ApprovalRequestConsumer approvalRequestConsumer;

    @Test
    @DisplayName("should create ApprovalTicket on first request")
    void createTicketOnFirstRequest() {
        ApprovalRequestEvent event = requestEvent(UUID.randomUUID());

        assertThatNoException()
                .isThrownBy(() -> approvalRequestConsumer.consume(event));

        verify(approvalTicketMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("should ignore duplicate request with same approvalRequestId")
    void ignoreDuplicateRequest() {
        UUID approvalRequestId = UUID.randomUUID();
        ApprovalRequestEvent first = requestEvent(approvalRequestId);
        ApprovalRequestEvent duplicate = requestEvent(approvalRequestId);

        approvalRequestConsumer.consume(first);
        approvalRequestConsumer.consume(duplicate);

        verify(approvalTicketMapper, times(1)).insert(any());
    }

    @Test
    @DisplayName("should allow multiple approvals for same execution with different triggeringSeq")
    void allowMultipleApprovalsForSameExecution() {
        ApprovalRequestEvent event1 = requestEvent(UUID.randomUUID(), 5);
        ApprovalRequestEvent event2 = requestEvent(UUID.randomUUID(), 8);

        assertThatNoException()
                .isThrownBy(() -> approvalRequestConsumer.consume(event1));
        assertThatNoException()
                .isThrownBy(() -> approvalRequestConsumer.consume(event2));

        verify(approvalTicketMapper, times(2)).insert(any());
    }

    private ApprovalRequestEvent requestEvent(UUID approvalRequestId) {
        return requestEvent(approvalRequestId, 5);
    }

    private ApprovalRequestEvent requestEvent(UUID approvalRequestId, int triggeringSeq) {
        return new ApprovalRequestEvent(
                approvalRequestId,
                1L,
                1L,
                1L,
                triggeringSeq,
                "FAST",
                "HIGH",
                "Delete production database",
                3,
                Instant.now()
        );
    }
}
