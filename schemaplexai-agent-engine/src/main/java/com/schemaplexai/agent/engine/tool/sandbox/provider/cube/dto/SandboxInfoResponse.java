package com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto;

public record SandboxInfoResponse(
    String sandboxID,
    String status,
    Long startedAt
) {
}
