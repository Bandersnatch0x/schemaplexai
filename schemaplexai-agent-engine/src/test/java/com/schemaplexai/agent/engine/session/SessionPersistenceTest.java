package com.schemaplexai.agent.engine.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxArtifact;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.ShellCommand;
import com.schemaplexai.agent.engine.tool.sandbox.ShellResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionPersistenceTest {

    @TempDir
    Path tempDir;

    private AgentSessionPersistence createPersistence() {
        AgentSessionPersistence persistence = new AgentSessionPersistence();
        persistence.setSnapshotRoot(tempDir);
        return persistence;
    }

    @Test
    void shouldPersistAndRestoreSandboxState() throws Exception {
        AgentSessionPersistence persistence = createPersistence();

        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(42L);
        execution.setMetadata("key", "value");

        Path workspace = Files.createTempDirectory("sandbox-");
        Files.writeString(workspace.resolve("hello.txt"), "world");

        StubSandboxSession sandbox = new StubSandboxSession(workspace);

        persistence.persistSession(execution, sandbox);

        Path snapshotDir = tempDir.resolve("42");
        assertTrue(Files.exists(snapshotDir.resolve("workspace/hello.txt")), "workspace file should be persisted");
        assertTrue(Files.exists(snapshotDir.resolve("metadata.json")), "metadata should be persisted");

        Map<String, Object> metadata = new ObjectMapper().readValue(snapshotDir.resolve("metadata.json").toFile(), Map.class);
        assertEquals("value", metadata.get("key"));

        Path newWorkspace = Files.createTempDirectory("sandbox-restore-");
        StubSandboxSession restoreSandbox = new StubSandboxSession(newWorkspace);
        persistence.restoreSession(execution, restoreSandbox);

        assertEquals("world", Files.readString(newWorkspace.resolve("hello.txt")));

        Files.walk(workspace).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
        });
        Files.walk(newWorkspace).sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
            try { Files.deleteIfExists(p); } catch (IOException ignored) {}
        });
    }

    @Test
    void shouldHandleMissingSnapshotGracefully() {
        AgentSessionPersistence persistence = createPersistence();

        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(999L);

        Path workspace = tempDir.resolve("no-snapshot-workspace");
        StubSandboxSession sandbox = new StubSandboxSession(workspace);

        assertDoesNotThrow(() -> persistence.restoreSession(execution, sandbox));
    }

    static class StubSandboxSession implements SandboxSession {
        private final Path workspaceRoot;

        StubSandboxSession(Path workspaceRoot) {
            this.workspaceRoot = workspaceRoot;
        }

        @Override
        public String sessionId() {
            return "stub-1";
        }

        @Override
        public Path workspaceRoot() {
            return workspaceRoot;
        }

        @Override
        public ShellResult exec(ShellCommand command) throws SandboxException {
            return null;
        }

        @Override
        public void writeFile(Path relativePath, byte[] content) throws SandboxException {
        }

        @Override
        public byte[] readFile(Path relativePath) throws SandboxException {
            return new byte[0];
        }

        @Override
        public List<SandboxArtifact> artifacts() {
            return List.of();
        }

        @Override
        public void close() {
        }
    }
}
