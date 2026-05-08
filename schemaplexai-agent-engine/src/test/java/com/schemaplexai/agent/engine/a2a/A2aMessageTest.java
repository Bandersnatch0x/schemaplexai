package com.schemaplexai.agent.engine.a2a;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A2aMessage")
class A2aMessageTest {

    @Test
    @DisplayName("should create message with all fields via builder")
    void shouldCreateWithBuilder() {
        Instant now = Instant.now();

        A2aMessage message = A2aMessage.builder()
                .messageId("msg-001")
                .senderAgentId("agent-a")
                .recipientAgentId("agent-b")
                .messageType(A2aMessage.MessageType.REQUEST)
                .payload("{\"query\":\"hello\"}")
                .timestamp(now)
                .correlationId("corr-001")
                .build();

        assertThat(message.getMessageId()).isEqualTo("msg-001");
        assertThat(message.getSenderAgentId()).isEqualTo("agent-a");
        assertThat(message.getRecipientAgentId()).isEqualTo("agent-b");
        assertThat(message.getMessageType()).isEqualTo(A2aMessage.MessageType.REQUEST);
        assertThat(message.getPayload()).isEqualTo("{\"query\":\"hello\"}");
        assertThat(message.getTimestamp()).isEqualTo(now);
        assertThat(message.getCorrelationId()).isEqualTo("corr-001");
    }

    @Test
    @DisplayName("should create message with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        A2aMessage message = new A2aMessage();

        assertThat(message.getMessageId()).isNull();
        assertThat(message.getMessageType()).isNull();
        assertThat(message.getPayload()).isNull();
    }

    @Test
    @DisplayName("should create message with all-args constructor")
    void shouldCreateWithAllArgsConstructor() {
        Instant now = Instant.now();

        A2aMessage message = new A2aMessage(
                "msg-002",
                "agent-c",
                "agent-d",
                A2aMessage.MessageType.RESPONSE,
                "ok",
                now,
                "corr-002"
        );

        assertThat(message.getMessageType()).isEqualTo(A2aMessage.MessageType.RESPONSE);
        assertThat(message.getPayload()).isEqualTo("ok");
    }

    @Test
    @DisplayName("should support all message types")
    void shouldSupportAllMessageTypes() {
        assertThat(A2aMessage.MessageType.REQUEST.name()).isEqualTo("REQUEST");
        assertThat(A2aMessage.MessageType.RESPONSE.name()).isEqualTo("RESPONSE");
        assertThat(A2aMessage.MessageType.STREAM.name()).isEqualTo("STREAM");
        assertThat(A2aMessage.MessageType.ERROR.name()).isEqualTo("ERROR");
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        A2aMessage message = new A2aMessage();
        message.setMessageId("msg-003");
        message.setSenderAgentId("agent-e");
        message.setRecipientAgentId("agent-f");
        message.setMessageType(A2aMessage.MessageType.ERROR);
        message.setPayload("error");
        message.setTimestamp(Instant.EPOCH);
        message.setCorrelationId("corr-003");

        assertThat(message.getMessageId()).isEqualTo("msg-003");
        assertThat(message.getMessageType()).isEqualTo(A2aMessage.MessageType.ERROR);
        assertThat(message.getTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    @DisplayName("should consider two messages equal when fields match")
    void shouldBeEqualWhenFieldsMatch() {
        Instant now = Instant.now();

        A2aMessage msg1 = A2aMessage.builder()
                .messageId("m1")
                .senderAgentId("a")
                .recipientAgentId("b")
                .messageType(A2aMessage.MessageType.REQUEST)
                .payload("p")
                .timestamp(now)
                .correlationId("c1")
                .build();

        A2aMessage msg2 = A2aMessage.builder()
                .messageId("m1")
                .senderAgentId("a")
                .recipientAgentId("b")
                .messageType(A2aMessage.MessageType.REQUEST)
                .payload("p")
                .timestamp(now)
                .correlationId("c1")
                .build();

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
        A2aMessage msg1 = A2aMessage.builder().messageId("m1").payload("p1").build();
        A2aMessage msg2 = A2aMessage.builder().messageId("m1").payload("p2").build();

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    @DisplayName("should produce meaningful toString")
    void shouldProduceMeaningfulToString() {
        A2aMessage message = A2aMessage.builder()
                .messageId("m1")
                .messageType(A2aMessage.MessageType.REQUEST)
                .build();

        assertThat(message.toString()).contains("m1");
        assertThat(message.toString()).contains("REQUEST");
    }
}
