package com.schemaplexai.agent.engine.tool.sandbox.util;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathSafetyGuardAdditionalTest {

    @Test
    void shouldRejectNullRoot() {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(null, Path.of("file.txt")));
        assertEquals(ToolErrorCategory.SANDBOX_ERROR, ex.getCategory());
        assertTrue(ex.getMessage().contains("workspace root is null"));
    }

    @Test
    void shouldRejectNullRelativePath(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, null));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("relative path is null"));
    }

    @Test
    void shouldAcceptSimpleFile(@TempDir Path root) throws Exception {
        Path resolved = PathSafetyGuard.resolveSafe(root, Path.of("file.txt"));
        assertEquals(root.resolve("file.txt").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void shouldAcceptNestedPath(@TempDir Path root) throws Exception {
        Path resolved = PathSafetyGuard.resolveSafe(root, Path.of("a/b/c/file.txt"));
        assertTrue(resolved.startsWith(root.toAbsolutePath().normalize()));
    }

    @Test
    void shouldRejectTraversalWithDotDot(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("a/../../outside.txt")));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
    }

    @Test
    void shouldRejectTraversalStartingWithDotDot(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("../outside.txt")));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
    }

    @Test
    void shouldRejectAbsolutePath(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, root.resolve("file.txt").toAbsolutePath()));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
    }

    @Test
    void shouldRejectHiddenFile(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of(".hidden")));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("hidden component not allowed"));
    }

    @Test
    void shouldRejectHiddenInNestedPath(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("dir/.hidden/file.txt")));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
    }

    @Test
    void shouldAcceptDotInMiddleOfName(@TempDir Path root) throws Exception {
        Path resolved = PathSafetyGuard.resolveSafe(root, Path.of("file.name.txt"));
        assertEquals(root.resolve("file.name.txt").toAbsolutePath().normalize(), resolved);
    }

    @Test
    void shouldRejectSymlinkInPath(@TempDir Path root) throws Exception {
        Path target = root.resolve("real");
        Files.createDirectory(target);
        Path link = root.resolve("link");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException e) {
            // Skip on systems without symlink permission
            return;
        }

        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.resolveSafe(root, Path.of("link/file.txt")));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("symlink not allowed"));
    }

    @Test
    void shouldReturnRootWhenCandidateIsNull(@TempDir Path root) throws Exception {
        Path result = PathSafetyGuard.requireInsideRoot(root, null);
        assertEquals(root.toAbsolutePath().normalize(), result);
    }

    @Test
    void shouldAcceptCandidateInsideRoot(@TempDir Path root) throws Exception {
        Path subdir = root.resolve("subdir");
        Files.createDirectory(subdir);
        Path result = PathSafetyGuard.requireInsideRoot(root, subdir);
        assertEquals(subdir.toAbsolutePath().normalize(), result);
    }

    @Test
    void shouldRejectCandidateOutsideRoot(@TempDir Path root) {
        SandboxException ex = assertThrows(SandboxException.class,
                () -> PathSafetyGuard.requireInsideRoot(root, Path.of("/tmp/outside")));
        assertEquals(ToolErrorCategory.PATH_VIOLATION, ex.getCategory());
        assertTrue(ex.getMessage().contains("working directory outside workspace root"));
    }

    @Test
    void deleteRecursivelyShouldHandleNull() {
        assertDoesNotThrow(() -> PathSafetyGuard.deleteRecursively(null));
    }

    @Test
    void deleteRecursivelyShouldHandleNonExistentPath(@TempDir Path root) {
        Path nonExistent = root.resolve("does-not-exist");
        assertDoesNotThrow(() -> PathSafetyGuard.deleteRecursively(nonExistent));
    }

    @Test
    void deleteRecursivelyShouldDeleteDirectory(@TempDir Path root) throws Exception {
        Path dir = root.resolve("to-delete");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file.txt"), "content");

        PathSafetyGuard.deleteRecursively(dir);

        assertFalse(Files.exists(dir));
    }

    @Test
    void deleteRecursivelyShouldDeleteNestedStructure(@TempDir Path root) throws Exception {
        Path dir = root.resolve("nested");
        Files.createDirectories(dir.resolve("a/b/c"));
        Files.writeString(dir.resolve("a/file1.txt"), "1");
        Files.writeString(dir.resolve("a/b/file2.txt"), "2");

        PathSafetyGuard.deleteRecursively(dir);

        assertFalse(Files.exists(dir));
    }
}
