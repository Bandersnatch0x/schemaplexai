package com.schemaplexai.agent.engine.orchestrator;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentMessageTest {

    @Test
    void shouldCreateRecordWithAllFields() {
        Instant now = Instant.now();
        Map<String, Object> metadata = Map.of("key", "value");

        AgentMessage message = new AgentMessage(
                "agent-1", "agent-2", "subtask", "do something", metadata, now
        );

        assertThat(message.fromAgentId()).isEqualTo("agent-1");
        assertThat(message.toAgentId()).isEqualTo("agent-2");
        assertThat(message.messageType()).isEqualTo("subtask");
        assertThat(message.content()).isEqualTo("do something");
        assertThat(message.metadata()).containsEntry("key", "value");
        assertThat(message.timestamp()).isEqualTo(now);
    }

    @Test
    void shouldDefaultTimestampToNow() {
        Instant before = Instant.now();

        AgentMessage message = new AgentMessage(
                "agent-1", "agent-2", "subtask", "content", null, null
        );

        assertThat(message.timestamp()).isBetween(before, Instant.now());
    }

    @Test
    void shouldDefaultMetadataToEmptyMap() {
        AgentMessage message = new AgentMessage(
                "agent-1", "agent-2", "subtask", "content", null, Instant.now()
        );

        assertThat(message.metadata()).isEmpty();
    }

    @Test
    void shouldRejectBlankFromAgentId() {
        assertThatThrownBy(() ->
                new AgentMessage("", "agent-2", "subtask", "content", null, Instant.now())
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromAgentId");
    }

    @Test
    void shouldRejectNullFromAgentId() {
        assertThatThrownBy(() ->
                new AgentMessage(null, "agent-2", "subtask", "content", null, Instant.now())
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromAgentId");
    }

    @Test
    void shouldRejectBlankToAgentId() {
        assertThatThrownBy(() ->
                new AgentMessage("agent-1", "", "subtask", "content", null, Instant.now())
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toAgentId");
    }

    @Test
    void shouldRejectBlankMessageType() {
        assertThatThrownBy(() ->
                new AgentMessage("agent-1", "agent-2", "", "content", null, Instant.now())
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("messageType");
    }

    @Test
    void subtaskFactoryShouldCreateCorrectMessage() {
        AgentMessage message = AgentMessage.subtask("coordinator", "coder", "write a function");

        assertThat(message.fromAgentId()).isEqualTo("coordinator");
        assertThat(message.toAgentId()).isEqualTo("coder");
        assertThat(message.messageType()).isEqualTo("subtask");
        assertThat(message.content()).isEqualTo("write a function");
    }

    @Test
    void resultFactoryShouldCreateCorrectMessage() {
        AgentMessage message = AgentMessage.result("coder", "coordinator", "done");

        assertThat(message.messageType()).isEqualTo("result");
    }

    @Test
    void errorFactoryShouldCreateCorrectMessage() {
        AgentMessage message = AgentMessage.error("coder", "coordinator", "failed");

        assertThat(message.messageType()).isEqualTo("error");
    }

    @Test
    void recordsWithSameFieldsShouldBeEqual() {
        Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");

        AgentMessage msg1 = new AgentMessage("a", "b", "type", "content", Map.of(), fixedTime);
        AgentMessage msg2 = new AgentMessage("a", "b", "type", "content", Map.of(), fixedTime);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }

    @Test
    void recordsWithDifferentFieldsShouldNotBeEqual() {
        Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");

        AgentMessage msg1 = new AgentMessage("a", "b", "type", "content1", Map.of(), fixedTime);
        AgentMessage msg2 = new AgentMessage("a", "b", "type", "content2", Map.of(), fixedTime);

        assertThat(msg1).isNotEqualTo(msg2);
    }
}
