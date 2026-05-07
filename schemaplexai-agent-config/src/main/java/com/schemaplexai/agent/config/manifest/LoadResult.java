package com.schemaplexai.agent.config.manifest;

import java.nio.file.Path;

/**
 * 单个 AGENTS.md 文件的加载结果。
 *
 * @param file     源文件路径
 * @param success  是否成功
 * @param agentId  加载后的 agent ID（失败时为 null）
 * @param name     manifest 名称（失败时为 null）
 * @param error    失败原因（成功时为 null）
 */
public record LoadResult(
        Path file,
        boolean success,
        Long agentId,
        String name,
        String error
) {

    public static LoadResult ok(Path file, Long agentId, String name) {
        return new LoadResult(file, true, agentId, name, null);
    }

    public static LoadResult failed(Path file, String error) {
        return new LoadResult(file, false, null, null, error);
    }
}
