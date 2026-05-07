package com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto;

public record ExecResponse(
    Integer exitCode,
    String stdout,
    String stderr
) {
}
