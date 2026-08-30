-- SchemaPlexAI Agent Domain Initialization

-- ============================================
-- Agent Domain
-- ============================================

CREATE TABLE sf_agent (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL, -- SOLO / TEAM
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_agent_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    max_rounds      INT NOT NULL DEFAULT 20,
    max_tools       INT NOT NULL DEFAULT 10,
    max_input_tokens BIGINT NOT NULL DEFAULT 32000,
    max_output_tokens BIGINT NOT NULL DEFAULT 4096,
    system_prompt   TEXT,
    model_id        BIGINT,
    temperature     DECIMAL(3,2) DEFAULT 0.7,
    execution_mode  VARCHAR(32) NOT NULL DEFAULT 'AUTO', -- AUTO / PLAN / SUGGEST
    config_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_agent_shadow_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    feedback_actions_json TEXT,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_agent_tool_binding (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    tool_name       VARCHAR(128) NOT NULL,
    tool_type       VARCHAR(32) NOT NULL, -- BUILTIN / SKILL / MCP / API
    config_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_agent_execution (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    state           VARCHAR(32) NOT NULL DEFAULT 'INITIALIZING',
    token_budget_json TEXT,
    pause_reason    VARCHAR(32),
    paused_at       TIMESTAMP,
    cancel_reason   VARCHAR(256),
    cancelled_at    TIMESTAMP,
    completed_at    TIMESTAMP,
    failure_reason  VARCHAR(256),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_agent_execution_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    execution_id    BIGINT NOT NULL,
    state           VARCHAR(32) NOT NULL,
    message         TEXT,
    details_json    TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_agent_execution_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    execution_id    BIGINT NOT NULL,
    snapshot_json   TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- Chat Message with partitioning by hash(conversation_id)
CREATE TABLE sf_chat_message (
    id              BIGSERIAL,
    tenant_id       BIGINT NOT NULL,
    conversation_id VARCHAR(64) NOT NULL,
    turn_index      INT NOT NULL,
    role            VARCHAR(32) NOT NULL, -- SYSTEM / USER / ASSISTANT / TOOL
    content         TEXT,
    tool_calls      JSONB,
    token_count     INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, conversation_id)
) PARTITION BY HASH (conversation_id);

CREATE TABLE sf_chat_message_p0 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 0);
CREATE TABLE sf_chat_message_p1 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 1);
CREATE TABLE sf_chat_message_p2 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 2);
CREATE TABLE sf_chat_message_p3 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 3);
CREATE TABLE sf_chat_message_p4 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 4);
CREATE TABLE sf_chat_message_p5 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 5);
CREATE TABLE sf_chat_message_p6 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 6);
CREATE TABLE sf_chat_message_p7 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 7);
CREATE TABLE sf_chat_message_p8 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 8);
CREATE TABLE sf_chat_message_p9 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 9);
CREATE TABLE sf_chat_message_p10 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 10);
CREATE TABLE sf_chat_message_p11 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 11);
CREATE TABLE sf_chat_message_p12 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 12);
CREATE TABLE sf_chat_message_p13 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 13);
CREATE TABLE sf_chat_message_p14 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 14);
CREATE TABLE sf_chat_message_p15 PARTITION OF sf_chat_message FOR VALUES WITH (MODULUS 16, REMAINDER 15);

CREATE INDEX idx_chat_msg_conversation ON sf_chat_message(conversation_id, turn_index);
CREATE INDEX idx_chat_msg_tenant ON sf_chat_message(tenant_id, created_at);

