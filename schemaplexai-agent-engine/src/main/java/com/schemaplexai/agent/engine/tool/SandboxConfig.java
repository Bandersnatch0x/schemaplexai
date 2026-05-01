package com.schemaplexai.agent.engine.tool;

import java.time.Duration;

/**
 * 沙箱执行配置，定义资源限制。
 */
public record SandboxConfig(Duration timeout, long memoryLimitMb, long cpuLimitMillis) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final long DEFAULT_MEMORY_LIMIT_MB = 512;
    private static final long DEFAULT_CPU_LIMIT_MILLIS = 1000;

    public static SandboxConfig defaultConfig() {
        return new SandboxConfig(DEFAULT_TIMEOUT, DEFAULT_MEMORY_LIMIT_MB, DEFAULT_CPU_LIMIT_MILLIS);
    }
}
