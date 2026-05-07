package com.schemaplexai.agent.engine.skill;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentSkill;
import com.schemaplexai.agent.engine.mapper.SfAgentSkillMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
                entity.getContent()
        );
        cache.put(cacheKey, def);
        return def;
    }

    public void invalidate(String skillName, String tenantId) {
        cache.invalidate(tenantId + ":" + skillName);
    }
}
