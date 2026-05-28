-- V2026_05_09_03__add_budget_config_table.sql
-- M7.8: Add sf_budget_config table (ops module)

CREATE TABLE IF NOT EXISTS sf_budget_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(64) NOT NULL,
    monthly_limit   DECIMAL(18,4) NOT NULL,
    currency        VARCHAR(8) NOT NULL DEFAULT 'USD',
    alert_threshold DECIMAL(5,2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id)
);
