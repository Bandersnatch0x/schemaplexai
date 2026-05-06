package com.schemaplexai.agent.engine.tool.sandbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 沙箱会话级配置（取代仅限单次调用的 {@link com.schemaplexai.agent.engine.tool.SandboxConfig}）。
 *
 * <p>对应 design.md §1.2：在 timeout / memory / cpu 之上新增工作镜像、环境变量、
 * 网络策略与挂载点声明，便于不同 Provider 实现（本地、容器、云沙箱）落地。
 */
public record SandboxSessionConfig(
        Duration timeout,
        long memoryLimitMb,
        long cpuLimitMillis,
        String workspaceImage,
        Map<String, String> envVars,
        NetworkPolicy networkPolicy,
        List<MountSpec> mountPaths
) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final long DEFAULT_MEMORY_LIMIT_MB = 512L;
    private static final long DEFAULT_CPU_LIMIT_MILLIS = 30_000L;

    public SandboxSessionConfig {
        Objects.requireNonNull(timeout, "timeout required");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (memoryLimitMb <= 0) {
            throw new IllegalArgumentException("memoryLimitMb must be positive");
        }
        if (cpuLimitMillis <= 0) {
            throw new IllegalArgumentException("cpuLimitMillis must be positive");
        }
        envVars = envVars == null ? Map.of() : Map.copyOf(envVars);
        mountPaths = mountPaths == null ? List.of() : List.copyOf(mountPaths);
        networkPolicy = networkPolicy == null ? NetworkPolicy.NONE : networkPolicy;
    }

    public static SandboxSessionConfig defaults() {
        return new SandboxSessionConfig(
                DEFAULT_TIMEOUT,
                DEFAULT_MEMORY_LIMIT_MB,
                DEFAULT_CPU_LIMIT_MILLIS,
                null,
                Map.of(),
                NetworkPolicy.NONE,
                List.of()
        );
    }
}
