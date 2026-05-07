package com.schemaplexai.common.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTypeTest {

    @Test
    void allValues_areDefined() {
        assertThat(MessageType.values()).containsExactly(
                MessageType.AGENT_RESPONSE,
                MessageType.SSE_EVENT,
                MessageType.ERROR,
                MessageType.SYSTEM,
                MessageType.CHUNK
        );
    }

    @Test
    void valueOf_returnsCorrectEnum() {
        assertThat(MessageType.valueOf("AGENT_RESPONSE")).isEqualTo(MessageType.AGENT_RESPONSE);
        assertThat(MessageType.valueOf("SSE_EVENT")).isEqualTo(MessageType.SSE_EVENT);
        assertThat(MessageType.valueOf("ERROR")).isEqualTo(MessageType.ERROR);
        assertThat(MessageType.valueOf("SYSTEM")).isEqualTo(MessageType.SYSTEM);
        assertThat(MessageType.valueOf("CHUNK")).isEqualTo(MessageType.CHUNK);
    }
}
