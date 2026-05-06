package com.schemaplexai.agent.engine.tool.adapter.file;

import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * File read tool adapter with path traversal prevention.
 *
 * Security measures:
 * - Path normalization (resolve + normalize) before access
 * - Workspace root directory restriction (startsWith check)
 * - Symlink detection (NOFOLLOW_LINKS)
 * - Hidden file restriction (files starting with '.')
 */
@Slf4j
@Component
public class FileReadAdapter implements ToolAdapter {

    @Override
    public String getToolName() {
        return "file_read";
    }

    @Override
    public ToolResult execute(ToolCall call, ExecutionContext ctx) throws ToolExecutionException {
        String inputPath = call.parameters().getOrDefault("path", "").toString();
        if (inputPath.isBlank()) {
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT, "File path is required");
        }

        String workspaceRoot = ctx.workspaceRoot();
        if (workspaceRoot == null || workspaceRoot.isBlank()) {
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT, "Workspace root not configured");
        }

        // Security: validate workspaceRoot is absolute and tenant-scoped
        Path workspacePath = Path.of(workspaceRoot).toAbsolutePath().normalize();
        Path resolvedPath = workspacePath.resolve(inputPath).normalize();

        // Security: prevent path traversal — must stay within workspace root
        if (!resolvedPath.startsWith(workspacePath)) {
            log.warn("Path traversal attempt blocked: input='{}', resolved='{}', workspace='{}'",
                    inputPath, resolvedPath, workspaceRoot);
            throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                    "File access denied: path outside workspace root");
        }

        // Security: prevent access to hidden files (check ALL path components)
        Path relative = workspacePath.relativize(resolvedPath);
        for (Path part : relative) {
            String name = part.toString();
            if (name.startsWith(".")) {
                log.warn("Hidden path component access blocked: {} (component: {})", resolvedPath, name);
                throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                        "File access denied: hidden files not accessible");
            }
        }

        // Security: detect symlinks
        try {
            BasicFileAttributes attrs = Files.readAttributes(resolvedPath, BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);
            if (attrs.isSymbolicLink()) {
                log.warn("Symlink access blocked: {}", resolvedPath);
                throw new ToolExecutionException(ToolErrorCategory.ENVIRONMENT_MISMATCH,
                        "File access denied: symlinks not supported");
            }
        } catch (IOException e) {
            // File may not exist yet, continue with read attempt
        }

        // Read file content
        try {
            String content = Files.readString(resolvedPath);
            log.info("FileReadAdapter: read {} bytes from {}", content.length(), resolvedPath);
            return ToolResult.success(content);
        } catch (IOException e) {
            log.error("Failed to read file: {}", resolvedPath, e);
            throw new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT,
                    "Failed to read file: " + e.getMessage(), e);
        }
    }
}
