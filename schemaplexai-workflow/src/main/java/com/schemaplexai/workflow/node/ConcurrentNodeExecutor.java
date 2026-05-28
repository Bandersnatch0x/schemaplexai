package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Validates CONCURRENT node inputs until a real sub-task runtime is configured.
 *
 * <p>Input format:
 * <pre>
 * {
 *   "subTasks": [
 *     {"name": "task1", "prompt": "Generate unit tests"},
 *     {"name": "task2", "prompt": "Review code quality"}
 *   ],
 *   "timeoutSeconds": 60
 * }
 * </pre>
 */
@Slf4j
@Component
public class ConcurrentNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "CONCURRENT";
    }

    @Override
    @SuppressWarnings("unchecked")
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        List<Map<String, Object>> subTasks = extractSubTasks(input);
        if (subTasks.isEmpty()) {
            return NodeExecutionResult.failure("Missing or empty required field: subTasks");
        }

        for (Map<String, Object> subTask : subTasks) {
            String prompt = (String) subTask.get("prompt");
            if (prompt == null || prompt.isBlank()) {
                return NodeExecutionResult.failure("Missing prompt in sub-task");
            }
        }

        log.warn("CONCURRENT node cannot execute without a configured concurrent task runtime: subTaskCount={}, tenantId={}",
                subTasks.size(), tenantId);
        return NodeExecutionResult.failure(
                "CONCURRENT node execution is not implemented. Configure a concurrent task runtime before enabling CONCURRENT nodes.");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractSubTasks(Map<String, Object> input) {
        Object subTasksObj = input.get("subTasks");
        if (subTasksObj instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

}
