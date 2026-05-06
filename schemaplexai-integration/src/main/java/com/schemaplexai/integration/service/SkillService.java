package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.integration.entity.SfSkill;

import java.util.List;

public interface SkillService extends IService<SfSkill> {

    /**
     * Create a new version of an existing skill.
     */
    SfSkill createVersion(Long skillId, String content, String changeNote);

    /**
     * List all versions of a skill by its code.
     */
    List<SfSkill> listVersions(String skillCode);

    /**
     * Validate skill syntax (frontmatter + body structure).
     */
    boolean validateSkill(String content);

    /**
     * Trigger skill execution via the tool execution service.
     */
    String executeSkill(Long skillId, java.util.Map<String, Object> parameters);
}
