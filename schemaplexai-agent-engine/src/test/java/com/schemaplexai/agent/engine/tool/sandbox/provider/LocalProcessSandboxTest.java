package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import com.schemaplexai.agent.engine.tool.sandbox.ShellCommand;
import com.schemaplexai.agent.engine.tool.sandbox.ShellResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalProcessSandboxTest {

    private final LocalProcessSandbox sandbox = new LocalProcessSandbox();

    @Test
    void shouldExposeProviderId() {
        assertEquals("local", sandbox.providerId());
    }

    @Test
    void shouldCreateSessionWithExistingWorkspace() throws Exception {
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            assertNotNull(session.sessionId());
            Path root = session.workspaceRoot();
            assertTrue(Files.exists(root));
            assertTrue(Files.isDirectory(root));
        }
    }

    @Test
    void shouldExecuteSimpleCommand() throws Exception {
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            ShellResult result = session.exec(echo("hello"));
            assertTrue(result.isSuccess(), "stderr=" + result.stderr());
            assertTrue(result.stdout().contains("hello"));
            assertEquals(0, result.exitCode());
            assertFalse(result.timedOut());
        }
    }

    @Test
    void shouldTimeoutLongRunningCommand() throws Exception {
        SandboxSessionConfig config = new SandboxSessionConfig(
                Duration.ofMillis(500),
                512L,
                30_000L,
                null,
                Map.of(),
                com.schemaplexai.agent.engine.tool.sandbox.NetworkPolicy.NONE,
                List.of()
        );
        try (SandboxSession session = sandbox.create(config)) {
            ShellResult result = session.exec(sleep(5));
            assertTrue(result.timedOut(), "expected timeout");
            assertNotEquals(0, result.exitCode());
        }
    }

    @Test
    void shouldRejectTraversalOnWriteFile() throws Exception {
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            SandboxException ex = assertThrows(SandboxException.class,
                    () -> session.writeFile(Path.of("../escape.txt"), new byte[]{1, 2, 3}));
            assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
        }
    }

    @Test
    void shouldRejectHiddenFile() throws Exception {
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            assertThrows(SandboxException.class,
                    () -> session.writeFile(Path.of(".secret"), new byte[]{1}));
        }
    }

    @Test
    void shouldWriteAndReadBack() throws Exception {
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            byte[] payload = "hello world".getBytes();
            session.writeFile(Path.of("input.txt"), payload);
            byte[] read = session.readFile(Path.of("input.txt"));
            assertArrayEquals(payload, read);
            assertEquals(1, session.artifacts().size());
        }
    }

    @Test
    void shouldCleanupWorkspaceOnClose() throws Exception {
        Path captured;
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            captured = session.workspaceRoot();
            session.writeFile(Path.of("a.txt"), new byte[]{0});
        }
        assertFalse(Files.exists(captured),
                "workspace should be deleted after close: " + captured);
    }

    @Test
    void shouldCleanupEvenAfterExecFailure() throws Exception {
        Path captured;
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            captured = session.workspaceRoot();
            // fire a non-existent command, expect a graceful failure (not a crash on close)
            try {
                ShellResult r = session.exec(ShellCommand.of("non-existent-cmd-xyz"));
                // if exec returned (e.g. via spawn failure converted to result), it's still fine
                assertNotNull(r);
            } catch (SandboxException ignored) {
                // also acceptable
            }
        }
        assertFalse(Files.exists(captured));
    }

    @Test
    void shouldSurviveCloseWithoutAnyExec() throws Exception {
        SandboxSession session = sandbox.create(SandboxSessionConfig.defaults());
        Path root = session.workspaceRoot();
        session.close();
        assertFalse(Files.exists(root));
    }

    @Test
    void shouldRejectExecAfterClose() throws Exception {
        SandboxSession session = sandbox.create(SandboxSessionConfig.defaults());
        session.close();
        assertThrows(SandboxException.class,
                () -> session.exec(ShellCommand.of("echo", "hi")));
    }

    // --- platform helpers ---

    private static ShellCommand echo(String text) {
        if (isWindows()) {
            return new ShellCommand(List.of("cmd", "/c", "echo", text), Map.of(), null, null);
        }
        return ShellCommand.of("echo", text);
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void shouldHonorEnvVars() throws Exception {
        SandboxSessionConfig config = new SandboxSessionConfig(
                Duration.ofSeconds(10),
                512L,
                30_000L,
                null,
                Map.of("MY_VAR", "abc123"),
                com.schemaplexai.agent.engine.tool.sandbox.NetworkPolicy.NONE,
                List.of()
        );
        try (SandboxSession session = sandbox.create(config)) {
            ShellResult result = session.exec(
                    new ShellCommand(List.of("sh", "-c", "echo $MY_VAR"), Map.of(), null, null));
            assertTrue(result.stdout().contains("abc123"));
        }
    }

    private static ShellCommand sleep(int seconds) {
        if (isWindows()) {
            return new ShellCommand(
                    List.of("cmd", "/c", "ping", "-n", String.valueOf(seconds + 1), "127.0.0.1"),
                    Map.of(), null, null);
        }
        return new ShellCommand(List.of("sleep", String.valueOf(seconds)), Map.of(), null, null);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
