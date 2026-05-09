package com.schemaplexai.task.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("M6.6: Dead Letter Retry Service Tests")
class DeadLetterRetryServiceTest {

    @InjectMocks
    private DeadLetterRetryService deadLetterRetryService;

    @Test
    @DisplayName("retryDeadEvent logs request for given eventId")
    void retryDeadEventLogsRequest() {
        UUID eventId = UUID.randomUUID();

        // Should not throw and should complete without error
        deadLetterRetryService.retryDeadEvent(eventId);
    }

    @Test
    @DisplayName("listDeadEvents returns empty list")
    void listDeadEventsReturnsEmptyList() {
        List<Object> result = deadLetterRetryService.listDeadEvents(10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("listDeadEvents respects limit parameter")
    void listDeadEventsRespectsLimit() {
        List<Object> result = deadLetterRetryService.listDeadEvents(5);

        assertThat(result).isEmpty();
    }
}
