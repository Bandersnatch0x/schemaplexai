package com.schemaplexai.integration.dto;

import com.schemaplexai.integration.entity.SfSkill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillContentTest {

    @Test
    void from_null_returnsNull() {
        assertNull(SkillContent.from(null));
    }

    @Test
    void from_skill_mapsIdCodeContent() {
        SfSkill skill = new SfSkill();
        skill.setId(1L);
        skill.setCode("test-skill");
        skill.setContent("---\nname: Test\n---\nBody content here");

        SkillContent content = SkillContent.from(skill);

        assertNotNull(content);
        assertEquals(1L, content.id());
        assertEquals("test-skill", content.code());
        assertEquals("---\nname: Test\n---\nBody content here", content.content());
    }

    @Test
    void from_skillWithNullContent_returnsNullContent() {
        SfSkill skill = new SfSkill();
        skill.setId(3L);
        skill.setCode("empty-skill");
        skill.setContent(null);

        SkillContent content = SkillContent.from(skill);

        assertNotNull(content);
        assertEquals(3L, content.id());
        assertEquals("empty-skill", content.code());
        assertNull(content.content());
    }
}
