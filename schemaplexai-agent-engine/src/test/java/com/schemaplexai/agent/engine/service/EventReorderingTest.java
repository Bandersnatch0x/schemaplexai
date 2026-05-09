package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Out-of-order event buffering and gap recovery")
class EventReorderingTest {

    @InjectMocks
    private ExecutionEventBuffer eventBuffer;

    @Test
    @DisplayName("should apply events in seq order regardless of arrival order")
    void applyEventsInOrder() {
        ExecutionEvent eventSeq3 = event(3, "TOOL_RESULT");
        ExecutionEvent eventSeq1 = event(1, "TOOL_CALLED");
        ExecutionEvent eventSeq2 = event(2, "THOUGHT");

        List<ExecutionEvent> result = eventBuffer.applyInOrder(List.of(eventSeq3, eventSeq1, eventSeq2));

        assertThat(result).extracting(ExecutionEvent::getSeq).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("should buffer events with gaps until missing seq arrives")
    void bufferEventsWithGaps() {
        ExecutionEvent eventSeq1 = event(1, "TOOL_CALLED");
        ExecutionEvent eventSeq3 = event(3, "TOOL_RESULT");

        List<ExecutionEvent> result = eventBuffer.applyInOrder(List.of(eventSeq1, eventSeq3));

        assertThat(result).extracting(ExecutionEvent::getSeq).containsExactly(1);
    }

    @Test
    @DisplayName("should release buffered events when gap is filled")
    void releaseBufferedOnGapFill() {
        ExecutionEvent eventSeq1 = event(1, "TOOL_CALLED");
        ExecutionEvent eventSeq3 = event(3, "TOOL_RESULT");
        ExecutionEvent eventSeq2 = event(2, "THOUGHT");

        eventBuffer.applyInOrder(List.of(eventSeq1, eventSeq3));
        List<ExecutionEvent> result = eventBuffer.applyInOrder(List.of(eventSeq2));

        assertThat(result).extracting(ExecutionEvent::getSeq).containsExactly(2, 3);
    }

    private ExecutionEvent event(int seq, String type) {
        ExecutionEvent e = new ExecutionEvent();
        e.setEventId(UUID.randomUUID());
        e.setExecutionId(1L);
        e.setSeq(seq);
        e.setEventType(type);
        e.setOccurredAt(Instant.now());
        e.setTenantId(1L);
        return e;
    }
}
