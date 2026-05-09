package com.schemaplexai.web.service;

import com.schemaplexai.web.dto.SseEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Replays execution events for SSE reconnect, filtering EPHEMERAL events.
 */
@Service
public class SseReplayService {

    private final JdbcTemplate jdbcTemplate;

    public SseReplayService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SseEvent> replayEvents(Long executionId, int lastSeq) {
        String sql = """
                SELECT execution_id, seq, event_type, payload::text, sensitivity
                FROM sf_execution_event
                WHERE execution_id = ? AND seq > ? AND COALESCE(sensitivity, '') != 'EPHEMERAL'
                ORDER BY seq
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SseEvent(
                rs.getLong("execution_id"),
                rs.getInt("seq"),
                rs.getString("event_type"),
                rs.getString("payload"),
                rs.getString("sensitivity")
        ), executionId, lastSeq);
    }
}
