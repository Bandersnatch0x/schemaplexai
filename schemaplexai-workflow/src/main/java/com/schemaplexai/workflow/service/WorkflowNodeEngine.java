package com.schemaplexai.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.mapper.SfWorkflowNodeExecutionMapper;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import com.schemaplexai.workflow.node.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowNodeEngine {

    private final List<NodeExecutor> executorList;
    private final SfWorkflowNodeExecutionMapper nodeExecutionMapper;
    private final ObjectMapper objectMapper;

    private Map<String, NodeExecutor> executors;

    // Optional wiring (ticket 924 trigger point 3): published after COMPLETED
    // nodes. Setter-injected so existing tests constructing the engine with
    // three dependencies keep working; null = trigger disabled.
    private WorkflowQualityCheckPublisher qualityCheckPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setQualityCheckPublisher(WorkflowQualityCheckPublisher qualityCheckPublisher) {
        this.qualityCheckPublisher = qualityCheckPublisher;
    }

    // Retry policy (spec §8: 3 retries with exponential backoff). Only transient
    // (retryable) failures and thrown exceptions are retried; deterministic failures and
    // terminal TIMEOUT results are not. Injectable so tests run without real sleeping.
    private int maxRetries = 3;
    private long retryBaseDelayMillis = 1000L;
    private RetrySleeper retrySleeper = Thread::sleep;

    @FunctionalInterface
    public interface RetrySleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setRetryBaseDelayMillis(long retryBaseDelayMillis) {
        this.retryBaseDelayMillis = retryBaseDelayMillis;
    }

    public void setRetrySleeper(RetrySleeper retrySleeper) {
        this.retrySleeper = retrySleeper;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.executors = executorList.stream()
                .collect(Collectors.toMap(NodeExecutor::getNodeType, Function.identity()));
    }

    // Deliberately NOT @Transactional: node execution wraps external calls (LLM/HTTP) and
    // must record FAILED status durably even when the execution throws. Inside a wrapping
    // transaction the failure-status update was rolled back along with the rethrown
    // exception, leaving no trace of the failure in the database.
    public NodeExecutionResult executeNode(SfWorkflowNodeExecution nodeExecution) {
        NodeExecutor executor = executors.get(nodeExecution.getNodeType());
        if (executor == null) {
            throw new BaseException(ResultCode.ERROR,
                    "No executor for node type: " + nodeExecution.getNodeType());
        }

        // Bridge callers (Flowable delegates) hand in a never-persisted record; the
        // orchestrated path already inserted it. Insert if needed so every node
        // execution is recorded regardless of entry point.
        if (nodeExecution.getId() == null) {
            if (nodeExecution.getStatus() == null) {
                nodeExecution.setStatus("PENDING");
            }
            if (nodeExecution.getTenantId() == null) {
                nodeExecution.setTenantId(TenantContextHolder.getTenantId());
            }
            nodeExecutionMapper.insert(nodeExecution);
        }

        nodeExecution.setStatus("RUNNING");
        nodeExecutionMapper.updateById(nodeExecution);

        Map<String, Object> input = parseInput(nodeExecution.getInputJson());

        NodeExecutionResult result = null;
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                sleepBeforeRetry(attempt, nodeExecution.getNodeId());
            }
            try {
                result = executor.execute(input, nodeExecution.getTenantId());
                lastException = null;
                // Success and terminal TIMEOUT stop the loop; deterministic failures stop
                // too (retrying cannot help). Only transient failures continue to retry.
                if (result.isSuccess() || result.isTimeout() || !result.isRetryable()) {
                    break;
                }
            } catch (Exception e) {
                lastException = e;
                result = null;
            }
        }

        if (lastException != null) {
            log.error("Node execution failed after {} attempt(s): nodeId={}, instanceId={}",
                    maxRetries + 1, nodeExecution.getNodeId(), nodeExecution.getInstanceId(), lastException);
            nodeExecution.setStatus("FAILED");
            nodeExecution.setOutputJson(toErrorJson(lastException.getMessage()));
            nodeExecutionMapper.updateById(nodeExecution);
            throw new BaseException(ResultCode.ERROR, "Node execution failed: " + lastException.getMessage());
        }

        String status = result.isSuccess() ? "COMPLETED" : (result.isTimeout() ? "TIMEOUT" : "FAILED");
        try {
            nodeExecution.setStatus(status);
            nodeExecution.setOutputJson(objectMapper.writeValueAsString(result.getOutput()));
            nodeExecutionMapper.updateById(nodeExecution);
        } catch (Exception e) {
            log.warn("Failed to persist node execution result for nodeId={}", nodeExecution.getNodeId(), e);
        }

        // Ticket 924 trigger point 3: post-node quality check for COMPLETED
        // nodes. Best-effort — gate publication must never fail the flow.
        if (result.isSuccess() && qualityCheckPublisher != null) {
            String outputSnippet;
            try {
                String full = objectMapper.writeValueAsString(result.getOutput());
                outputSnippet = full.length() > 500 ? full.substring(0, 500) : full;
            } catch (Exception e) {
                outputSnippet = String.valueOf(result.getOutput());
            }
            qualityCheckPublisher.publishPostNodeCheck(
                    nodeExecution.getId(), nodeExecution.getInstanceId(),
                    nodeExecution.getNodeId(), nodeExecution.getTenantId(), outputSnippet);
        }
        return result;
    }

    private void sleepBeforeRetry(int attempt, String nodeId) {
        long delay = retryBaseDelayMillis * (1L << (attempt - 1));
        log.info("Retrying node {} (attempt {}/{}) after {}ms backoff",
                nodeId, attempt, maxRetries, delay);
        try {
            retrySleeper.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String toErrorJson(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message != null ? message : "unknown"));
        } catch (Exception e) {
            return "{\"error\":\"unknown\"}";
        }
    }

    private Map<String, Object> parseInput(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(inputJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse input JSON, using empty map", e);
            return Map.of();
        }
    }
}
