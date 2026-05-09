package com.schemaplexai.agent.engine.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    private GapRecoveryJob gapRecoveryJob;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        gapRecoveryJob = new GapRecoveryJob(executionEventMapper, rabbitTemplate, objectMapper);
    }

    @Test
    @DisplayName("Detects and reports seq gaps for an execution")
    void detectsSeqGaps() {
        List<ExecutionEvent> events = List.of(
                createEvent(1L, 1), createEvent(1L, 2),
                createEvent(1L, 4), createEvent(1L, 5) // gap at seq 3
        );
        when(executionEventMapper.selectByExecutionIdOrdered(1L)).thenReturn(events);

        gapRecoveryJob.recoverGapsForExecution(1L);

        verify(rabbitTemplate).convertAndSend(eq("execution_events"), eq("execution.gap"), any(String.class));
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
