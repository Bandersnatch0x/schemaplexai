package com.schemaplexai.agent.engine.reasoning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChainOfThoughtVisualizer Tests")
class ChainOfThoughtVisualizerTest {

    private ChainOfThoughtVisualizer visualizer;

    @BeforeEach
    void setUp() {
        visualizer = new ChainOfThoughtVisualizer();
        visualizer.clearAll();
    }

    @Test
    @DisplayName("newExecutionId returns non-blank UUID")
    void newExecutionId() {
        String id = visualizer.newExecutionId();
        assertNotNull(id);
        assertFalse(id.isBlank());
    }

    @Test
    @DisplayName("recordStep assigns incrementing step numbers")
    void recordStepIncrements() {
        String id = visualizer.newExecutionId();

        int s1 = visualizer.recordStep(id, "Analyze", "First thought");
        int s2 = visualizer.recordStep(id, "Synthesize", "Second thought");

        assertEquals(1, s1);
        assertEquals(2, s2);
    }

    @Test
    @DisplayName("getCoTTrace returns ordered steps")
    void getCoTTraceOrdered() {
        String id = visualizer.newExecutionId();
        visualizer.recordStep(id, "A", "Reason A");
        visualizer.recordStep(id, "B", "Reason B");

        List<ReasoningStep> trace = visualizer.getCoTTrace(id);
        assertEquals(2, trace.size());
        assertEquals("A", trace.get(0).description());
        assertEquals("B", trace.get(1).description());
        assertEquals(1, trace.get(0).stepNumber());
        assertEquals(2, trace.get(1).stepNumber());
    }

    @Test
    @DisplayName("getCoTTrace returns empty list for unknown execution")
    void getCoTTraceUnknown() {
        assertTrue(visualizer.getCoTTrace("non-existent-id").isEmpty());
    }

    @Test
    @DisplayName("getCoTTrace returns empty list for null or blank id")
    void getCoTTraceNullBlank() {
        assertTrue(visualizer.getCoTTrace(null).isEmpty());
        assertTrue(visualizer.getCoTTrace("   ").isEmpty());
    }

    @Test
    @DisplayName("recordStep throws for null or blank arguments")
    void recordStepValidation() {
        String id = visualizer.newExecutionId();
        assertThrows(IllegalArgumentException.class, () -> visualizer.recordStep(null, "A", "R"));
        assertThrows(IllegalArgumentException.class, () -> visualizer.recordStep("", "A", "R"));
        assertThrows(IllegalArgumentException.class, () -> visualizer.recordStep(id, null, "R"));
        assertThrows(IllegalArgumentException.class, () -> visualizer.recordStep(id, "", "R"));
    }

    @Test
    @DisplayName("trace is immutable from outside")
    void traceImmutability() {
        String id = visualizer.newExecutionId();
        visualizer.recordStep(id, "A", "R");

        List<ReasoningStep> trace = visualizer.getCoTTrace(id);
        assertThrows(UnsupportedOperationException.class, trace::clear);
    }

    @Test
    @DisplayName("exportToMarkdown contains execution metadata and steps")
    void exportToMarkdown() {
        String id = visualizer.newExecutionId();
        visualizer.recordStep(id, "Analyze", "Breaking down the problem");
        visualizer.recordStep(id, "Conclude", "Therefore, the answer is 42");

        String md = visualizer.exportToMarkdown(id);
        assertTrue(md.contains("# Chain of Thought Trace"));
        assertTrue(md.contains(id));
        assertTrue(md.contains("Total Steps"));
        assertTrue(md.contains("## Step 1: Analyze"));
        assertTrue(md.contains("## Step 2: Conclude"));
        assertTrue(md.contains("Breaking down the problem"));
        assertTrue(md.contains("Therefore, the answer is 42"));
    }

    @Test
    @DisplayName("exportToMarkdown returns placeholder for unknown execution")
    void exportToMarkdownUnknown() {
        String md = visualizer.exportToMarkdown("unknown-id");
        assertTrue(md.contains("No steps recorded"));
        assertTrue(md.contains("unknown-id"));
    }

    @Test
    @DisplayName("multiple executions are isolated")
    void multipleExecutionsIsolated() {
        String id1 = visualizer.newExecutionId();
        String id2 = visualizer.newExecutionId();

        visualizer.recordStep(id1, "A", "R1");
        visualizer.recordStep(id2, "X", "R2");

        assertEquals(1, visualizer.getCoTTrace(id1).size());
        assertEquals(1, visualizer.getCoTTrace(id2).size());
        assertEquals("A", visualizer.getCoTTrace(id1).get(0).description());
        assertEquals("X", visualizer.getCoTTrace(id2).get(0).description());
    }

    @Test
    @DisplayName("clearAll removes all traces")
    void clearAll() {
        String id = visualizer.newExecutionId();
        visualizer.recordStep(id, "A", "R");
        assertEquals(1, visualizer.getCoTTrace(id).size());

        visualizer.clearAll();
        assertTrue(visualizer.getCoTTrace(id).isEmpty());
    }
}
