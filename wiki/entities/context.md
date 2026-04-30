---
title: Context Domain Entities
type: model
source: docker/postgres/init/03-init-schema-others.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, context, rag, knowledge, workspace]
confidence: high
---

# Context Domain Entities

> One-sentence summary: Workspace-organized context items with snapshots, plus knowledge document ingestion pipeline with MinIO/Tika integration.

## sf_workspace
- name, description, parent_id (hierarchical)

## sf_context
- workspace_id, name, type (GLOBAL/WORKSPACE/BRANCH)

## sf_context_item
- context_id, item_key, item_value, item_type

## sf_context_snapshot
- context_id, snapshot_json, version

## sf_knowledge_doc
- title, file_name, file_url, file_size, status, doc_type
- sync_status (PENDING/SYNCED/FAILED)

## sf_knowledge_doc_version
- doc_id, version, file_url, change_log

## Backlinks

- RAG service: [[services/rag-service]]
- See [[data-model]] for full schema
