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
