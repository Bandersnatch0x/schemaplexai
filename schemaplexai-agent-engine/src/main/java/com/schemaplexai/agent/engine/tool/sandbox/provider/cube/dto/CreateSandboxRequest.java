package com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto;

import java.util.Map;

public record CreateSandboxRequest(
    String templateID,
    Map<String, String> envVars,
    Integer timeoutMs
) {
    public CreateSandboxRequest {
        if (templateID == null) templateID = "default";
    }
}
