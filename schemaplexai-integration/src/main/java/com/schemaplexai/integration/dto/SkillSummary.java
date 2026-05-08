package com.schemaplexai.integration.dto;

import com.schemaplexai.integration.entity.SfSkill;

/**
 * Lightweight representation of a skill that excludes the full content field.
 *
 * <p>Used for list operations and browsing to reduce token costs and latency.
 * The full content should only be fetched on demand via
 * {@code GET /integration/skills/{id}/content}.
 *
 * @param id          the skill ID
 * @param name        the skill name
 * @param code        the skill code
 * @param description the skill description
 * @param status      the skill status (1 = active, 0 = inactive)
 */
public record SkillSummary(
        Long id,
        String name,
        String code,
        String description,
        Integer status
) {

    /**
     * Convert a full {@link SfSkill} entity to a summary (strips content).
     */
    public static SkillSummary from(SfSkill skill) {
        if (skill == null) {
            return null;
        }
        return new SkillSummary(
                skill.getId(),
                skill.getName(),
                skill.getCode(),
                skill.getDescription(),
                skill.getStatus()
        );
    }
}
