package com.schemaplexai.agent.engine.guardrails;

import com.schemaplexai.agent.engine.evaluation.ValidationResult;

import java.util.List;

/**
 * Guardrails engine - orchestrates multiple guardrail rules and executes validation
 * against inputs, outputs, and tool calls.
 */
public class GuardrailsEngine {

    private final List<Guardrail> guardrails;

    public GuardrailsEngine(List<Guardrail> guardrails) {
        this.guardrails = guardrails != null ? guardrails : List.of();
    }

    /**
     * Validate input against all guardrails.
     * Returns on the first failure (short-circuit).
     *
     * @param input the input text
     * @return validation result
     */
    public ValidationResult validateInput(String input) {
        for (Guardrail guardrail : guardrails) {
            ValidationResult result = guardrail.validateInput(input);
            if (!result.success()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Validate output against all guardrails.
     * Returns on the first failure (short-circuit).
     *
     * @param output the output text
     * @return validation result
     */
    public ValidationResult validateOutput(String output) {
        for (Guardrail guardrail : guardrails) {
            ValidationResult result = guardrail.validateOutput(output);
            if (!result.success()) {
                return result;
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Check if any guardrail flags a tool call as high-risk.
     *
     * @param toolName the tool name
     * @return true if any guardrail considers the operation high-risk
     */
    public boolean isHighRiskOperation(String toolName) {
        return guardrails.stream()
                .anyMatch(g -> g.isHighRisk(toolName));
    }
}
