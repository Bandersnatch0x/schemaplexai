package com.schemaplexai.workflow.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${path}} placeholders in node inputs against the workflow execution
 * context, wiring upstream node outputs into downstream node inputs (spec §3.2:
 * "${input.xxx} in prompt is replaced with upstream node outputs").
 *
 * <p>Context namespaces:
 * <ul>
 *   <li>{@code input.<key>} — flattened outputs of all upstream nodes (later nodes win),
 *       seeded with the instance-level input data</li>
 *   <li>{@code <nodeId>.<key>} — the output of a specific upstream node</li>
 * </ul>
 *
 * <p>A value that is exactly one placeholder preserves the resolved type
 * ({@code ${n1.score}} may resolve to a Number); placeholders embedded in a larger
 * string interpolate via {@code String.valueOf}. Unresolvable placeholders are left
 * unchanged so missing data stays visible instead of silently vanishing.
 */
public final class NodeVariableSubstitutor {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private NodeVariableSubstitutor() {
    }

    /**
     * Substitute placeholders in every string value of the node input (recursively
     * through nested maps and lists).
     *
     * @param input   raw node input from the template definition
     * @param context execution context (upstream outputs, see class javadoc)
     * @return a new map with placeholders resolved; never null
     */
    public static Map<String, Object> substitute(Map<String, Object> input, Map<String, Object> context) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (input == null) {
            return resolved;
        }
        Map<String, Object> safeContext = context != null ? context : Map.of();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            resolved.put(entry.getKey(), resolveValue(entry.getValue(), safeContext));
        }
        return resolved;
    }

    private static Object resolveValue(Object value, Map<String, Object> context) {
        if (value instanceof String s) {
            return resolveString(s, context);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                nested.put(String.valueOf(entry.getKey()), resolveValue(entry.getValue(), context));
            }
            return nested;
        }
        if (value instanceof List<?> list) {
            List<Object> nested = new ArrayList<>(list.size());
            for (Object item : list) {
                nested.add(resolveValue(item, context));
            }
            return nested;
        }
        return value;
    }

    private static Object resolveString(String value, Map<String, Object> context) {
        Matcher full = PLACEHOLDER.matcher(value);
        if (full.matches()) {
            Object resolved = lookup(full.group(1).trim(), context);
            return resolved != null ? resolved : value;
        }

        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            Object resolved = lookup(matcher.group(1).trim(), context);
            String replacement = resolved != null ? String.valueOf(resolved) : matcher.group();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Object lookup(String path, Map<String, Object> context) {
        if (path.isEmpty()) {
            return null;
        }
        String[] parts = path.split("\\.");
        Object current = context.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(parts[i]);
            } else {
                return null;
            }
        }
        return current;
    }
}
