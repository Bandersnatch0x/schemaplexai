package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.ArtifactKind;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxArtifact;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import com.schemaplexai.agent.engine.tool.sandbox.ShellCommand;
import com.schemaplexai.agent.engine.tool.sandbox.ShellResult;
import com.schemaplexai.agent.engine.tool.sandbox.util.PathSafetyGuard;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link SandboxSession} backed by a Docker container.
 *
 * <p>All filesystem operations are performed against the host bind mount
 * directory (the workspace root). The container itself provides process
 * isolation; IO does not go through the Docker API.
 *
 * <p>{@link #close()} stops and removes the underlying Docker container,
 * then deletes the host workspace directory.
 */
@Slf4j
public class ContainerSandboxSession implements SandboxSession {

    private final String sessionId;
    private final String containerId;
    private final SandboxSessionConfig config;
    private final Path workspaceRoot;
    private final DockerClientAdapter docker;
    private final List<SandboxArtifact> artifacts = new CopyOnWriteArrayList<>();
    private volatile boolean closed = false;

    ContainerSandboxSession(String sessionId, String containerId,
                            SandboxSessionConfig config, Path workspaceRoot,
                            DockerClientAdapter docker) {
        this.sessionId = sessionId;
        this.containerId = containerId;
        this.config = config;
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.docker = docker;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public Path workspaceRoot() {
        return workspaceRoot;
    }

    @Override
    public ShellResult exec(ShellCommand command) throws SandboxException {
        ensureOpen();
        if (command == null) {
            throw new SandboxException("command required", ToolErrorCategory.INVALID_ARGUMENT);
        }
        Duration timeout = command.timeout() != null ? command.timeout() : config.timeout();
        Path cwd = PathSafetyGuard.requireInsideRoot(workspaceRoot, command.workingDir());

        try {
            return docker.execInContainer(containerId, command.argv(), command.env(),
                    cwd.toString(), timeout);
        } catch (Exception e) {
            throw new SandboxException(
                    "container exec failed: " + e.getMessage(), e,
                    ToolErrorCategory.SANDBOX_ERROR);
        }
    }

    @Override
    public void writeFile(Path relativePath, byte[] content) throws SandboxException {
        ensureOpen();
        if (content == null) {
            throw new SandboxException("content required", ToolErrorCategory.INVALID_ARGUMENT);
        }
        Path target = PathSafetyGuard.resolveSafe(workspaceRoot, relativePath);
        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            artifacts.add(new SandboxArtifact(
                    workspaceRoot.relativize(target),
                    content.length,
                    Instant.now(),
                    ArtifactKind.FILE
            ));
        } catch (IOException e) {
            throw new SandboxException(
                    "failed to write file: " + relativePath, e,
                    ToolErrorCategory.SANDBOX_ERROR);
        }
    }

    @Override
    public byte[] readFile(Path relativePath) throws SandboxException {
        ensureOpen();
        Path target = PathSafetyGuard.resolveSafe(workspaceRoot, relativePath);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new SandboxException(
                    "failed to read file: " + relativePath, e,
                    ToolErrorCategory.SANDBOX_ERROR);
        }
    }

    @Override
    public List<SandboxArtifact> artifacts() {
        return List.copyOf(artifacts);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            docker.stopContainer(containerId);
            docker.removeContainer(containerId);
            log.info("Stopped and removed container {} for session {}", containerId, sessionId);
        } catch (Exception e) {
            log.warn("Failed to stop/remove container {}: {}", containerId, e.getMessage());
        }
        try {
            PathSafetyGuard.deleteRecursively(workspaceRoot);
            log.info("Cleaned workspace for session {}", sessionId);
        } catch (IOException e) {
            log.warn("Failed to clean workspace for session {}: {}", sessionId, e.getMessage());
        }
    }

    private void ensureOpen() throws SandboxException {
        if (closed) {
            throw new SandboxException(
                    "session " + sessionId + " is already closed",
                    ToolErrorCategory.SANDBOX_ERROR);
        }
    }
}
