package com.schemaplexai.common.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Fallback configuration when OpenTelemetry is not enabled.
 * Provides a no-op {@link OpenTelemetryTracingService} that creates no spans,
 * allowing business code to use the tracing API without null checks.
 */
@AutoConfiguration(after = OpenTelemetryConfig.class)
public class ObservabilityFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(OpenTelemetryTracingService.class)
    public OpenTelemetryTracingService tracingService() {
        return new OpenTelemetryTracingService(null);
    }
}
