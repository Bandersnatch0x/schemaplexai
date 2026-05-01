package com.schemaplexai.agent.engine.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnthropicProviderTest {

    @InjectMocks
    private AnthropicProvider anthropicProvider;

    @Test
    void getProviderNameShouldReturnAnthropic() {
        assertEquals("ANTHROPIC", anthropicProvider.getProviderName());
    }

    @Test
    void isHealthyShouldReturnTrueByDefault() {
        assertTrue(anthropicProvider.isHealthy());
    }

    @Test
    void generateShouldReturnEmptyString() {
        String result = anthropicProvider.generate("prompt", "claude-3", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateWithMessagesShouldReturnEmptyString() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        String result = anthropicProvider.generateWithMessages(messages, "claude-3", 0.7);
        assertEquals("", result);
    }
}
