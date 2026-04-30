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
