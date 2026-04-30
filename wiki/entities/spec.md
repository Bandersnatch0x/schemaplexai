---
title: Spec Domain Entities
type: model
source: docker/postgres/init/01-init-schema.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, spec, document, version]
confidence: high
---

# Spec Domain Entities

> One-sentence summary: Specification document management with versioning, steering, and review — covering requirements, designs, tasks, and steering docs.

## sf_spec
- title, type (REQUIREMENT/DESIGN/TASK/STEERING), status (DRAFT), content, version

## sf_spec_document
- spec_id, title, content, doc_type

## sf_spec_version
- spec_id, version, content, change_log

## sf_spec_template
- name, category, content, status

## sf_spec_steering
- spec_id, direction, constraints, acceptance_criteria

## sf_spec_review
- spec_id, reviewer_id, status (PENDING), comment

## Backlinks

- Spec service: see `schemaplexai-spec` module
- See [[data-model]] for full schema
