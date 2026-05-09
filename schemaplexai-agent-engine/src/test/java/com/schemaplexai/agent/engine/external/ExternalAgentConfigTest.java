package com.schemaplexai.agent.engine.external;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExternalAgentConfigTest {

    @Test
    void defaultValues_shouldBeAsExpected() {
        ExternalAgentConfig config = new ExternalAgentConfig();

        assertFalse(config.isEnabled(), "enabled should default to false");
        assertEquals("", config.getProvider(), "provider should default to empty string");
        assertEquals("", config.getModel(), "model should default to empty string");
        assertEquals("", config.getApiKey(), "apiKey should default to empty string");
        assertEquals("", config.getBaseUrl(), "baseUrl should default to empty string");
        assertEquals(60000, config.getTimeoutMs(), "timeoutMs should default to 60000");
        assertEquals(3, config.getMaxRetries(), "maxRetries should default to 3");
    }

    @Test
    void settersAndGetters_shouldWork() {
        ExternalAgentConfig config = new ExternalAgentConfig();

        config.setEnabled(true);
        config.setProvider("codex");
        config.setModel("gpt-4o");
        config.setApiKey("sk-test");
        config.setBaseUrl("https://api.example.com");
        config.setTimeoutMs(30000);
        config.setMaxRetries(5);

        assertTrue(config.isEnabled());
        assertEquals("codex", config.getProvider());
        assertEquals("gpt-4o", config.getModel());
        assertEquals("sk-test", config.getApiKey());
        assertEquals("https://api.example.com", config.getBaseUrl());
        assertEquals(30000, config.getTimeoutMs());
        assertEquals(5, config.getMaxRetries());
    }
}
