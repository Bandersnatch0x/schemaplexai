package com.schemaplexai.agent.engine.tool.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 在沙箱会话中执行的 shell 命令描述。
 *
 * @param argv       命令与参数；不可为空
 * @param env        额外环境变量（覆盖会话级 env）
 * @param timeout    本次执行的超时；null 表示采用会话级 timeout
 * @param workingDir 执行目录；null 表示 workspaceRoot；非 null 时由实现校验是否在 workspaceRoot 内
 */
public record ShellCommand(
        List<String> argv,
        Map<String, String> env,
        Duration timeout,
        Path workingDir
) {

    public ShellCommand {
        Objects.requireNonNull(argv, "argv required");
        if (argv.isEmpty()) {
            throw new IllegalArgumentException("argv must not be empty");
        }
        if (timeout != null && (timeout.isNegative() || timeout.isZero())) {
            throw new IllegalArgumentException("timeout must be positive when set");
        }
        argv = List.copyOf(argv);
        env = env == null ? Map.of() : Map.copyOf(env);
    }

    /** 便捷构造：无 env、无自定义超时、使用 workspaceRoot 作为工作目录 */
    public static ShellCommand of(String... command) {
        return new ShellCommand(List.of(command), Map.of(), null, null);
    }
}
