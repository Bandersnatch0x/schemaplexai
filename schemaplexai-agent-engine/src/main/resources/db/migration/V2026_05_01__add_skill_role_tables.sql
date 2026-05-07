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
