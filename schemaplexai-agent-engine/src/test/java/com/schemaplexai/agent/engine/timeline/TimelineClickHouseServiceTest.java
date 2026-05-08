package com.schemaplexai.agent.engine.timeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TimelineClickHouseServiceTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void whenDisabled_enqueueDoesNothing() {
        TimelineClickHouseService service = new TimelineClickHouseService(null, objectMapper, false);

        AgentTimelineEvent event = AgentTimelineEvent.of(1L, "thought", "test", null, 42L);
        // Should not throw even with null DataSource
        assertDoesNotThrow(() -> service.enqueue(event));
    }

    @Test
    void whenDisabled_queryReturnsEmptyList() {
        TimelineClickHouseService service = new TimelineClickHouseService(null, objectMapper, false);

        var result = service.queryByExecutionId(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void agentTimelineEventBuilder() {
        AgentTimelineEvent event = AgentTimelineEvent.builder()
                .executionId(1L)
                .eventType("thought")
                .content("thinking hard")
                .metadataJson("{\"key\": \"value\"}")
                .tenantId(42L)
                .build();

        assertEquals(1L, event.getExecutionId());
        assertEquals("thought", event.getEventType());
        assertEquals("thinking hard", event.getContent());
        assertEquals("{\"key\": \"value\"}", event.getMetadataJson());
        assertEquals(42L, event.getTenantId());
    }

    @Test
    void agentTimelineEventOfWithMetadata() {
        AgentTimelineEvent event = AgentTimelineEvent.of(1L, "tool_call", "search", "{\"tool\": \"search\"}", 42L);

        assertEquals(1L, event.getExecutionId());
        assertEquals("tool_call", event.getEventType());
        assertEquals("search", event.getContent());
        assertEquals("{\"tool\": \"search\"}", event.getMetadataJson());
        assertEquals(42L, event.getTenantId());
        assertNotNull(event.getCreatedAt());
    }

    @Test
    void agentTimelineEventOfWithoutMetadata() {
        AgentTimelineEvent event = AgentTimelineEvent.of(1L, "output", "result", 42L);

        assertEquals(1L, event.getExecutionId());
        assertEquals("output", event.getEventType());
        assertEquals("result", event.getContent());
        assertNull(event.getMetadataJson());
        assertEquals(42L, event.getTenantId());
    }

    @Test
    void timelineEventTypesCoverAllRequired() {
        // Verify all required event types exist in the enum
        String[] required = {
            "state_transition", "thought", "tool_call", "tool_result",
            "approval_req", "approval_resp", "plan", "file_diff",
            "output", "error", "completed"
        };

        for (String req : required) {
            boolean found = false;
            for (TimelineEventType type : TimelineEventType.values()) {
                if (type.value().equals(req)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Missing TimelineEventType: " + req);
        }
    }
}
