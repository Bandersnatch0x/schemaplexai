package com.schemaplexai.agent.engine.extractor;

import com.schemaplexai.agent.engine.tool.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FinalAnswerExtractor")
class FinalAnswerExtractorTest {

    private FinalAnswerExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new FinalAnswerExtractor();
    }

    @Nested
    @DisplayName("extractFinalAnswer")
    class FinalAnswerTests {

        @Test
        @DisplayName("should extract Final Answer from standard ReAct output")
        void shouldExtractFinalAnswer() {
            String output = """
                    Thought: I need to search for the capital of France.
                    Action: web_search
                    Action Input: {"query": "capital of France"}

                    Observation: Paris is the capital of France.

                    Thought: I now know the answer.
                    Final Answer: The capital of France is Paris.""";

            String result = extractor.extractFinalAnswer(output);
            assertEquals("The capital of France is Paris.", result);
        }

        @Test
        @DisplayName("should extract Final Answer with lowercase")
        void shouldExtractFinalAnswerCaseInsensitive() {
            String output = "final answer: 42";
            assertEquals("42", extractor.extractFinalAnswer(output));
        }

        @Test
        @DisplayName("should extract Final Answer with extra whitespace")
        void shouldExtractFinalAnswerWithExtraWhitespace() {
            String output = "Final   Answer  :   The answer is blue.  ";
            assertEquals("The answer is blue.", extractor.extractFinalAnswer(output));
        }

        @Test
        @DisplayName("should extract multi-line Final Answer")
        void shouldExtractMultilineFinalAnswer() {
            String output = """
                    Thought: I have the answer.
                    Final Answer: The result is as follows:
                    - Item 1
                    - Item 2
                    This concludes the analysis.""";

            String result = extractor.extractFinalAnswer(output);
            assertTrue(result.startsWith("The result is as follows:"));
            assertTrue(result.contains("- Item 1"));
            assertTrue(result.contains("This concludes the analysis."));
        }

        @Test
        @DisplayName("should return null when no Final Answer present")
        void shouldReturnNullWhenNoFinalAnswer() {
            String output = """
                    Thought: I need to search.
                    Action: web_search
                    Action Input: {"query": "test"}""";

            assertNull(extractor.extractFinalAnswer(output));
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInput() {
            assertNull(extractor.extractFinalAnswer(null));
        }

        @Test
        @DisplayName("should return null for blank input")
        void shouldReturnNullForBlankInput() {
            assertNull(extractor.extractFinalAnswer("   "));
        }
    }

    @Nested
    @DisplayName("hasFinalAnswer")
    class HasFinalAnswerTests {

        @Test
        @DisplayName("should return true when Final Answer exists")
        void shouldReturnTrueWhenFinalAnswerExists() {
            assertTrue(extractor.hasFinalAnswer("Final Answer: Yes"));
        }

        @Test
        @DisplayName("should return false when no Final Answer")
        void shouldReturnFalseWhenNoFinalAnswer() {
            assertFalse(extractor.hasFinalAnswer("Just some text"));
        }

        @Test
        @DisplayName("should return false for null")
        void shouldReturnFalseForNull() {
            assertFalse(extractor.hasFinalAnswer(null));
        }
    }

    @Nested
    @DisplayName("extractThoughts")
    class ThoughtTests {

        @Test
        @DisplayName("should extract all thoughts in order")
        void shouldExtractAllThoughts() {
            String output = """
                    Thought: I should search for the answer first.
                    Action: web_search
                    Action Input: {"query": "capital"}
                    Observation: Paris.
                    Thought: Now I can answer the question.
                    Thought: Let me double check.
                    Final Answer: Paris""";

            List<String> thoughts = extractor.extractThoughts(output);
            assertEquals(3, thoughts.size());
            assertEquals("I should search for the answer first.", thoughts.get(0));
            assertEquals("Now I can answer the question.", thoughts.get(1));
            assertEquals("Let me double check.", thoughts.get(2));
        }

        @Test
        @DisplayName("should extract last thought")
        void shouldExtractLastThought() {
            String output = """
                    Thought: First thought
                    Thought: Second thought
                    Thought: Final thought""";

            String last = extractor.extractLastThought(output);
            assertEquals("Final thought", last);
        }

        @Test
        @DisplayName("should return empty list for no thoughts")
        void shouldReturnEmptyForNoThoughts() {
            assertTrue(extractor.extractThoughts("No thoughts here.").isEmpty());
        }

        @Test
        @DisplayName("extractLastThought should return null when no thoughts")
        void extractLastThoughtShouldReturnNull() {
            assertNull(extractor.extractLastThought("Nothing"));
        }
    }

    @Nested
    @DisplayName("extractToolCall")
    class ToolCallTests {

        @Test
        @DisplayName("should extract ToolCall from Action/Action Input pair")
        void shouldExtractToolCallFromAction() {
            String output = """
                    Thought: I need to search.
                    Action: web_search
                    Action Input: {"query": "Java 21 features", "max_results": 5}
                    """;

            ToolCall call = extractor.extractToolCall(output);
            assertNotNull(call);
            assertEquals("web_search", call.toolName());
            assertEquals("Java 21 features", call.parameters().get("query"));
            assertEquals(5L, call.parameters().get("max_results"));
        }

        @Test
        @DisplayName("should extract ToolCall when Action Input is simple string")
        void shouldExtractToolCallWithStringInput() {
            String output = """
                    Action: calculator
                    Action Input: 2 + 2 * 3
                    """;

            ToolCall call = extractor.extractToolCall(output);
            assertNotNull(call);
            assertEquals("calculator", call.toolName());
            assertEquals("2 + 2 * 3", call.parameters().get("input"));
        }

        @Test
        @DisplayName("should extract only the first ToolCall when multiple exist")
        void shouldExtractOnlyFirstToolCall() {
            String output = """
                    Action: web_search
                    Action Input: {"query": "first"}
                    Observation: result 1
                    Action: calculator
                    Action Input: {"expression": "1+1"}
                    """;

            ToolCall call = extractor.extractToolCall(output);
            assertNotNull(call);
            assertEquals("web_search", call.toolName());
        }

        @Test
        @DisplayName("should return null when no Action present")
        void shouldReturnNullWhenNoAction() {
            assertNull(extractor.extractToolCall("Just a Final Answer: done."));
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNullInputToolCall() {
            assertNull(extractor.extractToolCall(null));
        }

        @Test
        @DisplayName("should extract ToolCall with boolean and null parameters")
        void shouldExtractToolCallWithBooleanParams() {
            String output = """
                    Action: set_flag
                    Action Input: {"enabled": true, "verbose": false, "cache": null}
                    """;

            ToolCall call = extractor.extractToolCall(output);
            assertNotNull(call);
            assertEquals(true, call.parameters().get("enabled"));
            assertEquals(false, call.parameters().get("verbose"));
            assertNull(call.parameters().get("cache"));
        }
    }

    @Nested
    @DisplayName("extractAllToolCalls")
    class AllToolCallsTests {

        @Test
        @DisplayName("should extract all ToolCalls from ReAct cycle")
        void shouldExtractAllToolCalls() {
            String output = """
                    Action: web_search
                    Action Input: {"query": "Java 21"}
                    Observation: Java 21 released
                    Action: calculator
                    Action Input: {"expression": "2+2"}
                    Observation: 4
                    Final Answer: Done""";

            List<ToolCall> calls = extractor.extractAllToolCalls(output);
            assertEquals(2, calls.size());
            assertEquals("web_search", calls.get(0).toolName());
            assertEquals("calculator", calls.get(1).toolName());
        }

        @Test
        @DisplayName("should return empty list for no tool calls")
        void shouldReturnEmptyForNoToolCalls() {
            List<ToolCall> calls = extractor.extractAllToolCalls("Just text, no actions.");
            assertTrue(calls.isEmpty());
        }

        @Test
        @DisplayName("should return empty list for null input")
        void shouldReturnEmptyForNullInputAllCalls() {
            assertTrue(extractor.extractAllToolCalls(null).isEmpty());
        }
    }
}
