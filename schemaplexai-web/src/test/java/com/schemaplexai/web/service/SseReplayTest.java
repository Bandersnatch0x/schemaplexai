package com.schemaplexai.web.service;

import com.schemaplexai.web.dto.SseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SSE event replay with EPHEMERAL filtering")
class SseReplayTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SseReplayService sseReplayService;

    @Test
    @DisplayName("should replay events with seq greater than lastSeq")
    void replayEventsAfterLastSeq() {
        List<SseEvent> mockEvents = List.of(
                new SseEvent(1L, 6, "STATUS", "{}", "AUDIT"),
                new SseEvent(1L, 7, "STATUS", "{}", "DEBUG")
        );
        when(jdbcTemplate.query(any(), any(RowMapper.class), eq(1L), eq(5))).thenReturn(mockEvents);

        List<SseEvent> events = sseReplayService.replayEvents(1L, 5);

        assertThat(events).isNotEmpty();
        assertThat(events).allMatch(e -> e.seq() > 5);
    }

    @Test
    @DisplayName("should exclude EPHEMERAL events from replay")
    void excludeEphemeralEvents() {
        List<SseEvent> mockEvents = List.of(
                new SseEvent(1L, 1, "STATUS", "{}", "AUDIT"),
                new SseEvent(1L, 2, "STATUS", "{}", "DEBUG")
        );
        when(jdbcTemplate.query(any(), any(RowMapper.class), eq(1L), eq(0))).thenReturn(mockEvents);

        List<SseEvent> events = sseReplayService.replayEvents(1L, 0);

        assertThat(events)
                .extracting(SseEvent::sensitivity)
                .doesNotContain("EPHEMERAL");
    }

    @Test
    @DisplayName("should include AUDIT and DEBUG events in replay")
    void includeAuditAndDebugEvents() {
        List<SseEvent> mockEvents = List.of(
                new SseEvent(1L, 1, "STATUS", "{}", "AUDIT"),
                new SseEvent(1L, 2, "STATUS", "{}", "DEBUG")
        );
        when(jdbcTemplate.query(any(), any(RowMapper.class), eq(1L), eq(0))).thenReturn(mockEvents);

        List<SseEvent> events = sseReplayService.replayEvents(1L, 0);

        assertThat(events)
                .extracting(SseEvent::sensitivity)
                .contains("AUDIT", "DEBUG");
    }
}
