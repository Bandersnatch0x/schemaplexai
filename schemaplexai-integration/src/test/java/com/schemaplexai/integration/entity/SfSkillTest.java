package com.schemaplexai.integration.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SfSkillTest {

    @Test
    void gettersAndSetters() {
        SfSkill skill = new SfSkill();
        skill.setId(1L);
        skill.setTenantId("t1");
        skill.setName("My Skill");
        skill.setCode("my-skill");
        skill.setDescription("A useful skill");
        skill.setContent("---\nname: My Skill\n---\nBody");
        skill.setStatus(1);
        skill.setDeleted(0);

        assertThat(skill.getId()).isEqualTo(1L);
        assertThat(skill.getTenantId()).isEqualTo("t1");
        assertThat(skill.getName()).isEqualTo("My Skill");
        assertThat(skill.getCode()).isEqualTo("my-skill");
        assertThat(skill.getDescription()).isEqualTo("A useful skill");
        assertThat(skill.getContent()).isEqualTo("---\nname: My Skill\n---\nBody");
        assertThat(skill.getStatus()).isEqualTo(1);
        assertThat(skill.getDeleted()).isEqualTo(0);
    }
}
