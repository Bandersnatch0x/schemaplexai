package com.schemaplexai.agent.engine.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OpenAiProviderTest {

    @InjectMocks
    private OpenAiProvider openAiProvider;

    @Test
    void getProviderNameShouldReturnOpenAi() {
        assertEquals("OPENAI", openAiProvider.getProviderName());
    }

    @Test
    void isHealthyShouldReturnTrueByDefault() {
        assertTrue(openAiProvider.isHealthy());
    }

    @Test
    void generateShouldReturnEmptyString() {
        String result = openAiProvider.generate("prompt", "gpt-4", 0.7);
        assertEquals("", result);
    }

    @Test
    void generateWithMessagesShouldReturnEmptyString() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        String result = openAiProvider.generateWithMessages(messages, "gpt-4", 0.7);
        assertEquals("", result);
    }
}
