package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ScriptNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "SCRIPT";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String script = (String) input.get("script");
        log.info("Executing SCRIPT node with script: {}", script);
        // Phase 1: Placeholder — actual script execution (e.g., Groovy/JS) to be added
        return NodeExecutionResult.success(Map.of("result", "script executed"));
    }
}
