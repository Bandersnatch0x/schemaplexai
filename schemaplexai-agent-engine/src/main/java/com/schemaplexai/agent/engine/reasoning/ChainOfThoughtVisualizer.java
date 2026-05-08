package com.schemaplexai.agent.engine.reasoning;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records and visualizes Chain-of-Thought reasoning steps.
 * Each execution is tracked by a unique execution ID and can be exported to Markdown.
 */
@Component
public class ChainOfThoughtVisualizer {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final Map<String, List<ReasoningStep>> traces = new ConcurrentHashMap<>();

    /**
     * Records a reasoning step for the given execution.
     *
     * @param executionId the execution trace identifier
     * @param step        a short label for the step
     * @param reasoning   the detailed reasoning content
     * @return the step number assigned to this recorded step
     */
    public int recordStep(String executionId, String step, String reasoning) {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId must not be blank");
        }
        if (step == null || step.isBlank()) {
            throw new IllegalArgumentException("step must not be blank");
        }

        List<ReasoningStep> trace = traces.computeIfAbsent(executionId, k -> new ArrayList<>());
        int stepNumber = trace.size() + 1;
        ReasoningStep reasoningStep = new ReasoningStep(stepNumber, step, reasoning, 0.0);
        trace.add(reasoningStep);
        return stepNumber;
    }

    /**
     * Generates a new unique execution ID for a reasoning trace.
     *
     * @return a UUID-based execution identifier
     */
    public String newExecutionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Retrieves the recorded CoT trace for an execution.
     *
     * @param executionId the execution identifier
     * @return an unmodifiable list of reasoning steps, or empty list if not found
     */
    public List<ReasoningStep> getCoTTrace(String executionId) {
        if (executionId == null || executionId.isBlank()) {
            return List.of();
        }
        List<ReasoningStep> trace = traces.get(executionId);
        return trace == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(trace));
    }

    /**
     * Exports the CoT trace for an execution to a Markdown document.
     *
     * @param executionId the execution identifier
     * @return a Markdown-formatted string, or a placeholder if no trace exists
     */
    public String exportToMarkdown(String executionId) {
        List<ReasoningStep> trace = getCoTTrace(executionId);
        if (trace.isEmpty()) {
            return "# Chain of Thought Trace\n\n*No steps recorded for execution `" + executionId + "`.*\n";
        }

        StringBuilder md = new StringBuilder();
        md.append("# Chain of Thought Trace\n\n");
        md.append("**Execution ID:** `").append(executionId).append("`  \n");
        md.append("**Total Steps:** ").append(trace.size()).append("  \n\n");
        md.append("---\n\n");

        for (ReasoningStep s : trace) {
            md.append("## Step ").append(s.stepNumber()).append(": ").append(escapeMarkdown(s.description())).append("\n\n");
            md.append("**Timestamp:** ").append(TIMESTAMP_FORMATTER.format(s.timestamp())).append("  \n");
            md.append("**Confidence:** ").append(String.format("%.2f", s.confidenceScore())).append("  \n\n");
            md.append("### Reasoning\n\n");
            md.append("```text\n");
            md.append(s.reasoning()).append("\n");
            md.append("```\n\n");
            md.append("---\n\n");
        }

        return md.toString();
    }

    /**
     * Clears all recorded traces. Primarily useful for testing.
     */
    public void clearAll() {
        traces.clear();
    }

    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("<", "\\<").replace(">", "\\>");
    }
}
