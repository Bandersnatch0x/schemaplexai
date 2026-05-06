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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * {@link SandboxSession} 的本地进程实现。
 *
 * <p>详见 design.md §1.3：
 * <ol>
 *   <li>每个 session 有一个独立 temp 目录作为 workspaceRoot</li>
 *   <li>所有 IO 必须经过 {@link PathSafetyGuard#resolveSafe} 4 道防御</li>
 *   <li>exec 通过 ProcessBuilder + waitFor(timeout) 实现超时控制；超时则 destroyForcibly</li>
 *   <li>close 使用 walkFileTree 递归删除 workspace；幂等</li>
 * </ol>
 */
@Slf4j
public class LocalProcessSandboxSession implements SandboxSession {

    private static final int MAX_OUTPUT_BYTES = 1 << 20;   // 1 MiB

    private final String sessionId;
    private final Path workspaceRoot;
    private final SandboxSessionConfig config;
    private final List<SandboxArtifact> artifacts = new CopyOnWriteArrayList<>();
    private volatile boolean closed = false;

    LocalProcessSandboxSession(String sessionId, Path workspaceRoot, SandboxSessionConfig config) {
        this.sessionId = sessionId;
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
        this.config = config;
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
        Path cwd = PathSafetyGuard.requireInsideRoot(workspaceRoot, command.workingDir());
        Duration timeout = command.timeout() != null ? command.timeout() : config.timeout();

        ProcessBuilder pb = new ProcessBuilder(command.argv());
        pb.directory(cwd.toFile());

        Map<String, String> env = pb.environment();
        // session-level vars first
        env.putAll(config.envVars());
        // command-level overrides
        env.putAll(command.env());
        // soft network policy: NONE marks the env so cooperating clients can opt out
        if (config.networkPolicy() == com.schemaplexai.agent.engine.tool.sandbox.NetworkPolicy.NONE) {
            env.put("SCHEMAPLEXAI_SANDBOX_NETWORK", "NONE");
        }

        Instant start = Instant.now();
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            log.warn("Failed to spawn process for session {}: {}", sessionId, e.getMessage());
            throw new SandboxException(
                    "failed to start process: " + e.getMessage(), e,
                    ToolErrorCategory.SANDBOX_ERROR);
        }

        StreamCollector stdoutCollector = new StreamCollector(process.getInputStream());
        StreamCollector stderrCollector = new StreamCollector(process.getErrorStream());
        Thread stdoutThread = new Thread(stdoutCollector, "sbx-stdout-" + sessionId);
        Thread stderrThread = new Thread(stderrCollector, "sbx-stderr-" + sessionId);
        stdoutThread.setDaemon(true);
        stderrThread.setDaemon(true);
        stdoutThread.start();
        stderrThread.start();

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new SandboxException(
                    "interrupted while waiting for process",
                    e, ToolErrorCategory.SANDBOX_ERROR);
        }

        boolean timedOut = false;
        if (!finished) {
            timedOut = true;
            process.destroyForcibly();
            try {
                process.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // join collectors
        joinQuietly(stdoutThread, 500);
        joinQuietly(stderrThread, 500);

        Duration elapsed = Duration.between(start, Instant.now());
        int exitCode = timedOut ? -1 : process.exitValue();

        return new ShellResult(
                exitCode,
                stdoutCollector.content(),
                stderrCollector.content(),
                elapsed,
                timedOut
        );
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
            PathSafetyGuard.deleteRecursively(workspaceRoot);
            log.info("Closed and cleaned sandbox session {}", sessionId);
        } catch (IOException e) {
            log.warn("Failed to fully clean sandbox session {}: {}", sessionId, e.getMessage());
        }
    }

    private void ensureOpen() throws SandboxException {
        if (closed) {
            throw new SandboxException(
                    "session " + sessionId + " is already closed",
                    ToolErrorCategory.SANDBOX_ERROR);
        }
    }

    private static void joinQuietly(Thread t, long millis) {
        try {
            t.join(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Reads a stream into a bounded buffer; truncates beyond {@link #MAX_OUTPUT_BYTES}. */
    private static final class StreamCollector implements Runnable {
        private final InputStream in;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private boolean truncated = false;

        StreamCollector(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            byte[] chunk = new byte[4096];
            try {
                int read;
                while ((read = in.read(chunk)) >= 0) {
                    if (buf.size() + read > MAX_OUTPUT_BYTES) {
                        int allowed = MAX_OUTPUT_BYTES - buf.size();
                        if (allowed > 0) {
                            buf.write(chunk, 0, allowed);
                        }
                        truncated = true;
                        // drain remaining to avoid blocking the process
                        while (in.read(chunk) >= 0) {
                            // discard
                        }
                        break;
                    }
                    buf.write(chunk, 0, read);
                }
            } catch (IOException e) {
                // swallow; partial output is acceptable
            }
        }

        String content() {
            String s = buf.toString();
            return truncated ? s + "\n[...truncated...]" : s;
        }
    }
}
