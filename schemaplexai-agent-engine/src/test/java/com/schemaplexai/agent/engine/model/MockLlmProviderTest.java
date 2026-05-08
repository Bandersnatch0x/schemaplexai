package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockLlmProviderTest {

    private MockLlmProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockLlmProvider();
    }

    @Test
    void getProviderNameShouldReturnMock() {
        assertEquals("MOCK", provider.getProviderName());
    }

    @Test
    void isHealthyShouldAlwaysReturnTrue() {
        assertTrue(provider.isHealthy());
    }

    @Test
    void generateShouldReturnNonEmptyResponse() {
        String response = provider.generate("test prompt", "mock-model", 0.7);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    @Test
    void generateWithMessagesShouldReturnNonEmptyResponse() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "You are a helpful assistant"),
                new LlmMessage("user", "Analyze the architecture")
        );
        String response = provider.generateWithMessages(messages, "mock-model", 0.7);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    @Test
    void generateWithToolsShouldReturnNonEmptyResponse() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "Review the code")
        );
        List<ToolDefinition> tools = List.of(
                new ToolDefinition("read_file", "Read a file from the codebase",
                        List.of(new ToolParameter("path", "string", "File path", true)),
                        "string")
        );
        String response = provider.generateWithTools(messages, tools, "mock-model", 0.7);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }

    @Test
    void generateShouldCycleThroughThreeDemoResponses() {
        String response1 = provider.generate("q1", null, null);
        String response2 = provider.generate("q2", null, null);
        String response3 = provider.generate("q3", null, null);

        // All three should be distinct
        assertNotEquals(response1, response2);
        assertNotEquals(response2, response3);
        assertNotEquals(response1, response3);
    }

    @Test
    void responsesShouldContainReActOrFinalAnswerFormatting() {
        // Collect all 3 demo responses
        String response1 = provider.generate("q1", null, null);
        String response2 = provider.generate("q2", null, null);
        String response3 = provider.generate("q3", null, null);

        // Response 1 and 2 should contain ReAct "Thought:" / "Action:" formatting
        assertTrue(response1.contains("Thought:"), "Response 1 should use ReAct format");
        assertTrue(response1.contains("Action:"), "Response 1 should specify an action");
        assertTrue(response2.contains("Thought:"), "Response 2 should use ReAct format");
        assertTrue(response2.contains("Action:"), "Response 2 should specify an action");

        // Response 3 should be a direct Final Answer
        assertTrue(response3.contains("Final Answer:"), "Response 3 should use Final Answer format");
    }

    @Test
    void roundRobinShouldWrapAround() {
        // Generate 6 responses (2 full cycles) and verify the 4th matches the 1st
        String first = provider.generate("a", null, null);
        provider.generate("b", null, null);
        provider.generate("c", null, null);
        String fourth = provider.generate("d", null, null);

        assertEquals(first, fourth, "Round-robin should wrap after 3 responses");
    }

    @Test
    void generateShouldHandleNullMessagesGracefully() {
        // Should not throw even with null messages list
        String response = provider.generateWithMessages(null, "mock-model", 0.7);
        assertNotNull(response);
    }

    @Test
    void generateWithToolsShouldHandleNullToolsList() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "hello"));
        String response = provider.generateWithTools(messages, null, "mock-model", 0.7);
        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}
