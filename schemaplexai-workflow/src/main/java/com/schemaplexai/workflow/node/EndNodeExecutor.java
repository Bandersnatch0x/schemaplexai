package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class EndNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "END";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        Object result = input.get("result");
        Object status = input.getOrDefault("status", "COMPLETED");

        Map<String, Object> output = new HashMap<>();
        if (result != null) {
            output.put("result", result);
        }
        output.put("status", status);
        output.put("endedAt", Instant.now().toString());

        log.info("Workflow ended: status={}, tenantId={}", status, tenantId);
        return NodeExecutionResult.success(output);
    }
}
