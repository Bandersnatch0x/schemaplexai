package com.schemaplexai.common.observability;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(afterName = "org.springframework.boot.actuate.autoconfigure.tracing.OpenTelemetryAutoConfiguration")
public class OpenTelemetryConfig {

    @Bean
    @ConditionalOnMissingBean(TenantIdSpanProcessor.class)
    public SpanProcessor tenantIdSpanProcessor() {
        return new TenantIdSpanProcessor();
    }

    @Bean
    @ConditionalOnBean(Tracer.class)
    @ConditionalOnMissingBean(OpenTelemetryTracingService.class)
    public OpenTelemetryTracingService tracingService(Tracer tracer) {
        return new OpenTelemetryTracingService(tracer);
    }
}
