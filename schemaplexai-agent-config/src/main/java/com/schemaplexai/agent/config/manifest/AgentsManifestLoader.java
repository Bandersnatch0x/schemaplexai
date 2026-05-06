package com.schemaplexai.agent.config.manifest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.mapper.SfAgentConfigMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.agent.config.service.AgentConfigService;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.manifest.AgentsManifest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
    private final AgentConfigService agentConfigService;

    /**
     * 从 manifest 加载 agent 配置到数据库。
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

    private SfAgent upsertAgent(AgentsManifest manifest) {
        SfAgent existing = agentMapper.selectOne(
                new LambdaQueryWrapper<SfAgent>()
                        .eq(SfAgent::getName, manifest.name())
        );
        if (existing == null) {
            SfAgent fresh = new SfAgent();
            fresh.setName(manifest.name());
            fresh.setType(manifest.type() != null ? manifest.type() : DEFAULT_AGENT_TYPE);
            fresh.setStatus(DEFAULT_STATUS);
            fresh.setDescription(manifest.description());
            agentConfigService.createAgent(fresh);
            return fresh;
        }
        existing.setType(manifest.type() != null ? manifest.type() : DEFAULT_AGENT_TYPE);
        existing.setDescription(manifest.description());
        agentConfigService.updateAgent(existing);
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
        agentConfigService.saveAgentConfig(cfg);
    }

    private void replaceToolBindings(Long agentId, AgentsManifest manifest) {
        List<SfAgentToolBinding> bindings = new ArrayList<>(manifest.tools().size());
        for (AgentsManifest.ToolBinding tool : manifest.tools()) {
            SfAgentToolBinding binding = new SfAgentToolBinding();
            binding.setAgentId(agentId);
            binding.setToolName(tool.name());
            binding.setToolType(tool.type() != null ? tool.type() : DEFAULT_TOOL_TYPE);
            binding.setConfigJson(tool.configJson());
            bindings.add(binding);
        }
        agentConfigService.saveToolBindings(agentId, bindings);
    }
}
