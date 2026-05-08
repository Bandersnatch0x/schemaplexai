package com.schemaplexai.agent.engine.timeline;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Immutable data object representing a single timeline event in an agent execution.
 * Written to ClickHouse for historical querying and emitted via SSE for real-time streaming.
 */
@Data
@Builder
public class AgentTimelineEvent {

    private Long executionId;
    private String eventType;
    private String content;
    private String metadataJson;
    private Long tenantId;
    private LocalDateTime createdAt;

    public static AgentTimelineEvent of(Long executionId, String eventType, String content,
                                         String metadataJson, Long tenantId) {
        return AgentTimelineEvent.builder()
                .executionId(executionId)
                .eventType(eventType)
                .content(content)
                .metadataJson(metadataJson)
                .tenantId(tenantId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AgentTimelineEvent of(Long executionId, String eventType, String content, Long tenantId) {
        return of(executionId, eventType, content, null, tenantId);
    }
}