-- Archive table for cold data
CREATE TABLE sf_chat_message_archive (
    LIKE sf_chat_message INCLUDING ALL,
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sf_agent_memory (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    memory_type     VARCHAR(32) NOT NULL, -- SHORT_TERM / LONG_TERM
    content         TEXT,
    source_execution_id BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- Indexes
CREATE TABLE sf_prompt_version (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    config_id       BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    version         INT NOT NULL,
    content         TEXT NOT NULL,
    label           VARCHAR(64),
    change_note     VARCHAR(256),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_agent_tenant ON sf_agent(tenant_id);
CREATE INDEX idx_agent_config_agent ON sf_agent_config(agent_id);
CREATE INDEX idx_execution_agent ON sf_agent_execution(agent_id);
CREATE INDEX idx_execution_conversation ON sf_agent_execution(conversation_id);
CREATE INDEX idx_execution_state ON sf_agent_execution(state);
CREATE INDEX idx_execution_log_execution ON sf_agent_execution_log(execution_id);
CREATE INDEX idx_memory_agent ON sf_agent_memory(agent_id);
CREATE INDEX idx_prompt_version_config ON sf_prompt_version(config_id, version);

-- ============================================
-- Consolidated from Flyway migrations (browser verification round 2):
-- fresh deployments get these objects from init, Flyway baselines past them.
-- ============================================

-- V2026_05_01__add_skill_role_tables.sql
-- Creates tables for agent skills and roles with multi-tenant isolation

CREATE TABLE sf_agent_skill (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    name         VARCHAR(64) NOT NULL,
    description  VARCHAR(500),
    content      TEXT NOT NULL,
    version      INTEGER NOT NULL DEFAULT 1,
    status       SMALLINT NOT NULL DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, name)
);

CREATE TABLE sf_agent_skill_version (
    id           BIGSERIAL PRIMARY KEY,
    skill_id     BIGINT NOT NULL REFERENCES sf_agent_skill(id),
    tenant_id    BIGINT NOT NULL,
    version      INTEGER NOT NULL,
    content      TEXT NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (skill_id, version)
);

CREATE TABLE sf_agent_role (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    name         VARCHAR(64) NOT NULL,
    description  VARCHAR(500),
    overlay      TEXT NOT NULL,
    status       SMALLINT NOT NULL DEFAULT 1,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, name)
);

CREATE INDEX idx_skill_tenant_name ON sf_agent_skill(tenant_id, name);
CREATE INDEX idx_skill_version_skill ON sf_agent_skill_version(skill_id, version);
CREATE INDEX idx_role_tenant_name ON sf_agent_role(tenant_id, name);

-- V2026_05_02__add_execution_skill_role_columns.sql
-- Adds skill_name and role_name columns to sf_agent_execution for skill/role tracking

ALTER TABLE sf_agent_execution ADD COLUMN skill_name VARCHAR(64);
ALTER TABLE sf_agent_execution ADD COLUMN role_name VARCHAR(64);

-- V2026_05_03__extend_mcp_server.sql
-- Extends sf_mcp_server with command/args/envVars for stdio transport,
-- serverPublicKey for signature verification, protocolVersion, and toolWhitelist.

ALTER TABLE sf_mcp_server ADD COLUMN command VARCHAR(255);
ALTER TABLE sf_mcp_server ADD COLUMN args JSONB;
ALTER TABLE sf_mcp_server ADD COLUMN env_vars JSONB;
ALTER TABLE sf_mcp_server ADD COLUMN server_public_key TEXT;
ALTER TABLE sf_mcp_server ADD COLUMN protocol_version VARCHAR(32);
ALTER TABLE sf_mcp_server ADD COLUMN tool_whitelist JSONB;

-- V2026_05_08_01__add_skill_tier.sql
-- Adds tier column to sf_agent_skill for progressive skill disclosure

ALTER TABLE sf_agent_skill ADD COLUMN tier SMALLINT NOT NULL DEFAULT 1;

ALTER TABLE sf_agent_execution_snapshot ADD COLUMN snapshot_hash VARCHAR(64);
ALTER TABLE sf_agent_execution_snapshot ADD COLUMN hash_chain VARCHAR(64);
-- Phase 0: Execution Control Plane baseline tables
-- Owner: schemaplexai-agent-engine (Flyway)

-- 1. Extend sf_agent_execution with optimistic locking and event sequencing
ALTER TABLE sf_agent_execution
    ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_event_seq INT NOT NULL DEFAULT 0;

-- 2. Execution event stream (append-only, immutable)
CREATE TABLE IF NOT EXISTS sf_execution_event (
    event_id      UUID PRIMARY KEY,
    execution_id  BIGINT NOT NULL,
    seq           INT NOT NULL,
    event_type    VARCHAR(32) NOT NULL,
    payload       JSONB,
    occurred_at   TIMESTAMPTZ NOT NULL,
    tenant_id     BIGINT NOT NULL,
    agent_id      BIGINT,
    sensitivity   VARCHAR(16),
    UNIQUE(execution_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_exec_event_execution_id ON sf_execution_event(execution_id);
CREATE INDEX IF NOT EXISTS idx_exec_event_tenant_id ON sf_execution_event(tenant_id);

-- 3. Outbox for reliable event publishing (Engine writes, background job publishes)
CREATE TABLE IF NOT EXISTS sf_execution_outbox (
    id            BIGSERIAL PRIMARY KEY,
    event_id      UUID NOT NULL UNIQUE,
    execution_id  BIGINT NOT NULL,
    seq           INT NOT NULL,
    topic         VARCHAR(64) NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    published_at  TIMESTAMPTZ,
    retry_count   INT DEFAULT 0,
    CHECK (retry_count <= 5)
);

CREATE INDEX IF NOT EXISTS idx_outbox_published_at ON sf_execution_outbox(published_at) WHERE published_at IS NULL;

-- 4. Inbox / processed event deduplication (composite PK: each consumer independent)
CREATE TABLE IF NOT EXISTS sf_processed_event (
    event_id      UUID NOT NULL,
    consumer_name VARCHAR(64) NOT NULL,
    processed_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, consumer_name)
);

-- 5. Approval ticket (unified truth for all approvals)
CREATE TABLE IF NOT EXISTS sf_approval_ticket (
    id                BIGSERIAL PRIMARY KEY,
    ticket_id         UUID NOT NULL UNIQUE,
    execution_id      BIGINT NOT NULL,
    tenant_id         BIGINT NOT NULL,
    agent_id          BIGINT,
    approval_request_id UUID NOT NULL UNIQUE,
    stage             VARCHAR(32) NOT NULL,   -- PENDING_FAST, PENDING_BPMN, APPROVED, REJECTED
    handler           VARCHAR(16) NOT NULL,   -- FAST, BPMN
    risk_level        VARCHAR(16),
    action_description TEXT,
    triggering_seq    INT NOT NULL,
    deferred          BOOLEAN NOT NULL DEFAULT FALSE,
    decided_at        TIMESTAMPTZ,
    decision_version  INT NOT NULL DEFAULT 0,
    expected_execution_version INT,
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_approval_ticket_execution ON sf_approval_ticket(execution_id);
CREATE INDEX IF NOT EXISTS idx_approval_ticket_tenant ON sf_approval_ticket(tenant_id);
CREATE INDEX IF NOT EXISTS idx_approval_ticket_stage ON sf_approval_ticket(stage);

-- V2026_05_09_02__add_audit_cost_projection_tables.sql
-- M4.4 + M4.5: Audit Trail and Cost Projection tables

-- 1. Extend sf_audit_event for projection integrity
ALTER TABLE sf_audit_event
    ADD COLUMN IF NOT EXISTS execution_id BIGINT,
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT,
    ADD COLUMN IF NOT EXISTS event_id UUID,
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS corrupted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_audit_event_execution ON sf_audit_event(execution_id);
CREATE INDEX IF NOT EXISTS idx_audit_event_occurred ON sf_audit_event(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_event_event_id ON sf_audit_event(event_id);

-- 2. Cost record table (PG short-path for v1)
CREATE TABLE IF NOT EXISTS sf_cost_record (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL,
    record_id         VARCHAR(64),
    service_name      VARCHAR(64),
    model_name        VARCHAR(64),
    provider          VARCHAR(32),
    request_type      VARCHAR(32),
    input_tokens      BIGINT,
    output_tokens     BIGINT,
    total_tokens      BIGINT,
    cost_amount       NUMERIC(18, 8),
    currency          VARCHAR(8),
    occurred_at       TIMESTAMPTZ,
    execution_id      BIGINT,
    agent_id          BIGINT,
    workflow_instance_id BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_cost_record_tenant ON sf_cost_record(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cost_record_execution ON sf_cost_record(execution_id);
CREATE INDEX IF NOT EXISTS idx_cost_record_occurred ON sf_cost_record(occurred_at);

ALTER TABLE sf_agent_execution ADD COLUMN tool_calls_today INT DEFAULT 0;
