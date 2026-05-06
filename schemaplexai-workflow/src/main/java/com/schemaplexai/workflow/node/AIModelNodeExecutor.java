package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AIModelNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

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

        String generatedText = callAgentEngine(prompt, modelUsed, tenantId);

        Map<String, Object> output = new HashMap<>();
        output.put("generatedText", generatedText);
        output.put("modelUsed", modelUsed);

        log.info("AI model node executed: modelUsed={}, promptLength={}, tenantId={}",
                modelUsed, prompt.length(), tenantId);
        return NodeExecutionResult.success(output);
    }

    private String callAgentEngine(String prompt, String modelUsed, String tenantId) {
        if (agentEngineUrl == null || agentEngineUrl.isBlank()) {
            log.warn("agent.engine.url not configured, falling back to simulated response");
            return simulateResponse(prompt);
        }

        String url = agentEngineUrl + "/agent/execute";
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("modelId", modelUsed);
            requestBody.put("tenantId", tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            log.info("Calling agent-engine LLM service at: {}", url);
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.get("data") != null) {
                return response.get("data").toString();
            }
            log.warn("Agent-engine returned empty response, falling back to simulated");
            return simulateResponse(prompt);

        } catch (RestClientException e) {
            log.error("Failed to call agent-engine at {}: {}", url, e.getMessage());
            return simulateResponse(prompt);
        }
    }

    private String simulateResponse(String prompt) {
        String preview = prompt.substring(0, Math.min(50, prompt.length()));
        return "[AI response for: " + preview + "...]";
    }
}
