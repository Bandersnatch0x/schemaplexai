package com.schemaplexai.agent.engine.observability;

import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ObservabilityRecorderTest {

    @Autowired(required = false)
    private ObservabilityRecorder recorder;

    @Test
    void shouldStartAndEndTrace() {
        assertNotNull(recorder);

        ObservabilityTrace trace = recorder.startTrace(
            "agent-exec-1", "test run", "user-1", "session-1", "{\"prompt\":\"hi\"}");

        assertNotNull(trace.getTraceId());
        assertEquals("test run", trace.getName());

        recorder.endTrace(trace.getTraceId(), "{\"result\":\"ok\"}");
    }

    @Test
    void shouldAddSpanToTrace() {
        ObservabilityTrace trace = recorder.startTrace(
            "agent-exec-2", "span test", "user-1", "sess-1", "{}");

        ObservabilitySpan span = recorder.addSpan(
            trace.getTraceId(), null, "tool-call", "SPAN",
            System.currentTimeMillis(), System.currentTimeMillis() + 100,
            "{\"tool\":\"bash\"}", "{\"exitCode\":0}", "SUCCESS");

        assertNotNull(span.getSpanId());
        assertEquals(trace.getTraceId(), span.getTraceId());
    }
}
