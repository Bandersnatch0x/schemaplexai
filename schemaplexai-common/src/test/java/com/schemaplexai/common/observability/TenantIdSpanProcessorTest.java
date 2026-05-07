package com.schemaplexai.common.observability;

import com.schemaplexai.common.context.TenantContextHolder;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIdSpanProcessorTest {

    @Mock
    private ReadWriteSpan span;

    private TenantIdSpanProcessor processor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        processor = new TenantIdSpanProcessor();
        TenantContextHolder.clear();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldAddTenantIdAttributeWhenPresent() {
        TenantContextHolder.setTenantId("tenant-42");

        processor.onStart(Context.current(), span);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(span).setAttribute(keyCaptor.capture(), valueCaptor.capture());
        assertEquals("tenant.id", keyCaptor.getValue());
        assertEquals("tenant-42", valueCaptor.getValue());
    }

    @Test
    void shouldNotAddTenantIdWhenNull() {
        processor.onStart(Context.current(), span);

        verify(span, never()).setAttribute(anyString(), anyString());
    }

    @Test
    void shouldNotAddTenantIdWhenBlank() {
        TenantContextHolder.setTenantId("  ");

        processor.onStart(Context.current(), span);

        verify(span, never()).setAttribute(anyString(), anyString());
    }

    @Test
    void shouldAllowEndWithoutException() {
        processor.onStart(Context.current(), span);
        processor.onEnd(span);

        verifyNoMoreInteractions(span);
    }
}
