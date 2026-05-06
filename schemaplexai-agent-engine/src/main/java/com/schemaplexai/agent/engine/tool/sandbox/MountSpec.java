package com.schemaplexai.agent.engine.tool.sandbox;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 一个绑定挂载（host → workspace）的描述。
 *
 * <p>{@link com.schemaplexai.agent.engine.tool.sandbox.provider.LocalProcessSandbox}
 * 实现仅支持 {@code readOnly = true} 的语义性挂载（不强制 OS 级别只读，由 Provider 自行
 * 决定如何投影）。Container 类 Provider 应通过 {@code -v host:workspace:ro} 挂载。
 */
public record MountSpec(Path hostPath, Path containerPath, boolean readOnly) {

    public MountSpec {
        Objects.requireNonNull(hostPath, "hostPath required");
        Objects.requireNonNull(containerPath, "containerPath required");
    }
}
