package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AIModelNodeExecutor implements NodeExecutor {

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

        String preview = prompt.substring(0, Math.min(50, prompt.length()));
        String generatedText = "[AI response for: " + preview + "...]";

        Map<String, Object> output = new HashMap<>();
        output.put("generatedText", generatedText);
        output.put("modelUsed", modelUsed);

        log.info("AI model node executed: modelUsed={}, promptLength={}, tenantId={}",
                modelUsed, prompt.length(), tenantId);
        return NodeExecutionResult.success(output);
    }
}
