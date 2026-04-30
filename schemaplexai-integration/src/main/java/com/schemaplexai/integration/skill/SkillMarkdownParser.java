package com.schemaplexai.integration.skill;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkillMarkdownParser {

    private static final Pattern FRONTMATTER_PATTERN =
        Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n(.*)", Pattern.DOTALL);

    public record SkillMeta(String name, String version, String description, List<String> tags) {}

    public static SkillMeta parseMeta(String markdown) {
        Matcher m = FRONTMATTER_PATTERN.matcher(markdown);
        if (!m.find()) {
            throw new IllegalArgumentException("Missing YAML frontmatter (--- blocks)");
        }
        String yaml = m.group(1);
        Map<String, String> fields = parseYamlMap(yaml);

        String name = fields.get("name");
        String version = fields.get("version");
        String description = fields.get("description");
        String tagsRaw = fields.getOrDefault("tags", "");

        if (name == null || version == null || description == null) {
            throw new IllegalArgumentException(
                "Missing required fields in frontmatter. Required: name, version, description");
        }

        List<String> tags = parseYamlList(tagsRaw);
        return new SkillMeta(name, version, description, tags);
    }

    public static String parseBody(String markdown) {
        Matcher m = FRONTMATTER_PATTERN.matcher(markdown);
        if (m.find()) {
            return m.group(2).trim();
        }
        return markdown;
    }

    private static Map<String, String> parseYamlMap(String yaml) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon > 0) {
                String key = trimmed.substring(0, colon).trim();
                String value = trimmed.substring(colon + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private static List<String> parseYamlList(String raw) {
        String cleaned = raw.replaceAll("[\\[\\]]", "").trim();
        if (cleaned.isEmpty()) return List.of();
        return Arrays.stream(cleaned.split(","))
            .map(s -> s.trim().replaceAll("^\"|\"$", ""))
            .filter(s -> !s.isEmpty())
            .toList();
    }
}
