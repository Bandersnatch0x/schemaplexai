package com.schemaplexai.agent.engine.guardrails;

import com.schemaplexai.agent.engine.evaluation.ValidationResult;

/**
 * Guardrail interface - defines a single guardrail rule for input/output validation.
 */
public interface Guardrail {

    /**
     * Validate input text.
     *
     * @param input the input text
     * @return validation result
     */
    ValidationResult validateInput(String input);

    /**
     * Validate output text.
     *
     * @param output the output text
     * @return validation result
     */
    ValidationResult validateOutput(String output);

    /**
     * Check if a tool call is high-risk.
     *
     * @param toolName the tool name
     * @return true if high-risk
     */
    boolean isHighRisk(String toolName);

    /**
     * Get the guardrail name.
     *
     * @return guardrail name
     */
    String getName();
}
