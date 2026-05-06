package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxProvider;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 默认 {@link SandboxProvider} 实现：基于本地进程与临时目录。
 *
 * <p>用途：开发 / CI / 受控工作流。隔离强度低于容器或云沙箱，对应 ADR-A1：
 * Phase 1 不引入外部依赖，先通过抽象层把接口打通。后续 E2B / Daytona Provider
 * 在不动调用方代码的前提下替换。
 */
@Slf4j
@Component("localProcessSandbox")
public class LocalProcessSandbox implements SandboxProvider {

    private static final String PROVIDER_ID = "local";
    private static final String WORKSPACE_PREFIX = "splx-sbx-";

    @Override
    public SandboxSession create(SandboxSessionConfig config) throws SandboxException {
        if (config == null) {
            throw new SandboxException("config required", ToolErrorCategory.SANDBOX_ERROR);
        }
        String sessionId = UUID.randomUUID().toString();
        try {
            Path workspaceRoot = Files.createTempDirectory(WORKSPACE_PREFIX + sessionId + "-");
            log.info("Created sandbox session {} at {}", sessionId, workspaceRoot);
            return new LocalProcessSandboxSession(sessionId, workspaceRoot, config);
        } catch (IOException e) {
            throw new SandboxException(
                    "failed to create workspace directory", e, ToolErrorCategory.SANDBOX_ERROR);
        }
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }
}
