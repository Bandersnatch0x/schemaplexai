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
