package com.schemaplexai.integration.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.integration.dto.SkillContent;
import com.schemaplexai.integration.dto.SkillSummary;
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

    /**
     * Get all skills as lightweight summaries (without content).
     * Reduces token costs for list/browse operations.
     */
    List<SkillSummary> listSummaries();

    /**
     * Get a skill summary by ID (without content).
     *
     * @param id the skill ID
     * @return the summary, or null if not found
     */
    SkillSummary getSummaryById(Long id);

    /**
     * Load skill content on demand.
     *
     * @param id the skill ID
     * @return the content, or null if not found
     */
    SkillContent getContent(Long id);
}
