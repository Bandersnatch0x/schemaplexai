package com.schemaplexai.agent.engine.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.schemaplexai.agent.engine.entity.SfAgentSkill;
import com.schemaplexai.agent.engine.mapper.SfAgentSkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachingSkillRegistry implements SkillRegistry {

    private final SfAgentSkillMapper skillMapper;

    private final Cache<String, SkillDefinition> cache = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public SkillDefinition resolve(String skillName, String tenantId) {
        String key = tenantId + ":" + skillName + ":latest";
        return cache.get(key, k -> loadFromDb(skillName, tenantId));
    }

    @Override
    public SkillDefinition resolveVersion(String skillName, Integer version, String tenantId) {
        String key = tenantId + ":" + skillName + ":" + version;
        return cache.get(key, k -> loadVersionFromDb(skillName, version, tenantId));
    }

    private SkillDefinition loadFromDb(String skillName, String tenantId) {
        SfAgentSkill entity = skillMapper.selectOne(
                new LambdaQueryWrapper<SfAgentSkill>()
                        .eq(SfAgentSkill::getName, skillName)
                        .eq(SfAgentSkill::getTenantId, tenantId)
                        .eq(SfAgentSkill::getStatus, 1)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return new SkillDefinition(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getContent(), entity.getVersion(), entity.getStatus());
    }

    private SkillDefinition loadVersionFromDb(String skillName, Integer version, String tenantId) {
        SfAgentSkill entity = skillMapper.selectOne(
                new LambdaQueryWrapper<SfAgentSkill>()
                        .eq(SfAgentSkill::getName, skillName)
                        .eq(SfAgentSkill::getVersion, version)
                        .eq(SfAgentSkill::getTenantId, tenantId)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return new SkillDefinition(entity.getId(), entity.getName(), entity.getDescription(),
                entity.getContent(), entity.getVersion(), entity.getStatus());
    }
}
