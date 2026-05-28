package com.schemaplexai.common.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityAutoConfigurationTest {

    @Test
    void springBootApplicationAutoConfiguresNoopTracingServiceWhenOtlpTracingDisabled() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                .web(WebApplicationType.NONE)
                .properties("jwt.secret=this-is-a-very-long-test-secret-for-common")
                .run()) {

            assertThat(context.getBeansOfType(OpenTelemetryTracingService.class)).hasSize(1);
            assertThat(context.getBean(OpenTelemetryTracingService.class).isEnabled()).isFalse();
        }
    }

    @SpringBootApplication(scanBasePackages = "com.schemaplexai.common.observability.none")
    static class TestApplication {
    }
}
