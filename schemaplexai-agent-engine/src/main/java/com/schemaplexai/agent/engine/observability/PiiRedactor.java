package com.schemaplexai.agent.engine.observability;

import java.util.List;
import java.util.regex.Pattern;

public final class PiiRedactor {

    private static final List<Pattern> REDACTION_PATTERNS = List.of(
        Pattern.compile("(?i)(password|passwd|pwd)\\s*[=:]\\s*[^\\s&]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)(api[_-]?key|secret|token)\\s*[=:]\\s*[^\\s&]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:(?:\\+?86)?1[3-9]\\d{9})|(?:\\d{3,4}-\\d{7,8})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("sk-[a-zA-Z0-9]{48}", Pattern.CASE_INSENSITIVE)
    );

    private static final String REPLACEMENT = "[REDACTED]";

    public static String redact(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        for (Pattern pattern : REDACTION_PATTERNS) {
            result = pattern.matcher(result).replaceAll(REPLACEMENT);
        }
        return result;
    }
}
