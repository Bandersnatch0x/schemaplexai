-- SchemaPlexAI Database Initialization Script
-- Run order: 01-init-schema.sql

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- Core Governance Domain
-- ============================================

CREATE TABLE sf_tenant (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(64) NOT NULL UNIQUE,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    config_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_user (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    username        VARCHAR(64) NOT NULL,
    password        VARCHAR(256) NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(32),
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id, username)
);

CREATE TABLE sf_role (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    code            VARCHAR(64) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id, code)
);

CREATE TABLE sf_permission (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    code            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL, -- MENU / BUTTON / API
    parent_id       BIGINT,
    path            VARCHAR(256),
    sort_order      INT DEFAULT 0,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_user_role (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, user_id, role_id)
);

CREATE TABLE sf_role_permission (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, role_id, permission_id)
);

CREATE TABLE sf_model_provider (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(64) NOT NULL,
    code            VARCHAR(64) NOT NULL UNIQUE,
    api_base_url    VARCHAR(256),
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    rate_limit      INT DEFAULT 100,
    config_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_ai_model (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(64) NOT NULL,
    provider_id     BIGINT NOT NULL,
    model_code      VARCHAR(128) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    config_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_config (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    config_key      VARCHAR(128) NOT NULL,
    config_value    TEXT,
    description     VARCHAR(256),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id, config_key)
);

-- ============================================
-- Spec Domain
-- ============================================

CREATE TABLE sf_spec (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    title           VARCHAR(256) NOT NULL,
    type            VARCHAR(32) NOT NULL, -- REQUIREMENT / DESIGN / TASK / STEERING
    status          VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    content         TEXT,
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_spec_document (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    spec_id         BIGINT NOT NULL,
    title           VARCHAR(256) NOT NULL,
    content         TEXT,
    doc_type        VARCHAR(32) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_spec_version (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    spec_id         BIGINT NOT NULL,
    version         INT NOT NULL,
    content         TEXT,
    change_log      VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_spec_template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(64),
    content         TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_spec_steering (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    spec_id         BIGINT NOT NULL,
    direction       TEXT,
    constraints     TEXT,
    acceptance_criteria TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_spec_review (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    spec_id         BIGINT NOT NULL,
    reviewer_id     BIGINT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_user_tenant ON sf_user(tenant_id);
CREATE INDEX idx_role_tenant ON sf_role(tenant_id);
CREATE INDEX idx_spec_tenant ON sf_spec(tenant_id);
CREATE INDEX idx_model_tenant ON sf_ai_model(tenant_id);
