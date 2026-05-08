package com.schemaplexai.integration.dto;

import com.schemaplexai.integration.entity.SfSkill;

/**
 * On-demand response containing only the skill content body.
 *
 * <p>Fetched separately to avoid loading full content in list/browse operations,
 * reducing token costs and latency.
 *
 * @param id      the skill ID
 * @param code    the skill code
 * @param content the full skill content (markdown with frontmatter)
 */
public record SkillContent(
        Long id,
        String code,
        String content
) {

    /**
     * Create from a full {@link SfSkill} entity.
     */
    public static SkillContent from(SfSkill skill) {
        if (skill == null) {
            return null;
        }
        return new SkillContent(skill.getId(), skill.getCode(), skill.getContent());
    }
}
