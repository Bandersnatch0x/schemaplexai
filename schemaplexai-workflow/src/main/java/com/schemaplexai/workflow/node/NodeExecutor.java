package com.schemaplexai.workflow.node;

import java.util.Map;

public interface NodeExecutor {

    String getNodeType();

    NodeExecutionResult execute(Map<String, Object> input, String tenantId);
}
