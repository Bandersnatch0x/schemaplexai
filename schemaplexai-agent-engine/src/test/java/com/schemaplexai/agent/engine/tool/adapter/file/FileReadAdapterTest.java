package com.schemaplexai.agent.engine.tool.adapter.file;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FileReadAdapterTest {

    private final FileReadAdapter adapter = new FileReadAdapter();

    @Test
    void shouldReturnToolName() {
        assertEquals("file_read", adapter.getToolName());
    }

    @Test
    void shouldReadFileSuccessfully(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        ToolCall call = new ToolCall("file_read", Map.of("path", "test.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("hello world", result.output());
    }

    @Test
    void shouldRejectBlankPath() {
        ToolCall call = new ToolCall("file_read", Map.of("path", ""));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("File path is required"));
    }

    @Test
    void shouldRejectMissingPathParameter() {
        ToolCall call = new ToolCall("file_read", Map.of());
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
    }

    @Test
    void shouldRejectNullWorkspaceRoot() {
        ToolCall call = new ToolCall("file_read", Map.of("path", "test.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, null);

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Workspace root not configured"));
    }

    @Test
    void shouldRejectBlankWorkspaceRoot() {
        ToolCall call = new ToolCall("file_read", Map.of("path", "test.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "  ");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
    }

    @Test
    void shouldBlockPathTraversal(@TempDir Path tempDir) {
        ToolCall call = new ToolCall("file_read", Map.of("path", "../secret.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("outside workspace root"));
    }

    @Test
    void shouldBlockHiddenFile(@TempDir Path tempDir) {
        ToolCall call = new ToolCall("file_read", Map.of("path", ".secret"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("hidden files not accessible"));
    }

    @Test
    void shouldBlockHiddenComponentInDeepPath(@TempDir Path tempDir) {
        ToolCall call = new ToolCall("file_read", Map.of("path", "dir/.hidden/file.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("hidden"));
    }

    @Test
    void shouldBlockSymlink(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("real.txt");
        Files.writeString(target, "real");
        Path link = tempDir.resolve("link.txt");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            // Skip on systems without symlink permission
            return;
        }

        ToolCall call = new ToolCall("file_read", Map.of("path", "link.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("symlinks not supported"));
    }

    @Test
    void shouldFailForNonExistentFile(@TempDir Path tempDir) {
        ToolCall call = new ToolCall("file_read", Map.of("path", "nonexistent.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Failed to read file"));
    }

    @Test
    void shouldReadFileInSubdirectory(@TempDir Path tempDir) throws Exception {
        Path subdir = tempDir.resolve("subdir");
        Files.createDirectory(subdir);
        Path file = subdir.resolve("nested.txt");
        Files.writeString(file, "nested content");

        ToolCall call = new ToolCall("file_read", Map.of("path", "subdir/nested.txt"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, tempDir.toString());

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("nested content", result.output());
    }
}
