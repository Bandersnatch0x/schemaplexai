package com.schemaplexai.common.observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Facade for OpenTelemetry span creation.
 * Provides a simpler API for instrumenting business logic with distributed tracing.
 *
 * <p>When a {@link Tracer} is available (i.e. OTel is configured), all operations
 * create proper OTel spans. When no tracer is available, operations execute without
 * tracing (no-op behavior).
 *
 * <p>Usage:
 * <pre>{@code
 * // Simple span
 * tracingService.runInSpan("tool-execution", span -> {
 *     span.setAttribute("tool.name", "fileRead");
 *     // do work
 * });
 *
 * // Span with return value
 * String result = tracingService.callInSpan("llm-call", span -> {
 *     span.setAttribute("model", "gpt-4");
 *     return llmClient.call(prompt);
 * });
 * }</pre>
 */
@Slf4j
public class OpenTelemetryTracingService {

    private final Tracer tracer;

    public OpenTelemetryTracingService(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Execute a runnable within a new span.
     *
     * @param spanName the span name
     * @param body     consumer that receives the span for setting attributes
     */
    public void runInSpan(String spanName, Consumer<Span> body) {
        if (tracer == null) {
            body.accept(Span.getInvalid());
            return;
        }

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            body.accept(span);
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute a callable within a new span, returning the result.
     *
     * @param spanName the span name
     * @param body     callable that receives the span for setting attributes
     * @return the result of the callable
     */
    public <T> T callInSpan(String spanName, SpanCallable<T> body) {
        if (tracer == null) {
            try {
                return body.call(Span.getInvalid());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            T result = body.call(span);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    /**
     * Create a child span linked to a parent context.
     *
     * @param spanName    the span name
     * @param parentContext the parent context (or null for current)
     * @param attributes  initial attributes to set on the span
     * @return the created span (caller must call {@code span.end()})
     */
    public Span createSpan(String spanName, Context parentContext, Map<String, String> attributes) {
        if (tracer == null) {
            return Span.getInvalid();
        }

        var builder = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL);
        if (parentContext != null) {
            builder.setParent(parentContext);
        }
        if (attributes != null) {
            attributes.forEach((k, v) -> builder.setAttribute(
                    io.opentelemetry.api.common.AttributeKey.stringKey(k), v));
        }
        return builder.startSpan();
    }

    /**
     * Set attributes on a span using a map.
     */
    public static void setAttributes(Span span, Map<String, String> attributes) {
        if (span == null || attributes == null) {
            return;
        }
        attributes.forEach((k, v) ->
                span.setAttribute(io.opentelemetry.api.common.AttributeKey.stringKey(k), v));
    }

    /**
     * Set attributes on a span using a map of objects (converted to strings).
     */
    public static void setAttributes(Span span, Attributes attributes) {
        if (span == null || attributes == null) {
            return;
        }
        attributes.forEach((key, value) -> {
            String k = key.getKey();
            if (value instanceof String str) {
                span.setAttribute(k, str);
            } else if (value instanceof Long l) {
                span.setAttribute(k, l);
            } else if (value instanceof Integer i) {
                span.setAttribute(k, i);
            } else if (value instanceof Double d) {
                span.setAttribute(k, d);
            } else if (value instanceof Boolean b) {
                span.setAttribute(k, b);
            }
        });
    }

    /**
     * Whether tracing is enabled (tracer is available).
     */
    public boolean isEnabled() {
        return tracer != null;
    }

    @FunctionalInterface
    public interface SpanCallable<T> {
        T call(Span span) throws Exception;
    }
}
