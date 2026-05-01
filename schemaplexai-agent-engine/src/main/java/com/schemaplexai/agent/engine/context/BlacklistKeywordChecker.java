package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.tool.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 基于黑名单关键词的输入检查器。
 */
@Component
public class BlacklistKeywordChecker implements InputValidator {

    private static final Set<String> BLACKLISTED_KEYWORDS = Set.of(
        "ignore previous instructions",
        "ignore your instructions",
        "forget what you know",
        "repeat your programming",
        "reveal your instructions",
        "bypass safety",
        "jailbreak"
    );

    @Override
    public ValidationResult validate(String input) {
        if (input == null || input.isBlank()) {
            return ValidationResult.valid();
        }

        String lowerInput = input.toLowerCase();
        for (String keyword : BLACKLISTED_KEYWORDS) {
            if (lowerInput.contains(keyword)) {
                return ValidationResult.invalid("Input contains blocked keyword: " + keyword);
            }
        }

        return ValidationResult.valid();
    }
}
