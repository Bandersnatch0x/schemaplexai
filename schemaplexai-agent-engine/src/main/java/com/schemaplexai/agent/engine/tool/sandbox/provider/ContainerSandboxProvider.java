package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxProvider;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Container-level {@link SandboxProvider} backed by Docker.
 *
 * <p>Creates isolated Docker containers per sandbox session with:
 * <ul>
 *   <li>{@code --network none} for network isolation</li>
 *   <li>{@code --read-only} root filesystem</li>
 *   <li>Memory and CPU limits from {@link SandboxSessionConfig}</li>
 *   <li>Host bind mount to {@code /workspace}</li>
 * </ul>
 *
 * <p>Only activated when {@code sandbox.provider=container} is set in configuration.
 * If the Docker Java API is not available on the classpath, this provider uses
 * compile-safe stub classes that mirror the required Docker Java types.
 *
 * @see ContainerSandboxSession
 */
@Slf4j
@Component("containerSandbox")
@ConditionalOnProperty(name = "sandbox.provider", havingValue = "container")
public class ContainerSandboxProvider implements SandboxProvider {

    private static final String PROVIDER_ID = "container";
    private static final String WORKSPACE_PREFIX = "splx-csbx-";
    private static final String DEFAULT_IMAGE = "sandbox-base:latest";
    private static final String WORKSPACE_CONTAINER_PATH = "/workspace";

    private final DockerClientAdapter docker;

    /**
     * Default constructor — attempts to create a real Docker client.
     * If Docker Java API is unavailable, falls back to a no-op stub.
     */
    public ContainerSandboxProvider() {
        this.docker = DockerClientAdapter.create();
    }

    /** Package-private constructor for testing with a custom Docker adapter. */
    ContainerSandboxProvider(DockerClientAdapter docker) {
        this.docker = docker;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public SandboxSession create(SandboxSessionConfig config) throws SandboxException {
        if (config == null) {
            throw new SandboxException("config required", ToolErrorCategory.SANDBOX_ERROR);
        }
        if (!docker.isAvailable()) {
            throw new SandboxException(
                    "Docker is not available", ToolErrorCategory.SANDBOX_ERROR);
        }

        String sessionId = UUID.randomUUID().toString();
        Path workspaceRoot;
        try {
            workspaceRoot = Files.createTempDirectory(WORKSPACE_PREFIX + sessionId + "-");
        } catch (IOException e) {
            throw new SandboxException(
                    "failed to create workspace directory", e, ToolErrorCategory.SANDBOX_ERROR);
        }

        String image = config.workspaceImage() != null ? config.workspaceImage() : DEFAULT_IMAGE;
        String containerId;
        try {
            containerId = docker.createContainer(buildCreateOptions(config, image, workspaceRoot));
            docker.startContainer(containerId);
            log.info("Created container sandbox session {} (containerId={})", sessionId, containerId);
        } catch (Exception e) {
            cleanupQuietly(workspaceRoot, null);
            throw new SandboxException(
                    "failed to create container sandbox: " + e.getMessage(), e,
                    ToolErrorCategory.SANDBOX_ERROR);
        }

        return new ContainerSandboxSession(sessionId, containerId, config, workspaceRoot, docker);
    }

    private DockerCreateOptions buildCreateOptions(SandboxSessionConfig config, String image, Path workspaceRoot) {
        DockerCreateOptions options = new DockerCreateOptions();
        options.setImage(image);
        options.setNetworkMode("none");
        options.setReadOnlyRootfs(true);
        options.setMemoryLimitBytes(config.memoryLimitMb() * 1024 * 1024);
        options.setCpuQuotaMicros(config.cpuLimitMillis() * 1000L);
        options.setAutoRemove(false);
        options.setEnv(config.envVars());
        options.addBindMount(workspaceRoot.toAbsolutePath().toString(), WORKSPACE_CONTAINER_PATH, false);
        return options;
    }

    static void cleanupQuietly(Path workspaceRoot, String containerId) {
        if (containerId != null) {
            try {
                DockerClientAdapter.create().stopContainer(containerId);
                DockerClientAdapter.create().removeContainer(containerId);
            } catch (Exception e) {
                log.warn("Failed to cleanup container {}: {}", containerId, e.getMessage());
            }
        }
        if (workspaceRoot != null) {
            try {
                com.schemaplexai.agent.engine.tool.sandbox.util.PathSafetyGuard.deleteRecursively(workspaceRoot);
            } catch (IOException e) {
                log.warn("Failed to cleanup workspace {}: {}", workspaceRoot, e.getMessage());
            }
        }
    }
}
