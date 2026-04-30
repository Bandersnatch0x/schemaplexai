---
title: Workflow Domain Entities
type: model
source: docker/postgres/init/03-init-schema-others.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, workflow, bpmn, flowable]
confidence: high
---

# Workflow Domain Entities

> One-sentence summary: Flowable BPMN workflow templates, instances, and node executions supporting TRIGGER/DOCUMENT/AGENT/APPROVAL/QUALITY/NOTIFICATION/ARTIFACT node types.

## sf_workflow_template
- name, description, node_config_json, status

## sf_workflow_instance
- template_id, status (RUNNING), trigger_type (MANUAL/SCHEDULED/EVENT), trigger_config, started_at, completed_at

## sf_workflow_node_execution
- instance_id, node_id, node_type (TRIGGER/DOCUMENT/AGENT/APPROVAL/QUALITY/NOTIFICATION/ARTIFACT)
- status (PENDING/RUNNING/COMPLETED/FAILED), input_json, output_json, started_at, completed_at

## Backlinks

- Workflow node engine: [[services/workflow-node-engine]]
- See [[data-model]] for full schema
