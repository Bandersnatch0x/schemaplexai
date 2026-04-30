package com.schemaplexai.workflow.node;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    private boolean success;
    private String message;
    private Map<String, Object> output;

    public static NodeExecutionResult success(Map<String, Object> output) {
        return new NodeExecutionResult(true, null, output);
    }

    public static NodeExecutionResult success() {
        return new NodeExecutionResult(true, null, Map.of());
    }

    public static NodeExecutionResult failure(String message) {
        return new NodeExecutionResult(false, message, Map.of());
    }
}
