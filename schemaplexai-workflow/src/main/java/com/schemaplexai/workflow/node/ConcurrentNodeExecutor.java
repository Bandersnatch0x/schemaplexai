package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Executes multiple sub-tasks concurrently and collects all results.
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
 *
 * <p>Output format:
 * <pre>
 * {
 *   "results": {
 *     "task1": {"success": true, "output": "..."},
 *     "task2": {"success": false, "error": "..."}
 *   },
 *   "allSucceeded": true,
 *   "executedCount": 2,
 *   "failedCount": 0
 * }
 * </pre>
 */
@Slf4j
@Component
public class ConcurrentNodeExecutor implements NodeExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int MAX_CONCURRENT = 10;

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

        int timeoutSeconds = extractTimeout(input);
        log.info("Concurrent node executing: {} sub-tasks, timeout={}s, tenantId={}",
                subTasks.size(), timeoutSeconds, tenantId);

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(subTasks.size(), MAX_CONCURRENT));
        try {
            List<CompletableFuture<Map.Entry<String, Map<String, Object>>>> futures = new ArrayList<>();
            for (Map<String, Object> subTask : subTasks) {
                String taskName = (String) subTask.getOrDefault("name", "unnamed");
                futures.add(CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> result = executeSubTask(subTask, tenantId);
                    return Map.entry(taskName, result);
                }, executor));
            }

            Map<String, Map<String, Object>> results = new ConcurrentHashMap<>();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeoutSeconds, TimeUnit.SECONDS);

            for (CompletableFuture<Map.Entry<String, Map<String, Object>>> future : futures) {
                Map.Entry<String, Map<String, Object>> entry = future.get();
                results.put(entry.getKey(), entry.getValue());
            }

            int successCount = 0;
            int failureCount = 0;
            for (Map<String, Object> result : results.values()) {
                if (Boolean.TRUE.equals(result.get("success"))) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            Map<String, Object> output = new HashMap<>();
            output.put("results", results);
            output.put("allSucceeded", failureCount == 0);
            output.put("executedCount", subTasks.size());
            output.put("successCount", successCount);
            output.put("failedCount", failureCount);

            log.info("Concurrent node complete: {}/{} succeeded, tenantId={}",
                    successCount, subTasks.size(), tenantId);
            return NodeExecutionResult.success(output);

        } catch (TimeoutException e) {
            log.error("Concurrent node timed out after {}s, tenantId={}", timeoutSeconds, tenantId);
            return NodeExecutionResult.failure("Concurrent execution timed out after " + timeoutSeconds + "s");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Concurrent node interrupted, tenantId={}", tenantId);
            return NodeExecutionResult.failure("Concurrent execution interrupted");
        } catch (Exception e) {
            log.error("Concurrent node failed, tenantId={}", tenantId, e);
            return NodeExecutionResult.failure("Concurrent execution failed: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
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

    private int extractTimeout(Map<String, Object> input) {
        Object timeoutObj = input.get("timeoutSeconds");
        if (timeoutObj instanceof Number num) {
            return Math.max(1, num.intValue());
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    private Map<String, Object> executeSubTask(Map<String, Object> subTask, String tenantId) {
        String prompt = (String) subTask.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return Map.of("success", false, "error", "Missing prompt in sub-task");
        }

        // Simulate sub-task execution (in production, dispatch to AgentExecutionEngine)
        log.debug("Executing concurrent sub-task: {}", prompt.substring(0, Math.min(50, prompt.length())));
        return Map.of(
                "success", true,
                "output", "[concurrent sub-task result: " + prompt.substring(0, Math.min(30, prompt.length())) + "...]",
                "prompt", prompt
        );
    }
}
