---
title: AI Model Entities
type: model
source: docker/postgres/init/01-init-schema.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, ai-model, llm, provider]
confidence: high
---

# AI Model Entities

> One-sentence summary: Model provider registry and tenant-scoped AI model configurations for LLM orchestration via LangChain4j.

## sf_model_provider
- name, code, api_base_url, status, rate_limit (default 100), config_json
- **Global table** — no tenant_id

## sf_ai_model
- tenant_id, name, provider_id, model_code, status, config_json

## Backlinks

- System controllers: [[controllers/system-controllers]]
- See [[data-model]] for full schema
