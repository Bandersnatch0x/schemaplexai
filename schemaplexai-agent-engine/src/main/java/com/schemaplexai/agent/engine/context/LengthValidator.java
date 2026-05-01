package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.tool.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * 基于长度限制的输入检查器。
 */
@Component
public class LengthValidator implements InputValidator {

    private static final int DEFAULT_MAX_LENGTH = 100000;

    private final int maxLength;

    public LengthValidator(int maxLength) {
        this.maxLength = maxLength;
    }

    public LengthValidator() {
        this(DEFAULT_MAX_LENGTH);
    }

    @Override
    public ValidationResult validate(String input) {
        if (input == null) {
            return ValidationResult.valid();
        }

        if (input.length() > maxLength) {
            return ValidationResult.invalid(
                "Input length " + input.length() + " exceeds max " + maxLength);
        }

        return ValidationResult.valid();
    }
}
