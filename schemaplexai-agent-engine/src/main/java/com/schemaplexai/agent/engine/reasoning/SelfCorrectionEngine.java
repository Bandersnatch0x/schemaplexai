package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.model.LlmProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements an explicit self-correction loop: generate -> critique -> refine.
 * Iterates up to a configurable maximum, stopping early when critique signals
 * the output is satisfactory (convergence).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfCorrectionEngine {

    private static final String DEFAULT_CRITIQUE_PROMPT = """
            You are a critical reviewer. Evaluate the following output for accuracy,
            completeness, clarity, and correctness. If the output is fully satisfactory,
            respond with exactly "SATISFACTORY". Otherwise, provide specific critique
            and suggestions for improvement.
            """;

    private static final String DEFAULT_REFINE_PROMPT = """
            You are an expert editor. Improve the following output based on the critique
            provided. Preserve all correct information and address every issue raised.
            Output only the refined result with no meta-commentary.
            """;

    private static final String CONVERGENCE_MARKER = "SATISFACTORY";
    private static final int DEFAULT_MAX_ITERATIONS = 5;

    private final LlmProvider llmProvider;

    /**
     * Generates output with iterative self-correction.
     *
     * @param prompt        the original user prompt
     * @param maxIterations maximum correction iterations (must be >= 1)
     * @return the final {@link ReasoningResult} containing answer and all steps
     */
    public ReasoningResult generateWithCorrection(String prompt, int maxIterations) {
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be >= 1");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }

        List<ReasoningStep> steps = new ArrayList<>();
        String currentOutput = generateInitial(prompt);
        steps.add(new ReasoningStep(1, "Initial Generation", currentOutput, estimateConfidence(currentOutput)));

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            String critique = critiqueOutput(currentOutput);
            steps.add(new ReasoningStep(
                    steps.size() + 1,
                    "Critique (iteration " + iteration + ")",
                    critique,
                    estimateConfidence(critique)
            ));

            if (isConverged(critique)) {
                log.info("Self-correction converged after {} critique iteration(s)", iteration);
                break;
            }

            currentOutput = refineOutput(currentOutput, critique);
            steps.add(new ReasoningStep(
                    steps.size() + 1,
                    "Refinement (iteration " + iteration + ")",
                    currentOutput,
                    estimateConfidence(currentOutput)
            ));
        }

        double avgConfidence = steps.stream()
                .mapToDouble(ReasoningStep::confidenceScore)
                .average()
                .orElse(0.0);

        return new ReasoningResult(currentOutput, steps, steps.size(), avgConfidence);
    }

    /**
     * Critiques the given output for quality and correctness.
     *
     * @param output the output to critique
     * @return critique text, or "SATISFACTORY" if no issues found
     */
    public String critiqueOutput(String output) {
        if (output == null || output.isBlank()) {
            return "Output is empty — provide substantive content.";
        }
        String prompt = DEFAULT_CRITIQUE_PROMPT + "\n\nOutput to critique:\n" + output;
        return llmProvider.generate(prompt, null, 0.3);
    }

    /**
     * Refines the output based on the provided critique.
     *
     * @param output   the current output
     * @param critique the critique to address
     * @return the refined output
     */
    public String refineOutput(String output, String critique) {
        if (output == null) {
            throw new IllegalArgumentException("output must not be null");
        }
        if (critique == null || critique.isBlank()) {
            return output;
        }
        String prompt = DEFAULT_REFINE_PROMPT
                + "\n\nOriginal Output:\n" + output
                + "\n\nCritique:\n" + critique;
        return llmProvider.generate(prompt, null, 0.5);
    }

    private String generateInitial(String prompt) {
        return llmProvider.generate(prompt, null, 0.7);
    }

    private boolean isConverged(String critique) {
        return critique != null && critique.trim().equalsIgnoreCase(CONVERGENCE_MARKER);
    }

    private double estimateConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        // Simple heuristic: longer, well-structured text gets higher confidence
        double base = Math.min(1.0, text.length() / 1000.0);
        // Boost if text contains reasoning markers
        if (text.contains("because") || text.contains("therefore") || text.contains("step")) {
            base = Math.min(1.0, base + 0.1);
        }
        return Math.round(base * 100.0) / 100.0;
    }
}
