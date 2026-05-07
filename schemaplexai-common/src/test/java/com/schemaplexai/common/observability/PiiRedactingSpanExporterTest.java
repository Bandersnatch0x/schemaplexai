package com.schemaplexai.common.observability;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PiiRedactingSpanExporterTest {

    @Mock
    private io.opentelemetry.sdk.trace.export.SpanExporter delegate;

    private PiiRedactingSpanExporter exporter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        exporter = new PiiRedactingSpanExporter(delegate);
    }

    @Test
    void shouldRedactPasswordInAttributes() {
        SpanData span = createSpanWithAttribute("input", "password=secret123");

        when(delegate.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        exporter.export(List.of(span));

        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(delegate).export(captor.capture());
        SpanData exported = captor.getValue().iterator().next();
        assertEquals("[REDACTED]", exported.getAttributes().get(AttributeKey.stringKey("input")));
    }

    @Test
    void shouldRedactApiKeyInAttributes() {
        SpanData span = createSpanWithAttribute("output", "api_key=sk-abcd1234");

        when(delegate.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        exporter.export(List.of(span));

        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(delegate).export(captor.capture());
        SpanData exported = captor.getValue().iterator().next();
        assertTrue(exported.getAttributes().get(AttributeKey.stringKey("output")).contains("[REDACTED]"));
    }

    @Test
    void shouldRedactEmailInAttributes() {
        SpanData span = createSpanWithAttribute("message", "Contact us at admin@example.com");

        when(delegate.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        exporter.export(List.of(span));

        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(delegate).export(captor.capture());
        SpanData exported = captor.getValue().iterator().next();
        assertTrue(exported.getAttributes().get(AttributeKey.stringKey("message")).contains("[REDACTED]"));
    }

    @Test
    void shouldPassThroughNonSensitiveAttributes() {
        SpanData span = createSpanWithAttribute("agent.id", "agent-42");

        when(delegate.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        exporter.export(List.of(span));

        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(delegate).export(captor.capture());
        SpanData exported = captor.getValue().iterator().next();
        assertEquals("agent-42", exported.getAttributes().get(AttributeKey.stringKey("agent.id")));
    }

    @Test
    void shouldDelegateFlushAndShutdown() {
        when(delegate.flush()).thenReturn(CompletableResultCode.ofSuccess());
        when(delegate.shutdown()).thenReturn(CompletableResultCode.ofSuccess());

        assertDoesNotThrow(() -> exporter.flush());
        assertDoesNotThrow(() -> exporter.shutdown());

        verify(delegate).flush();
        verify(delegate).shutdown();
    }

    private SpanData createSpanWithAttribute(String key, String value) {
        return new SpanData() {
            @Override public String getName() { return "test"; }
            @Override public SpanKind getKind() { return SpanKind.INTERNAL; }
            @Override public SpanContext getSpanContext() { return SpanContext.getInvalid(); }
            @Override public SpanContext getParentSpanContext() { return SpanContext.getInvalid(); }
            @Override public long getStartEpochNanos() { return 0; }
            @Override public Attributes getAttributes() { return Attributes.of(AttributeKey.stringKey(key), value); }
            @Override public List<EventData> getEvents() { return List.of(); }
            @Override public List<LinkData> getLinks() { return List.of(); }
            @Override public long getEndEpochNanos() { return 1; }
            @Override public boolean hasEnded() { return true; }
            @Override public int getTotalRecordedEvents() { return 0; }
            @Override public int getTotalRecordedLinks() { return 0; }
            @Override public int getTotalAttributeCount() { return 1; }
            @Override public Resource getResource() { return Resource.empty(); }
            @Override public io.opentelemetry.sdk.trace.data.StatusData getStatus() { return io.opentelemetry.sdk.trace.data.StatusData.unset(); }
            @Override public io.opentelemetry.sdk.common.InstrumentationScopeInfo getInstrumentationScopeInfo() {
                return io.opentelemetry.sdk.common.InstrumentationScopeInfo.builder("test").build();
            }
            @Override public io.opentelemetry.sdk.common.InstrumentationLibraryInfo getInstrumentationLibraryInfo() {
                return io.opentelemetry.sdk.common.InstrumentationLibraryInfo.create("test", "1.0");
            }
        };
    }
}
