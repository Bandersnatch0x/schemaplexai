package com.schemaplexai.model.entity.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ObservabilityTrace")
class ObservabilityTraceTest {

    @Test
    @DisplayName("should create with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        ObservabilityTrace trace = new ObservabilityTrace();
        assertThat(trace.getTraceId()).isNull();
        assertThat(trace.getName()).isNull();
        assertThat(trace.getUserId()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setTraceId("trace-1");
        trace.setName("agent-execution");
        trace.setUserId("user-1");
        trace.setSessionId("session-1");
        trace.setInput("input");
        trace.setOutput("output");
        trace.setMetadata("{}");
        trace.setTags("[\"tag1\"]");
        trace.setVersion("1.0");

        assertThat(trace.getTraceId()).isEqualTo("trace-1");
        assertThat(trace.getName()).isEqualTo("agent-execution");
        assertThat(trace.getUserId()).isEqualTo("user-1");
        assertThat(trace.getSessionId()).isEqualTo("session-1");
        assertThat(trace.getInput()).isEqualTo("input");
        assertThat(trace.getOutput()).isEqualTo("output");
        assertThat(trace.getMetadata()).isEqualTo("{}");
        assertThat(trace.getTags()).isEqualTo("[\"tag1\"]");
        assertThat(trace.getVersion()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("should inherit BaseEntity fields")
    void shouldInheritBaseEntityFields() {
        ObservabilityTrace trace = new ObservabilityTrace();
        trace.setId(1L);
        trace.setTenantId("t1");

        assertThat(trace.getId()).isEqualTo(1L);
        assertThat(trace.getTenantId()).isEqualTo("t1");
    }
}
