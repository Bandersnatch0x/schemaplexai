package com.schemaplexai.agent.engine.skill;

import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Parses skill definitions from Markdown files with YAML frontmatter.
 * Uses flexmark-java with YamlFrontMatterExtension for spec-compliant parsing.
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
    private static final long PARSE_TIMEOUT_SECONDS = 5;

    private final ExecutorService parseExecutor = Executors.newFixedThreadPool(2);
    private final Parser flexmarkParser;

    public SkillLoader() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Collections.singletonList(YamlFrontMatterExtension.create()));
        this.flexmarkParser = Parser.builder(options).build();
    }

    @PreDestroy
    public void shutdown() {
        parseExecutor.shutdown();
        try {
            if (!parseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                parseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            parseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

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

        // Parse with flexmark-java
        Node document = flexmarkParser.parse(markdown);

        // Extract YAML frontmatter using flexmark visitor
        AbstractYamlFrontMatterVisitor visitor = new AbstractYamlFrontMatterVisitor();
        visitor.visit(document);
        Map<String, List<String>> yamlData = visitor.getData();

        // Flatten to single-value map (take first value for each key)
        Map<String, String> frontMatter = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : yamlData.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                frontMatter.put(entry.getKey(), values.get(0));
            }
        }

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

        // Extract body: use flexmark AST to get content after frontmatter
        String body = extractBodyAfterFrontMatter(document);

        // Reject HTML tags (XSS prevention)
        if (HTML_TAG.matcher(body).find()) {
            throw new ValidationException(
                    "HTML tags are not allowed in skill content");
        }

        return new SkillDefinition(name.trim(), description, body.trim());
    }

    /**
     * Extract the markdown body after the YAML frontmatter block.
     * The frontmatter block is delimited by '---' markers. We find the second
     * '---' and take everything after it as the body.
     */
    private String extractBodyAfterFrontMatter(Node document) {
        // Walk the AST to find content nodes that are NOT part of the frontmatter.
        // flexmark's YamlFrontMatterExtension represents the frontmatter as a single
        // block node. Everything after it is the document body.
        StringBuilder body = new StringBuilder();
        boolean pastFrontMatter = false;

        for (Node child = document.getFirstChild(); child != null; child = child.getNext()) {
            String className = child.getClass().getSimpleName();
            if (className.contains("YamlFrontMatter")) {
                pastFrontMatter = true;
                continue;
            }
            if (pastFrontMatter) {
                if (body.length() > 0) {
                    body.append("\n");
                }
                body.append(child.getChars());
            }
        }

        // Fallback: if no frontmatter node found, use raw text after second '---'
        if (!pastFrontMatter) {
            return extractBodyFallback(document.getChars().toString());
        }

        return body.toString();
    }

    /**
     * Fallback body extraction: split on '---' delimiters.
     */
    private String extractBodyFallback(String markdown) {
        String[] lines = markdown.split("\n", -1);
        int separatorCount = 0;
        int bodyStart = 0;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].trim().equals("---")) {
                separatorCount++;
                if (separatorCount == 2) {
                    bodyStart = i + 1;
                    break;
                }
            }
        }
        return String.join("\n",
                java.util.Arrays.copyOfRange(lines, bodyStart, lines.length));
    }

}
