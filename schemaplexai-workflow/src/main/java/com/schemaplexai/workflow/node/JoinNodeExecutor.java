package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges results from parallel (CONCURRENT) branches at a JOIN point.
 *
 * <p>Input format:
 * <pre>
 * {
 *   "sourceResults": {
 *     "branch1": {"success": true, "output": "data A"},
 *     "branch2": {"success": true, "output": "data B"}
 *   },
 *   "mergeStrategy": "CONCAT"
 * }
 * </pre>
 *
 * <p>Supported merge strategies:
 * <ul>
 *   <li><b>CONCAT</b> — concatenates all textual outputs</li>
 *   <li><b>FIRST_SUCCESS</b> — returns the first successful result</li>
 *   <li><b>AGGREGATE</b> — returns a structured aggregation of all results</li>
 * </ul>
 *
 * <p>Output format:
 * <pre>
 * {
 *   "mergedContent": "...",
 *   "strategy": "CONCAT",
 *   "sourceCount": 2,
 *   "successCount": 2
 * }
 * </pre>
 */
@Slf4j
@Component
public class JoinNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "JOIN";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String strategy = (String) input.getOrDefault("mergeStrategy", "AGGREGATE");
        Map<String, Object> sourceResults = extractSourceResults(input);

        if (sourceResults.isEmpty()) {
            return NodeExecutionResult.failure("Missing or empty required field: sourceResults");
        }

        log.info("Join node executing: strategy={}, sources={}, tenantId={}",
                strategy, sourceResults.size(), tenantId);

        Map<String, Object> output;
        switch (strategy.toUpperCase()) {
            case "CONCAT" -> output = mergeConcat(sourceResults, tenantId);
            case "FIRST_SUCCESS" -> output = mergeFirstSuccess(sourceResults, tenantId);
            case "AGGREGATE" -> output = mergeAggregate(sourceResults, tenantId);
            default -> output = mergeAggregate(sourceResults, tenantId);
        }

        return NodeExecutionResult.success(output);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractSourceResults(Map<String, Object> input) {
        Object sourceObj = input.get("sourceResults");
        if (sourceObj instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    result.put(key, entry.getValue());
                }
            }
            return result;
        }
        // Accept a List of results keyed by position
        if (sourceObj instanceof List<?> list) {
            Map<String, Object> result = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                result.put("source_" + i, list.get(i));
            }
            return result;
        }
        return Map.of();
    }

    private Map<String, Object> mergeConcat(Map<String, Object> sourceResults, String tenantId) {
        StringBuilder merged = new StringBuilder();
        int successCount = 0;

        for (Map.Entry<String, Object> entry : sourceResults.entrySet()) {
            String text = extractOutputText(entry.getValue());
            if (text != null) {
                if (!merged.isEmpty()) {
                    merged.append("\n\n---\n\n");
                }
                merged.append("[").append(entry.getKey()).append("]\n").append(text);
                successCount++;
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("mergedContent", merged.toString());
        output.put("strategy", "CONCAT");
        output.put("sourceCount", sourceResults.size());
        output.put("successCount", successCount);

        log.info("Join concat complete: {} sources merged, {} succeeded, tenantId={}",
                sourceResults.size(), successCount, tenantId);
        return output;
    }

    private Map<String, Object> mergeFirstSuccess(Map<String, Object> sourceResults, String tenantId) {
        for (Map.Entry<String, Object> entry : sourceResults.entrySet()) {
            String text = extractOutputText(entry.getValue());
            if (text != null) {
                Map<String, Object> output = new HashMap<>();
                output.put("mergedContent", text);
                output.put("strategy", "FIRST_SUCCESS");
                output.put("sourceKey", entry.getKey());
                output.put("sourceCount", sourceResults.size());
                output.put("successCount", 1);

                log.info("Join first-success complete: picked '{}' from {}, tenantId={}",
                        entry.getKey(), sourceResults.size(), tenantId);
                return output;
            }
        }

        log.warn("Join first-success: no successful source found, tenantId={}", tenantId);
        Map<String, Object> output = new HashMap<>();
        output.put("mergedContent", "");
        output.put("strategy", "FIRST_SUCCESS");
        output.put("sourceCount", sourceResults.size());
        output.put("successCount", 0);
        return output;
    }

    private Map<String, Object> mergeAggregate(Map<String, Object> sourceResults, String tenantId) {
        List<Map<String, Object>> sources = new ArrayList<>();
        int successCount = 0;

        for (Map.Entry<String, Object> entry : sourceResults.entrySet()) {
            Map<String, Object> sourceEntry = new HashMap<>();
            sourceEntry.put("key", entry.getKey());
            sourceEntry.put("output", extractOutputText(entry.getValue()));
            sourceEntry.put("success", entry.getValue() instanceof Map<?, ?> m
                    && Boolean.TRUE.equals(m.get("success")));
            sources.add(sourceEntry);
            if (Boolean.TRUE.equals(sourceEntry.get("success"))) {
                successCount++;
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("sources", sources);
        output.put("strategy", "AGGREGATE");
        output.put("sourceCount", sourceResults.size());
        output.put("successCount", successCount);

        log.info("Join aggregate complete: {}/{} succeeded, tenantId={}",
                successCount, sourceResults.size(), tenantId);
        return output;
    }

    private String extractOutputText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof Map<?, ?> map) {
            Object output = map.get("output");
            if (output != null) {
                return output.toString();
            }
            // Check if this is a ConcurrentNodeExecutor result
            Object results = map.get("results");
            if (results != null) {
                return results.toString();
            }
            // Check nested success flag
            if (Boolean.TRUE.equals(map.get("success"))) {
                return value.toString();
            }
        }
        return value.toString();
    }
}
