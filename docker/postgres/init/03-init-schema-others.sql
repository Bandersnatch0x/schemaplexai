-- SchemaPlexAI Other Domains Initialization

-- ============================================
-- Workflow Domain
-- ============================================

CREATE TABLE sf_workflow_template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    node_config_json TEXT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_workflow_instance (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    template_id     BIGINT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'RUNNING',
    trigger_type    VARCHAR(32) NOT NULL, -- MANUAL / SCHEDULED / EVENT
    trigger_config  TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_workflow_node_execution (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    instance_id     BIGINT NOT NULL,
    node_id         VARCHAR(64) NOT NULL,
    node_type       VARCHAR(32) NOT NULL, -- TRIGGER / DOCUMENT / AGENT / APPROVAL / QUALITY / NOTIFICATION / ARTIFACT
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input_json      TEXT,
    output_json     TEXT,
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- ============================================
-- Context Domain
-- ============================================

CREATE TABLE sf_workspace (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    parent_id       BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_context (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    workspace_id    BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL, -- GLOBAL / WORKSPACE / BRANCH
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_context_item (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    context_id      BIGINT NOT NULL,
    item_key        VARCHAR(128) NOT NULL,
    item_value      TEXT,
    item_type       VARCHAR(32) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_context_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    context_id      BIGINT NOT NULL,
    snapshot_json   TEXT NOT NULL,
    version         INT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_knowledge_doc (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    title           VARCHAR(256) NOT NULL,
    file_name       VARCHAR(256),
    file_url        VARCHAR(512),
    file_size       BIGINT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    doc_type        VARCHAR(32),
    sync_status     VARCHAR(32) DEFAULT 'PENDING', -- PENDING / SYNCED / FAILED
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_knowledge_doc_version (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    doc_id          BIGINT NOT NULL,
    version         INT NOT NULL,
    file_url        VARCHAR(512),
    change_log      VARCHAR(512),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

-- ============================================
-- Quality & Security Domain
-- ============================================

CREATE TABLE sf_quality_gate (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    rules_json      TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_quality_issue (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    execution_id    BIGINT,
    issue_type      VARCHAR(32) NOT NULL, -- HALLUCINATION / TOOL_MISUSE / SPEC_DEVIATION
    severity        VARCHAR(32) NOT NULL, -- LOW / MEDIUM / HIGH / CRITICAL
    description     TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_security_policy (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    policy_type     VARCHAR(32) NOT NULL,
    rules_json      TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_audit_event (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    resource_type   VARCHAR(64) NOT NULL,
    resource_id     VARCHAR(64),
    action          VARCHAR(64) NOT NULL,
    details_json    TEXT,
    user_id         BIGINT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_review_record (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    resource_type   VARCHAR(32) NOT NULL,
    resource_id     BIGINT NOT NULL,
    reviewer_id     BIGINT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_tool_approval_amendment (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    tool_name       VARCHAR(128) NOT NULL,
    approval_threshold INT NOT NULL DEFAULT 5,
    current_count   INT NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- ============================================
-- Integration Domain
-- ============================================

CREATE TABLE sf_integration (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32) NOT NULL, -- GITHUB / GITLAB / JENKINS / MCP / SKILL / API
    config_json     TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_skill (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    code            VARCHAR(128) NOT NULL,
    description     TEXT,
    content         TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_mcp_server (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    endpoint        VARCHAR(512) NOT NULL,
    transport       VARCHAR(32) NOT NULL DEFAULT 'HTTP',
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- ============================================
-- Ops Domain
-- ============================================

CREATE TABLE sf_artifact (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    version         VARCHAR(64),
    file_url        VARCHAR(512),
    artifact_type   VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_delivery_record (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    artifact_id     BIGINT NOT NULL,
    delivery_type   VARCHAR(32) NOT NULL,
    recipient       VARCHAR(256),
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_notification (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    type            VARCHAR(32) NOT NULL, -- IN_APP / EMAIL / IM
    title           VARCHAR(256) NOT NULL,
    content         TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_budget (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    budget_type     VARCHAR(32) NOT NULL, -- MONTHLY / PROJECT
    limit_amount    DECIMAL(18,4) NOT NULL,
    used_amount     DECIMAL(18,4) NOT NULL DEFAULT 0,
    alert_threshold DECIMAL(5,2) DEFAULT 80.00,
    currency        VARCHAR(8) DEFAULT 'USD',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_eval_dataset (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    data_json       TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE TABLE sf_eval_task (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    dataset_id      BIGINT NOT NULL,
    agent_id        BIGINT NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    result_json     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INT NOT NULL DEFAULT 0
);

-- ============================================
-- ShedLock
-- ============================================

CREATE TABLE IF NOT EXISTS shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- ============================================
-- Sync & Idempotency Domain
-- ============================================

CREATE TABLE sf_sync_cursor (
    id              BIGSERIAL PRIMARY KEY,
    sync_name       VARCHAR(128) NOT NULL UNIQUE,
    source_table    VARCHAR(128) NOT NULL,
    target_table    VARCHAR(128) NOT NULL,
    last_sync_id    BIGINT NOT NULL DEFAULT 0,
    last_sync_time  TIMESTAMP NOT NULL DEFAULT '1970-01-01',
    sync_batch_size INT NOT NULL DEFAULT 1000,
    sync_interval_sec INT NOT NULL DEFAULT 60,
    failed_count    INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sf_sync_batch_log (
    id              BIGSERIAL PRIMARY KEY,
    sync_name       VARCHAR(128) NOT NULL,
    batch_id        VARCHAR(64) NOT NULL UNIQUE,
    start_id        BIGINT NOT NULL,
    end_id          BIGINT NOT NULL,
    record_count    INT NOT NULL,
    status          VARCHAR(32) NOT NULL,
    error_msg       TEXT,
    started_at      TIMESTAMP NOT NULL,
    completed_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sf_idempotency_key (
    id              BIGSERIAL PRIMARY KEY,
    message_id      VARCHAR(128) NOT NULL,
    consumer_group  VARCHAR(128) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    consumed_at     TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, consumer_group)
);

-- Indexes
CREATE TABLE sf_message_fail_log (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    message_id      VARCHAR(128),
    exchange        VARCHAR(128),
    routing_key     VARCHAR(128),
    payload         TEXT,
    error_msg       TEXT,
    consumer_group  VARCHAR(128),
    status          VARCHAR(32) DEFAULT 'PENDING',
    retry_count     INT DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_workflow_instance_template ON sf_workflow_instance(template_id);
CREATE INDEX idx_node_execution_instance ON sf_workflow_node_execution(instance_id);
CREATE INDEX idx_knowledge_doc_tenant ON sf_knowledge_doc(tenant_id);
CREATE INDEX idx_quality_issue_execution ON sf_quality_issue(execution_id);
CREATE INDEX idx_audit_event_tenant ON sf_audit_event(tenant_id, created_at);
CREATE INDEX idx_notification_user ON sf_notification(user_id, status);
CREATE INDEX idx_message_fail_log_status ON sf_message_fail_log(status, created_at);
