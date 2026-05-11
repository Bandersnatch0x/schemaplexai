-- V2026_05_10__add_tenant_skill_unlock.sql
-- Tracks per-tenant skill tier unlock state for progressive disclosure
-- Matches BaseEntity contract: id, tenant_id, tier, unlocked_at, unlocked_by, reason,
-- created_at, updated_at, created_by, updated_by, deleted

CREATE TABLE sf_tenant_skill_unlock (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    tier         SMALLINT NOT NULL,  -- 1=TIER_1, 2=TIER_2, 3=TIER_3
    unlocked_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    unlocked_by  VARCHAR(32) NOT NULL DEFAULT 'AUTO',  -- 'AUTO' or 'ADMIN'
    reason       VARCHAR(256),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by   BIGINT,
    updated_by   BIGINT,
    deleted      SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, tier)
);

CREATE INDEX idx_tenant_skill_unlock_tenant ON sf_tenant_skill_unlock(tenant_id);
