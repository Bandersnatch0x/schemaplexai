---
title: Ops Domain Entities
type: model
source: docker/postgres/init/03-init-schema-others.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, ops, artifact, budget, evaluation]
confidence: high
---

# Ops Domain Entities

> One-sentence summary: Artifact management, delivery records, notifications, budget tracking, and evaluation datasets for cost analytics and delivery operations.

## sf_artifact
- name, version, file_url, artifact_type, status

## sf_delivery_record
- artifact_id, delivery_type, recipient, status (PENDING)

## sf_notification
- user_id, type (IN_APP/EMAIL/IM), title, content, status (UNREAD)

## sf_budget
- budget_type (MONTHLY/PROJECT), limit_amount, used_amount, alert_threshold (default 80%), currency (USD)

## sf_eval_dataset
- name, description, data_json

## sf_eval_task
- dataset_id, agent_id, status (PENDING), result_json

## Backlinks

- Ops service: `schemaplexai-ops` module
- See [[data-model]] for full schema
