package com.schemaplexai.agent.engine.tool.registry;

import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.agent.engine.tool.parser.ToolCallParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ToolRegistry (Spring Component)")
class ToolRegistryComponentTest {

    private ToolAdapter mockAdapter1;
    private ToolAdapter mockAdapter2;
    private ToolCallParser mockParser;
    private LlmProvider mockProvider;

    @BeforeEach
    void setUp() {
        mockAdapter1 = mock(ToolAdapter.class);
        when(mockAdapter1.getToolName()).thenReturn("fileRead");

        mockAdapter2 = mock(ToolAdapter.class);
        when(mockAdapter2.getToolName()).thenReturn("httpCall");

        mockParser = mock(ToolCallParser.class);
        when(mockParser.getProviderName()).thenReturn("openai");

        mockProvider = mock(LlmProvider.class);
        when(mockProvider.getProviderName()).thenReturn("openai");
    }

    @Nested
    @DisplayName("constructor auto-discovery")
    class ConstructorTests {

        @Test
        @DisplayName("should auto-discover adapters and parsers from Spring beans")
        void shouldAutoDiscoverBeans() {
            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1, mockAdapter2),
                    List.of(mockParser)
            );

            assertEquals(2, registry.getAdapterCount());
            assertTrue(registry.isRegistered("fileRead"));
            assertTrue(registry.isRegistered("httpCall"));
        }

        @Test
        @DisplayName("should handle empty adapter and parser lists")
        void shouldHandleEmptyLists() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(),
                    Collections.emptyList()
            );

            assertEquals(0, registry.getAdapterCount());
            assertTrue(registry.getRegisteredToolNames().isEmpty());
        }
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register a tool adapter by name")
        void shouldRegisterAdapter() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), Collections.emptyList()
            );
            registry.register(mockAdapter1);

            assertTrue(registry.isRegistered("fileRead"));
            assertEquals(1, registry.getAdapterCount());
        }

        @Test
        @DisplayName("should overwrite adapter with same name")
        void shouldOverwriteDuplicate() {
            ToolAdapter anotherAdapter = mock(ToolAdapter.class);
            when(anotherAdapter.getToolName()).thenReturn("fileRead");

            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), Collections.emptyList()
            );
            registry.register(mockAdapter1);
            registry.register(anotherAdapter);

            assertEquals(1, registry.getAdapterCount());
            assertSame(anotherAdapter, registry.resolve("fileRead"));
        }
    }

    @Nested
    @DisplayName("resolve")
    class ResolveTests {

        @Test
        @DisplayName("should resolve registered tool to its adapter")
        void shouldResolveRegisteredTool() {
            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1), Collections.emptyList()
            );

            ToolAdapter resolved = registry.resolve("fileRead");
            assertSame(mockAdapter1, resolved);
        }

        @Test
        @DisplayName("should return null for unregistered tool")
        void shouldReturnNullForUnknown() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), Collections.emptyList()
            );

            assertNull(registry.resolve("unknownTool"));
        }
    }

    @Nested
    @DisplayName("isRegistered")
    class IsRegisteredTests {

        @Test
        @DisplayName("should return true for registered tool")
        void shouldReturnTrueForRegistered() {
            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1), Collections.emptyList()
            );

            assertTrue(registry.isRegistered("fileRead"));
        }

        @Test
        @DisplayName("should return false for unregistered tool")
        void shouldReturnFalseForUnregistered() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), Collections.emptyList()
            );

            assertFalse(registry.isRegistered("nonexistent"));
        }
    }

    @Nested
    @DisplayName("parse")
    class ParseTests {

        @Test
        @DisplayName("should return empty list for null content")
        void shouldReturnEmptyForNullContent() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), List.of(mockParser)
            );

            List<ToolCall> calls = registry.parse(null, mockProvider);
            assertThat(calls).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank content")
        void shouldReturnEmptyForBlankContent() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), List.of(mockParser)
            );

            List<ToolCall> calls = registry.parse("   ", mockProvider);
            assertThat(calls).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when no parser matches provider")
        void shouldReturnEmptyWhenNoParser() {
            LlmProvider unknownProvider = mock(LlmProvider.class);
            when(unknownProvider.getProviderName()).thenReturn("unknown");

            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), List.of(mockParser)
            );

            List<ToolCall> calls = registry.parse("some content", unknownProvider);
            assertThat(calls).isEmpty();
        }

        @Test
        @DisplayName("should parse and filter only registered tools")
        void shouldParseAndFilterRegisteredTools() {
            ToolCall registeredCall = new ToolCall("fileRead", Map.of("path", "/tmp"));
            ToolCall unregisteredCall = new ToolCall("hackerTool", Map.of());

            when(mockParser.parse("content", mockProvider))
                    .thenReturn(List.of(registeredCall, unregisteredCall));

            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1), List.of(mockParser)
            );

            List<ToolCall> calls = registry.parse("content", mockProvider);
            assertThat(calls).hasSize(1);
            assertThat(calls.get(0).toolName()).isEqualTo("fileRead");
        }

        @Test
        @DisplayName("should return empty list when parser throws exception")
        void shouldReturnEmptyOnParserException() {
            when(mockParser.parse(anyString(), any()))
                    .thenThrow(new RuntimeException("Parse error"));

            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), List.of(mockParser)
            );

            List<ToolCall> calls = registry.parse("bad content", mockProvider);
            assertThat(calls).isEmpty();
        }

        @Test
        @DisplayName("should return all calls when all tools are registered")
        void shouldReturnAllWhenAllRegistered() {
            ToolCall call1 = new ToolCall("fileRead", Map.of("path", "/a"));
            ToolCall call2 = new ToolCall("httpCall", Map.of("url", "http://x"));

            when(mockParser.parse("content", mockProvider))
                    .thenReturn(List.of(call1, call2));

            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1, mockAdapter2), List.of(mockParser)
            );

            List<ToolCall> calls = registry.parse("content", mockProvider);
            assertThat(calls).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getRegisteredToolNames")
    class GetRegisteredToolNamesTests {

        @Test
        @DisplayName("should return all registered tool names")
        void shouldReturnAllNames() {
            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1, mockAdapter2), Collections.emptyList()
            );

            List<String> names = registry.getRegisteredToolNames();
            assertThat(names).containsExactlyInAnyOrder("fileRead", "httpCall");
        }

        @Test
        @DisplayName("should return empty list when no tools registered")
        void shouldReturnEmptyWhenNone() {
            ToolRegistry registry = new ToolRegistry(
                    Collections.emptyList(), Collections.emptyList()
            );

            assertThat(registry.getRegisteredToolNames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAdapterCount")
    class GetAdapterCountTests {

        @Test
        @DisplayName("should return correct count")
        void shouldReturnCorrectCount() {
            ToolRegistry registry = new ToolRegistry(
                    List.of(mockAdapter1, mockAdapter2), Collections.emptyList()
            );

            assertEquals(2, registry.getAdapterCount());
        }
    }
}
