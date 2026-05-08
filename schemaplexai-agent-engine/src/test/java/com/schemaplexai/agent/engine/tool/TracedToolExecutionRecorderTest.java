package com.schemaplexai.agent.engine.tool;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.observability.OpenTelemetryTracingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TracedToolExecutionRecorderTest {

    @Mock
    private ToolExecutionRecorder delegate;

    @Mock
    private OpenTelemetryTracingService tracingService;

    @Mock
    private ToolCallBudgetService toolCallBudgetService;

    @InjectMocks
    private TracedToolExecutionRecorder recorder;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("tenant-1");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void record_delegatesToRecorderWhenTracingDisabled() {
        when(tracingService.isEnabled()).thenReturn(false);
        when(toolCallBudgetService.consume("tenant-1")).thenReturn(true);
        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content", 150, 42);

        recorder.record(100L, result);

        verify(delegate).record(100L, result);
        verify(toolCallBudgetService).consume("tenant-1");
    }

    @Test
    void record_createsSpanWhenTracingEnabled() {
        when(tracingService.isEnabled()).thenReturn(true);
        when(toolCallBudgetService.consume("tenant-1")).thenReturn(true);
        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content", 150, 42);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            var body = (java.util.function.Consumer<io.opentelemetry.api.trace.Span>) invocation.getArgument(1);
            body.accept(io.opentelemetry.api.trace.Span.getInvalid());
            return null;
        }).when(tracingService).runInSpan(anyString(), any());

        recorder.record(100L, result);

        verify(delegate).record(100L, result);
        verify(tracingService).runInSpan(eq("tool-call:fileRead"), any());
    }

    @Test
    void record_consumesTenantBudget() {
        when(tracingService.isEnabled()).thenReturn(false);
        when(toolCallBudgetService.consume("tenant-1")).thenReturn(true);
        ToolExecutionResult result = ToolExecutionResult.success("apiCall", "ok", 100, 10);

        recorder.record(200L, result);

        verify(toolCallBudgetService).consume("tenant-1");
    }

    @Test
    void record_logsWhenBudgetExceeded() {
        when(tracingService.isEnabled()).thenReturn(false);
        when(toolCallBudgetService.consume("tenant-1")).thenReturn(false);
        ToolExecutionResult result = ToolExecutionResult.success("apiCall", "ok", 100, 10);

        recorder.record(300L, result);

        // Should still delegate (just logs warning)
        verify(delegate).record(300L, result);
    }

    @Test
    void record_handlesNullTenantId() {
        TenantContextHolder.clear();
        when(tracingService.isEnabled()).thenReturn(false);
        ToolExecutionResult result = ToolExecutionResult.success("fileRead", "content", 150, 42);

        recorder.record(400L, result);

        verify(toolCallBudgetService, never()).consume(anyString());
        verify(delegate).record(400L, result);
    }
}
