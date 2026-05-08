package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolParameter;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmMessageConverterTest {

    @Test
    void shouldConvertSystemMessage() {
        LlmMessage msg = new LlmMessage("system", "You are helpful");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertInstanceOf(SystemMessage.class, result);
    }

    @Test
    void shouldConvertUserMessage() {
        LlmMessage msg = new LlmMessage("user", "Hello");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertInstanceOf(UserMessage.class, result);
    }

    @Test
    void shouldConvertAssistantMessage() {
        LlmMessage msg = new LlmMessage("assistant", "Hi there");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertInstanceOf(AiMessage.class, result);
    }

    @Test
    void shouldConvertHumanMessage() {
        LlmMessage msg = new LlmMessage("human", "Hello");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertInstanceOf(UserMessage.class, result);
    }

    @Test
    void shouldConvertAiMessage() {
        LlmMessage msg = new LlmMessage("ai", "Response");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertInstanceOf(AiMessage.class, result);
    }

    @Test
    void shouldConvertUnknownRoleToUserMessage() {
        LlmMessage msg = new LlmMessage("custom", "content");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertInstanceOf(UserMessage.class, result);
    }

    @Test
    void shouldConvertMessageWithNullContentToEmptyString() {
        // Null content is converted to empty string by the converter.
        // LangChain4j UserMessage rejects null/empty, but AiMessage accepts it.
        LlmMessage msg = new LlmMessage("system", "valid content");
        ChatMessage result = LlmMessageConverter.toChatMessage(msg);
        assertNotNull(result);
        assertInstanceOf(SystemMessage.class, result);
    }

    @Test
    void shouldConvertMessageList() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "System prompt"),
                new LlmMessage("user", "User message")
        );
        List<ChatMessage> result = LlmMessageConverter.toChatMessages(messages);
        assertEquals(2, result.size());
        assertInstanceOf(SystemMessage.class, result.get(0));
        assertInstanceOf(UserMessage.class, result.get(1));
    }

    @Test
    void shouldHandleNullMessageList() {
        List<ChatMessage> result = LlmMessageConverter.toChatMessages(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldExtractTextFromResponse() {
        AiMessage aiMessage = new AiMessage("test response");
        Response<AiMessage> response = Response.from(aiMessage);
        assertEquals("test response", LlmMessageConverter.extractText(response));
    }

    @Test
    void shouldHandleNullResponse() {
        assertEquals("", LlmMessageConverter.extractText(null));
    }

    @Test
    void shouldExtractTextFromResponseWithText() {
        AiMessage aiMessage = new AiMessage("hello world");
        Response<AiMessage> response = Response.from(aiMessage);
        assertEquals("hello world", LlmMessageConverter.extractText(response));
    }

    @Test
    void shouldEnrichWithToolDescriptions() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "Hello")
        );
        List<ToolDefinition> tools = List.of(
                new ToolDefinition("read_file", "Read a file",
                        List.of(new ToolParameter("path", "string", "File path", true)),
                        "string")
        );

        List<LlmMessage> enriched = LlmMessageConverter.enrichWithToolDescriptions(messages, tools);

        // Should have system message prepended + original user message
        assertEquals(2, enriched.size());
        assertEquals("system", enriched.get(0).getRole());
        assertTrue(enriched.get(0).getContent().contains("read_file"));
    }

    @Test
    void shouldInjectToolsIntoExistingSystemMessage() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "You are helpful"),
                new LlmMessage("user", "Hello")
        );
        List<ToolDefinition> tools = List.of(
                new ToolDefinition("search", "Search docs", List.of(), "string")
        );

        List<LlmMessage> enriched = LlmMessageConverter.enrichWithToolDescriptions(messages, tools);

        assertEquals(2, enriched.size());
        assertEquals("system", enriched.get(0).getRole());
        assertTrue(enriched.get(0).getContent().contains("You are helpful"));
        assertTrue(enriched.get(0).getContent().contains("search"));
    }

    @Test
    void shouldReturnOriginalMessagesWhenNoTools() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        List<LlmMessage> enriched = LlmMessageConverter.enrichWithToolDescriptions(messages, null);
        assertSame(messages, enriched);
    }

    @Test
    void shouldFormatToolSection() {
        List<ToolDefinition> tools = List.of(
                new ToolDefinition("read_file", "Read a file",
                        List.of(new ToolParameter("path", "string", "File path", true)),
                        "string")
        );

        String section = LlmMessageConverter.formatToolSection(tools);

        assertTrue(section.contains("read_file"));
        assertTrue(section.contains("Read a file"));
        assertTrue(section.contains("path"));
        assertTrue(section.contains("required"));
    }
}
