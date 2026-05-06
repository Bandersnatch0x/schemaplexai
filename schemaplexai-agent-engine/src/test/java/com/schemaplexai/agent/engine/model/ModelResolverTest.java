package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelResolverTest {

    @Test
    void resolveShouldUseMetadataModelIdWhenAvailable() {
        ModelResolver resolver = new ModelResolver("");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setMetadata("modelId", "claude-3-sonnet");

        String result = resolver.resolve(execution);

        assertEquals("claude-3-sonnet", result);
    }

    @Test
    void resolveShouldUseApplicationConfigWhenNoMetadata() {
        ModelResolver resolver = new ModelResolver("gpt-4o");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);

        String result = resolver.resolve(execution);

        assertEquals("gpt-4o", result);
    }

    @Test
    void resolveShouldUseFallbackWhenNoConfigAvailable() {
        ModelResolver resolver = new ModelResolver("");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);

        String result = resolver.resolve(execution);

        assertEquals("gpt-4", result);
    }

    @Test
    void resolveShouldPreferMetadataOverConfig() {
        ModelResolver resolver = new ModelResolver("gpt-4o");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setMetadata("modelId", "claude-3-haiku");

        String result = resolver.resolve(execution);

        assertEquals("claude-3-haiku", result);
    }

    @Test
    void resolveShouldNormalizeModelIdCase() {
        ModelResolver resolver = new ModelResolver("");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setMetadata("modelId", "GPT-4O");

        String result = resolver.resolve(execution);

        assertEquals("gpt-4o", result);
    }

    @Test
    void resolveShouldFallbackForUnsupportedModel() {
        ModelResolver resolver = new ModelResolver("");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setMetadata("modelId", "unsupported-model-xyz");

        String result = resolver.resolve(execution);

        assertEquals("gpt-4", result);
    }

    @Test
    void resolveShouldHandleNullExecution() {
        ModelResolver resolver = new ModelResolver("");

        String result = resolver.resolve(null);

        assertEquals("gpt-4", result);
    }

    @Test
    void resolveShouldHandleBlankConfig() {
        ModelResolver resolver = new ModelResolver("   ");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);

        String result = resolver.resolve(execution);

        assertEquals("gpt-4", result);
    }

    @Test
    void resolveShouldHandleBlankMetadataModelId() {
        ModelResolver resolver = new ModelResolver("gpt-4o");
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setMetadata("modelId", "   ");

        String result = resolver.resolve(execution);

        assertEquals("gpt-4o", result);
    }

    @Test
    void isSupportedShouldReturnTrueForKnownModels() {
        ModelResolver resolver = new ModelResolver("");

        assertTrue(resolver.isSupported("gpt-4"));
        assertTrue(resolver.isSupported("gpt-4o"));
        assertTrue(resolver.isSupported("claude-3-sonnet"));
    }

    @Test
    void isSupportedShouldReturnFalseForUnknownModels() {
        ModelResolver resolver = new ModelResolver("");

        assertFalse(resolver.isSupported("unknown-model"));
        assertFalse(resolver.isSupported(""));
        assertFalse(resolver.isSupported(null));
    }

    @Test
    void isSupportedShouldBeCaseInsensitive() {
        ModelResolver resolver = new ModelResolver("");

        assertTrue(resolver.isSupported("GPT-4"));
        assertTrue(resolver.isSupported("Claude-3-Sonnet"));
    }
}
