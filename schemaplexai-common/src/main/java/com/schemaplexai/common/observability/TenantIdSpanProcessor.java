package com.schemaplexai.common.observability;

import com.schemaplexai.common.context.TenantContextHolder;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class TenantIdSpanProcessor implements SpanProcessor {

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            span.setAttribute("tenant.id", tenantId);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // no-op
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
    }
}
