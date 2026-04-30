package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class HttpNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "HTTP";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String url = (String) input.get("url");
        String method = (String) input.getOrDefault("method", "GET");
        log.info("Executing HTTP node: {} {}", method, url);
        // Phase 1: Placeholder — actual HTTP call via RestTemplate/WebClient to be added
        return NodeExecutionResult.success(Map.of("statusCode", 200, "body", "placeholder"));
    }
}
