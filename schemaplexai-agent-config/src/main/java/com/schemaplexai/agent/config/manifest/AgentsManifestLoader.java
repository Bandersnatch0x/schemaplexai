package com.schemaplexai.agent.config.manifest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.mapper.SfAgentConfigMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.agent.config.mapper.SfAgentToolBindingMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.manifest.AgentsManifest;
import com.schemaplexai.common.manifest.AgentsManifestParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AGENTS.md → 数据库的装载器。
 *
 * <p>对接 OpenAI Agents SDK 2026 的 AGENTS.md 协议（见 design.md §B）。
 * 单向 idempotent upsert：依据 (name, tenantId) 唯一确定一个 agent；
 * 同 tenant 下多次加载同一 manifest 应得到相同 agentId 与 config，工具绑定整体覆盖。
 *
 * <p>事务边界：整个 upsert 必须在同一事务内完成，避免脏写或半写状态。
 *
 * <h3>字段映射</h3>
 * <pre>
 * AgentsManifest          → SfAgent                  · SfAgentConfig
 * ─────────────────────────────────────────────────────────────────
 * name                    → name
 * description             → description
 * type (default "general")→ type
 * (always)                → status = "active"
 * modelId                 →                            modelId
 * maxRounds               →                            maxRounds (Long)
 * maxTools                →                            maxTools (Long)
 * maxInputTokens          →                            maxInputTokens (Long)
 * maxOutputTokens         →                            maxOutputTokens (Long)
 * temperature             →                            temperature
 * executionMode           →                            executionMode
 * systemPrompt            →                            systemPrompt
 * tools[]                 → SfAgentToolBinding[]（先删后插）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentsManifestLoader {

    private static final String DEFAULT_AGENT_TYPE = "general";
    private static final String DEFAULT_STATUS = "active";
    private static final String DEFAULT_TOOL_TYPE = "builtin";

    private final SfAgentMapper agentMapper;
    private final SfAgentConfigMapper agentConfigMapper;
    private final SfAgentToolBindingMapper toolBindingMapper;

    /**
     * 从仓库根目录发现并加载所有 AGENTS.md。
     *
     * <p>发现路径（按优先级）：
     * <ol>
     *   <li>{@code repoRoot/AGENTS.md}</li>
     *   <li>{@code repoRoot/.agents/*.md}</li>
     *   <li>{@code repoRoot/agents/&#42;&#42;/*.md}</li>
     * </ol>
     *
     * @param repoRoot 仓库根目录
     * @param tenantId 目标租户（不可为 null/blank）
     * @return 加载报告（不会抛异常；单文件失败记录到 {@link LoadResult#error}）
     */
    @Transactional(rollbackFor = Exception.class)
    public LoadReport load(Path repoRoot, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank");
        }
        if (repoRoot == null || !Files.isDirectory(repoRoot)) {
            return new LoadReport(List.of());
        }

        String previousTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            List<Path> files = discoverManifestFiles(repoRoot);
            List<LoadResult> results = new ArrayList<>(files.size());
            for (Path file : files) {
                results.add(loadOne(file));
            }
            return new LoadReport(results);
        } finally {
            if (previousTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(previousTenant);
            }
        }
    }

    /**
     * 从单个已解析的 manifest 加载 agent 配置到数据库。
     *
     * @param manifest 已解析的 manifest（不可为 null）
     * @param tenantId 目标租户（不可为 null/blank）
     * @return 创建或更新后的 agent ID
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public Long loadFromManifest(AgentsManifest manifest, String tenantId) {
        if (manifest == null) {
            throw new IllegalArgumentException("manifest must not be null");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be null or blank");
        }

        String previousTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            SfAgent agent = upsertAgent(manifest);
            upsertConfig(agent.getId(), manifest);
            replaceToolBindings(agent.getId(), manifest);
            log.info("Loaded AGENTS.md manifest '{}' for tenant '{}' -> agentId={}",
                    manifest.name(), tenantId, agent.getId());
            return agent.getId();
        } finally {
            if (previousTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(previousTenant);
            }
        }
    }

    // --- discovery ---

    private List<Path> discoverManifestFiles(Path repoRoot) {
        List<Path> files = new ArrayList<>();

        // (a) repoRoot/AGENTS.md
        Path rootManifest = repoRoot.resolve("AGENTS.md");
        if (Files.isRegularFile(rootManifest)) {
            files.add(rootManifest);
        }

        // (b) repoRoot/.agents/*.md
        Path dotAgentsDir = repoRoot.resolve(".agents");
        if (Files.isDirectory(dotAgentsDir)) {
            try (Stream<Path> stream = Files.list(dotAgentsDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .forEach(files::add);
            } catch (IOException e) {
                log.warn("Failed to list {}: {}", dotAgentsDir, e.getMessage());
            }
        }

        // (c) repoRoot/agents/**/*.md
        Path agentsDir = repoRoot.resolve("agents");
        if (Files.isDirectory(agentsDir)) {
            try (Stream<Path> stream = Files.walk(agentsDir, FileVisitOption.FOLLOW_LINKS)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .forEach(files::add);
            } catch (IOException e) {
                log.warn("Failed to walk {}: {}", agentsDir, e.getMessage());
            }
        }

        return files.stream().distinct().collect(Collectors.toList());
    }

    private LoadResult loadOne(Path file) {
        try {
            String content = Files.readString(file);
            AgentsManifestParser parser = new AgentsManifestParser();
            AgentsManifest manifest = parser.parse(content);
            Long agentId = loadFromManifest(manifest, TenantContextHolder.getTenantId());
            return LoadResult.ok(file, agentId, manifest.name());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Failed to load manifest {}: {}", file, msg);
            return LoadResult.failed(file, msg);
        }
    }

    // --- upsert ---

    private SfAgent upsertAgent(AgentsManifest manifest) {
        SfAgent existing = agentMapper.findByNameAndTenant(manifest.name(), TenantContextHolder.getTenantId());
        if (existing == null) {
            SfAgent fresh = new SfAgent();
            fresh.setName(manifest.name());
            fresh.setType(manifest.type() != null ? manifest.type() : DEFAULT_AGENT_TYPE);
            fresh.setStatus(DEFAULT_STATUS);
            fresh.setDescription(manifest.description());
            agentMapper.insert(fresh);
            return fresh;
        }
        existing.setType(manifest.type() != null ? manifest.type() : DEFAULT_AGENT_TYPE);
        existing.setDescription(manifest.description());
        agentMapper.updateById(existing);
        return existing;
    }

    private void upsertConfig(Long agentId, AgentsManifest manifest) {
        SfAgentConfig existing = agentConfigMapper.selectOne(
                new LambdaQueryWrapper<SfAgentConfig>()
                        .eq(SfAgentConfig::getAgentId, agentId)
        );
        SfAgentConfig cfg = existing != null ? existing : new SfAgentConfig();
        cfg.setAgentId(agentId);
        cfg.setMaxRounds(manifest.maxRounds());
        cfg.setMaxTools(manifest.maxTools());
        cfg.setMaxInputTokens(manifest.maxInputTokens());
        cfg.setMaxOutputTokens(manifest.maxOutputTokens());
        cfg.setSystemPrompt(manifest.systemPrompt());
        cfg.setModelId(manifest.modelId());
        cfg.setTemperature(manifest.temperature());
        cfg.setExecutionMode(manifest.executionMode());
        if (cfg.getId() == null) {
            agentConfigMapper.insert(cfg);
        } else {
            agentConfigMapper.updateById(cfg);
        }
    }

    private void replaceToolBindings(Long agentId, AgentsManifest manifest) {
        toolBindingMapper.delete(
                new LambdaQueryWrapper<SfAgentToolBinding>()
                        .eq(SfAgentToolBinding::getAgentId, agentId)
        );
        List<SfAgentToolBinding> bindings = new ArrayList<>(manifest.tools().size());
        for (AgentsManifest.ToolBinding tool : manifest.tools()) {
            SfAgentToolBinding binding = new SfAgentToolBinding();
            binding.setAgentId(agentId);
            binding.setToolName(tool.name());
            binding.setToolType(tool.type() != null ? tool.type() : DEFAULT_TOOL_TYPE);
            binding.setConfigJson(tool.configJson());
            bindings.add(binding);
        }
        for (SfAgentToolBinding binding : bindings) {
            toolBindingMapper.insert(binding);
        }
    }
}
