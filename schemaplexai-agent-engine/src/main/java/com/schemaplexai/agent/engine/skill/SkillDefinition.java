package com.schemaplexai.agent.engine.skill;

/**
 * Parsed skill definition extracted from Markdown with YAML frontmatter.
 *
 * @param name         skill identifier (max 64 chars)
 * @param description  human-readable summary (max 500 chars, nullable)
 * @param instructions markdown body with skill instructions
 */
public record SkillDefinition(
    String name,
    String description,
    String instructions
) {}
