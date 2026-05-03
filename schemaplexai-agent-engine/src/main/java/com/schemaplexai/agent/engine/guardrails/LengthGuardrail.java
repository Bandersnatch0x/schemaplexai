package com.schemaplexai.agent.engine.guardrails;

import com.schemaplexai.agent.engine.evaluation.ValidationResult;

/**
 * Length-based guardrail that enforces maximum input/output length.
 */
public class LengthGuardrail implements Guardrail {

    private static final int DEFAULT_MAX_LENGTH = 100_000;

    private final int maxInputLength;
    private final int maxOutputLength;

    public LengthGuardrail(int maxInputLength, int maxOutputLength) {
        this.maxInputLength = maxInputLength;
        this.maxOutputLength = maxOutputLength;
    }

    public LengthGuardrail() {
        this(DEFAULT_MAX_LENGTH, DEFAULT_MAX_LENGTH);
    }

    @Override
    public ValidationResult validateInput(String input) {
        return checkLength(input, maxInputLength, "Input");
    }

    @Override
    public ValidationResult validateOutput(String output) {
        return checkLength(output, maxOutputLength, "Output");
    }

    @Override
    public boolean isHighRisk(String toolName) {
        // Length guardrail does not flag tools as high-risk
        return false;
    }

    @Override
    public String getName() {
        return "LengthGuardrail";
    }

    private ValidationResult checkLength(String text, int maxLength, String label) {
        if (text == null) {
            return ValidationResult.valid();
        }

        if (text.length() > maxLength) {
            return ValidationResult.invalid(
                    label + " length " + text.length() + " exceeds max " + maxLength
            );
        }

        return ValidationResult.valid();
    }
}
