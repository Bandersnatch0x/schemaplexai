package com.schemaplexai.agent.engine.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.model.event.ExecutionEventMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M4.3: Gap Recovery Job Tests")
class GapRecoveryJobTest {

    @Mock
    private ExecutionEventMapper executionEventMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private ObjectMapper objectMapper;
    private GapRecoveryJob gapRecoveryJob;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        gapRecoveryJob = new GapRecoveryJob(executionEventMapper, rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("Publishes a gap alert without routing it back as a recovered execution event")
    void publishesGapAlertWithoutPretendingToRecoverMissingEvent() throws Exception {
        List<ExecutionEvent> events = List.of(
                createEvent(1L, 1), createEvent(1L, 2),
                createEvent(1L, 4), createEvent(1L, 5) // gap at seq 3
        );
        when(executionEventMapper.selectByExecutionIdOrdered(1L)).thenReturn(events);

        gapRecoveryJob.recoverGapsForExecution(1L);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq("execution_events"), eq("execution-gap.detected"), payloadCaptor.capture());

        ExecutionEventMessage alert = objectMapper.readValue(payloadCaptor.getValue(), ExecutionEventMessage.class);
        assertAll(
                () -> assertNotNull(alert.eventId()),
                () -> assertEquals(1L, alert.executionId()),
                () -> assertEquals(3, alert.seq()),
                () -> assertEquals("GAP_DETECTED", alert.eventType()),
                () -> assertFalse(alert.payload().contains("recovered"))
        );
    }

    @Test
    @DisplayName("Does nothing when no gaps exist")
    void noGapsWhenSequential() {
        List<ExecutionEvent> events = List.of(
                createEvent(1L, 1), createEvent(1L, 2), createEvent(1L, 3)
        );
        when(executionEventMapper.selectByExecutionIdOrdered(1L)).thenReturn(events);

        gapRecoveryJob.recoverGapsForExecution(1L);

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Does nothing for single event")
    void noActionForSingleEvent() {
        when(executionEventMapper.selectByExecutionIdOrdered(1L)).thenReturn(List.of(createEvent(1L, 1)));

        gapRecoveryJob.recoverGapsForExecution(1L);

        verifyNoInteractions(rabbitTemplate);
    }

    private ExecutionEvent createEvent(Long executionId, int seq) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(UUID.randomUUID());
        event.setExecutionId(executionId);
        event.setSeq(seq);
        event.setEventType("TEST");
        event.setPayload("{}");
        event.setOccurredAt(Instant.now());
        return event;
    }
}
