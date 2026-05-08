package com.schemaplexai.common.observability;

import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback configuration when OpenTelemetry is not enabled.
 * Provides a no-op {@link OpenTelemetryTracingService} that creates no spans,
 * allowing business code to use the tracing API without null checks.
 */
@Configuration
@ConditionalOnMissingBean(OpenTelemetryConfig.class)
public class ObservabilityFallbackConfig {

    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetryTracingService tracingService() {
        return new OpenTelemetryTracingService(null);
    }
}
