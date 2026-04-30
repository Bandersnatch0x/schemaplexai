package com.schemaplexai.agent.engine.entity;

import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilitySpanTest {

    @Test
    void shouldCreateGenerationSpanWithModelAndUsage() {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setSpanId("span-001");
        span.setTraceId("trace-001");
        span.setName("llm-call");
        span.setType("GENERATION");
        span.setModel("gpt-4");
        span.setUsageDetails("{\"input\":100,\"output\":50}");
        span.setCostDetails("{\"total\":0.015}");

        assertEquals("GENERATION", span.getType());
        assertEquals("gpt-4", span.getModel());
        assertTrue(span.getUsageDetails().contains("100"));
    }

    @Test
    void shouldSupportNestedSpansViaParentSpanId() {
        ObservabilitySpan child = new ObservabilitySpan();
        child.setSpanId("span-002");
        child.setParentSpanId("span-001");

        assertEquals("span-001", child.getParentSpanId());
    }
}
