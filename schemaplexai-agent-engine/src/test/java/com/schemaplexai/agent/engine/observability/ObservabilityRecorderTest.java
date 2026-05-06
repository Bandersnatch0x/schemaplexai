package com.schemaplexai.agent.engine.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.mapper.ObservabilitySpanMapper;
import com.schemaplexai.agent.engine.mapper.ObservabilityTraceMapper;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservabilityRecorderTest {

    @Mock
    private ObservabilityTraceMapper traceMapper;

    @Mock
    private ObservabilitySpanMapper spanMapper;

    private ObservabilityRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ObservabilityRecorder(traceMapper, spanMapper);
    }

    @Test
    void shouldStartAndEndTrace() {
        when(traceMapper.insert(any(ObservabilityTrace.class))).thenReturn(1);

        ObservabilityTrace trace = recorder.startTrace(
            "agent-exec-1", "test run", "user-1", "session-1", "{\"prompt\":\"hi\"}");

        assertNotNull(trace.getTraceId());
        assertEquals("test run-agent-exec-1", trace.getName());

        ObservabilityTrace storedTrace = new ObservabilityTrace();
        storedTrace.setTraceId(trace.getTraceId());
        when(traceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(storedTrace);
        when(traceMapper.updateById(any(ObservabilityTrace.class))).thenReturn(1);

        recorder.endTrace(trace.getTraceId(), "{\"result\":\"ok\"}");

        verify(traceMapper).updateById(any(ObservabilityTrace.class));
    }

    @Test
    void shouldAddSpanToTrace() {
        when(traceMapper.insert(any(ObservabilityTrace.class))).thenReturn(1);
        when(spanMapper.insert(any(ObservabilitySpan.class))).thenReturn(1);

        ObservabilityTrace trace = recorder.startTrace(
            "agent-exec-2", "span test", "user-1", "sess-1", "{}");

        ObservabilitySpan span = recorder.addSpan(
            trace.getTraceId(), null, "tool-call", "SPAN",
            System.currentTimeMillis(), System.currentTimeMillis() + 100,
            "{\"tool\":\"bash\"}", "{\"exitCode\":0}", "SUCCESS");

        assertNotNull(span.getSpanId());
        assertEquals(trace.getTraceId(), span.getTraceId());
        verify(spanMapper).insert(any(ObservabilitySpan.class));
    }
}
