package com.schemaplexai.agent.engine.tool.parser;

import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AnthropicToolCallParserTest {

    private AnthropicToolCallParser parser;
    private LlmProvider provider;

    @BeforeEach
    void setUp() {
        parser = new AnthropicToolCallParser();
        provider = mock(LlmProvider.class);
        when(provider.getProviderName()).thenReturn("ANTHROPIC");
    }

    @Test
    void shouldReturnProviderName() {
        assertEquals("ANTHROPIC", parser.getProviderName());
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
    void shouldParseSingleToolUse() {
        String content = "<tool_use><name>file_read</name><parameter name=\"path\">/tmp/test.txt</parameter></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
        assertEquals("/tmp/test.txt", calls.get(0).parameters().get("path"));
    }

    @Test
    void shouldParseMultipleToolUses() {
        String content = "<tool_use><name>file_read</name><parameter name=\"path\">/tmp/a.txt</parameter></tool_use>" +
                "<tool_use><name>file_write</name><parameter name=\"path\">/tmp/b.txt</parameter><parameter name=\"content\">hello</parameter></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(2, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
        assertEquals("file_write", calls.get(1).toolName());
        assertEquals("hello", calls.get(1).parameters().get("content"));
    }

    @Test
    void shouldParseToolUseWithMultipleParameters() {
        String content = "<tool_use><name>web_search</name>" +
                "<parameter name=\"query\">java testing</parameter>" +
                "<parameter name=\"max_results\">10</parameter>" +
                "</tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("web_search", calls.get(0).toolName());
        assertEquals("java testing", calls.get(0).parameters().get("query"));
        assertEquals("10", calls.get(0).parameters().get("max_results"));
    }

    @Test
    void shouldSkipToolUseWithoutName() {
        String content = "<tool_use><parameter name=\"path\">/tmp/test.txt</parameter></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldSkipToolUseWithBlankName() {
        String content = "<tool_use><name>   </name><parameter name=\"path\">/tmp/test.txt</parameter></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldParseToolUseWithNewlinesAndWhitespace() {
        String content = """
                <tool_use>
                    <name>file_read</name>
                    <parameter name="path">/tmp/test.txt</parameter>
                </tool_use>
                """;

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
        assertEquals("/tmp/test.txt", calls.get(0).parameters().get("path"));
    }

    @Test
    void shouldParseToolUseWithoutParameters() {
        String content = "<tool_use><name>get_time</name></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("get_time", calls.get(0).toolName());
        assertTrue(calls.get(0).parameters().isEmpty());
    }

    @Test
    void shouldHandleContentWithoutToolUseTags() {
        String content = "Hello, how can I help you today?";

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldHandleMalformedXml() {
        String content = "<tool_use><name>file_read</name><parameter name=\"path\">/tmp/test.txt";

        List<ToolCall> calls = parser.parse(content, provider);

        assertTrue(calls.isEmpty());
    }

    @Test
    void shouldTrimParameterValues() {
        String content = "<tool_use><name>file_read</name><parameter name=\"path\">  /tmp/test.txt  </parameter></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals("/tmp/test.txt", calls.get(0).parameters().get("path"));
    }

    @Test
    void shouldTrimToolName() {
        String content = "<tool_use><name>  file_read  </name><parameter name=\"path\">/tmp/test.txt</parameter></tool_use>";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals("file_read", calls.get(0).toolName());
    }

    @Test
    void shouldHandleMixedContentWithTextAndToolUse() {
        String content = "Let me read that file for you. <tool_use><name>file_read</name><parameter name=\"path\">/tmp/test.txt</parameter></tool_use> Here is the content.";

        List<ToolCall> calls = parser.parse(content, provider);

        assertEquals(1, calls.size());
        assertEquals("file_read", calls.get(0).toolName());
    }
}
