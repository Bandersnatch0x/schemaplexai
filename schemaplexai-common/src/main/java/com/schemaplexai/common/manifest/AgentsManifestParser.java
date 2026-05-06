package com.schemaplexai.common.manifest;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AGENTS.md 解析器：YAML frontmatter + Markdown body。
 *
 * <p>遵循 OpenAI Agents SDK 2026 的 AGENTS.md 协议：
 * <pre>
 * ---
 * name: code-reviewer
 * description: ...
 * tools:
 *   - name: file_read
 *     type: builtin
 * ---
 *
 * &lt;system prompt body&gt;
 * </pre>
 *
 * <p>实现要点（见 design.md §B.2）：
 * <ul>
 *   <li>使用 snakeyaml 2.2 + {@link SafeConstructor}（拒绝任意类型反序列化，防御 CVE-2017-18640 类问题）</li>
 *   <li>frontmatter 必须以首行 {@code ---} 开始，第二个 {@code ---} 结束</li>
 *   <li>{@code name} 是唯一必填字段，缺失或空白即拒绝</li>
 *   <li>所有数值字段以 {@link Long}/{@link Double} 形式存储（见 SfAgentConfig 实体类型）</li>
 * </ul>
 *
 * <p>无状态、可线程安全；消费侧可使用 {@code @Bean} 注入。
 */
public class AgentsManifestParser {

    private static final String DELIMITER = "---";

    /**
     * 解析 AGENTS.md 内容。
     *
     * @param markdown 完整 markdown 文本，必须非空且包含 frontmatter
     * @return 解析后的不可变 manifest
     * @throws ManifestParseException 输入无效、frontmatter 缺失、必填字段缺失或字段类型错误时抛出
     */
    public AgentsManifest parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new ManifestParseException("manifest content is null or blank");
        }

        Frontmatter fm = splitFrontmatter(markdown);
        Map<String, Object> meta = parseYaml(fm.yaml());

        String name = stringField(meta, "name");
        if (name == null || name.isBlank()) {
            throw new ManifestParseException("manifest 'name' field is required and must not be blank");
        }

        return new AgentsManifest(
                name,
                stringField(meta, "description"),
                stringField(meta, "model"),
                stringField(meta, "type"),
                longField(meta, "maxRounds"),
                longField(meta, "maxTools"),
                longField(meta, "maxInputTokens"),
                longField(meta, "maxOutputTokens"),
                doubleField(meta, "temperature"),
                stringField(meta, "executionMode"),
                toolsField(meta),
                fm.body()
        );
    }

    // --- frontmatter splitting ---

    private record Frontmatter(String yaml, String body) {}

    private Frontmatter splitFrontmatter(String markdown) {
        // normalize line endings to simplify parsing
        String normalized = markdown.replace("\r\n", "\n");

        if (!normalized.startsWith(DELIMITER)) {
            throw new ManifestParseException(
                    "manifest must start with '---' frontmatter delimiter");
        }

        // find the second delimiter on its own line
        int firstNewline = normalized.indexOf('\n');
        if (firstNewline < 0) {
            throw new ManifestParseException("frontmatter is incomplete (no newline after opening '---')");
        }
        String afterFirst = normalized.substring(firstNewline + 1);

        int closeIdx = findClosingDelimiter(afterFirst);
        if (closeIdx < 0) {
            throw new ManifestParseException("frontmatter is not closed (missing second '---')");
        }
        String yaml = afterFirst.substring(0, closeIdx);
        String body = afterFirst.substring(closeIdx);
        // strip the closing delimiter line itself
        int bodyNewline = body.indexOf('\n');
        body = bodyNewline < 0 ? "" : body.substring(bodyNewline + 1);
        return new Frontmatter(yaml, body);
    }

    private int findClosingDelimiter(String text) {
        if (text.startsWith(DELIMITER + "\n") || text.equals(DELIMITER)) {
            return 0;
        }
        int idx = 0;
        while (idx < text.length()) {
            int newline = text.indexOf('\n', idx);
            int lineEnd = newline < 0 ? text.length() : newline;
            String line = text.substring(idx, lineEnd);
            if (line.equals(DELIMITER)) {
                return idx;
            }
            if (newline < 0) {
                break;
            }
            idx = newline + 1;
        }
        return -1;
    }

    // --- YAML parsing (safe) ---

    private Map<String, Object> parseYaml(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            throw new ManifestParseException("frontmatter YAML is empty");
        }
        try {
            LoaderOptions opts = new LoaderOptions();
            opts.setAllowDuplicateKeys(false);
            opts.setMaxAliasesForCollections(50);
            Yaml yaml = new Yaml(new SafeConstructor(opts));
            Object parsed = yaml.load(yamlText);
            if (parsed == null) {
                throw new ManifestParseException("frontmatter YAML parsed to null");
            }
            if (!(parsed instanceof Map<?, ?> map)) {
                throw new ManifestParseException(
                        "frontmatter must be a YAML mapping, got " + parsed.getClass().getSimpleName());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        } catch (YAMLException e) {
            throw new ManifestParseException("invalid YAML frontmatter: " + e.getMessage(), e);
        }
    }

    // --- field extraction with type-safe coercion ---

    private String stringField(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof String s) {
            return s;
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        throw new ManifestParseException(
                "field '" + key + "' must be a string, got " + v.getClass().getSimpleName());
    }

    private Long longField(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Integer i) {
            return i.longValue();
        }
        if (v instanceof Short s) {
            return s.longValue();
        }
        throw new ManifestParseException(
                "field '" + key + "' must be an integer, got " + v.getClass().getSimpleName());
    }

    private Double doubleField(Map<String, Object> meta, String key) {
        Object v = meta.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Double d) {
            return d;
        }
        if (v instanceof Float f) {
            return f.doubleValue();
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        throw new ManifestParseException(
                "field '" + key + "' must be a number, got " + v.getClass().getSimpleName());
    }

    private List<AgentsManifest.ToolBinding> toolsField(Map<String, Object> meta) {
        Object v = meta.get("tools");
        if (v == null) {
            return List.of();
        }
        if (!(v instanceof List<?> list)) {
            throw new ManifestParseException(
                    "field 'tools' must be a list, got " + v.getClass().getSimpleName());
        }
        List<AgentsManifest.ToolBinding> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (!(item instanceof Map<?, ?> tm)) {
                throw new ManifestParseException(
                        "tools[" + i + "] must be a mapping, got "
                                + (item == null ? "null" : item.getClass().getSimpleName()));
            }
            Object n = tm.get("name");
            Object t = tm.get("type");
            Object c = tm.get("config");
            if (!(n instanceof String) || ((String) n).isBlank()) {
                throw new ManifestParseException("tools[" + i + "].name is required and must be a non-blank string");
            }
            if (!(t instanceof String) || ((String) t).isBlank()) {
                throw new ManifestParseException("tools[" + i + "].type is required and must be a non-blank string");
            }
            String configJson = null;
            if (c != null) {
                configJson = c.toString();
            }
            out.add(new AgentsManifest.ToolBinding((String) n, (String) t, configJson));
        }
        return List.copyOf(out);
    }
}
