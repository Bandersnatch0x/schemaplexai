package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AIModelNodeExecutor implements NodeExecutor {

    /** Spec §3.2 / §8: Agent node default timeout is 300 seconds. */
    static final int DEFAULT_TIMEOUT_SECONDS = 300;
    /** Connect bound kept short; the read timeout carries the per-node budget. */
    private static final int CONNECT_TIMEOUT_SECONDS = 10;

    @Value("${agent.engine.url:http://localhost:8084}")
    private String agentEngineUrl;

    @Override
    public String getNodeType() {
        return "AI_MODEL";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String prompt = (String) input.get("prompt");
        if (prompt == null || prompt.isBlank()) {
            return NodeExecutionResult.failure("Missing or empty required field: prompt");
        }

        String modelId = (String) input.get("modelId");
        String modelUsed = modelId != null ? modelId : "default";
        Object agentId = input.get("agentId");
        int timeoutSeconds = readTimeoutSeconds(input);
        boolean waitForCompletion = readWaitForCompletion(input);

        NodeExecutionResult agentResult =
                callAgentEngine(prompt, modelUsed, agentId, tenantId, timeoutSeconds, waitForCompletion);
        if (!agentResult.isSuccess()) {
            return agentResult;
        }

        Map<String, Object> output = new HashMap<>(agentResult.getOutput());
        output.put("modelUsed", modelUsed);
        if (agentId != null) {
            output.put("agentId", String.valueOf(agentId));
        }

        log.info("AI model node executed: modelUsed={}, timeoutSeconds={}, promptLength={}, tenantId={}",
                modelUsed, timeoutSeconds, prompt.length(), tenantId);
        return NodeExecutionResult.success(output);
    }

    private int readTimeoutSeconds(Map<String, Object> input) {
        Object raw = input.get("timeoutSeconds");
        if (raw instanceof Number number) {
            int value = number.intValue();
            if (value > 0) {
                return value;
            }
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    private boolean readWaitForCompletion(Map<String, Object> input) {
        Object raw = input.get("waitForCompletion");
        if (raw instanceof Boolean flag) {
            return flag;
        }
        return true;
    }

    private NodeExecutionResult callAgentEngine(String prompt, String modelUsed, Object agentId,
                                                String tenantId, int timeoutSeconds,
                                                boolean waitForCompletion) {
        if (agentEngineUrl == null || agentEngineUrl.isBlank()) {
            log.warn("agent.engine.url not configured; AI_MODEL node cannot execute");
            return NodeExecutionResult.failure("agent-engine URL is not configured");
        }

        String url = agentEngineUrl + "/agent/execute";
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("modelId", modelUsed);
            requestBody.put("tenantId", tenantId);
            if (agentId != null) {
                requestBody.put("agentId", String.valueOf(agentId));
            }
            requestBody.put("waitForCompletion", waitForCompletion);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Calling agent-engine LLM service at: {} (timeout={}s, waitForCompletion={})",
                    url, timeoutSeconds, waitForCompletion);
            Map<String, Object> response = buildRestTemplate(timeoutSeconds)
                    .postForObject(url, request, Map.class);

            if (response != null && response.get("data") != null) {
                return NodeExecutionResult.success(Map.of("generatedText", response.get("data").toString()));
            }
            log.warn("agent-engine returned empty response for AI_MODEL node");
            return NodeExecutionResult.retryableFailure("agent-engine returned empty response");

        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                log.error("AI model node timed out after {}s calling {}", timeoutSeconds, url);
                return NodeExecutionResult.timeout(
                        "agent-engine call timed out after " + timeoutSeconds + "s");
            }
            log.error("Failed to call agent-engine at {} (transient): {}", url, e.getMessage());
            return NodeExecutionResult.retryableFailure("agent-engine request failed: " + e.getMessage());
        } catch (RestClientException e) {
            log.error("Failed to call agent-engine at {}: {}", url, e.getMessage());
            return NodeExecutionResult.retryableFailure("agent-engine request failed: " + e.getMessage());
        }
    }

    private boolean isTimeout(ResourceAccessException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private RestTemplate buildRestTemplate(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.min(CONNECT_TIMEOUT_SECONDS, timeoutSeconds)));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return new RestTemplate(factory);
    }
}
