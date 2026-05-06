package com.schemaplexai.agent.engine.tool.sandbox.util;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * 沙箱内路径安全工具：复用 {@link com.schemaplexai.agent.engine.tool.adapter.file.FileReadAdapter}
 * 的 4 道防御。
 *
 * <ol>
 *   <li>拒绝绝对路径输入（必须是相对路径）</li>
 *   <li>normalize + 必须 startsWith(root)（traversal 防御）</li>
 *   <li>任何路径组件以 {@code .} 开头则拒绝（hidden）</li>
 *   <li>任何已存在的父级组件不能是 symlink（NOFOLLOW_LINKS）</li>
 * </ol>
 */
public final class PathSafetyGuard {

    private PathSafetyGuard() {
    }

    /**
     * 校验并解析相对路径到 root 内的绝对路径。
     *
     * @throws SandboxException 任何一道防御失败时抛出（{@link ToolErrorCategory#PATH_VIOLATION}）
     */
    public static Path resolveSafe(Path root, Path relativePath) throws SandboxException {
        if (root == null) {
            throw new SandboxException("workspace root is null", ToolErrorCategory.SANDBOX_ERROR);
        }
        if (relativePath == null) {
            throw new SandboxException("relative path is null", ToolErrorCategory.PATH_VIOLATION);
        }
        if (relativePath.isAbsolute()) {
            throw new SandboxException(
                    "absolute path not allowed: " + relativePath,
                    ToolErrorCategory.PATH_VIOLATION);
        }

        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relativePath).normalize();

        // (1) traversal
        if (!resolved.startsWith(normalizedRoot)) {
            throw new SandboxException(
                    "path outside workspace root: " + relativePath,
                    ToolErrorCategory.PATH_VIOLATION);
        }

        // (2) hidden — every component
        Path relative = normalizedRoot.relativize(resolved);
        for (Path part : relative) {
            String name = part.toString();
            if (name.startsWith(".")) {
                throw new SandboxException(
                        "hidden component not allowed: " + name,
                        ToolErrorCategory.PATH_VIOLATION);
            }
        }

        // (3) symlink — walk every existing parent
        Path cursor = normalizedRoot;
        for (Path part : relative) {
            cursor = cursor.resolve(part);
            try {
                if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)
                        && Files.isSymbolicLink(cursor)) {
                    throw new SandboxException(
                            "symlink not allowed in path: " + cursor,
                            ToolErrorCategory.PATH_VIOLATION);
                }
            } catch (SecurityException e) {
                throw new SandboxException(
                        "filesystem access denied for: " + cursor,
                        e,
                        ToolErrorCategory.PATH_VIOLATION);
            }
        }

        return resolved;
    }

    /**
     * 校验给定的工作目录是否在 root 内（用于 {@link com.schemaplexai.agent.engine.tool.sandbox.ShellCommand}
     * 的 workingDir 参数）。
     */
    public static Path requireInsideRoot(Path root, Path candidate) throws SandboxException {
        if (candidate == null) {
            return root;
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            throw new SandboxException(
                    "working directory outside workspace root: " + candidate,
                    ToolErrorCategory.PATH_VIOLATION);
        }
        return normalizedCandidate;
    }

    /**
     * 自顶向下递归删除。即便其中某个文件被外部进程占用也尽量继续，最后用 IOException 包装失败信息。
     */
    public static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        Files.walkFileTree(root, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file,
                                                          java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                Files.deleteIfExists(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                Files.deleteIfExists(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }
}
