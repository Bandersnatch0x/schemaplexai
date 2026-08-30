-- Issue 926: test schema for sf_notification (matches the entity mapping,
-- read BOOLEAN flavor as in docker/postgres/init/04-notification.sql).
-- Used only by NotificationPaginationTenantIntegrationTest via @Sql.
CREATE TABLE IF NOT EXISTS sf_notification (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(64),
    user_id BIGINT,
    title VARCHAR(255),
    content TEXT,
    type VARCHAR(64),
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted INT DEFAULT 0
);
