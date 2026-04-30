---
title: Quality Domain Entities
type: model
source: docker/postgres/init/03-init-schema-others.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, quality, security, audit]
confidence: high
---

# Quality Domain Entities

> One-sentence summary: Quality gating, issue tracking, security policies, audit events, and tool approval workflow for AI agent governance.

## sf_quality_gate
- name, rules_json, status

## sf_quality_issue
- execution_id, issue_type (HALLUCINATION/TOOL_MISUSE/SPEC_DEVIATION)
- severity (LOW/MEDIUM/HIGH/CRITICAL), description, status (OPEN)

## sf_security_policy
- name, policy_type, rules_json, status

## sf_audit_event
- event_type, resource_type, resource_id, action, details_json, user_id, ip_address

## sf_review_record
- resource_type, resource_id, reviewer_id, status (PENDING), comment

## sf_tool_approval_amendment
- tool_name, approval_threshold (default 5), current_count, status (PENDING)

## Backlinks

- Quality service: `schemaplexai-quality` module
- See [[data-model]] for full schema
