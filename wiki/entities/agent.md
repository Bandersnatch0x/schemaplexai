---
title: Agent Domain Entities
type: model
source: docker/postgres/init/02-init-schema-agent.sql
creation_date: 2026-04-30
update_date: 2026-04-30
tags: [entity, agent, llm, execution]
confidence: high
---

# Agent Domain Entities

> One-sentence summary: Agent definitions, configurations, executions, chat messages, and memory — the core AI orchestration domain.

## sf_agent
- name, type (SOLO/TEAM), status, description

## sf_agent_config
- agent_id, max_rounds (default 20), max_tools (default 10)
- max_input_tokens (default 32000), max_output_tokens (default 4096)
- system_prompt, model_id, temperature (default 0.7)
- execution_mode (AUTO/PLAN/SUGGEST)
- config_json (flexible extension)

## sf_agent_shadow_config
- Self-improvement feedback loop config
- feedback_actions_json, enabled (default false)

## sf_agent_tool_binding
- agent_id, tool_name, tool_type (BUILTIN/SKILL/MCP/API)
- config_json

## sf_agent_execution
- agent_id, conversation_id, state (INITIALIZING/RUNNING/PAUSED/CANCELLED/COMPLETED/FAILED)
- token_budget_json, pause_reason, paused_at, cancel_reason, cancelled_at, completed_at, failure_reason

## sf_agent_execution_log
- execution_id, state, message, details_json

## sf_agent_execution_snapshot
- execution_id, snapshot_json (for resume/pause)

## sf_chat_message
- conversation_id, turn_index, role (SYSTEM/USER/ASSISTANT/TOOL)
- content, tool_calls (JSONB), token_count
- **HASH partitioned by conversation_id into 16 partitions**

## sf_agent_memory
- agent_id, memory_type (SHORT_TERM/LONG_TERM), content, source_execution_id

## Backlinks

- Agent execution engine: [[services/agent-execution-engine]]
- Execution controller: [[controllers/agent-execution-controller]]
- See [[data-model]] for full schema
