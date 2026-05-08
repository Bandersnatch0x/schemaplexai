package com.schemaplexai.common.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnProperty(name = "management.otlp.tracing.enabled", havingValue = "true", matchIfMissing = false)
public class OpenTelemetryConfig {

    @Bean
    @ConditionalOnMissingBean
    public SpanExporter otlpHttpSpanExporter(
            @Value("${management.otlp.tracing.endpoint:http://localhost:4318/v1/traces}") String endpoint) {
        return OtlpHttpSpanExporter.builder()
                .setEndpoint(endpoint)
                .build();
    }

    @Bean
    public SpanExporter piiRedactingSpanExporter(SpanExporter delegate) {
        return new PiiRedactingSpanExporter(delegate);
    }

    @Bean
    public SpanProcessor tenantIdSpanProcessor() {
        return new TenantIdSpanProcessor();
    }

    @Bean
    @ConditionalOnMissingBean
    public SdkTracerProvider sdkTracerProvider(SpanExporter piiRedactingSpanExporter,
                                                SpanProcessor tenantIdSpanProcessor,
                                                @Value("${spring.application.name:schemaplexai}") String serviceName) {
        return SdkTracerProvider.builder()
                .setResource(Resource.create(
                        Attributes.of(AttributeKey.stringKey("service.name"), serviceName)))
                .addSpanProcessor(tenantIdSpanProcessor)
                .addSpanProcessor(BatchSpanProcessor.builder(piiRedactingSpanExporter)
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .setScheduleDelay(5000, TimeUnit.MILLISECONDS)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenTelemetry openTelemetry(SdkTracerProvider sdkTracerProvider) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .build();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry,
                         @Value("${spring.application.name:schemaplexai}") String serviceName) {
        return openTelemetry.getTracer(serviceName);
    }

    @Bean
    public OpenTelemetryTracingService tracingService(Tracer tracer) {
        return new OpenTelemetryTracingService(tracer);
    }
}
