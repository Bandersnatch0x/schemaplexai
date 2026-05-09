CREATE TABLE IF NOT EXISTS sf_notification (
    id BIGINT DEFAULT NEXTVAL('seq_notification') PRIMARY KEY,
    tenant_id VARCHAR(64),
    user_id BIGINT,
    title VARCHAR(255),
    content TEXT,
    type VARCHAR(64),
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sf_tenant_environment_config (
    id BIGINT DEFAULT NEXTVAL('seq_tenant_env') PRIMARY KEY,
    tenant_id VARCHAR(64),
    environment VARCHAR(64),
    allowed_tools TEXT,
    security_level VARCHAR(64),
    allow_http_calls BOOLEAN,
    allow_file_read BOOLEAN,
    allow_irreversible_ops BOOLEAN,
    max_concurrent_tool_calls INT,
    extra_config TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);

CREATE SEQUENCE IF NOT EXISTS seq_notification START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS seq_tenant_env START WITH 1 INCREMENT BY 1;
