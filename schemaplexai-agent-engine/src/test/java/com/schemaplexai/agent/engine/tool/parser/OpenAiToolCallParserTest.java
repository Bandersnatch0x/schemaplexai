package com.schemaplexai.agent.engine.tool.parser;

import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAiToolCallParserTest {

    private OpenAiToolCallParser parser;
    private LlmProvider provider;

    @BeforeEach
    void setUp() {
        parser = new OpenAiToolCallParser();
        provider = mock(LlmProvider.class);
        when(provider.getProviderName()).thenReturn("OPENAI");
    }

    @Test
    void shouldReturnProviderName() {
        assertEquals("OPENAI", parser.getProviderName());
    }

    @Test
    void shouldReturnEmptyListForNullContent() {
        List<ToolCall> calls = parser.parse(null, provider);
        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForBlankContent() {
        List<ToolCall> calls = parser.parse("   ", provider);
        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForEmptyString() {
        List<ToolCall> calls = parser.parse("", provider);
        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldParseSingleToolCall() {
        String content = """
                {"tool_calls":[{"id":"call_1","type":"function","function":{"name":"file_read","arguments":"{\\"path\\":\\"/tmp/test.txt\\"}"}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
        assertEquals("/tmp/test.txt", calls.get(0).parameters().get("path"));
    }

    @Test
    void shouldParseMultipleToolCalls() {
        String content = """
                {"tool_calls":[
                    {"id":"call_1","type":"function","function":{"name":"file_read","arguments":"{\\"path\\":\\"/tmp/a.txt\\"}"}},
                    {"id":"call_2","type":"function","function":{"name":"file_write","arguments":"{\\"path\\":\\"/tmp/b.txt\\",\\"content\\":\\"hello\\"}"}}
                ]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(2, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
        assertEquals("file_write", calls.get(1).toolName());
        assertEquals("hello", calls.get(1).parameters().get("content"));
    }

    @Test
    void shouldParseToolCallWithObjectArguments() {
        // Some LLM providers send arguments as a JSON object node directly
        // instead of a JSON-encoded string.
        String content = "{\"tool_calls\":[{\"function\":{\"name\":\"web_search\",\"arguments\":{\"query\":\"java testing\",\"max_results\":\"10\"}}}]}";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("web_search", calls.get(0).toolName());
        assertEquals("java testing", calls.get(0).parameters().get("query"));
        assertEquals("10", calls.get(0).parameters().get("max_results"));
    }

    @Test
    void shouldSkipToolCallWithoutFunction() {
        String content = """
                {"tool_calls":[{"id":"call_1","type":"function"}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldSkipToolCallWithoutName() {
        String content = """
                {"tool_calls":[{"function":{"arguments":"{\\"path\\":\\"/tmp/test.txt\\"}"}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldSkipToolCallWithBlankName() {
        String content = """
                {"tool_calls":[{"function":{"name":"","arguments":"{\\"path\\":\\"/tmp/test.txt\\"}"}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldHandleToolCallsFieldMissing() {
        String content = """
                {"other_field":"value"}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldHandleToolCallsNotArray() {
        String content = """
                {"tool_calls":"not an array"}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldHandleEmptyToolCallsArray() {
        String content = """
                {"tool_calls":[]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldHandleNullArguments() {
        String content = """
                {"tool_calls":[{"function":{"name":"get_time","arguments":null}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("get_time", calls.get(0).toolName());
        assertTrue(calls.get(0).parameters().isEmpty());
    }

    @Test
    void shouldHandleMissingArguments() {
        String content = """
                {"tool_calls":[{"function":{"name":"get_time"}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("get_time", calls.get(0).toolName());
        assertTrue(calls.get(0).parameters().isEmpty());
    }

    @Test
    void shouldHandleInvalidJsonArguments() {
        String content = """
                {"tool_calls":[{"function":{"name":"file_read","arguments":"not valid json"}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
        assertTrue(calls.get(0).parameters().isEmpty());
    }

    @Test
    void shouldHandleInvalidJson() {
        String content = "not valid json at all";

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldHandleEmptyJsonObject() {
        String content = "{}";

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldParseArgumentsWithSpecialCharacters() {
        String content = "{\"tool_calls\":[{\"function\":{\"name\":\"file_read\",\"arguments\":\"{\\\"path\\\":\\\"/tmp/file with spaces.txt\\\"}\"}}]}";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("/tmp/file with spaces.txt", calls.get(0).parameters().get("path"));
    }

    @Test
    void shouldParseNestedJsonArguments() {
        String content = """
                {"tool_calls":[{"function":{"name":"api_call","arguments":"{\\"config\\":{\\"timeout\\":30},\\"url\\":\\"http://example.com\\"}"}}]}
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("api_call", calls.get(0).toolName());
        assertEquals("http://example.com", calls.get(0).parameters().get("url"));
    }
}
