package com.schemaplexai.agent.engine.tool.sandbox;

import java.time.Duration;

/**
 * Shell 命令的执行结果。
 *
 * @param exitCode 退出码；超时 / 强制终止时为 -1
 * @param stdout   标准输出（截断到实现限定的最大字节数）
 * @param stderr   错误输出（同样可能被截断）
 * @param elapsed  实际耗时
 * @param timedOut 是否因为超时被强制终止
 */
public record ShellResult(
        int exitCode,
        String stdout,
        String stderr,
        Duration elapsed,
        boolean timedOut
) {

    public ShellResult {
        if (stdout == null) {
            stdout = "";
        }
        if (stderr == null) {
            stderr = "";
        }
        if (elapsed == null) {
            elapsed = Duration.ZERO;
        }
    }

    public boolean isSuccess() {
        return !timedOut && exitCode == 0;
    }
}
