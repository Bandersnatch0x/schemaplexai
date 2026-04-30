package com.schemaplexai.common.message;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;

class UnifiedMessageTest {

    @Test
    void shouldBuildAgentResponseMessage() {
        long now = Instant.now().toEpochMilli();
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.AGENT_RESPONSE)
            .source("agent-engine")
            .target("web-client-1")
            .payload("{\"output\":\"Hello, world!\"}")
            .timestamp(now)
            .traceId("trace-001")
            .build();

        assertThat(msg.getType()).isEqualTo(MessageType.AGENT_RESPONSE);
        assertThat(msg.getSource()).isEqualTo("agent-engine");
        assertThat(msg.getTarget()).isEqualTo("web-client-1");
        assertThat(msg.getPayload()).contains("Hello");
        assertThat(msg.getTimestamp()).isEqualTo(now);
        assertThat(msg.getTraceId()).isEqualTo("trace-001");
    }

    @Test
    void shouldHandleSseEventMessage() {
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.SSE_EVENT)
            .source("web")
            .target("sse-subscriber-1")
            .eventName("agent-step")
            .payload("{\"step\":\"thinking\",\"content\":\"...\"}")
            .timestamp(Instant.now().toEpochMilli())
            .traceId("trace-002")
            .build();

        assertThat(msg.getEventName()).isEqualTo("agent-step");
        assertThat(msg.getType()).isEqualTo(MessageType.SSE_EVENT);
        assertThat(msg.getTraceId()).isEqualTo("trace-002");
    }

    @Test
    void shouldSupportErrorMessageType() {
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.ERROR)
            .source("system")
            .target("web-client-1")
            .payload("{\"error\":\"Connection timeout\"}")
            .timestamp(Instant.now().toEpochMilli())
            .build();

        assertThat(msg.getType()).isEqualTo(MessageType.ERROR);
        assertThat(msg.getSource()).isEqualTo("system");
    }

    @Test
    void shouldSupportChunkMessageType() {
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.CHUNK)
            .source("agent-engine")
            .target("web-client-1")
            .payload("{\"chunk\":\"partial data\"}")
            .timestamp(Instant.now().toEpochMilli())
            .build();

        assertThat(msg.getType()).isEqualTo(MessageType.CHUNK);
    }

    @Test
    void shouldSupportSystemMessageType() {
        UnifiedMessage msg = UnifiedMessage.builder()
            .type(MessageType.SYSTEM)
            .source("gateway")
            .target("all-clients")
            .payload("{\"notice\":\"Maintenance scheduled\"}")
            .timestamp(Instant.now().toEpochMilli())
            .build();

        assertThat(msg.getType()).isEqualTo(MessageType.SYSTEM);
    }
}
