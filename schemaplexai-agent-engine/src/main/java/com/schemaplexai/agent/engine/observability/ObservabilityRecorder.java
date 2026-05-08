package com.schemaplexai.agent.engine.observability;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.mapper.ObservabilitySpanMapper;
import com.schemaplexai.agent.engine.mapper.ObservabilityTraceMapper;
import com.schemaplexai.common.observability.PiiRedactor;
import com.schemaplexai.model.entity.observability.ObservabilitySpan;
import com.schemaplexai.model.entity.observability.ObservabilityTrace;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ObservabilityRecorder — backward-compatible adapter.
 * Delegates to OpenTelemetry Tracer when available; falls back to PostgreSQL storage.
 *
 * <p>Set {@code agent.engine.observability.pg-storage-enabled=false} to disable PostgreSQL
 * trace/span persistence when using Jaeger/Tempo as the primary trace store.
 */
@Service
@RequiredArgsConstructor
public class ObservabilityRecorder {

    private final ObservabilityTraceMapper traceMapper;
    private final ObservabilitySpanMapper spanMapper;

    private Tracer tracer;
    private final ConcurrentHashMap<String, Span> activeSpans = new ConcurrentHashMap<>();

    @Value("${agent.engine.observability.pg-storage-enabled:true}")
    private boolean pgStorageEnabled = true;

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
                    .setAttribute(AttributeKey.stringKey("execution.id"), executionId)
                    .setAttribute(AttributeKey.stringKey("user.id"), userId != null ? userId : "")
                    .setAttribute(AttributeKey.stringKey("session.id"), sessionId != null ? sessionId : "")
                    .startSpan();
            trace.setTraceId(span.getSpanContext().getTraceId());
            activeSpans.put(trace.getTraceId(), span);
        } else {
            trace.setTraceId(UUID.randomUUID().toString());
        }

        if (pgStorageEnabled) {
            traceMapper.insert(trace);
        }
        return trace;
    }

    @Transactional
    public void endTrace(String traceId, String output) {
        if (pgStorageEnabled) {
            ObservabilityTrace trace = traceMapper.selectOne(
                new LambdaQueryWrapper<ObservabilityTrace>()
                    .eq(ObservabilityTrace::getTraceId, traceId));
            if (trace != null) {
                trace.setOutput(PiiRedactor.redact(output));
                traceMapper.updateById(trace);
            }
        }

        Span span = activeSpans.remove(traceId);
        if (span != null) {
            span.setAttribute(AttributeKey.stringKey("output"), PiiRedactor.redact(output));
            span.setStatus(StatusCode.OK);
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

        // Create OTel child span if tracer is available
        if (tracer != null) {
            Span otelSpan = tracer.spanBuilder(name)
                    .setAttribute(AttributeKey.stringKey("span.type"), type != null ? type : "")
                    .setAttribute(AttributeKey.stringKey("span.status"), status != null ? status : "")
                    .startSpan();
            otelSpan.end();
        }

        if (pgStorageEnabled) {
            spanMapper.insert(span);
        }
        return span;
    }

    /**
     * Add an event to the current active trace span.
     *
     * @param traceId  the trace ID
     * @param eventName the event name
     * @param attributes key-value attributes for the event
     */
    public void addTraceEvent(String traceId, String eventName,
                               io.opentelemetry.api.common.Attributes attributes) {
        Span span = activeSpans.get(traceId);
        if (span != null) {
            span.addEvent(eventName, attributes);
        }
    }

    /**
     * Whether PG storage is enabled for this recorder.
     */
    public boolean isPgStorageEnabled() {
        return pgStorageEnabled;
    }
}
