package com.schemaplexai.agent.engine.skill;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentSkill;
import com.schemaplexai.agent.engine.mapper.SfAgentSkillMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SkillRegistry {

    private final SfAgentSkillMapper skillMapper;
    private final Cache<String, SkillDefinition> cache;

    public SkillRegistry(SfAgentSkillMapper skillMapper) {
        this.skillMapper = skillMapper;
        // Cache skill definitions for 30 minutes
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    private static Integer defaultTier(Integer tier) {
        return tier != null ? tier : 1;
    }

    /**
     * Resolve a skill name to a SkillDefinition.
     * Cache key: tenantId:skillName
     * Returns null if skill not found or inactive.
     */
    public SkillDefinition resolve(String skillName, String tenantId) {
        String cacheKey = tenantId + ":" + skillName;
        return Optional.ofNullable(cache.getIfPresent(cacheKey))
                .orElseGet(() -> loadAndCache(skillName, tenantId, cacheKey));
    }

    /**
     * Reload skill from DB and update cache.
     */
    public SkillDefinition reload(String skillName, String tenantId) {
        String cacheKey = tenantId + ":" + skillName;
        cache.invalidate(cacheKey);
        return loadAndCache(skillName, tenantId, cacheKey);
    }

    private SkillDefinition loadAndCache(String skillName, String tenantId, String cacheKey) {
        SfAgentSkill entity = skillMapper.selectOne(
                new LambdaQueryWrapper<SfAgentSkill>()
                        .eq(SfAgentSkill::getName, skillName)
                        .eq(SfAgentSkill::getTenantId, tenantId)
                        .eq(SfAgentSkill::getStatus, 1)
        );
        if (entity == null) {
            log.debug("Skill not found: {}", skillName);
            return null;
        }
        SkillDefinition def = new SkillDefinition(
                entity.getName(),
                entity.getDescription(),
                entity.getContent(),
                defaultTier(entity.getTier())
        );
        cache.put(cacheKey, def);
        return def;
    }

    /**
     * Resolve all active skills up to the given tier (inclusive).
     * Cache key: tenantId:available:maxTier
     */
    public List<SkillDefinition> resolveAvailable(String tenantId, int maxTier) {
        String cacheKey = tenantId + ":available:" + maxTier;
        // Note: Caffeine Cache<String, SkillDefinition> can't hold List; skip list caching.
        List<SfAgentSkill> entities = skillMapper.selectList(
                new LambdaQueryWrapper<SfAgentSkill>()
                        .eq(SfAgentSkill::getTenantId, tenantId)
                        .eq(SfAgentSkill::getStatus, 1)
                        .le(SfAgentSkill::getTier, maxTier)
        );
        List<SkillDefinition> defs = entities.stream()
                .map(e -> new SkillDefinition(
                        e.getName(),
                        e.getDescription(),
                        e.getContent(),
                        defaultTier(e.getTier())
                ))
                .collect(Collectors.toList());
        if (!defs.isEmpty()) {
            // Cache individual skills and the list result
            for (SkillDefinition def : defs) {
                cache.put(tenantId + ":" + def.name(), def);
            }
            // Caffeine Cache<String, SkillDefinition> can't hold List; we rely on individual skill caching.
        }
        return defs;
    }

    /**
     * Resolve a single skill if it exists and its tier <= maxTier.
     */
    public SkillDefinition resolveByTier(String skillName, String tenantId, int maxTier) {
        SkillDefinition def = resolve(skillName, tenantId);
        if (def == null) {
            return null;
        }
        if (def.tier() > maxTier) {
            log.warn("Skill '{}' tier {} exceeds maxTier {} for tenant {}", skillName, def.tier(), maxTier, tenantId);
            return null;
        }
        return def;
    }

    public void invalidate(String skillName, String tenantId) {
        cache.invalidate(tenantId + ":" + skillName);
    }
}
