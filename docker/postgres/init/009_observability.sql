CREATE TABLE IF NOT EXISTS sf_observability_trace (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(64),
    trace_id VARCHAR(64) NOT NULL,
    name VARCHAR(255),
    user_id VARCHAR(64),
    session_id VARCHAR(64),
    input TEXT,
    output TEXT,
    metadata TEXT,
    tags VARCHAR(512),
    version VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE INDEX idx_obs_trace_trace_id ON sf_observability_trace(trace_id);
CREATE INDEX idx_obs_trace_session_id ON sf_observability_trace(session_id);

CREATE TABLE IF NOT EXISTS sf_observability_span (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(64),
    span_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    parent_span_id VARCHAR(64),
    name VARCHAR(255),
    type VARCHAR(32),
    start_time BIGINT,
    end_time BIGINT,
    input TEXT,
    output TEXT,
    metadata TEXT,
    status VARCHAR(32),
    model VARCHAR(64),
    model_parameters TEXT,
    usage_details TEXT,
    cost_details TEXT,
    prompt_name VARCHAR(128),
    prompt_version VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE INDEX idx_obs_span_trace_id ON sf_observability_span(trace_id);
CREATE INDEX idx_obs_span_parent ON sf_observability_span(parent_span_id);
