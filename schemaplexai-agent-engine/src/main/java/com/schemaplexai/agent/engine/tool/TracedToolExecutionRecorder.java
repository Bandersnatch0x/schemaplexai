package com.schemaplexai.agent.engine.tool;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.observability.OpenTelemetryTracingService;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Wraps {@link ToolExecutionRecorder} with OpenTelemetry span creation
 * and per-tenant tool-call budget enforcement.
 *
 * <p>Each tool call produces a child span with:
 * <ul>
 *   <li>{@code tool.name} — the tool that was called</li>
 *   <li>{@code tool.success} — whether it succeeded</li>
 *   <li>{@code tool.blocked} — whether it was blocked by safety guard</li>
 *   <li>{@code tool.latency_ms} — execution latency</li>
 *   <li>{@code tool.token_count} — tokens consumed</li>
 *   <li>{@code tool.error_category} — error category if failed</li>
 * </ul>
 *
 * <p>When OTel is not configured, all operations are transparently passed through
 * to the underlying recorder without span overhead.
 *
 * <p>Each successful tool call also consumes one unit from the tenant's daily
 * tool-call budget via {@link ToolCallBudgetService}.
 */
@Slf4j
@Component
public class TracedToolExecutionRecorder {

    private final ToolExecutionRecorder delegate;
    private final OpenTelemetryTracingService tracingService;
    private final ToolCallBudgetService toolCallBudgetService;

    public TracedToolExecutionRecorder(ToolExecutionRecorder delegate,
                                       OpenTelemetryTracingService tracingService,
                                       ToolCallBudgetService toolCallBudgetService) {
        this.delegate = delegate;
        this.tracingService = tracingService;
        this.toolCallBudgetService = toolCallBudgetService;
    }

    /**
     * Record a tool execution result with distributed tracing and budget tracking.
     *
     * @param executionId the agent execution ID
     * @param result      the tool execution result
     */
    public void record(Long executionId, ToolExecutionResult result) {
        // Consume tenant-level tool-call budget (across all executions)
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            boolean withinBudget = toolCallBudgetService.consume(tenantId);
            if (!withinBudget) {
                log.warn("Tenant {} daily tool-call budget exceeded", tenantId);
            }
        }

        if (tracingService.isEnabled()) {
            tracingService.runInSpan("tool-call:" + result.toolName(), span -> {
                setSpanAttributes(span, executionId, result);
                delegate.record(executionId, result);
            });
        } else {
            delegate.record(executionId, result);
        }
    }

    private void setSpanAttributes(Span span, Long executionId, ToolExecutionResult result) {
        span.setAttribute(AttributeKey.stringKey("execution.id"), String.valueOf(executionId));
        span.setAttribute(AttributeKey.stringKey("tool.name"), result.toolName());
        span.setAttribute(AttributeKey.booleanKey("tool.success"), result.success());
        span.setAttribute(AttributeKey.booleanKey("tool.blocked"), result.blocked());
        span.setAttribute(AttributeKey.longKey("tool.latency_ms"), result.latencyMs());
        span.setAttribute(AttributeKey.longKey("tool.token_count"), result.tokenCount());

        if (result.errorCategory() != null) {
            span.setAttribute(AttributeKey.stringKey("tool.error_category"),
                    result.errorCategory().name());
        }
        if (result.errorMessage() != null) {
            span.setAttribute(AttributeKey.stringKey("tool.error_message"), result.errorMessage());
        }
    }
}
