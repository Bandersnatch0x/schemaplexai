package com.schemaplexai.agent.engine.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Async ClickHouse persistence for agent timeline events.
 * Uses an in-memory queue + background flush to avoid blocking the SSE stream.
 */
@Slf4j
@Service
public class TimelineClickHouseService {

    private static final String INSERT_SQL = """
            INSERT INTO agent_timeline_event
            (execution_id, event_type, content, metadata_json, tenant_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final int DEFAULT_BATCH_SIZE = 50;
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;
    private static final int DEFAULT_FLUSH_INTERVAL_MS = 5000;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final BlockingQueue<AgentTimelineEvent> eventQueue;

    public TimelineClickHouseService(
            DataSource dataSource,
            ObjectMapper objectMapper,
            @Value("${clickhouse.enabled:false}") boolean enabled) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.eventQueue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);

        if (enabled) {
            startBackgroundFlush();
        }
    }

    /**
     * Enqueue an event for async persistence. Never blocks the caller.
     */
    public void enqueue(AgentTimelineEvent event) {
        if (!enabled) {
            return;
        }
        if (!eventQueue.offer(event)) {
            log.warn("Timeline event queue full, dropping event for execution {}", event.getExecutionId());
        }
    }

    /**
     * Synchronously query timeline events by execution ID.
     */
    public List<AgentTimelineEvent> queryByExecutionId(Long executionId) {
        if (!enabled) {
            return List.of();
        }

        String sql = """
                SELECT execution_id, event_type, content, metadata_json, tenant_id, created_at
                FROM agent_timeline_event
                WHERE execution_id = ?
                ORDER BY created_at ASC
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, executionId);
            var rs = ps.executeQuery();
            List<AgentTimelineEvent> results = new java.util.ArrayList<>();
            while (rs.next()) {
                results.add(AgentTimelineEvent.builder()
                        .executionId(rs.getLong("execution_id"))
                        .eventType(rs.getString("event_type"))
                        .content(rs.getString("content"))
                        .metadataJson(rs.getString("metadata_json"))
                        .tenantId(rs.getLong("tenant_id"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .build());
            }
            return results;
        } catch (SQLException e) {
            log.error("Failed to query timeline for execution {}", executionId, e);
            return List.of();
        }
    }

    private void startBackgroundFlush() {
        Thread flushThread = new Thread(this::flushLoop, "timeline-ch-flush");
        flushThread.setDaemon(true);
        flushThread.start();
    }

    private void flushLoop() {
        List<AgentTimelineEvent> batch = new java.util.ArrayList<>(DEFAULT_BATCH_SIZE);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                AgentTimelineEvent event = eventQueue.poll(DEFAULT_FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                if (event != null) {
                    batch.add(event);
                }
                if (batch.size() >= DEFAULT_BATCH_SIZE || (event == null && !batch.isEmpty())) {
                    flushBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
    }

    private void flushBatch(List<AgentTimelineEvent> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            for (AgentTimelineEvent event : batch) {
                setParams(ps, event);
                ps.addBatch();
            }
            ps.executeBatch();
            log.debug("Flushed {} timeline events to ClickHouse", batch.size());
        } catch (SQLException e) {
            log.error("Failed to flush timeline batch of size {}", batch.size(), e);
        }
    }

    private void setParams(PreparedStatement ps, AgentTimelineEvent event) throws SQLException {
        ps.setLong(1, event.getExecutionId());
        ps.setString(2, event.getEventType());
        ps.setString(3, truncate(event.getContent(), 10000));
        ps.setString(4, event.getMetadataJson());
        ps.setLong(5, event.getTenantId() != null ? event.getTenantId() : 0L);
        ps.setTimestamp(6, Timestamp.valueOf(event.getCreatedAt()));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
