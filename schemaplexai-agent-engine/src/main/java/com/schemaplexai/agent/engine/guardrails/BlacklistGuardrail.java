package com.schemaplexai.agent.engine.guardrails;

import com.schemaplexai.agent.engine.evaluation.ValidationResult;

import java.util.Set;

/**
 * Blacklist-based guardrail that blocks prompt injection attempts and unsafe content.
 */
public class BlacklistGuardrail implements Guardrail {

    private static final Set<String> BLACKLISTED_KEYWORDS = Set.of(
            "ignore previous instructions",
            "ignore your instructions",
            "forget what you know",
            "repeat your programming",
            "reveal your instructions",
            "bypass safety",
            "jailbreak"
    );

    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
            "databaseDrop",
            "volumeDelete",
            "hardDelete",
            "permanentDelete",
            "purge"
    );

    @Override
    public ValidationResult validateInput(String input) {
        return checkBlacklist(input, "Input");
    }

    @Override
    public ValidationResult validateOutput(String output) {
        return checkBlacklist(output, "Output");
    }

    @Override
    public boolean isHighRisk(String toolName) {
        return toolName != null && HIGH_RISK_TOOLS.contains(toolName);
    }

    @Override
    public String getName() {
        return "BlacklistGuardrail";
    }

    private ValidationResult checkBlacklist(String text, String label) {
        if (text == null || text.isBlank()) {
            return ValidationResult.valid();
        }

        String lowerText = text.toLowerCase();
        for (String keyword : BLACKLISTED_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return ValidationResult.invalid(
                        label + " contains blocked keyword: " + keyword
                );
            }
        }

        return ValidationResult.valid();
    }
}
