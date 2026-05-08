package com.schemaplexai.integration.dto;

import com.schemaplexai.integration.entity.SfSkill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillSummaryTest {

    @Test
    void from_null_returnsNull() {
        assertNull(SkillSummary.from(null));
    }

    @Test
    void from_skill_mapsFieldsWithoutContent() {
        SfSkill skill = new SfSkill();
        skill.setId(1L);
        skill.setName("Test Skill");
        skill.setCode("test-skill");
        skill.setDescription("A test skill");
        skill.setStatus(1);
        skill.setContent("---\nname: Test\n---\nBody content here");

        SkillSummary summary = SkillSummary.from(skill);

        assertNotNull(summary);
        assertEquals(1L, summary.id());
        assertEquals("Test Skill", summary.name());
        assertEquals("test-skill", summary.code());
        assertEquals("A test skill", summary.description());
        assertEquals(1, summary.status());
        // Content is NOT included in summary
    }

    @Test
    void from_skillWithNullFields_handlesGracefully() {
        SfSkill skill = new SfSkill();
        skill.setId(2L);

        SkillSummary summary = SkillSummary.from(skill);

        assertNotNull(summary);
        assertEquals(2L, summary.id());
        assertNull(summary.name());
        assertNull(summary.code());
        assertNull(summary.description());
        assertNull(summary.status());
    }
}
