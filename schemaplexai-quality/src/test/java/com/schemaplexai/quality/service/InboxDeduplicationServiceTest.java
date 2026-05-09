package com.schemaplexai.quality.service;

import com.schemaplexai.quality.entity.SfProcessedEvent;
import com.schemaplexai.quality.mapper.SfProcessedEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.5: Inbox Deduplication Service Tests")
class InboxDeduplicationServiceTest {

    @Mock
    private SfProcessedEventMapper processedEventMapper;

    private InboxDeduplicationService inboxDeduplicationService;

    @BeforeEach
    void setUp() {
        inboxDeduplicationService = new InboxDeduplicationService(processedEventMapper);
    }

    @Test
    @DisplayName("isProcessed returns false for new event")
    void isProcessedReturnsFalseForNewEvent() {
        UUID eventId = UUID.randomUUID();
        String consumerName = "AuditEventConsumer";

        when(processedEventMapper.exists(eventId, consumerName)).thenReturn(false);

        boolean result = inboxDeduplicationService.isProcessed(eventId, consumerName);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isProcessed returns true after markProcessed")
    void isProcessedReturnsTrueAfterMarkProcessed() {
        UUID eventId = UUID.randomUUID();
        String consumerName = "AuditEventConsumer";

        when(processedEventMapper.exists(eventId, consumerName)).thenReturn(true);

        boolean result = inboxDeduplicationService.isProcessed(eventId, consumerName);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("markProcessed inserts record with correct consumer name")
    void markProcessedInsertsRecordWithCorrectConsumerName() {
        UUID eventId = UUID.randomUUID();
        String consumerName = "AuditEventConsumer";

        when(processedEventMapper.insertProcessed(eventId, consumerName)).thenReturn(1);

        inboxDeduplicationService.markProcessed(eventId, consumerName);

        verify(processedEventMapper).insertProcessed(eventId, consumerName);
    }
}
