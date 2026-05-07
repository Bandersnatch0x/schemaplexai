package com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto;

public record ExecRequest(
    String command,
    String stdin,
    Integer timeoutMs
) {
}
