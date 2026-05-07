package com.schemaplexai.agent.engine.skill;

import com.schemaplexai.agent.engine.entity.SfAgentRole;
import com.schemaplexai.agent.engine.entity.SfAgentSkill;
import com.schemaplexai.agent.engine.entity.SfAgentSkillVersion;
import com.schemaplexai.agent.engine.mapper.SfAgentRoleMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentSkillMapper;
import com.schemaplexai.agent.engine.mapper.SfAgentSkillVersionMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SfAgentSkill, SfAgentSkillVersion, and SfAgentRole entities.
 * Verifies entity construction, field access, and mapper interface compilation.
 *
 * Note: Full integration tests (insert/select via mapper) require Testcontainers + Docker.
 * These tests verify the domain model is correctly wired.
 */
class SfAgentSkillTest {

    @Test
    void shouldCreateSkillWithAllFields() {
        SfAgentSkill skill = new SfAgentSkill();
        skill.setTenantId("1");
        skill.setName("test-skill");
        skill.setDescription("A test skill");
        skill.setContent("# Instructions\nDo something.");
        skill.setVersion(1);
        skill.setStatus(1);

        assertEquals("test-skill", skill.getName());
        assertEquals("A test skill", skill.getDescription());
        assertEquals("# Instructions\nDo something.", skill.getContent());
        assertEquals(1, skill.getVersion());
        assertEquals(1, skill.getStatus());
        assertEquals("1", skill.getTenantId());
    }

    @Test
    void shouldCreateSkillVersionWithAllFields() {
        SfAgentSkillVersion version = new SfAgentSkillVersion();
        version.setTenantId("1");
        version.setSkillId(100L);
        version.setVersion(2);
        version.setContent("# Updated instructions");

        assertEquals(100L, version.getSkillId());
        assertEquals(2, version.getVersion());
        assertEquals("# Updated instructions", version.getContent());
    }

    @Test
    void shouldCreateRoleWithAllFields() {
        SfAgentRole role = new SfAgentRole();
        role.setTenantId("1");
        role.setName("reviewer");
        role.setDescription("Code review role");
        role.setOverlay("system: you are a code reviewer");
        role.setStatus(1);

        assertEquals("reviewer", role.getName());
        assertEquals("Code review role", role.getDescription());
        assertEquals("system: you are a code reviewer", role.getOverlay());
        assertEquals(1, role.getStatus());
    }

    @Test
    void skillShouldInheritBaseEntityFields() {
        SfAgentSkill skill = new SfAgentSkill();
        skill.setId(42L);
        skill.setCreatedAt(java.time.LocalDateTime.now());
        skill.setDeleted(0);

        assertEquals(42L, skill.getId());
        assertNotNull(skill.getCreatedAt());
        assertEquals(0, skill.getDeleted());
    }

    @Test
    void mapperInterfacesShouldCompile() {
        // Verify mapper interfaces exist and are properly typed.
        // This is a compile-time check — if these don't resolve, the test won't compile.
        Class<SfAgentSkillMapper> skillMapperClass = SfAgentSkillMapper.class;
        Class<SfAgentSkillVersionMapper> versionMapperClass = SfAgentSkillVersionMapper.class;
        Class<SfAgentRoleMapper> roleMapperClass = SfAgentRoleMapper.class;

        assertNotNull(skillMapperClass);
        assertNotNull(versionMapperClass);
        assertNotNull(roleMapperClass);
    }
}
