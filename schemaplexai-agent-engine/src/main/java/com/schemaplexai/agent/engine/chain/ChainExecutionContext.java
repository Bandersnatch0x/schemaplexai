package com.schemaplexai.agent.engine.chain;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mutable execution context that accumulates step outputs as a prompt chain executes.
 * <p>
 * Each step's output is stored under its {@code outputKey} and can be referenced
 * by downstream steps via {@code {variable}} placeholders in prompt templates.
 */
public class ChainExecutionContext {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{(\\w+)}");

    private final Map<String, String> stepOutputs = new HashMap<>();
    private final Map<String, Object> initialInputs;

    public ChainExecutionContext(Map<String, Object> initialInputs) {
        this.initialInputs = initialInputs != null ? Map.copyOf(initialInputs) : Map.of();
    }

    /**
     * Returns the output produced by the step with the given id.
     *
     * @param stepId the step id (or output key)
     * @return the output string, or {@code null} if not found
     */
    public String getOutput(String stepId) {
        return stepOutputs.get(stepId);
    }

    /**
     * Stores an output for the given step id.
     *
     * @param stepId the step id / output key
     * @param output the LLM-generated output
     */
    public void setOutput(String stepId, String output) {
        stepOutputs.put(stepId, output);
    }

    /**
     * Returns an unmodifiable view of all accumulated step outputs.
     */
    public Map<String, String> getAllOutputs() {
        return Map.copyOf(stepOutputs);
    }

    /**
     * Resolves a template string by replacing {@code {variable}} placeholders
     * with values from initial inputs and step outputs.
     * <p>
     * Step outputs take precedence over initial inputs when keys collide.
     *
     * @param template the template string containing {@code {var}} placeholders
     * @return the resolved string; unresolved placeholders are left as-is
     */
    public String resolveTemplate(String template) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Matcher matcher = TEMPLATE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement = lookupVariable(variable);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String lookupVariable(String variable) {
        // Step outputs take precedence
        String output = stepOutputs.get(variable);
        if (output != null) {
            return output;
        }
        // Fall back to initial inputs
        Object initial = initialInputs.get(variable);
        if (initial != null) {
            return initial.toString();
        }
        // Unresolved — leave placeholder intact
        return "{" + variable + "}";
    }
}
