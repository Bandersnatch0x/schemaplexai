---
title: Integration Domain Entities
type: model
source: docker/postgres/init/03-init-schema-others.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, integration, mcp, skill, github]
confidence: high
---

# Integration Domain Entities

> One-sentence summary: External integrations (GitHub/GitLab/Jenkins), MCP servers, reusable skills, and API gateway configurations.

## sf_integration
- name, type (GITHUB/GITLAB/JENKINS/MCP/SKILL/API), config_json, status

## sf_skill
- name, code, description, content, status

## sf_mcp_server
- name, endpoint, transport (HTTP), status

## Backlinks

- Integration service: `schemaplexai-integration` module
- See [[data-model]] for full schema
