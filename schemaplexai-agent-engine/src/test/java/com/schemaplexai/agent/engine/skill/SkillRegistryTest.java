package com.schemaplexai.agent.engine.skill;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfAgentSkill;
import com.schemaplexai.agent.engine.mapper.SfAgentSkillMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillRegistryTest {

    @Mock
    private SfAgentSkillMapper skillMapper;

    private SkillRegistry registry;
    private static final String TENANT = "tenant-1";
    private static final String SKILL_NAME = "code-reviewer";

    @BeforeEach
    void setUp() {
        registry = new SkillRegistry(skillMapper);
    }

    @Test
    void shouldReturnNullWhenSkillNotFound() {
        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SkillDefinition result = registry.resolve(SKILL_NAME, TENANT);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnSkillDefinitionWhenFound() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Reviews code for quality");
        entity.setContent("You are a code reviewer...");
        entity.setStatus(1);
        entity.setTier(1);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        SkillDefinition result = registry.resolve(SKILL_NAME, TENANT);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(SKILL_NAME);
        assertThat(result.description()).isEqualTo("Reviews code for quality");
        assertThat(result.instructions()).isEqualTo("You are a code reviewer...");
    }

    @Test
    void shouldCacheResultAndNotHitDbTwice() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Reviews code");
        entity.setContent("Instructions...");
        entity.setStatus(1);
        entity.setTier(1);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        // First call hits DB
        registry.resolve(SKILL_NAME, TENANT);
        // Second call should use cache
        SkillDefinition result = registry.resolve(SKILL_NAME, TENANT);

        assertThat(result).isNotNull();
        // selectOne should have been called only once
        verify(skillMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void shouldInvalidateCache() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Reviews code");
        entity.setContent("Instructions...");
        entity.setStatus(1);
        entity.setTier(1);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        registry.resolve(SKILL_NAME, TENANT);
        registry.invalidate(SKILL_NAME, TENANT);
        registry.resolve(SKILL_NAME, TENANT);

        // Should hit DB twice (pre-invalidation + post-invalidation)
        verify(skillMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void resolve_populatesTier() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Desc");
        entity.setContent("Content");
        entity.setStatus(1);
        entity.setTier(2);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        SkillDefinition result = registry.resolve(SKILL_NAME, TENANT);

        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(2);
    }

    @Test
    void resolve_defaultsNullTierToOne() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Desc");
        entity.setContent("Content");
        entity.setStatus(1);
        entity.setTier(null);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        SkillDefinition result = registry.resolve(SKILL_NAME, TENANT);

        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(1);
    }

    @Test
    void resolveAvailable_filtersByTier() {
        SfAgentSkill basic = new SfAgentSkill();
        basic.setName("basic-skill");
        basic.setDescription("Basic");
        basic.setContent("Basic content");
        basic.setStatus(1);
        basic.setTier(1);

        SfAgentSkill advanced = new SfAgentSkill();
        advanced.setName("advanced-skill");
        advanced.setDescription("Advanced");
        advanced.setContent("Advanced content");
        advanced.setStatus(1);
        advanced.setTier(3);

        when(skillMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(basic));

        List<SkillDefinition> result = registry.resolveAvailable(TENANT, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("basic-skill");
    }

    @Test
    void resolveByTier_skillBelowMaxTier_returnsSkill() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Desc");
        entity.setContent("Content");
        entity.setStatus(1);
        entity.setTier(2);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        SkillDefinition result = registry.resolveByTier(SKILL_NAME, TENANT, 3);

        assertThat(result).isNotNull();
        assertThat(result.tier()).isEqualTo(2);
    }

    @Test
    void resolveByTier_skillAboveMaxTier_returnsNull() {
        SfAgentSkill entity = new SfAgentSkill();
        entity.setName(SKILL_NAME);
        entity.setDescription("Desc");
        entity.setContent("Content");
        entity.setStatus(1);
        entity.setTier(3);

        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        SkillDefinition result = registry.resolveByTier(SKILL_NAME, TENANT, 2);

        assertThat(result).isNull();
    }

    @Test
    void resolveByTier_skillNotFound_returnsNull() {
        when(skillMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SkillDefinition result = registry.resolveByTier("missing-skill", TENANT, 2);

        assertThat(result).isNull();
    }
}
