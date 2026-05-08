package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.model.LlmProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SelfCorrectionEngine Tests")
class SelfCorrectionEngineTest {

    @Mock
    private LlmProvider llmProvider;

    private SelfCorrectionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SelfCorrectionEngine(llmProvider);
    }

    // --- generateWithCorrection ---

    @Test
    @DisplayName("generateWithCorrection returns result with steps")
    void generateWithCorrectionBasic() {
        when(llmProvider.generate(anyString(), isNull(), anyDouble()))
                .thenReturn("Initial output")
                .thenReturn("Needs improvement")
                .thenReturn("Refined output");

        ReasoningResult result = engine.generateWithCorrection("Solve 2+2", 3);

        assertEquals("Refined output", result.finalAnswer());
        assertTrue(result.totalSteps() >= 2);
        assertTrue(result.averageConfidence() >= 0.0);
    }

    @Test
    @DisplayName("generateWithCorrection converges early when critique is SATISFACTORY")
    void convergesEarly() {
        when(llmProvider.generate(anyString(), isNull(), anyDouble()))
                .thenReturn("Initial output")
                .thenReturn("SATISFACTORY");

        ReasoningResult result = engine.generateWithCorrection("Prompt", 5);

        assertEquals("Initial output", result.finalAnswer());
        assertEquals(2, result.totalSteps());
    }

    @Test
    @DisplayName("generateWithCorrection reaches max iterations without convergence")
    void maxIterationsNoConvergence() {
        when(llmProvider.generate(anyString(), isNull(), anyDouble()))
                .thenReturn("Initial output")
                .thenReturn("Critique 1")
                .thenReturn("Refined 1")
                .thenReturn("Critique 2")
                .thenReturn("Refined 2");

        ReasoningResult result = engine.generateWithCorrection("Prompt", 2);

        assertTrue(result.totalSteps() >= 3);
        assertNotNull(result.finalAnswer());
    }

    @Test
    @DisplayName("generateWithCorrection validates prompt")
    void validatePrompt() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generateWithCorrection(null, 3));
        assertThrows(IllegalArgumentException.class,
                () -> engine.generateWithCorrection("", 3));
        assertThrows(IllegalArgumentException.class,
                () -> engine.generateWithCorrection("   ", 3));
    }

    @Test
    @DisplayName("generateWithCorrection validates maxIterations")
    void validateMaxIterations() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.generateWithCorrection("Prompt", 0));
        assertThrows(IllegalArgumentException.class,
                () -> engine.generateWithCorrection("Prompt", -1));
    }

    // --- critiqueOutput ---

    @Test
    @DisplayName("critiqueOutput returns LLM critique")
    void critiqueOutput() {
        when(llmProvider.generate(anyString(), isNull(), anyDouble()))
                .thenReturn("The output is clear and correct.");

        String critique = engine.critiqueOutput("The answer is 42");

        assertNotNull(critique);
        verify(llmProvider, times(1)).generate(anyString(), isNull(), anyDouble());
    }

    @Test
    @DisplayName("critiqueOutput handles blank output without calling LLM")
    void critiqueOutputBlank() {
        String critique = engine.critiqueOutput("");
        assertTrue(critique.contains("empty"));
        verifyNoInteractions(llmProvider);
    }

    @Test
    @DisplayName("critiqueOutput handles null output without calling LLM")
    void critiqueOutputNull() {
        String critique = engine.critiqueOutput(null);
        assertTrue(critique.contains("empty"));
        verifyNoInteractions(llmProvider);
    }

    // --- refineOutput ---

    @Test
    @DisplayName("refineOutput returns refined text from LLM")
    void refineOutput() {
        when(llmProvider.generate(anyString(), isNull(), anyDouble()))
                .thenReturn("Improved answer: 42");

        String refined = engine.refineOutput("Answer: 42", "Add more detail");

        assertEquals("Improved answer: 42", refined);
    }

    @Test
    @DisplayName("refineOutput returns original when critique is blank")
    void refineOutputBlankCritique() {
        String refined = engine.refineOutput("Original", "");
        assertEquals("Original", refined);
        verifyNoInteractions(llmProvider);
    }

    @Test
    @DisplayName("refineOutput returns original when critique is null")
    void refineOutputNullCritique() {
        String refined = engine.refineOutput("Original", null);
        assertEquals("Original", refined);
        verifyNoInteractions(llmProvider);
    }

    @Test
    @DisplayName("refineOutput throws when output is null")
    void refineOutputNullOutput() {
        assertThrows(IllegalArgumentException.class,
                () -> engine.refineOutput(null, "Fix it"));
    }
}
