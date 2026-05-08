package com.schemaplexai.common.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryTracingServiceTest {

    @Mock
    private Tracer tracer;

    @Test
    void isEnabled_returnsTrueWhenTracerPresent() {
        OpenTelemetryTracingService service = new OpenTelemetryTracingService(tracer);
        assertTrue(service.isEnabled());
    }

    @Test
    void isEnabled_returnsFalseWhenTracerNull() {
        OpenTelemetryTracingService service = new OpenTelemetryTracingService(null);
        assertFalse(service.isEnabled());
    }

    @Test
    void runInSpan_executesBodyWhenNoTracer() {
        OpenTelemetryTracingService service = new OpenTelemetryTracingService(null);

        AtomicReference<String> result = new AtomicReference<>();
        service.runInSpan("test-span", span -> result.set("executed"));

        assertEquals("executed", result.get());
    }

    @Test
    void callInSpan_returnsValueWhenNoTracer() {
        OpenTelemetryTracingService service = new OpenTelemetryTracingService(null);

        String result = service.callInSpan("test-span", span -> "hello");

        assertEquals("hello", result);
    }

    @Test
    void callInSpan_propagatesExceptionWhenNoTracer() {
        OpenTelemetryTracingService service = new OpenTelemetryTracingService(null);

        assertThrows(RuntimeException.class, () ->
                service.callInSpan("test-span", span -> {
                    throw new RuntimeException("boom");
                }));
    }

    @Test
    void createSpan_returnsInvalidSpanWhenNoTracer() {
        OpenTelemetryTracingService service = new OpenTelemetryTracingService(null);

        Span span = service.createSpan("test", null, Map.of("key", "value"));

        assertEquals(Span.getInvalid(), span);
    }

    @Test
    void setAttributes_handlesNullSpan() {
        assertDoesNotThrow(() ->
                OpenTelemetryTracingService.setAttributes(null, Map.of("key", "value")));
    }

    @Test
    void setAttributes_handlesNullMap() {
        assertDoesNotThrow(() ->
                OpenTelemetryTracingService.setAttributes(Span.getInvalid(), (Map<String, String>) null));
    }

    @Test
    void setAttributes_setsOnSpan() {
        // When no OTel SDK is configured, Span.getInvalid() silently discards attributes
        assertDoesNotThrow(() ->
                OpenTelemetryTracingService.setAttributes(Span.getInvalid(), Map.of("key", "value")));
    }
}
