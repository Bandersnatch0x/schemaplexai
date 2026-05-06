package com.schemaplexai.agent.engine.tool.sandbox;

import java.nio.file.Path;
import java.util.List;

/**
 * 一个隔离的沙箱执行会话。
 *
 * <p>典型生命周期：
 * <pre>{@code
 *     try (SandboxSession s = provider.create(config)) {
 *         s.writeFile(Path.of("input.txt"), bytes);
 *         ShellResult r = s.exec(ShellCommand.of("python", "run.py"));
 *         byte[] output = s.readFile(Path.of("output.txt"));
 *     }   // close() 清理 workspace
 * }</pre>
 *
 * <p>所有 IO 操作必须落在 {@link #workspaceRoot()} 内；实现需复用
 * {@link com.schemaplexai.agent.engine.tool.adapter.file.FileReadAdapter} 的
 * 4 道路径防御（normalize、startsWith、symlink NOFOLLOW、hidden 拒绝）。
 */
public interface SandboxSession extends AutoCloseable {

    /**
     * 唯一会话 ID（UUID 推荐）。
     */
    String sessionId();

    /**
     * 工作空间根目录（绝对路径，已 normalize）。
     */
    Path workspaceRoot();

    /**
     * 在沙箱内执行 shell 命令；遵守 {@link NetworkPolicy} 与超时限制。
     */
    ShellResult exec(ShellCommand command) throws SandboxException;

    /**
     * 在 workspace 内写文件。
     *
     * @param relativePath 相对 workspaceRoot 的路径；不可越界、不可 symlink、不可 hidden
     */
    void writeFile(Path relativePath, byte[] content) throws SandboxException;

    /**
     * 读 workspace 内的文件。
     */
    byte[] readFile(Path relativePath) throws SandboxException;

    /**
     * 列出会话产出物（执行完后可调用）。
     */
    List<SandboxArtifact> artifacts();

    /**
     * 关闭并清理 workspace。
     *
     * <p>实现必须做到：即使 {@link #exec(ShellCommand)} 抛异常，close() 仍能将
     * workspace 完整清理；close() 自身不抛 checked 异常。
     */
    @Override
    void close();
}
