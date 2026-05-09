package com.schemaplexai.model.entity.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilitySpan")
class ObservabilitySpanTest {

    @Test
    @DisplayName("should create with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        ObservabilitySpan span = new ObservabilitySpan();
        assertThat(span.getSpanId()).isNull();
        assertThat(span.getTraceId()).isNull();
        assertThat(span.getName()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setSpanId("span-1");
        span.setTraceId("trace-1");
        span.setParentSpanId("parent-1");
        span.setName("tool-call");
        span.setType("LLM");
        span.setStartTime(1000L);
        span.setEndTime(2000L);
        span.setInput("input");
        span.setOutput("output");
        span.setMetadata("{}");
        span.setStatus("OK");
        span.setModel("gpt-4");
        span.setModelParameters("{temp:0.7}");
        span.setUsageDetails("{tokens:100}");
        span.setCostDetails("{cost:0.01}");
        span.setPromptName("default");
        span.setPromptVersion("1.0");

        assertThat(span.getSpanId()).isEqualTo("span-1");
        assertThat(span.getTraceId()).isEqualTo("trace-1");
        assertThat(span.getParentSpanId()).isEqualTo("parent-1");
        assertThat(span.getName()).isEqualTo("tool-call");
        assertThat(span.getType()).isEqualTo("LLM");
        assertThat(span.getStartTime()).isEqualTo(1000L);
        assertThat(span.getEndTime()).isEqualTo(2000L);
        assertThat(span.getInput()).isEqualTo("input");
        assertThat(span.getOutput()).isEqualTo("output");
        assertThat(span.getMetadata()).isEqualTo("{}");
        assertThat(span.getStatus()).isEqualTo("OK");
        assertThat(span.getModel()).isEqualTo("gpt-4");
        assertThat(span.getModelParameters()).isEqualTo("{temp:0.7}");
        assertThat(span.getUsageDetails()).isEqualTo("{tokens:100}");
        assertThat(span.getCostDetails()).isEqualTo("{cost:0.01}");
        assertThat(span.getPromptName()).isEqualTo("default");
        assertThat(span.getPromptVersion()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("should inherit BaseEntity fields")
    void shouldInheritBaseEntityFields() {
        ObservabilitySpan span = new ObservabilitySpan();
        span.setId(1L);
        span.setTenantId("t1");

        assertThat(span.getId()).isEqualTo(1L);
        assertThat(span.getTenantId()).isEqualTo("t1");
    }
}
