package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolRegistry")
class ToolRegistryTest {

    private ToolRegistry registry;
    private ToolDefinition searchTool;
    private ToolDefinition calcTool;

    @BeforeEach
    void setUp() {
        registry = new InMemoryToolRegistry();

        searchTool = new ToolDefinition(
                "web_search",
                "Search the web for information",
                List.of(
                        new ToolParameter("query", "string", "The search query", true),
                        new ToolParameter("max_results", "integer", "Maximum number of results", false)
                ),
                "SearchResult[]"
        );

        calcTool = new ToolDefinition(
                "calculator",
                "Perform mathematical calculations",
                List.of(
                        new ToolParameter("expression", "string", "Mathematical expression to evaluate", true)
                ),
                "number"
        );
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register a single tool definition")
        void shouldRegisterSingleTool() {
            registry.register(searchTool);

            assertTrue(registry.exists("web_search"));
            assertEquals(searchTool, registry.get("web_search"));
        }

        @Test
        @DisplayName("should throw when registering duplicate tool name")
        void shouldThrowOnDuplicate() {
            registry.register(searchTool);

            assertThrows(IllegalArgumentException.class, () -> registry.register(searchTool));
        }

        @Test
        @DisplayName("should throw when tool name is blank")
        void shouldThrowOnBlankName() {
            ToolDefinition badDef = new ToolDefinition("", "desc", List.of(), "string");

            assertThrows(IllegalArgumentException.class, () -> registry.register(badDef));
        }

        @Test
        @DisplayName("should throw when tool definition is null")
        void shouldThrowOnNull() {
            assertThrows(IllegalArgumentException.class, () -> registry.register(null));
        }
    }

    @Nested
    @DisplayName("registerAll")
    class RegisterAllTests {

        @Test
        @DisplayName("should register multiple tools")
        void shouldRegisterMultiple() {
            registry.registerAll(List.of(searchTool, calcTool));

            assertEquals(2, registry.getAll().size());
            assertTrue(registry.exists("web_search"));
            assertTrue(registry.exists("calculator"));
        }

        @Test
        @DisplayName("should throw if any tool in list duplicates existing")
        void shouldThrowOnPartialDuplicate() {
            registry.register(searchTool);

            assertThrows(IllegalArgumentException.class,
                    () -> registry.registerAll(List.of(searchTool, calcTool)));
        }

        @Test
        @DisplayName("should handle empty list gracefully")
        void shouldHandleEmptyList() {
            registry.registerAll(List.of());
            assertEquals(0, registry.getAll().size());
        }

        @Test
        @DisplayName("should handle null list gracefully")
        void shouldHandleNullList() {
            registry.registerAll(null);
            assertEquals(0, registry.getAll().size());
        }
    }

    @Nested
    @DisplayName("get / exists")
    class GetExistsTests {

        @Test
        @DisplayName("get should return null for unknown tool")
        void getShouldReturnNullForUnknown() {
            assertNull(registry.get("nonexistent"));
        }

        @Test
        @DisplayName("exists should return false for unknown tool")
        void existsShouldReturnFalseForUnknown() {
            assertFalse(registry.exists("nonexistent"));
        }

        @Test
        @DisplayName("get should return correct definition after registration")
        void getShouldReturnCorrectDefinition() {
            registry.register(searchTool);
            ToolDefinition def = registry.get("web_search");

            assertNotNull(def);
            assertEquals("web_search", def.name());
            assertEquals(2, def.parameters().size());
        }
    }

    @Nested
    @DisplayName("getAll / format exports")
    class ExportTests {

        @Test
        @DisplayName("getAll should return empty list when no tools registered")
        void getAllShouldReturnEmpty() {
            assertTrue(registry.getAll().isEmpty());
        }

        @Test
        @DisplayName("getAll should return all registered tools")
        void getAllShouldReturnAllTools() {
            registry.register(searchTool);
            registry.register(calcTool);

            List<ToolDefinition> all = registry.getAll();
            assertEquals(2, all.size());
        }

        @Test
        @DisplayName("getAllAsOpenAiFunctions should return correct OpenAI format")
        void getAllAsOpenAiFunctionsShouldReturnCorrectFormat() {
            registry.register(searchTool);

            List<Map<String, Object>> functions = registry.getAllAsOpenAiFunctions();
            assertEquals(1, functions.size());

            Map<String, Object> func = functions.get(0);
            assertEquals("function", func.get("type"));

            @SuppressWarnings("unchecked")
            Map<String, Object> funcDef = (Map<String, Object>) func.get("function");
            assertEquals("web_search", funcDef.get("name"));
            assertNotNull(funcDef.get("parameters"));
        }

        @Test
        @DisplayName("getAllAsAnthropicTools should return correct Anthropic format")
        void getAllAsAnthropicToolsShouldReturnCorrectFormat() {
            registry.register(calcTool);

            List<Map<String, Object>> tools = registry.getAllAsAnthropicTools();
            assertEquals(1, tools.size());

            Map<String, Object> tool = tools.get(0);
            assertEquals("calculator", tool.get("name"));
            assertEquals("Perform mathematical calculations", tool.get("description"));
            assertNotNull(tool.get("input_schema"));
        }

        @Test
        @DisplayName("export should return empty lists when no tools")
        void exportShouldReturnEmptyWhenNoTools() {
            assertTrue(registry.getAllAsOpenAiFunctions().isEmpty());
            assertTrue(registry.getAllAsAnthropicTools().isEmpty());
        }
    }

    @Nested
    @DisplayName("unregister")
    class UnregisterTests {

        @Test
        @DisplayName("should remove a registered tool")
        void shouldRemoveRegisteredTool() {
            registry.register(searchTool);
            registry.register(calcTool);

            ToolDefinition removed = registry.unregister("web_search");

            assertNotNull(removed);
            assertEquals("web_search", removed.name());
            assertFalse(registry.exists("web_search"));
            assertTrue(registry.exists("calculator"));
            assertEquals(1, registry.getAll().size());
        }

        @Test
        @DisplayName("should return null when unregistering unknown tool")
        void shouldReturnNullForUnknownTool() {
            ToolDefinition removed = registry.unregister("nonexistent");
            assertNull(removed);
        }
    }
}
