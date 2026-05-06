package com.schemaplexai.agent.engine.tool.sandbox.util;

import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathSafetyGuardTest {

    @Test
    void shouldAcceptPathInsideRoot(@TempDir Path root) throws Exception {
        Path resolved = PathSafetyGuard.resolveSafe(root, Path.of("subdir/file.txt"));
        assertTrue(resolved.startsWith(root.toAbsolutePath().normalize()));
    }

    @Test
    void shouldRejectTraversal(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("../escape.txt")));
        assertTrue(ex.getMessage().toLowerCase().contains("outside")
                || ex.getMessage().toLowerCase().contains("traversal"));
    }

    @Test
    void shouldRejectAbsolutePath(@TempDir Path root) {
        Path absolute = root.toAbsolutePath().getRoot().resolve("etc/passwd");
        assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, absolute));
    }

    @Test
    void shouldRejectHiddenComponent(@TempDir Path root) {
        assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of(".secret")));
    }

    @Test
    void shouldRejectHiddenInDeepPath(@TempDir Path root) {
        assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("a/.config/file.txt")));
    }

    @Test
    void shouldRejectSymlinkComponent(@TempDir Path root) throws Exception {
        Path target = root.resolve("real");
        Files.createDirectory(target);
        Path link = root.resolve("link");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            // Skip on systems without symlink permission (Windows non-admin)
            return;
        }
        assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("link/file.txt")));
    }
}
