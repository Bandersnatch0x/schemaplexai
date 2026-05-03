package com.schemaplexai.agent.engine.prompt;

import com.schemaplexai.agent.engine.tool.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReActPromptTemplate")
class ReActPromptTemplateTest {

    private InMemoryToolRegistry registry;
    private ReActPromptTemplate template;

    @BeforeEach
    void setUp() {
        registry = new InMemoryToolRegistry();

        registry.register(new ToolDefinition(
                "web_search",
                "Search the web for information",
                List.of(
                        new ToolParameter("query", "string", "The search query", true),
                        new ToolParameter("max_results", "integer", "Maximum results", false)
                ),
                "SearchResult[]"
        ));

        registry.register(new ToolDefinition(
                "calculator",
                "Perform mathematical calculations",
                List.of(
                        new ToolParameter("expression", "string", "Expression to evaluate", true)
                ),
                "number"
        ));

        template = new ReActPromptTemplate(registry, 5);
    }

    @Nested
    @DisplayName("buildSystemPrompt")
    class SystemPromptTests {

        @Test
        @DisplayName("should build complete system prompt with tools and format")
        void shouldBuildCompleteSystemPrompt() {
            String prompt = template.buildSystemPrompt();

            assertNotNull(prompt);
            assertFalse(prompt.isBlank());

            // Should contain format instructions
            assertTrue(prompt.contains("Thought:"), "Should contain Thought instruction");
            assertTrue(prompt.contains("Action:"), "Should contain Action instruction");
            assertTrue(prompt.contains("Action Input:"), "Should contain Action Input instruction");
            assertTrue(prompt.contains("Observation:"), "Should contain Observation instruction");
            assertTrue(prompt.contains("Final Answer:"), "Should contain Final Answer instruction");

            // Should contain tool names
            assertTrue(prompt.contains("web_search"), "Should list web_search tool");
            assertTrue(prompt.contains("calculator"), "Should list calculator tool");
        }

        @Test
        @DisplayName("should include max iterations constraint")
        void shouldIncludeMaxIterations() {
            String prompt = template.buildSystemPrompt();

            assertTrue(prompt.contains("Maximum iterations: 5"),
                    "Should specify max iterations");
        }

        @Test
        @DisplayName("should include tool descriptions")
        void shouldIncludeToolDescriptions() {
            String prompt = template.buildSystemPrompt();

            assertTrue(prompt.contains("Search the web for information"));
            assertTrue(prompt.contains("Perform mathematical calculations"));
        }

        @Test
        @DisplayName("should include parameter details")
        void shouldIncludeParameterDetails() {
            String prompt = template.buildSystemPrompt();

            assertTrue(prompt.contains("query"), "Should list query parameter");
            assertTrue(prompt.contains("search query"), "Should have query description (case insensitive)");
            assertTrue(prompt.contains("required"), "Should mark required parameter");
            assertTrue(prompt.contains("expression"), "Should list expression parameter");
        }
    }

    @Nested
    @DisplayName("buildToolsSection")
    class ToolsSectionTests {

        @Test
        @DisplayName("should build tools section with all registered tools")
        void shouldBuildToolsSection() {
            String section = template.buildToolsSection();

            assertTrue(section.contains("web_search"));
            assertTrue(section.contains("calculator"));
            assertTrue(section.contains("Available tools"));
        }

        @Test
        @DisplayName("should indicate when no tools are available")
        void shouldIndicateNoTools() {
            InMemoryToolRegistry emptyRegistry = new InMemoryToolRegistry();
            ReActPromptTemplate emptyTemplate = new ReActPromptTemplate(emptyRegistry);

            String section = emptyTemplate.buildToolsSection();
            assertTrue(section.contains("No tools are currently available"));
        }
    }

    @Nested
    @DisplayName("buildConstraintsSection")
    class ConstraintsSectionTests {

        @Test
        @DisplayName("should include max iterations in constraints")
        void shouldIncludeMaxIterations() {
            String constraints = template.buildConstraintsSection();

            assertTrue(constraints.contains("5"));
            assertTrue(constraints.contains("Maximum iterations"));
        }

        @Test
        @DisplayName("should include tool naming constraints")
        void shouldIncludeNamingConstraints() {
            String constraints = template.buildConstraintsSection();

            assertTrue(constraints.contains("exactly the tool names listed above"));
        }
    }

    @Nested
    @DisplayName("buildToolListString")
    class ToolListStringTests {

        @Test
        @DisplayName("should build simplified tool list string")
        void shouldBuildSimplifiedToolList() {
            String list = template.buildToolListString();

            assertTrue(list.contains("web_search: Search the web for information"));
            assertTrue(list.contains("calculator: Perform mathematical calculations"));
        }

        @Test
        @DisplayName("should return empty string when no tools")
        void shouldReturnEmptyForNoTools() {
            InMemoryToolRegistry emptyRegistry = new InMemoryToolRegistry();
            ReActPromptTemplate emptyTemplate = new ReActPromptTemplate(emptyRegistry);

            assertEquals("", emptyTemplate.buildToolListString());
        }
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should use default max iterations of 10")
        void shouldUseDefaultMaxIterations() {
            ReActPromptTemplate defaultTemplate = new ReActPromptTemplate(registry);
            assertEquals(10, defaultTemplate.getMaxIterations());
        }

        @Test
        @DisplayName("should use custom max iterations when specified")
        void shouldUseCustomMaxIterations() {
            ReActPromptTemplate customTemplate = new ReActPromptTemplate(registry, 15);
            assertEquals(15, customTemplate.getMaxIterations());
        }

        @Test
        @DisplayName("should fallback to default when max iterations <= 0")
        void shouldFallbackToDefaultWhenInvalid() {
            ReActPromptTemplate zeroTemplate = new ReActPromptTemplate(registry, 0);
            assertEquals(10, zeroTemplate.getMaxIterations());

            ReActPromptTemplate negTemplate = new ReActPromptTemplate(registry, -1);
            assertEquals(10, negTemplate.getMaxIterations());
        }
    }

    @Nested
    @DisplayName("prompt completeness")
    class CompletenessTests {

        @Test
        @DisplayName("prompt should contain all required ReAct components")
        void promptShouldContainAllReActComponents() {
            String prompt = template.buildSystemPrompt();

            // Verify complete ReAct format instruction structure
            assertTrue(prompt.contains("You are an AI assistant"),
                    "Should start with assistant declaration");
            assertTrue(prompt.contains("Thought:"));
            assertTrue(prompt.contains("Action:"));
            assertTrue(prompt.contains("Action Input:"));
            assertTrue(prompt.contains("Observation:"));
            assertTrue(prompt.contains("Final Answer:"));
            assertTrue(prompt.contains("Begin!"),
                    "Should end with Begin! marker");
        }

        @Test
        @DisplayName("prompt with many tools should render all")
        void promptWithManyToolsShouldRenderAll() {
            InMemoryToolRegistry bigRegistry = new InMemoryToolRegistry();
            for (int i = 0; i < 10; i++) {
                bigRegistry.register(new ToolDefinition(
                        "tool_" + i,
                        "Tool number " + i,
                        List.of(),
                        "void"
                ));
            }
            ReActPromptTemplate bigTemplate = new ReActPromptTemplate(bigRegistry);

            String prompt = bigTemplate.buildSystemPrompt();
            for (int i = 0; i < 10; i++) {
                assertTrue(prompt.contains("tool_" + i),
                        "Should contain tool tool_" + i);
            }
        }
    }
}
