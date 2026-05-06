package com.schemaplexai.workflow.node;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class StartNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "START";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        Object workflowInstanceId = input.get("workflowInstanceId");
        if (workflowInstanceId == null) {
            return NodeExecutionResult.failure("Missing required field: workflowInstanceId");
        }

        Map<String, Object> output = new HashMap<>();
        output.put("workflowInstanceId", workflowInstanceId);
        output.put("startedAt", Instant.now().toString());

        log.info("Workflow started: instanceId={}, tenantId={}", workflowInstanceId, tenantId);
        return NodeExecutionResult.success(output);
    }
}
