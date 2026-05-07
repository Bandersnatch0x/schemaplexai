---
topic: skill-markdown-spec
stage: standard
version: v1.0
status: approved
---

# Skill Markdown Specification v1.0

A Skill is a structured capability module defined as a Markdown file with YAML frontmatter.

## Format

```markdown
---
name: skill-name
version: 1.0
description: One-line summary of what this skill does
tags: [tag1, tag2]
---

# skill-name

## Description
Detailed explanation of what the skill does.

## Parameters
- `param1` (type, required/optional): Description

## Steps
1. Step one
2. Step two

## Output
What the skill produces.
```

## Frontmatter Fields

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| name | yes | string | Unique skill identifier (kebab-case) |
| version | yes | semver | Skill version |
| description | yes | string | One-line summary |
| tags | no | string[] | Categorization tags |

## File Naming

`SKILL.md` at the root of the skill directory.

## Progressive Loading

Skills are loaded only when the current task requires them. The parser extracts frontmatter metadata first (cheap), then the full body is loaded on demand.
