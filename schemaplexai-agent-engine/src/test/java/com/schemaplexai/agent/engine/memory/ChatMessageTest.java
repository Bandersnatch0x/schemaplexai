package com.schemaplexai.agent.engine.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessage")
class ChatMessageTest {

    @Test
    @DisplayName("should create message with role and content via constructor")
    void shouldCreateWithRoleAndContent() {
        ChatMessage message = new ChatMessage("user", "hello");

        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getContent()).isEqualTo("hello");
        assertThat(message.getTokenCount()).isNull();
    }

    @Test
    @DisplayName("should create message with all fields via all-args constructor")
    void shouldCreateWithAllFields() {
        ChatMessage message = new ChatMessage("assistant", "response", 42L);

        assertThat(message.getRole()).isEqualTo("assistant");
        assertThat(message.getContent()).isEqualTo("response");
        assertThat(message.getTokenCount()).isEqualTo(42L);
    }

    @Test
    @DisplayName("should create system message via factory method")
    void shouldCreateSystemMessage() {
        ChatMessage message = ChatMessage.system("You are helpful");

        assertThat(message.getRole()).isEqualTo("system");
        assertThat(message.getContent()).isEqualTo("You are helpful");
    }

    @Test
    @DisplayName("should create user message via factory method")
    void shouldCreateUserMessage() {
        ChatMessage message = ChatMessage.user("What is Java?");

        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getContent()).isEqualTo("What is Java?");
    }

    @Test
    @DisplayName("should create assistant message via factory method")
    void shouldCreateAssistantMessage() {
        ChatMessage message = ChatMessage.assistant("Java is a programming language.");

        assertThat(message.getRole()).isEqualTo("assistant");
        assertThat(message.getContent()).isEqualTo("Java is a programming language.");
    }

    @Test
    @DisplayName("should support no-args constructor for frameworks")
    void shouldSupportNoArgsConstructor() {
        ChatMessage message = new ChatMessage();

        assertThat(message.getRole()).isNull();
        assertThat(message.getContent()).isNull();
        assertThat(message.getTokenCount()).isNull();
    }

    @Test
    @DisplayName("should support setters and getters")
    void shouldSupportSettersAndGetters() {
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent("test");
        message.setTokenCount(10L);

        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getContent()).isEqualTo("test");
        assertThat(message.getTokenCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("should consider two messages equal when fields match")
    void shouldBeEqualWhenFieldsMatch() {
        ChatMessage msg1 = new ChatMessage("user", "hello", 5L);
        ChatMessage msg2 = new ChatMessage("user", "hello", 5L);

        assertThat(msg1).isEqualTo(msg2);
        assertThat(msg1.hashCode()).isEqualTo(msg2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
        ChatMessage msg1 = new ChatMessage("user", "hello", 5L);
        ChatMessage msg2 = new ChatMessage("assistant", "hello", 5L);

        assertThat(msg1).isNotEqualTo(msg2);
    }

    @Test
    @DisplayName("should produce meaningful toString")
    void shouldProduceMeaningfulToString() {
        ChatMessage message = new ChatMessage("user", "hello", 5L);

        assertThat(message.toString()).contains("user");
        assertThat(message.toString()).contains("hello");
    }
}
