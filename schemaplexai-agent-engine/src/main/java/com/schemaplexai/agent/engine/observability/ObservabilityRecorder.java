package com.schemaplexai.agent.engine.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.mapper.ObservabilitySpanMapper;
import com.schemaplexai.agent.engine.mapper.ObservabilityTraceMapper;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ObservabilityRecorder — backward-compatible adapter.
 * Delegates to OpenTelemetry Tracer when available; falls back to PostgreSQL storage.
 * @deprecated Use OpenTelemetry Tracer directly for new code.
 */
@Service
@RequiredArgsConstructor
public class ObservabilityRecorder {

    private final ObservabilityTraceMapper traceMapper;
    private final ObservabilitySpanMapper spanMapper;

    private Tracer tracer;
    private final ConcurrentHashMap<String, Span> activeSpans = new ConcurrentHashMap<>();

    @Autowired(required = false)
    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Transactional
    public ObservabilityTrace startTrace(String executionId, String name,
                                          String userId, String sessionId, String input) {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setName(name + "-" + executionId);
        trace.setUserId(userId);
        trace.setSessionId(sessionId);
        trace.setInput(PiiRedactor.redact(input));

        if (tracer != null) {
            Span span = tracer.spanBuilder(name)
                    .setAttribute("execution.id", executionId)
                    .setAttribute("user.id", userId != null ? userId : "")
                    .setAttribute("session.id", sessionId != null ? sessionId : "")
                    .startSpan();
            trace.setTraceId(span.getSpanContext().getTraceId());
            activeSpans.put(trace.getTraceId(), span);
        } else {
            trace.setTraceId(UUID.randomUUID().toString());
        }

        traceMapper.insert(trace);
        return trace;
    }

    @Transactional
    public void endTrace(String traceId, String output) {
        ObservabilityTrace trace = traceMapper.selectOne(
            new LambdaQueryWrapper<ObservabilityTrace>()
                .eq(ObservabilityTrace::getTraceId, traceId));
        if (trace != null) {
            trace.setOutput(PiiRedactor.redact(output));
            traceMapper.updateById(trace);
        }

        Span span = activeSpans.remove(traceId);
        if (span != null) {
            span.setAttribute("output", PiiRedactor.redact(output));
            span.end();
        }
    }

    @Transactional
    public ObservabilitySpan addSpan(String traceId, String parentSpanId,
                                      String name, String type,
                                      Long startTime, Long endTime,
                                      String input, String output, String status) {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setSpanId(UUID.randomUUID().toString());
        span.setTraceId(traceId);
        span.setParentSpanId(parentSpanId);
        span.setName(name);
        span.setType(type);
        span.setStartTime(startTime);
        span.setEndTime(endTime);
        span.setInput(PiiRedactor.redact(input));
        span.setOutput(PiiRedactor.redact(output));
        span.setStatus(status);
        spanMapper.insert(span);
        return span;
    }
}
