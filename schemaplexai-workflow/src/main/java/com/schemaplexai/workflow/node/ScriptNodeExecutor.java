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
        return NodeExecutionResult.failure("SCRIPT node execution is not implemented. Configure a script runtime before enabling SCRIPT nodes.");
    }
}
