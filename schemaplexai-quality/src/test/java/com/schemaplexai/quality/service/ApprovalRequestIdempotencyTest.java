package com.schemaplexai.quality.service;

import com.schemaplexai.model.event.ApprovalRequestEvent;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Approval request MQ consumer idempotency")
class ApprovalRequestIdempotencyTest {

    @Mock
    private ApprovalTicketMapper approvalTicketMapper;

    @Mock
    private InboxDeduplicationService dedupService;

    @InjectMocks
    private ApprovalRequestConsumer approvalRequestConsumer;

    @BeforeEach
    void setUp() {
        lenient().when(dedupService.isProcessed(any(), eq("ApprovalRequestConsumer"))).thenReturn(false);
    }

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

        when(dedupService.isProcessed(any(), eq("ApprovalRequestConsumer"))).thenReturn(false, true);

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

    @Test
    @DisplayName("should propagate dedup mark failure after creating ticket")
    void markProcessedFailureAfterTicketInsert_propagates() {
        UUID approvalRequestId = UUID.randomUUID();
        ApprovalRequestEvent event = requestEvent(approvalRequestId);

        doThrow(new RuntimeException("dedup mark failed"))
                .when(dedupService).markProcessed(approvalRequestId, "ApprovalRequestConsumer");

        assertThatThrownBy(() -> approvalRequestConsumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dedup mark failed");

        verify(approvalTicketMapper).insert(any());
        verify(dedupService).markProcessed(approvalRequestId, "ApprovalRequestConsumer");
    }

    @Test
    @DisplayName("should process ticket insert and dedup mark in one transaction")
    void consume_hasTransactionalBoundary() throws Exception {
        Method consume = ApprovalRequestConsumer.class.getMethod("consume", ApprovalRequestEvent.class);

        assertThat(consume.getAnnotation(Transactional.class)).isNotNull();
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
