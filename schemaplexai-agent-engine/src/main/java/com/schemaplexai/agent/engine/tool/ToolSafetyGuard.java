package com.schemaplexai.agent.engine.tool;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ToolSafetyGuard {

    private static final Set<String> IRREVERSIBLE_TOOLS = Set.of(
        "volumeDelete", "databaseDrop", "destroy", "purge", "hardDelete", "permanentDelete"
    );

    private static final Pattern IRREVERSIBLE_PATTERN = Pattern.compile(
        "(?i)(DROP\\s+TABLE|DROP\\s+DATABASE|DELETE\\s+FROM|TRUNCATE|RM\\s+-RF)"
    );

    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&#(\\d+);");
    private static final Pattern JSON_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    public SafetyCheckResult check(String toolName, String arguments) {
        return check(toolName, arguments, null);
    }

    public SafetyCheckResult check(String toolName, String arguments, String expectedEnvironment) {
        // Check 1: Irreversible tool names
        if (IRREVERSIBLE_TOOLS.contains(toolName)) {
            return SafetyCheckResult.reject(
                ToolErrorCategory.IRREVERSIBLE_OPERATION,
                "Operation blocked by safety policy."
            );
        }

        // Check 2: Irreversible commands in arguments (with normalization)
        if (arguments != null) {
            String normalized = normalizeInput(arguments);
            if (IRREVERSIBLE_PATTERN.matcher(normalized).find()) {
                return SafetyCheckResult.reject(
                    ToolErrorCategory.IRREVERSIBLE_OPERATION,
                    "Operation blocked by safety policy."
                );
            }
        }

        // Check 3: Environment/credential mismatch
        if (expectedEnvironment != null && arguments != null) {
            if (arguments.contains("prod") && !expectedEnvironment.contains("prod")) {
                return SafetyCheckResult.reject(
                    ToolErrorCategory.ENVIRONMENT_MISMATCH,
                    "Operation blocked by safety policy."
                );
            }
        }

        return SafetyCheckResult.permit();
    }

    /**
     * Normalizes input to detect obfuscated destructive commands.
     * Handles: Unicode homoglyphs (NFKC + custom mapping), HTML entities, JSON escape sequences,
     * and collapses whitespace variations.
     */
    static String normalizeInput(String input) {
        if (input == null) {
            return null;
        }

        String result = input;

        // Decode JSON escape sequences:   → space
        result = decodeJsonEscapes(result);

        // Decode HTML entities: &#68; → D
        result = decodeHtmlEntities(result);

        // Normalize Unicode homoglyphs: fullwidth letters, combining chars, etc.
        result = Normalizer.normalize(result, Normalizer.Form.NFKC);

        // Map visual homoglyphs that NFKC does not handle (Cyrillic → Latin)
        result = mapHomoglyphs(result);

        // Collapse all whitespace variations (tabs, newlines, multiple spaces) to single space
        result = result.replaceAll("\\s+", " ");

        return result;
    }

    private static String mapHomoglyphs(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            char mapped = mapSingleHomoglyph(c);
            sb.append(mapped);
        }
        return sb.toString();
    }

    private static char mapSingleHomoglyph(char c) {
        // Cyrillic lookalikes → Latin
        return switch (c) {
            case 'а' -> 'a'; // а
            case 'е' -> 'e'; // е
            case 'о' -> 'o'; // о
            case 'р' -> 'p'; // р
            case 'с' -> 'c'; // с
            case 'х' -> 'x'; // х
            case 'А' -> 'A'; // А
            case 'Е' -> 'E'; // Е
            case 'О' -> 'O'; // О
            case 'Р' -> 'P'; // Р
            case 'С' -> 'C'; // С
            case 'Х' -> 'X'; // Х
            default -> c;
        };
    }

    private static String decodeJsonEscapes(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 6 <= input.length() && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
                try {
                    String hex = input.substring(i + 2, i + 6);
                    int codePoint = Integer.parseInt(hex, 16);
                    sb.append((char) codePoint);
                    i += 6;
                    continue;
                } catch (NumberFormatException e) {
                    // Not a valid escape, fall through
                }
            }
            sb.append(input.charAt(i));
            i++;
        }
        return sb.toString();
    }

    private static String decodeHtmlEntities(String input) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == '&' && i + 1 < input.length() && input.charAt(i + 1) == '#') {
                int end = input.indexOf(';', i);
                if (end != -1) {
                    try {
                        String num = input.substring(i + 2, end);
                        int codePoint = Integer.parseInt(num);
                        sb.append((char) codePoint);
                        i = end + 1;
                        continue;
                    } catch (NumberFormatException e) {
                        // Not a valid entity, fall through
                    }
                }
            }
            sb.append(input.charAt(i));
            i++;
        }
        return sb.toString();
    }

    public record SafetyCheckResult(
        boolean allowed,
        boolean blocked,
        ToolErrorCategory errorCategory,
        String reason
    ) {
        public static SafetyCheckResult permit() {
            return new SafetyCheckResult(true, false, null, null);
        }

        public static SafetyCheckResult reject(ToolErrorCategory category, String reason) {
            return new SafetyCheckResult(false, true, category, reason);
        }
    }
}
