package com.schemaplexai.agent.engine.tool.sandbox;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * 一个沙箱会话产出物（写入到 workspace 的文件、日志或 snapshot）。
 *
 * @param relativePath 相对 workspaceRoot 的路径
 * @param sizeBytes    文件大小（字节）
 * @param createdAt    创建时间
 * @param kind         种类
 */
public record SandboxArtifact(
        Path relativePath,
        long sizeBytes,
        Instant createdAt,
        ArtifactKind kind
) {

    public SandboxArtifact {
        Objects.requireNonNull(relativePath, "relativePath required");
        Objects.requireNonNull(createdAt, "createdAt required");
        Objects.requireNonNull(kind, "kind required");
        if (sizeBytes < 0) {
            throw new IllegalArgumentException("sizeBytes must be non-negative");
        }
    }
}
