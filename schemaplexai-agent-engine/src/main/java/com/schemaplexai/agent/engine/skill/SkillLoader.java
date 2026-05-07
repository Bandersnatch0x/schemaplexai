package com.schemaplexai.agent.engine.skill;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Parses skill definitions from Markdown files with YAML frontmatter.
 * Security constraints:
 * - Name max 64 chars (matches DB column)
 * - Description max 500 chars (matches DB column)
 * - HTML tags rejected (XSS prevention)
 * - Parsing runs in isolated thread with 5s timeout (DoS prevention)
 */
@Component
public class SkillLoader {

    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern YAML_KEY_VALUE = Pattern.compile("^([a-zA-Z_-]+):\\s*(.*)$");
    private static final long PARSE_TIMEOUT_SECONDS = 5;

    private final ExecutorService parseExecutor = Executors.newFixedThreadPool(2);

    /**
     * Parse skill markdown with timeout isolation.
     *
     * @param markdown raw markdown with YAML frontmatter
     * @return parsed and validated skill definition
     * @throws ValidationException on any validation failure
     */
    public SkillDefinition parse(String markdown) {
        try {
            return parseExecutor.submit(() -> doParse(markdown))
                    .get(PARSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ValidationException(
                    "Skill markdown parsing timed out after " + PARSE_TIMEOUT_SECONDS + "s");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ValidationException) {
                throw (ValidationException) cause;
            }
            throw new ValidationException(
                    "Skill markdown parsing failed: " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ValidationException("Skill markdown parsing interrupted");
        }
    }

    private SkillDefinition doParse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new ValidationException("Skill markdown cannot be null or blank");
        }

        // Parse frontmatter and body
        ParsedMarkdown parsed = parseFrontMatterAndBody(markdown);
        Map<String, String> frontMatter = parsed.frontMatter();

        String name = frontMatter.get("name");
        String description = frontMatter.get("description");

        // Validate name
        if (name == null || name.isBlank()) {
            throw new ValidationException("Skill name is required");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(
                    "Skill name exceeds " + MAX_NAME_LENGTH + " chars");
        }

        // Validate description
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException(
                    "Skill description exceeds " + MAX_DESCRIPTION_LENGTH + " chars");
        }

        String body = parsed.body();

        // Reject HTML tags (XSS prevention)
        if (HTML_TAG.matcher(body).find()) {
            throw new ValidationException(
                    "HTML tags are not allowed in skill content");
        }

        return new SkillDefinition(name.trim(), description, body.trim());
    }

    /**
     * Parse YAML frontmatter between --- delimiters and extract body.
     * Simple key: value parser — no external YAML dependency needed.
     */
    private ParsedMarkdown parseFrontMatterAndBody(String markdown) {
        String[] lines = markdown.split("\n", -1);
        Map<String, String> frontMatter = new LinkedHashMap<>();
        int bodyStart = 0;

        // Expect first line to be ---
        if (lines.length > 0 && lines[0].trim().equals("---")) {
            int separatorCount = 1;
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.trim().equals("---")) {
                    separatorCount++;
                    bodyStart = i + 1;
                    break;
                }
                // Parse simple key: value
                var matcher = YAML_KEY_VALUE.matcher(line);
                if (matcher.matches()) {
                    String key = matcher.group(1).trim();
                    String value = matcher.group(2).trim();
                    // Strip surrounding quotes
                    if (value.length() >= 2
                            && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    frontMatter.put(key, value);
                }
            }
        }

        // Build body from remaining lines
        StringBuilder body = new StringBuilder();
        for (int i = bodyStart; i < lines.length; i++) {
            if (body.length() > 0) {
                body.append("\n");
            }
            body.append(lines[i]);
        }

        return new ParsedMarkdown(frontMatter, body.toString());
    }

    private record ParsedMarkdown(Map<String, String> frontMatter, String body) {}
}
