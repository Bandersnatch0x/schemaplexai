-- Notification Module
-- Run order: 04-notification.sql

CREATE TABLE sf_notification (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    title           VARCHAR(256) NOT NULL,
    content         TEXT,
    type            VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted         INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_tenant_user ON sf_notification(tenant_id, user_id);
CREATE INDEX idx_notification_tenant_user_read ON sf_notification(tenant_id, user_id, read);
CREATE INDEX idx_notification_created_at ON sf_notification(created_at);
