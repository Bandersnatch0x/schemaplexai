package com.schemaplexai.agent.engine.entity;

import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservabilityTraceTest {

    @Test
    void shouldCreateTraceWithRequiredFields() {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setTraceId("trace-001");
        trace.setName("agent-execution-42");
        trace.setUserId("user-1");
        trace.setSessionId("session-1");
        trace.setInput("{\"prompt\":\"hello\"}");

        assertEquals("trace-001", trace.getTraceId());
        assertEquals("agent-execution-42", trace.getName());
        assertEquals("user-1", trace.getUserId());
        assertEquals("session-1", trace.getSessionId());
    }
}
