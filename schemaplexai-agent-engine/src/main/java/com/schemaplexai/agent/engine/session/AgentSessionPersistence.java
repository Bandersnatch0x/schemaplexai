package com.schemaplexai.agent.engine.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 负责将 {@link SfAgentExecution} 及其关联的 {@link SandboxSession} 工作空间做快照持久化与恢复。
 *
 * <p>典型使用场景：
 * <ol>
 *   <li>执行中断（pause / checkpoint）时调用 {@link #persistSession} 保存 workspace + metadata</li>
 *   <li>恢复执行（resume）时调用 {@link #restoreSession} 将 workspace 还原到沙箱</li>
 * </ol>
 *
 * <p>快照目录结构：
 * <pre>
 *   {snapshot-root}/{executionId}/
 *     workspace/   ← 沙箱工作区完整副本
 *     metadata.json← execution.getMetadata() 的 JSON 序列化
 * </pre>
 */
@Slf4j
@Component
public class AgentSessionPersistence {

    @Value("${agent.session.snapshot-root:/tmp/splx-snapshots}")
    private Path snapshotRoot;

    /**
     * 设置快照根目录（供测试使用）。
     */
    void setSnapshotRoot(Path snapshotRoot) {
        this.snapshotRoot = snapshotRoot;
    }

    /**
     * 将 execution 的 metadata 与 sandbox 的 workspace 持久化到快照目录。
     */
    public void persistSession(SfAgentExecution execution, SandboxSession sandbox) {
        Path snapshotDir = snapshotRoot.resolve(execution.getId().toString());
        try {
            Files.createDirectories(snapshotDir);
            copyDirectory(sandbox.workspaceRoot(), snapshotDir.resolve("workspace"));
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(snapshotDir.resolve("metadata.json").toFile(), execution.getMetadata());
            log.info("Persisted session for execution {} to {}", execution.getId(), snapshotDir);
        } catch (IOException e) {
            log.error("Failed to persist session for execution {}", execution.getId(), e);
        }
    }

    /**
     * 从快照目录恢复 workspace 到 sandbox。
     *
     * <p>若快照不存在则静默跳过并记录 warn。
     */
    public void restoreSession(SfAgentExecution execution, SandboxSession sandbox) {
        Path snapshotDir = snapshotRoot.resolve(execution.getId().toString());
        if (!Files.exists(snapshotDir)) {
            log.warn("No snapshot found for execution {}, skipping restore", execution.getId());
            return;
        }
        try {
            copyDirectory(snapshotDir.resolve("workspace"), sandbox.workspaceRoot());
            log.info("Restored session for execution {} from {}", execution.getId(), snapshotDir);
        } catch (IOException e) {
            log.error("Failed to restore snapshot for execution {}", execution.getId(), e);
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
